package com.example.waitingservice.application.usecase.impl;

import com.alert.core.manager.ReactiveSubscribableAlertManager;
import com.example.waitingservice.adapter.queue.NamedAlertChannel;
import com.example.waitingservice.application.JwtTokenProvider;
import com.example.waitingservice.application.QueueManager;
import com.example.waitingservice.application.dto.RegisterWaitingResponse;
import com.example.waitingservice.application.usecase.RegisterWaitingUseCase;
import com.example.waitingservice.domain.model.UserPosition;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegisterWaitingService implements RegisterWaitingUseCase {
    private final QueueManager queuePort;
    private final JwtTokenProvider jwtTokenProvider;
    private final ReactiveSubscribableAlertManager<Flux<ServerSentEvent<Object>>> alertManager;

    @Override
    public Mono<RegisterWaitingResponse> register(String queueName, String id, String redirectUrl) {
        Instant now = Instant.now();
        NamedAlertChannel alertChannel = new NamedAlertChannel(queueName);

        return queuePort.registerWaiting(queueName, id, now)
                .doOnSuccess(users -> alertPositionUpdated(queueName, alertChannel))
                .map(it -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("userId", id);
                    data.put("redirectUrl", redirectUrl);
                    data.put("queueName", queueName);
                    data.put("order", it);
                    String token = jwtTokenProvider.generateToken(data, Duration.ofMinutes(5).toMillis());

                    return new RegisterWaitingResponse(it, token);
                });
    }

    private void alertPositionUpdated(String queueName, NamedAlertChannel alertChannel) {
        queuePort.getWaitingUsers(queueName)
                .flatMap(user -> sendWaitingQueueUpdate(user, alertChannel))
                .subscribe();
    }

    private Mono<Void> sendWaitingQueueUpdate(UserPosition users, NamedAlertChannel alertChannel) {
        return alertManager.notice(alertChannel, users.getUserId(), Map.of("position", users.getPosition()));
    }
}
