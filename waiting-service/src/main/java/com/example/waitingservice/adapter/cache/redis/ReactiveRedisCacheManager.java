package com.example.waitingservice.adapter.cache.redis;

import com.example.waitingservice.application.ReactiveCacheManager;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Component
public class ReactiveRedisCacheManager implements ReactiveCacheManager {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public ReactiveRedisCacheManager(ReactiveRedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> Mono<T> getOrSet(String key, Mono<T> fallback, Class<T> type, long ttlSeconds) {
        return redisTemplate.opsForValue()
                .get(key)
                .flatMap(value -> deserialize(value, type))
                .switchIfEmpty(
                        fallback.flatMap(data -> serialize(data)
                                .flatMap(serialized -> redisTemplate.opsForValue()
                                        .set(key, serialized)
                                        .then(redisTemplate.expire(key, java.time.Duration.ofSeconds(ttlSeconds)))
                                        .thenReturn(data)
                                )
                        )
                );
    }

    @Override
    public Mono<Boolean> evict(String key) {
        return redisTemplate.delete(key)
                .map(deletedCount -> deletedCount > 0);
    }

    private <T> Mono<T> deserialize(String value, Class<T> type) {
        try {
            return Mono.justOrEmpty(objectMapper.readValue(value, type));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    private <T> Mono<String> serialize(T data) {
        try {
            return Mono.justOrEmpty(objectMapper.writeValueAsString(data));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
