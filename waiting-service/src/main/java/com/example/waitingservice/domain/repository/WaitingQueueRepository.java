package com.example.waitingservice.domain.repository;

import com.example.waitingservice.domain.model.WaitingQueue;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface WaitingQueueRepository {
    Mono<WaitingQueue> save(WaitingQueue queue);

    Mono<WaitingQueue> findByApiKey(String apiKey);

    Mono<WaitingQueue> findByClientIdAndQueueName(UUID clientId, String queueName);
}
