package com.example.waitingservice.adapter.persistence.r2dbc;

import com.example.waitingservice.adapter.persistence.r2dbc.entity.WaitingQueueEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface WaitingQueueReactiveRepository extends ReactiveCrudRepository<WaitingQueueEntity, UUID> {
    Mono<WaitingQueueEntity> findByApiKey(String key);

    Mono<WaitingQueueEntity> findByClientIdAndQueueName(UUID clientId, String queueName);
}
