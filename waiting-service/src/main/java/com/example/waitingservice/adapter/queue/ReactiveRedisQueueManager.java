package com.example.waitingservice.adapter.queue;

import com.example.waitingservice.application.QueueManager;
import com.example.waitingservice.domain.model.QueueUser;
import com.example.waitingservice.domain.model.UserPosition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReactiveRedisQueueManager implements QueueManager {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<Long> registerWaitingScript;

    private static final String WAITING_QUEUE_KEY_TEMPLATE = "wait:queue:{%s}";
    private static final String WAITING_HEARTBEAT_KEY_TEMPLATE = "wait:queue:{%s}:heartbeat";
    private static final Long WAITING_USER_EXPIRATION_SECONDS = Duration.ofDays(1).toSeconds();
    private static final String WAITING_USER_EXPIRATION_STR = String.valueOf(WAITING_USER_EXPIRATION_SECONDS);

    @Override
    public Mono<Long> registerWaiting(String queueName, String id, Instant timestamp) {
        String queueKey = WAITING_QUEUE_KEY_TEMPLATE.formatted(queueName);
        String heartbeatKey = WAITING_HEARTBEAT_KEY_TEMPLATE.formatted(queueName);

        return redisTemplate.execute(registerWaitingScript,
                        List.of(queueKey, heartbeatKey),
                        id,
                        String.valueOf(timestamp.toEpochMilli()),
                        WAITING_USER_EXPIRATION_STR)
                .next()
                .switchIfEmpty(Mono.error(new IllegalStateException("Queue register failed")));
    }

    @Override
    public Flux<QueueUser> allow(String queueName, Long count) {
        String queueKey = WAITING_QUEUE_KEY_TEMPLATE.formatted(queueName);
        String heartbeatKey = WAITING_HEARTBEAT_KEY_TEMPLATE.formatted(queueName);

        return redisTemplate.opsForZSet()
                .popMin(queueKey, count)
                .collectList()
                .flatMapMany(users -> {
                    if (users.isEmpty()) return Flux.empty();
                    Object[] userIds = users.stream()
                            .map(ZSetOperations.TypedTuple::getValue)
                            .toArray();
                    return redisTemplate.opsForHash().remove(heartbeatKey, userIds)
                            .thenMany(Flux.fromIterable(users))
                            .map(u -> new QueueUser(u.getValue(), u.getScore()));
                });
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

    @Override
    public Mono<Boolean> touch(String queueName, String id) {
        String heartbeatKey = WAITING_HEARTBEAT_KEY_TEMPLATE.formatted(queueName);
        String queueKey = WAITING_QUEUE_KEY_TEMPLATE.formatted(queueName);

        long now = System.currentTimeMillis();

        return redisTemplate.opsForHash()
                .put(heartbeatKey, id, String.valueOf(now))
                .flatMap(result ->
                        redisTemplate.expire(queueKey, Duration.ofMinutes(10))
                                .and(redisTemplate.expire(heartbeatKey, Duration.ofMinutes(10)))
                                .thenReturn(true)
                );
    }
}
