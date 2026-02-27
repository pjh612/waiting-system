#!/bin/bash

# 1. 환경 설정
NAMESPACE="infra-dev"
K6_POD="k6-load-tester"
REDIS_POD="redis-cluster-0"
KAFKA_POD="kafka-0"

#
TOPIC_NAME="test-queue"
GROUP="default-group"
TARGET_URL=${1:-"http://waiting-service.infra-dev.svc.cluster.local:8081"}
AUTH_TOKEN=${2:-'$2a$10$SxutPlQV50WljccM84GrKeJpEcOWKrw10dxMSR9FPMZSrPBfpZoTi'}

# Legacy
#TARGET_URL=${1:-"http://waiting-service-legacy.infra-dev.svc.cluster.local:8082"}
#AUTH_TOKEN=${2:-'$2a$10$laL0l4lZWLT.RjEsVTOyDORXdZnarDWBN9pDnOebke2pDECpdYhcK'}
#TOPIC_NAME="legacy"
#GROUP="legacy"

USE_LOCAL=false
for arg in "$@"; do
  if [ "$arg" == "--local" ]; then
    USE_LOCAL=true
    break
  fi
done

## 2. URL 결정 로직
#if [ "$USE_LOCAL" = true ]; then
#    # 로컬 실행 시 기본 URL
#    DEFAULT_URL="http://136.110.76.115:8081"
#    TARGET_URL=${1:-$DEFAULT_URL}
#    # 만약 첫 번째 인자가 --local 이라면 DEFAULT_URL 사용
#    [[ "$TARGET_URL" == "--local" ]] && TARGET_URL=$DEFAULT_URL
#else
#    # GKE 파드 실행 시 기본 URL (내부 서비스 도메인)
#    #TARGET_URL=${1:-"http://waiting-service.infra-dev.svc.cluster.local:8081"}
#fi

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

stop_k6() {
    echo -e "\n${RED}🛑 테스트 중단 요청 감지!${NC}"
    if [ "$USE_LOCAL" = true ]; then
        pkill k6
    else
        kubectl exec -n $NAMESPACE $K6_POD -- pkill k6
    fi
    exit
}
trap stop_k6 INT TERM

echo "------------------------------------------------"
echo "🌐 대상 URL: ${YELLOW}$TARGET_URL${NC}"
if [ "$USE_LOCAL" = true ]; then
    echo "🚀 k6 부하 테스트 준비 (Mode: ${GREEN}LOCAL${NC})"
else
    echo "🚀 k6 부하 테스트 준비 (Mode: ${CYAN}GKE Pod: $K6_POD${NC})"
fi
echo "------------------------------------------------"

# 2. 테스트 전 Kafka Offset 측정
echo "📊 테스트 전 Kafka Offset 측정 중..."
BEFORE_OFFSET=$(kubectl exec -n $NAMESPACE $KAFKA_POD -- /opt/kafka/bin/kafka-get-offsets.sh \
  --bootstrap-server localhost:9092 --topic $TOPIC_NAME --time -1 | awk -F ":" '{sum+=$3} END {print sum}' | tr -d '\r' | xargs)
BEFORE_OFFSET=${BEFORE_OFFSET:-0}
echo "📍 시작 Offset: $BEFORE_OFFSET"

# k6 스크립트 복사
kubectl cp k6-allow.js $K6_POD:/home/k6/allow.js -n $NAMESPACE

# 3. k6 실행 (Background 실행)
echo "🚀 k6 부하 테스트 시작..."
if [ "$USE_LOCAL" = true ]; then
    # 로컬 실행: summary.json을 생성하도록 스크립트 실행 환경 구성
    k6 run -e BASE_URL=$TARGET_URL -e AUTH_TOKEN="$AUTH_TOKEN" k6-allow.js --summary-export=summary.json &
else
    # 원격 실행
    kubectl cp k6-allow.js $K6_POD:/home/k6/allow.js -n $NAMESPACE
    kubectl exec -n $NAMESPACE $K6_POD -- k6 run \
        -e BASE_URL=$TARGET_URL -e AUTH_TOKEN="$AUTH_TOKEN" /home/k6/allow.js &
fi
K6_PID=$!

# 4. 실시간 Lag 모니터링 루프
#echo "📊 실시간 Kafka Lag 모니터링 중..."
#while kill -0 $K6_PID 2>/dev/null; do
#    KAFKA_LAG_CURRENT=$(kubectl exec -n $NAMESPACE $KAFKA_POD -- /opt/kafka/bin/kafka-consumer-groups.sh \
#        --bootstrap-server localhost:9092 \
#        --group ${GROUP} --describe | awk 'NR>1 {sum+=$6} END {print sum}' | tr -d '\r' | xargs)
#
#    LAG_VAL=${KAFKA_LAG_CURRENT:-0}
#    echo "[$(date +%H:%M:%S)] 📥 Current Kafka Lag: $LAG_VAL"
#    sleep 5
#done

wait $K6_PID

echo "${YELLOW}🏁 k6 프로세스 종료. 10초 대기 후 최종 데이터 검증을 시작합니다...${NC}"
sleep 10

# 5. 테스트 후 데이터 수집
echo "📥 종료 시점 Kafka Offset 확인 중..."
AFTER_OFFSET=$(kubectl exec -n $NAMESPACE $KAFKA_POD -- /opt/kafka/bin/kafka-get-offsets.sh \
  --bootstrap-server localhost:9092 --topic $TOPIC_NAME --time -1 | awk -F ":" '{sum+=$3} END {print sum}' | tr -d '\r' | xargs)
AFTER_OFFSET=${AFTER_OFFSET:-0}
ACTUAL_KAFKA_COUNT=$((AFTER_OFFSET - BEFORE_OFFSET))

echo "📥 Redis 대기열 잔량 확인 중..."
REDIS_COUNT=$(kubectl exec -n $NAMESPACE $REDIS_POD -- redis-cli -c zcard wait:queue:{$TOPIC_NAME} | tr -d '\r' | xargs)
REDIS_COUNT=${REDIS_COUNT:-0}

echo "📥 최종 Kafka Consumer Lag 확인 중..."
KAFKA_LAG_FINAL=$(kubectl exec -n $NAMESPACE $KAFKA_POD -- /opt/kafka/bin/kafka-consumer-groups.sh \
    --bootstrap-server localhost:9092 \
    --group ${GROUP} --describe | awk 'NR>1 {sum+=$6} END {print sum}' | tr -d '\r' | xargs)
KAFKA_LAG_FINAL=${KAFKA_LAG_FINAL:-0}

# 6. k6 summary 파일 파싱
if [ "$USE_LOCAL" = true ]; then
    SUMMARY_CONTENT=$(cat summary)
else
    SUMMARY_CONTENT=$(kubectl exec -n $NAMESPACE $K6_POD -- cat summary)
fi

TOTAL_REG=$(echo "$SUMMARY_CONTENT" | grep "total_registered_users" | awk -F': ' '{print $2}' | tr -d ', ' | xargs)
SSE_CONN=$(echo "$SUMMARY_CONTENT" | grep "sse_connections_total" | awk -F': ' '{print $2}' | tr -d ', ' | xargs)
NOTIFIED=$(echo "$SUMMARY_CONTENT" | grep "redirect_success" | awk -F': ' '{print $2}' | tr -d ', ' | xargs)
TOTAL_ENTERED=$(echo "$SUMMARY_CONTENT" | grep "entered_users_count" | awk -F': ' '{print $2}' | tr -d ', ' | xargs)
ALLOW_CALL_COUNT=$(echo "$SUMMARY_CONTENT" | grep "allow_call_count" | awk -F': ' '{print $2}' | tr -d ', ' | xargs)
EXPECTED_REDIS_REMAINING=$(echo "$SUMMARY_CONTENT" | grep "expected_redis_remaining" | awk -F': ' '{print $2}' | tr -d ', ' | xargs)

TOTAL_REG=${TOTAL_REG:-0}
SSE_CONN=${SSE_CONN:-0}
NOTIFIED=${NOTIFIED:-0}
TOTAL_ENTERED=${TOTAL_ENTERED:-0}
ALLOW_CALL_COUNT=${ALLOW_CALL_COUNT:-0}
EXPECTED_REDIS_REMAINING=${EXPECTED_REDIS_REMAINING:-0}

EXPECTED_MESSAGE_COUNT=$((TOTAL_ENTERED + ALLOW_CALL_COUNT))
LOSS_COUNT=$((TOTAL_ENTERED - NOTIFIED))

# 7. 최종 결과 리포트
echo ""
echo "${CYAN}┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓${NC}"
echo "${CYAN}┃                📊 대기열 시스템 통합 정합성 리포트           ┃${NC}"
echo "${CYAN}┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛${NC}"

echo ""
echo "${YELLOW}1. 사용자 흐름 지표 (k6 Metrics)${NC}"
echo " - 총 등록 성공 (POST)     : ${GREEN}${TOTAL_REG}${NC} 명"
echo " - SSE 연결 성공           : ${GREEN}${SSE_CONN}${NC} 명"
echo " - 관리자 승인 처리 (Allow) : ${GREEN}${TOTAL_ENTERED}${NC} 명"
echo " - 최종 입장 알림 수신     : ${CYAN}${NOTIFIED}${NC} 명"

echo ""
echo "${YELLOW}2. 인프라 정합성 분석 (Server-side)${NC}"

if [ "$REDIS_COUNT" -eq "$EXPECTED_REDIS_REMAINING" ]; then
    echo " 💠 Redis 대기열 잔량     : ${GREEN}${REDIS_COUNT}명 / (기대치: ${EXPECTED_REDIS_REMAINING}명) ✅${NC}"
else
    echo " 💠 Redis 대기열 잔량     : ${RED}${REDIS_COUNT}명 (기대치: ${EXPECTED_REDIS_REMAINING}명) ❌${NC}"
fi

if [ "$EXPECTED_MESSAGE_COUNT" -eq "$ACTUAL_KAFKA_COUNT" ]; then
    echo " 💠 Kafka 발행 정합성     : ${GREEN}${ACTUAL_KAFKA_COUNT}건 / (기대치: ${EXPECTED_MESSAGE_COUNT}건) ✅${NC}"
else
    echo " 💠 Kafka 발행 정합성     : ${RED}${ACTUAL_KAFKA_COUNT}건 (기대치: ${EXPECTED_MESSAGE_COUNT}건) ❌${NC}"
fi

echo " 💠 Kafka 최종 Lag        : ${KAFKA_LAG_FINAL} $([ "$KAFKA_LAG_FINAL" == "0" ] && echo "${GREEN}✅${NC}" || echo "${RED}⚠️${NC}")"
echo " 💠 입장 알림 유실(Loss)  : $([ "$LOSS_COUNT" -eq 0 ] && echo "${GREEN}${LOSS_COUNT}${NC}" || echo "${RED}${LOSS_COUNT}${NC}") 건"

echo ""
echo "${YELLOW}3. 최종 판정${NC}"
IS_REDIS_OK=false
IS_KAFKA_OK=false
[ "$REDIS_COUNT" -eq "$EXPECTED_REDIS_REMAINING" ] && IS_REDIS_OK=true
[ "$EXPECTED_MESSAGE_COUNT" -eq "$ACTUAL_KAFKA_COUNT" ] && IS_KAFKA_OK=true

if $IS_REDIS_OK && $IS_KAFKA_OK && [ "$LOSS_COUNT" -eq 0 ]; then
    echo "${GREEN}🎉 결과: [성공] 모든 데이터 정합성이 완벽하게 일치합니다!${NC}"
else
    echo "${RED}🚨 결과: [실패] 데이터 불일치가 발견되었습니다.${NC}"
    $IS_REDIS_OK || echo "   - Redis 잔량 불일치"
    $IS_KAFKA_OK || echo "   - Kafka 발행량 불일치"
    [ "$LOSS_COUNT" -eq 0 ] || echo "   - 클라이언트 알림 유실 발생"
fi
echo "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"