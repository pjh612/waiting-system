package com.example.waitingservice.application.dto;

public record RegisterWaitingResponse(Long order, String token, long eventId) {
}
