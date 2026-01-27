for i in {0..5}; do
    echo "Cleaning redis-cluster-$i..."
    kubectl exec -it redis-cluster-$i -n infra-dev -- redis-cli flushall
    kubectl exec -it redis-cluster-$i -n infra-dev -- redis-cli cluster reset hard
done