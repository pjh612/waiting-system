package com.example.waitingservice.application.usecase.impl;

import com.alert.core.manager.ReactiveSubscribableAlertManager;
import com.example.waitingservice.adapter.queue.NamedAlertChannel;
import com.example.waitingservice.application.JwtTokenProvider;
import com.example.waitingservice.application.QueueManager;
import com.example.waitingservice.application.dto.AllowWaitingUserResponse;
import com.example.waitingservice.application.dto.WaitingQueueResponse;
import com.example.waitingservice.application.usecase.AllowWaitingUserUseCase;
import com.example.waitingservice.application.usecase.QueryWaitingQueueUseCase;
import com.example.waitingservice.domain.model.QueueUser;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Service
public class AllowWaitingUserService implements AllowWaitingUserUseCase {
    private final QueueManager queuePort;
    private final ReactiveSubscribableAlertManager<Flux<ServerSentEvent<Object>>> alertManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final QueryWaitingQueueUseCase queryWaitingQueueUseCase;

    public AllowWaitingUserService(QueueManager queuePort, ReactiveSubscribableAlertManager<Flux<ServerSentEvent<Object>>> alertManager, JwtTokenProvider jwtTokenProvider, QueryWaitingQueueUseCase queryWaitingQueueUseCase) {
        this.queuePort = queuePort;
        this.alertManager = alertManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.queryWaitingQueueUseCase = queryWaitingQueueUseCase;
    }

    public Mono<AllowWaitingUserResponse> allow(String apiKey, String queueName, Long count, String redirectUrl) {
        NamedAlertChannel alertChannel = new NamedAlertChannel(queueName);

        return queryWaitingQueueUseCase.getWaitingQueue(apiKey)
                .flatMap(queue -> queuePort.allow(queueName, count)
                        .flatMap(user -> processAllowedUser(user, queue, redirectUrl, alertChannel))
                        .collectList()
                        .flatMap(allowedUsers -> {
                            long actualCount = allowedUsers.size();

                            if (actualCount == 0) {
                                return Mono.just(new AllowWaitingUserResponse(count, 0L));
                            }

                            return alertPositionUpdated(queueName, alertChannel, actualCount)
                                    .thenReturn(new AllowWaitingUserResponse(count, actualCount));
                        })
                );
    }

    private Mono<QueueUser> processAllowedUser(QueueUser user, WaitingQueueResponse queue, String redirectUrl, NamedAlertChannel alertChannel) {
        String token = jwtTokenProvider.generateToken(
                Map.of(
                        "queueName", queue.name(),
                        "userId", user.getId(),
                        "redirectUrl", redirectUrl
                ),
                Duration.ofMinutes(60).toMillis(),
                queue.secret()
        );

        return alertManager.notice(alertChannel, user.getId(), Map.of("redirectUrl", redirectUrl, "token", token))
                .thenReturn(user);
    }

    private Mono<Void> alertPositionUpdated(String queueName, NamedAlertChannel alertChannel, Long count) {
        return alertManager.noticeByTag(alertChannel, queueName, Map.of("enteredCount", count));
    }
}