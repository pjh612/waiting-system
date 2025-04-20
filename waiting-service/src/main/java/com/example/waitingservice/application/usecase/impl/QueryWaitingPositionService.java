package com.example.waitingservice.application.usecase.impl;

import com.example.waitingservice.application.QueueManager;
import com.example.waitingservice.application.dto.WaitingPositionResponse;
import com.example.waitingservice.application.usecase.QueryWaitingPositionUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class QueryWaitingPositionService implements QueryWaitingPositionUseCase {
    private final QueueManager queuePort;

    private static final Logger log = LoggerFactory.getLogger(QueryWaitingPositionService.class);

    public QueryWaitingPositionService(QueueManager queuePort) {
        this.queuePort = queuePort;
    }

    @Override
    public Mono<WaitingPositionResponse> getPosition(String queueName, String id) {
        return queuePort.getOrder(queueName, id)
                .doOnSuccess(it-> log.debug("Get WaitingPositionResponse with id: {}", id))
                .map(WaitingPositionResponse::new);
    }
}
