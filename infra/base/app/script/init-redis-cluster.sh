#!/bin/bash

NAMESPACE="infra-dev"
SERVICE_NAME="redis-cluster"
POD_NAME="redis-cluster"
REPLICAS=6
PORT=6379

FQDNS=""

for ((i=0; i<$REPLICAS; i++)); do
    FQDN="$POD_NAME-$i.$SERVICE_NAME.$NAMESPACE.svc.cluster.local:$PORT "
    FQDNS="$FQDNS$FQDN"
done

echo "생성될 클러스터 노드 리스트:"
echo $FQDNS

kubectl exec -it $POD_NAME-0 -n $NAMESPACE -- redis-cli --cluster create $FQDNS --cluster-replicas 1 --cluster-yes


