package com.example.waitingservice.application.usecase.impl;

import com.example.waitingservice.application.QueueManager;
import com.example.waitingservice.application.dto.WaitingPositionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryWaitingPositionServiceTest {

    @Mock
    private QueueManager queuePort;

    @InjectMocks
    private QueryWaitingPositionService queryWaitingPositionService;

    private static final String QUEUE_NAME = "test-queue";
    private static final String USER_ID = "user-1";

    @Test
    @DisplayName("대기열에 존재하는 사용자의 순번을 올바르게 반환한다")
    void getPosition_존재하는사용자_순번반환() {
        // Given
        when(queuePort.getOrder(QUEUE_NAME, USER_ID)).thenReturn(Mono.just(5L));

        // When
        Mono<WaitingPositionResponse> result = queryWaitingPositionService.getPosition(QUEUE_NAME, USER_ID);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> assertThat(response.position()).isEqualTo(5L))
                .verifyComplete();
    }

    @Test
    @DisplayName("대기열에 존재하지 않는 사용자를 조회하면 빈 결과를 반환한다")
    void getPosition_존재하지않는사용자_빈결과반환() {
        // Given
        when(queuePort.getOrder(QUEUE_NAME, "unknown-user")).thenReturn(Mono.empty());

        // When
        Mono<WaitingPositionResponse> result = queryWaitingPositionService.getPosition(QUEUE_NAME, "unknown-user");

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("QueueManager에서 반환된 순번이 WaitingPositionResponse의 position 필드에 올바르게 매핑된다")
    void getPosition_순번매핑검증() {
        // Given
        long expectedPosition = 42L;
        when(queuePort.getOrder(QUEUE_NAME, USER_ID)).thenReturn(Mono.just(expectedPosition));

        // When
        Mono<WaitingPositionResponse> result = queryWaitingPositionService.getPosition(QUEUE_NAME, USER_ID);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response).isInstanceOf(WaitingPositionResponse.class);
                    assertThat(response.position()).isEqualTo(expectedPosition);
                })
                .verifyComplete();
    }
}