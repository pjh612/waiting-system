package com.example.waitingservice.application.usecase;

import com.example.waitingservice.application.dto.WaitingPositionResponse;
import reactor.core.publisher.Mono;

public interface QueryWaitingPositionUseCase {

    Mono<WaitingPositionResponse> getOrder(String queueName, String id);
}
