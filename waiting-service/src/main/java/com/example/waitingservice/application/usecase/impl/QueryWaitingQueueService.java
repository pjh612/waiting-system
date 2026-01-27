package com.example.waitingservice.application.usecase.impl;

import com.example.waitingservice.application.dto.WaitingQueueResponse;
import com.example.waitingservice.application.usecase.QueryWaitingQueueUseCase;
import com.example.waitingservice.application.ReactiveCacheManager;
import com.example.waitingservice.domain.repository.WaitingQueueRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class QueryWaitingQueueService implements QueryWaitingQueueUseCase {
    private final WaitingQueueRepository waitingQueueRepository;
    private final ReactiveCacheManager cacheProvider;

    public QueryWaitingQueueService(WaitingQueueRepository waitingQueueRepository, ReactiveCacheManager cacheProvider) {
        this.waitingQueueRepository = waitingQueueRepository;
        this.cacheProvider = cacheProvider;
    }

    public Mono<WaitingQueueResponse> getWaitingQueue(String apiKey) {
        String cacheKey = "queue:" + apiKey;

        return cacheProvider.getOrSet(cacheKey,
                waitingQueueRepository.findByApiKey(apiKey)
                        .map(it -> new WaitingQueueResponse(it.getQueueId(), it.getClientId(), it.getName(),
                                it.getRedirectUrl(), it.getSize(), it.getApiKey(), it.getSecret())),
                WaitingQueueResponse.class,
                3600
        ).doOnNext(it-> log.debug("GetWaitingQueue: {}", it));
    }
}
