package com.example.waitingservice.adapter.persistence.r2dbc;

import com.example.waitingservice.adapter.persistence.r2dbc.entity.WaitingQueueMapper;
import com.example.waitingservice.domain.model.WaitingQueue;
import com.example.waitingservice.domain.repository.WaitingQueueRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class WaitingQueueRepositoryAdapter implements WaitingQueueRepository {
    private final WaitingQueueReactiveRepository repository;

    public WaitingQueueRepositoryAdapter(WaitingQueueReactiveRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<WaitingQueue> save(WaitingQueue queue) {
        return repository.save(WaitingQueueMapper.toEntity(queue))
                .map(WaitingQueueMapper::toDomain);
    }

    @Override
    public Mono<WaitingQueue> findByApiKey(String apiKey) {
        return repository.findByApiKey(apiKey)
                .map(WaitingQueueMapper::toDomain);
    }
}
