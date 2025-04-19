package com.example.waitingservice.application;

import reactor.core.publisher.Mono;

public interface ReactiveCacheManager {
    <T> Mono<T> getOrSet(String key, Mono<T> fallback, Class<T> type, long ttlSeconds);

    Mono<Boolean> evict(String key);
}
