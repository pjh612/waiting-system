package com.example.waitingservice.application.dto;

import com.example.waitingservice.domain.model.WaitingQueue;

import java.util.UUID;

public record RegisterWaitingQueueResponse(UUID id,
                                           UUID clientId,
                                           String name,
                                           String redirectUrl,
                                           Long size,
                                           String key) {

    public static RegisterWaitingQueueResponse of(WaitingQueue waitingQueue) {
        return new RegisterWaitingQueueResponse(waitingQueue.getQueueId(),
                waitingQueue.getClientId(),
                waitingQueue.getName(),
                waitingQueue.getRedirectUrl(),
                waitingQueue.getSize(),
                waitingQueue.getApiKey());
    }
}
