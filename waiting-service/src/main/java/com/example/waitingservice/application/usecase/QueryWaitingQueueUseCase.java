package com.example.waitingservice.application.usecase;

import com.example.waitingservice.application.dto.WaitingQueueResponse;
import reactor.core.publisher.Mono;


public interface QueryWaitingQueueUseCase {
    Mono<WaitingQueueResponse> getWaitingQueue(String apiKey);
}
