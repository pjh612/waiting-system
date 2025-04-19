package com.example.waitingservice.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Client {
    private Long id;
    private UUID clientId;
    private String name;
    private String secret;
    private Instant createdAt;

    public Client(UUID clientId, String name, String secret) {
        this.clientId = clientId;
        this.name = name;
        this.secret = secret;
        this.createdAt = Instant.now();
    }
}
