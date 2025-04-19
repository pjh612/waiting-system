package com.example.waitingservice.adapter.queue;

import com.example.waitingservice.application.QueueManager;
import com.example.waitingservice.domain.model.QueueUser;
import com.example.waitingservice.domain.model.UserPosition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ReactiveRedisQueueManager implements QueueManager {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String WAITING_QUEUE_KEY_TEMPLATE = "wait:queue:%s";
    private static final String PROCEED_QUEUE_KEY_TEMPLATE = "proceed:queue:%s";

    @Override
    public Mono<Long> registerWaiting(String queueName, String id, Instant timestamp) {
        String key = WAITING_QUEUE_KEY_TEMPLATE.formatted(queueName);

        return redisTemplate.opsForZSet()
                .remove(key, id)
                .then(redisTemplate.opsForZSet()
                        .add(key, id, timestamp.getEpochSecond())
                        .filter(i -> i)
                        .switchIfEmpty(Mono.error(new RuntimeException("Failed to register in queue")))
                )
                .flatMap(i -> getOrder(queueName, id));
    }

    @Override
    public Flux<QueueUser> allow(String queueName, Long count) {
        long timestamp = Instant.now().getEpochSecond();
        String waitingQueueKey = WAITING_QUEUE_KEY_TEMPLATE.formatted(queueName);
        String proceedQueueKey = PROCEED_QUEUE_KEY_TEMPLATE.formatted(queueName);

        return redisTemplate.opsForZSet()
                .popMin(waitingQueueKey, count)
                .flatMap(user -> addToProceedQueue(proceedQueueKey, user, timestamp)
                        .thenReturn(new QueueUser(user.getValue(), user.getScore()))
                );
    }

    private Mono<Boolean> addToProceedQueue(String proceedQueueKey, ZSetOperations.TypedTuple<String> user, long timestamp) {
        return redisTemplate.opsForZSet()
                .add(proceedQueueKey, user.getValue(), timestamp);
    }

    @Override
    public Mono<Boolean> isAllowed(String queueName, String id) {
        String proceedQueueKey = PROCEED_QUEUE_KEY_TEMPLATE.formatted(queueName);

        return redisTemplate.opsForZSet()
                .rank(proceedQueueKey, id)
                .defaultIfEmpty(-1L)
                .map(rank -> rank >= 0);
    }

    @Override
    public Mono<Long> getOrder(String queueName, String id) {
        String waitingQueueKey = WAITING_QUEUE_KEY_TEMPLATE.formatted(queueName);

        return redisTemplate.opsForZSet()
                .rank(waitingQueueKey, id)
                .defaultIfEmpty(-1L)
                .map(rank -> rank == -1 ? 1 : rank + 1);
    }

    @Override
    public Flux<UserPosition> getWaitingUsers(String queueName) {
        String waitingQueueKey = WAITING_QUEUE_KEY_TEMPLATE.formatted(queueName);

        return redisTemplate.opsForZSet()
                .range(waitingQueueKey, Range.unbounded())
                .index()
                .map(tuple -> new UserPosition(tuple.getT2(), tuple.getT1() + 1));
    }
}
