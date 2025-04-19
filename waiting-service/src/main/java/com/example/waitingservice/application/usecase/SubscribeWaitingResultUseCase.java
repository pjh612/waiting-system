package com.example.waitingservice.application.usecase;


import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

public interface SubscribeWaitingResultUseCase {
    Flux<ServerSentEvent<Object>> subscribe(String queueName, String id, String lastEventId);
}
