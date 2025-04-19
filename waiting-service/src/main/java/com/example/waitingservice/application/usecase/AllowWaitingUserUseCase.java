package com.example.waitingservice.application.usecase;

import com.example.waitingservice.application.dto.AllowWaitingUserResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import reactor.core.publisher.Mono;

public interface AllowWaitingUserUseCase {
    Mono<AllowWaitingUserResponse> allow(String apiKey, String queueName, Long count, String redirectUrl) throws JsonProcessingException;
}
