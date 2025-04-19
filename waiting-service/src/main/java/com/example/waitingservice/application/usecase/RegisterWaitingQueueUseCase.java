package com.example.waitingservice.application.usecase;

import com.example.waitingservice.application.dto.RegisterWaitingQueueResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RegisterWaitingQueueUseCase {
    Mono<RegisterWaitingQueueResponse> register(UUID clientId, String queueName, String redirectUrl, Long queueSize, String secret);
}
