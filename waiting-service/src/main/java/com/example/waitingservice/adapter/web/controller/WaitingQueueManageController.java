package com.example.waitingservice.adapter.web.controller;

import com.example.waitingservice.application.dto.RegisterWaitingQueueRequest;
import com.example.waitingservice.application.usecase.RegisterWaitingQueueUseCase;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/queue")
public class WaitingQueueManageController {
    private final RegisterWaitingQueueUseCase registerWaitingQueueUsecase;

    public WaitingQueueManageController(RegisterWaitingQueueUseCase registerWaitingQueueUsecase) {
        this.registerWaitingQueueUsecase = registerWaitingQueueUsecase;
    }

    @PostMapping
    public Mono<?> register(@RequestBody RegisterWaitingQueueRequest request) {
        return registerWaitingQueueUsecase.register(request.clientId(), request.queueName(), request.redirectUrl(), request.queueSize(), request.secret());
    }
}
