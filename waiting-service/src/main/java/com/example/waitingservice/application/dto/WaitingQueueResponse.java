package com.example.waitingservice.application.dto;

import com.example.waitingservice.domain.model.WaitingQueue;

import java.util.UUID;

public record WaitingQueueResponse(
        UUID id,
        UUID clientId,
        String name,
        String redirectUrl,
        Long size,
        String key,
        String secret
) {

    public WaitingQueue toWaitingQueue() {
        return new WaitingQueue(id, clientId, name, redirectUrl, size, key, secret);
    }
}
