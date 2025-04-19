package com.example.waitingservice.adapter.web.controller;

import com.example.waitingservice.application.dto.RegisterWaitingRequest;
import com.example.waitingservice.application.dto.RegisterWaitingResponse;
import com.example.waitingservice.application.usecase.RegisterWaitingUseCase;
import com.example.waitingservice.domain.model.WaitingQueue;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/waiting")
public class UserWaitingController {
    private final RegisterWaitingUseCase registerWaitingUseCase;

    @PostMapping
    public Mono<RegisterWaitingResponse> register(@RequestBody RegisterWaitingRequest request, @AuthenticationPrincipal WaitingQueue waitingQueue) {
        return registerWaitingUseCase.register(waitingQueue.getName(), request.id(), waitingQueue.getRedirectUrl());
    }
}
