package com.example.waitingservice.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WaitingQueue {
    private Long id;
    private UUID queueId;
    private UUID clientId;
    private String name;
    private String redirectUrl;
    private Long size;
    private String apiKey;
    private String secret;
    private Instant createdAt;

    public WaitingQueue(UUID queueId, UUID clientId, String name, String redirectUrl, Long size, String apiKey, String secret) {
        this.queueId = queueId;
        this.clientId = clientId;
        this.name = name;
        this.redirectUrl = redirectUrl;
        this.size = size;
        this.apiKey = apiKey;
        this.secret = secret;
        this.createdAt = Instant.now();
    }
}
