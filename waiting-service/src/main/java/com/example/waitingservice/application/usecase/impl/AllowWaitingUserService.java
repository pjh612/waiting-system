package com.example.waitingservice.application.usecase.impl;

import com.alert.core.manager.ReactiveSubscribableAlertManager;
import com.example.waitingservice.adapter.queue.NamedAlertChannel;
import com.example.waitingservice.application.JwtTokenProvider;
import com.example.waitingservice.application.QueueManager;
import com.example.waitingservice.application.dto.AllowWaitingUserResponse;
import com.example.waitingservice.application.usecase.AllowWaitingUserUseCase;
import com.example.waitingservice.application.usecase.QueryWaitingQueueUseCase;
import com.example.waitingservice.domain.model.UserPosition;
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

        return queuePort.allow(queueName, count)
                .flatMap(it -> queryWaitingQueueUseCase.getWaitingQueue(apiKey)
                        .flatMap(queue -> {
                            String token = jwtTokenProvider.generateToken(
                                    Map.of(
                                            "queueName", queueName,
                                            "userId", it.getId(),
                                            "redirectUrl", redirectUrl
                                    ),
                                    Duration.ofMinutes(60).toMillis(),
                                    queue.secret()
                            );

                            return alertManager.notice(alertChannel, it.getId(),
                                    Map.of("redirectUrl", redirectUrl, "token", token)
                            ).thenReturn(it);
                        })
                )
                .collectList()
                .doOnSuccess(users -> alertPositionUpdated(queueName, alertChannel))
                .map(typedTuples -> new AllowWaitingUserResponse(count, (long) typedTuples.size())); // 최종 결과 매핑
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