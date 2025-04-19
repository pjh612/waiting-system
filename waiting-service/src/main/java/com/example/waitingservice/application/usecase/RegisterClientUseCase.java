package com.example.waitingservice.application.usecase;

import com.example.waitingservice.application.dto.RegisterClientRequest;
import com.example.waitingservice.application.dto.RegisterClientResponse;
import reactor.core.publisher.Mono;

public interface RegisterClientUseCase {
    Mono<RegisterClientResponse> register(RegisterClientRequest request);
}
