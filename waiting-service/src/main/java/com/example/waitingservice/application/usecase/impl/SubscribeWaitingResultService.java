package com.example.waitingservice.application.usecase.impl;

import com.alert.core.manager.ReactiveSubscribableAlertManager;
import com.example.waitingservice.adapter.queue.NamedAlertChannel;
import com.example.waitingservice.application.usecase.SubscribeWaitingResultUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscribeWaitingResultService implements SubscribeWaitingResultUseCase {
    private final ReactiveSubscribableAlertManager<Flux<ServerSentEvent<Object>>> alertManager;

    @Override
    public Flux<ServerSentEvent<Object>> subscribe(String queueName, String id, String lastEventId) {
        return alertManager.subscribe(new NamedAlertChannel(queueName), id, List.of(queueName), lastEventId, Duration.ofMinutes(3).toMillis());
    }
}
