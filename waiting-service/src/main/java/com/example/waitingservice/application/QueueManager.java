package com.example.waitingservice.application;

import com.example.waitingservice.domain.model.QueueUser;
import com.example.waitingservice.domain.model.UserPosition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface QueueManager {
    Mono<Long> registerWaiting(String queueName, String id, Instant timestamp);

    Flux<QueueUser> allow(String queueName, Long count);

    Mono<Boolean> isAllowed(String queueName, String id);

    Mono<Long> getOrder(String queueName, String id);

    Flux<UserPosition> getWaitingUsers(String queueName);
}
