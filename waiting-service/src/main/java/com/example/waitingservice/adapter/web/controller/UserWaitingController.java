package com.example.waitingservice.adapter.web.controller;

import com.example.waitingservice.application.JwtTokenProvider;
import com.example.waitingservice.application.dto.*;
import com.example.waitingservice.application.usecase.*;
import com.example.waitingservice.domain.model.WaitingQueue;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/waiting")
public class UserWaitingController {
    private final RegisterWaitingUseCase registerWaitingUseCase;
    private final AllowWaitingUserUseCase allowWaitingUserUseCase;
    private final SubscribeWaitingResultUseCase subscribeWaitingResultUseCase;
    private final QueryWaitingPositionUseCase queryWaitingPositionUseCase;
    private final TouchWaitingUserUseCase touchWaitingUserUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping
    public Mono<RegisterWaitingResponse> register(@RequestBody RegisterWaitingRequest request, @AuthenticationPrincipal WaitingQueue waitingQueue) {
        return registerWaitingUseCase.register(waitingQueue.getName(), request.id(), waitingQueue.getRedirectUrl());
    }

    @PostMapping("/allow")
    public Mono<AllowWaitingUserResponse> allowWaitingUser(@RequestBody AllowWaitingUserRequest request, @AuthenticationPrincipal WaitingQueue waitingQueue) throws JsonProcessingException {
        return allowWaitingUserUseCase.allow(waitingQueue.getApiKey(), waitingQueue.getName(), request.count(), waitingQueue.getRedirectUrl());
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> subscribe(
            @RequestHeader String token,
            @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId) {
        Map<String, ?> claims = jwtTokenProvider.getClaims(token);
        String id = claims.get("userId").toString();
        String queueName = claims.get("queueName").toString();
        String newToken = jwtTokenProvider.generateToken(new HashMap<>(claims), Duration.ofMinutes(5).toMillis());

        return Flux.concat(
                Flux.just(ServerSentEvent.builder()
                        .event("TOKEN_UPDATE")
                        .data(newToken)
                        .build()),
                subscribeWaitingResultUseCase.subscribe(queueName, id, lastEventId)
        );
    }

    @GetMapping("/position")
    public Mono<WaitingPositionResponse> getPosition(@RequestHeader String token) {
        Map<String, ?> claims = jwtTokenProvider.getClaims(token);
        String queueName = (String) claims.get("queueName");
        String userId = (String) claims.get("userId");


        return queryWaitingPositionUseCase.getPosition(queueName, userId)
                .doOnSuccess(it-> touchWaitingUserUseCase.touch(queueName, userId));
    }

}
