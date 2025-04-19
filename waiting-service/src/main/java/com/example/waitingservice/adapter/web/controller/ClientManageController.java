package com.example.waitingservice.adapter.web.controller;

import com.example.waitingservice.application.dto.RegisterClientRequest;
import com.example.waitingservice.application.usecase.RegisterClientUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/client")
public class ClientManageController {
    public final RegisterClientUseCase registerClientUseCase;

    @PostMapping
    public Mono<?> register(@RequestBody RegisterClientRequest request) {
        return registerClientUseCase.register(request);
    }
}
