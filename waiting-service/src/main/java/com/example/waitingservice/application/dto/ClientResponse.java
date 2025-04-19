package com.example.waitingservice.application.dto;

import com.example.waitingservice.domain.model.Client;

import java.time.Instant;
import java.util.UUID;

public record ClientResponse(
        UUID clientId,
        String secret,
        Instant createdAt
) {

    public static ClientResponse of(Client client) {
        return new ClientResponse(client.getClientId(), client.getSecret(), client.getCreatedAt());
    }
}
