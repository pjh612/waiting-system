package com.example.waitingservice.application.usecase.impl;

import com.example.waitingservice.application.dto.RegisterWaitingQueueResponse;
import com.example.waitingservice.application.usecase.RegisterWaitingQueueUseCase;
import com.example.waitingservice.domain.model.WaitingQueue;
import com.example.waitingservice.domain.repository.WaitingQueueRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class RegisterWaitingQueueService implements RegisterWaitingQueueUseCase {
    private final WaitingQueueRepository waitingQueueRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterWaitingQueueService(WaitingQueueRepository waitingQueueRepository, PasswordEncoder passwordEncoder) {
        this.waitingQueueRepository = waitingQueueRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Mono<RegisterWaitingQueueResponse> register(UUID clientId, String queueName, String redirectUrl, Long queueSize, String secret) {
        UUID queueId = UUID.randomUUID();
        String key = passwordEncoder.encode(queueId.toString());
        WaitingQueue waitingQueue = new WaitingQueue(queueId, clientId, queueName, redirectUrl, queueSize, key, secret);

        //client key 생성 및 전달
        return waitingQueueRepository.save(waitingQueue)
                .map(RegisterWaitingQueueResponse::of);
    }
}
