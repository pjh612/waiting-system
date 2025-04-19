package com.example.waitingservice.application.usecase.impl;

import com.example.waitingservice.application.JwtTokenProvider;
import com.example.waitingservice.application.QueueManager;
import com.example.waitingservice.application.dto.RegisterWaitingResponse;
import com.example.waitingservice.application.usecase.RegisterWaitingUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegisterWaitingService implements RegisterWaitingUseCase {
    private final QueueManager queuePort;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Mono<RegisterWaitingResponse> register(String queueName, String id, String redirectUrl) {
        Instant now = Instant.now();

        return queuePort.registerWaiting(queueName, id, now)
                .map(it -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("userId", id);
                    data.put("redirectUrl", redirectUrl);
                    data.put("queueName", queueName);
                    data.put("order", it);
                    String token = jwtTokenProvider.generateToken(data, Duration.ofMinutes(5).toMillis());

                    return new RegisterWaitingResponse(it, token);
                });
    }
}
