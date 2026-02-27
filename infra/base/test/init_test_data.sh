#!/bin/bash

# 설정값
NAMESPACE="infra-dev"
QUEUE_KEY="event:waiting:queue"
HEARTBEAT_KEY="event:heartbeat"
TTL=3600
COUNT=1  # 우선 테스트로 10건만
REDIS_SVC="redis-cluster"
LOCAL_LUA_PATH="../../../waiting-service/src/main/resources/redis/lua/register_waiting.lua"
REMOTE_LUA_PATH="/tmp/register_waiting.lua"

# 1. 대상 파드 하나 가져오기
REDIS_POD=redis-cluster-0
#
#if [ -z "$REDIS_POD" ]; then
#    echo "Redis 파드를 찾을 수 없습니다."
#    exit 1
#fi

# 2. 루아 파일을 파드 내부로 복사
echo "스크립트 복사 중: $LOCAL_LUA_PATH -> $REDIS_POD:$REMOTE_LUA_PATH"
kubectl cp $LOCAL_LUA_PATH $NAMESPACE/$REDIS_POD:$REMOTE_LUA_PATH

echo "데이터 세팅 시작..."

for i in $(seq 1 $COUNT)
do
  USER_ID="user_$i"
  SCORE=$(date +%s%3N)

  # 파드 내부의 경로($REMOTE_LUA_PATH)를 사용하므로 'Filename too long'이 발생하지 않음
  # Cluster 환경이므로 -c 옵션 추가
  kubectl exec -i pod/redis-cluster-0 -n $NAMESPACE -- redis-cli -c --eval $REMOTE_LUA_PATH $QUEUE_KEY $HEARTBEAT_KEY , $USER_ID $SCORE $TTL > /dev/null

  if [ $((i % 1000)) -eq 0 ]; then
    echo "$i 건 완료..."
  fi
done

# 3. 임시 파일 삭제 (선택 사항)
# kubectl exec -i $REDIS_POD -n $NAMESPACE -- rm $REMOTE_LUA_PATH

echo "세팅 완료!"