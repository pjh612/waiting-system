package com.example.waitingservice.domain.repository;

import com.example.waitingservice.domain.model.WaitingQueue;
import reactor.core.publisher.Mono;

public interface WaitingQueueRepository {
    Mono<WaitingQueue> save(WaitingQueue queue);

    Mono<WaitingQueue> findByApiKey(String apiKey);
}
