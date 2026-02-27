package com.example.waitingservice.application.usecase.impl;

import com.example.waitingservice.application.ReactiveCacheManager;
import com.example.waitingservice.application.dto.WaitingQueueResponse;
import com.example.waitingservice.domain.model.WaitingQueue;
import com.example.waitingservice.domain.repository.WaitingQueueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryWaitingQueueServiceTest {

    @Mock
    private WaitingQueueRepository waitingQueueRepository;

    @Mock
    private ReactiveCacheManager cacheProvider;

    @InjectMocks
    private QueryWaitingQueueService queryWaitingQueueService;

    private static final String API_KEY = "test-api-key";
    private static final String CACHE_KEY = "queue:" + API_KEY;

    @Test
    @DisplayName("캐시 히트 시 레포지토리 호출 없이 캐시 결과를 반환한다")
    void getWaitingQueue_캐시HIT_캐시결과반환() {
        // Given
        WaitingQueueResponse cachedResponse = new WaitingQueueResponse(
                UUID.randomUUID(), UUID.randomUUID(), "test-queue", "http://redirect.url", 100L, API_KEY, "secret"
        );
        doReturn(Mono.just(cachedResponse))
                .when(cacheProvider).getOrSet(eq(CACHE_KEY), any(), eq(WaitingQueueResponse.class), anyLong());

        // When
        Mono<WaitingQueueResponse> result = queryWaitingQueueService.getWaitingQueue(API_KEY);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(cachedResponse.id());
                    assertThat(response.key()).isEqualTo(cachedResponse.key());
                })
                .verifyComplete();

        verify(waitingQueueRepository, never()).findByApiKey(any());
    }

    @Test
    @DisplayName("캐시 미스 시 레포지토리에서 조회 후 3600초 TTL로 캐싱한다")
    void getWaitingQueue_캐시MISS_레포지토리조회후3600초TTL로캐싱() {
        // Given
        UUID queueId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        WaitingQueue waitingQueue = new WaitingQueue(queueId, clientId, "test-queue", "http://redirect.url", 100L, API_KEY, "secret");

        when(waitingQueueRepository.findByApiKey(API_KEY)).thenReturn(Mono.just(waitingQueue));
        doAnswer(invocation -> {
            Supplier<Mono<WaitingQueueResponse>> fallback = invocation.getArgument(1);
            return fallback.get();
        }).when(cacheProvider).getOrSet(eq(CACHE_KEY), any(), eq(WaitingQueueResponse.class), eq(3600L));

        // When
        Mono<WaitingQueueResponse> result = queryWaitingQueueService.getWaitingQueue(API_KEY);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(queueId);
                    assertThat(response.clientId()).isEqualTo(clientId);
                    assertThat(response.name()).isEqualTo("test-queue");
                    assertThat(response.key()).isEqualTo(API_KEY);
                })
                .verifyComplete();

        verify(waitingQueueRepository).findByApiKey(API_KEY);
        verify(cacheProvider).getOrSet(eq(CACHE_KEY), any(), eq(WaitingQueueResponse.class), eq(3600L));
    }

    @Test
    @DisplayName("유효하지 않은 API 키로 조회 시 에러를 반환한다")
    void getWaitingQueue_유효하지않은API키_에러반환() {
        // Given
        String invalidApiKey = "invalid-api-key";
        String invalidCacheKey = "queue:" + invalidApiKey;

        when(waitingQueueRepository.findByApiKey(invalidApiKey))
                .thenReturn(Mono.error(new NoSuchElementException("Queue not found")));
        doAnswer(invocation -> {
            Supplier<Mono<WaitingQueueResponse>> fallback = invocation.getArgument(1);
            return fallback.get();
        }).when(cacheProvider).getOrSet(eq(invalidCacheKey), any(), eq(WaitingQueueResponse.class), anyLong());

        // When
        Mono<WaitingQueueResponse> result = queryWaitingQueueService.getWaitingQueue(invalidApiKey);

        // Then
        StepVerifier.create(result)
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    @DisplayName("캐시 키가 'queue:' + apiKey 형식으로 생성된다")
    void getWaitingQueue_캐시키형식검증() {
        // Given
        WaitingQueueResponse cachedResponse = new WaitingQueueResponse(
                UUID.randomUUID(), UUID.randomUUID(), "test-queue", "http://redirect.url", 100L, API_KEY, "secret"
        );
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        doReturn(Mono.just(cachedResponse))
                .when(cacheProvider).getOrSet(anyString(), any(), eq(WaitingQueueResponse.class), anyLong());

        // When
        StepVerifier.create(queryWaitingQueueService.getWaitingQueue(API_KEY))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        verify(cacheProvider).getOrSet(keyCaptor.capture(), any(), eq(WaitingQueueResponse.class), anyLong());
        assertThat(keyCaptor.getValue()).isEqualTo("queue:" + API_KEY);
    }
}
