package com.example.waitingservice.application;

import reactor.core.publisher.Mono;

import java.util.function.Supplier;

public interface ReactiveCacheManager {
    <T> Mono<T> getOrSet(String key, Supplier<Mono<T>> fallback, Class<T> type, long ttlSeconds);

    Mono<Boolean> evict(String key);
}
