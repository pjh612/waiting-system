package com.example.waitingservice.application.usecase;

import com.example.waitingservice.application.dto.RegisterWaitingResponse;
import reactor.core.publisher.Mono;

public interface RegisterWaitingUseCase {
    Mono<RegisterWaitingResponse> register(String queueName, String id, String redirectUrl);
}
