package com.example.waitingservice.application.usecase.impl;

import com.example.waitingservice.application.QueueManager;
import com.example.waitingservice.application.dto.WaitingPositionResponse;
import com.example.waitingservice.application.usecase.QueryWaitingPositionUseCase;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class QueryWaitingPositionService implements QueryWaitingPositionUseCase {
    private final QueueManager queuePort;

    public QueryWaitingPositionService(QueueManager queuePort) {
        this.queuePort = queuePort;
    }

    @Override
    public Mono<WaitingPositionResponse> getOrder(String queueName, String id) {
        return queuePort.getOrder(queueName, id)
                .map(WaitingPositionResponse::new);
    }
}
