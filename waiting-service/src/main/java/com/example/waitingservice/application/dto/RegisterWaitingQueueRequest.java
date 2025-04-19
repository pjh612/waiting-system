package com.example.waitingservice.application.dto;

import java.util.UUID;

public record RegisterWaitingQueueRequest(
        UUID clientId,
        String queueName,
        String redirectUrl,
        Long queueSize,
        String secret
) {
}
