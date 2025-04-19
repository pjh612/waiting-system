package com.example.waitingservice.application.dto;

public record RegisterClientRequest(
        String name,
        String secret
) {
}
