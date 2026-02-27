package com.example.waitingservice.application.usecase.impl;

import com.example.waitingservice.application.JwtTokenProvider;
import com.example.waitingservice.application.QueueManager;
import com.example.waitingservice.application.dto.RegisterWaitingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RegisterWaitingServiceTest {

    private QueueManager queueManager;
    private JwtTokenProvider jwtTokenProvider;
    private RegisterWaitingService registerWaitingService;

    private static final String QUEUE_NAME = "test-queue";
    private static final String USER_ID = "user-123";
    private static final String REDIRECT_URL = "http://example.com/redirect";
    private static final String MOCK_TOKEN = "mock.jwt.token";

    @BeforeEach
    void setUp() {
        queueManager = mock(QueueManager.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);

        registerWaitingService = new RegisterWaitingService(queueManager, jwtTokenProvider);
    }

    @Nested
    @DisplayName("정상 케이스")
    class HappyCases {

        @Test
        void 사용자가_대기열에_정상_등록된다() {
            Long expectedOrder = 1L;

            when(queueManager.registerWaiting(eq(QUEUE_NAME), eq(USER_ID), any()))
                    .thenReturn(Mono.just(expectedOrder));
            when(jwtTokenProvider.generateToken(anyMap(), eq(Duration.ofMinutes(5).toMillis())))
                    .thenReturn(MOCK_TOKEN);

            StepVerifier.create(registerWaitingService.register(QUEUE_NAME, USER_ID, REDIRECT_URL))
                    .assertNext(response -> {
                        assertThat(response.order()).isEqualTo(expectedOrder);
                        assertThat(response.token()).isEqualTo(MOCK_TOKEN);
                    })
                    .verifyComplete();

            verify(queueManager).registerWaiting(eq(QUEUE_NAME), eq(USER_ID), any());
        }

        @Test
        void 등록_후_올바른_순번이_반환된다() {
            Long expectedOrder = 5L;

            when(queueManager.registerWaiting(eq(QUEUE_NAME), eq(USER_ID), any()))
                    .thenReturn(Mono.just(expectedOrder));
            when(jwtTokenProvider.generateToken(anyMap(), anyLong()))
                    .thenReturn(MOCK_TOKEN);

            StepVerifier.create(registerWaitingService.register(QUEUE_NAME, USER_ID, REDIRECT_URL))
                    .assertNext(response -> {
                        assertThat(response.order()).isEqualTo(expectedOrder);
                    })
                    .verifyComplete();
        }

        @Test
        void JWT_토큰에_5분_유효시간과_올바른_클레임이_포함된다() {
            Long expectedOrder = 1L;

            when(queueManager.registerWaiting(eq(QUEUE_NAME), eq(USER_ID), any()))
                    .thenReturn(Mono.just(expectedOrder));
            when(jwtTokenProvider.generateToken(anyMap(), eq(Duration.ofMinutes(5).toMillis())))
                    .thenReturn(MOCK_TOKEN);

            StepVerifier.create(registerWaitingService.register(QUEUE_NAME, USER_ID, REDIRECT_URL))
                    .expectNextCount(1)
                    .verifyComplete();

            ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(jwtTokenProvider).generateToken(
                    claimsCaptor.capture(),
                    eq(Duration.ofMinutes(5).toMillis())
            );

            Map<String, Object> capturedClaims = claimsCaptor.getValue();
            assertThat(capturedClaims).containsEntry("userId", USER_ID);
            assertThat(capturedClaims).containsEntry("redirectUrl", REDIRECT_URL);
            assertThat(capturedClaims).containsEntry("queueName", QUEUE_NAME);
            assertThat(capturedClaims).containsEntry("order", expectedOrder);
        }

        @Test
        void RegisterWaitingResponse에_순번_토큰_타임스탬프가_포함된다() {
            Long expectedOrder = 1L;

            when(queueManager.registerWaiting(eq(QUEUE_NAME), eq(USER_ID), any()))
                    .thenReturn(Mono.just(expectedOrder));
            when(jwtTokenProvider.generateToken(anyMap(), anyLong()))
                    .thenReturn(MOCK_TOKEN);

            StepVerifier.create(registerWaitingService.register(QUEUE_NAME, USER_ID, REDIRECT_URL))
                    .assertNext(response -> {
                        assertThat(response.order()).isNotNull();
                        assertThat(response.token()).isNotNull();
                        assertThat(response.eventId()).isGreaterThan(0);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("오류 케이스")
    class ErrorCases {

        @Test
        void QueueManager_registerWaiting_실패_시_에러를_전파한다() {
            when(queueManager.registerWaiting(eq(QUEUE_NAME), eq(USER_ID), any()))
                    .thenReturn(Mono.error(new RuntimeException("User already registered")));

            StepVerifier.create(registerWaitingService.register(QUEUE_NAME, USER_ID, REDIRECT_URL))
                    .expectErrorMatches(throwable ->
                            throwable instanceof RuntimeException
                            && throwable.getMessage().equals("User already registered"))
                    .verify();
        }

        @Test
        void JwtTokenProvider_토큰_생성_실패_시_에러를_전파한다() {
            when(queueManager.registerWaiting(eq(QUEUE_NAME), eq(USER_ID), any()))
                    .thenReturn(Mono.just(1L));
            when(jwtTokenProvider.generateToken(anyMap(), anyLong()))
                    .thenThrow(new RuntimeException("Token generation failed"));

            StepVerifier.create(registerWaitingService.register(QUEUE_NAME, USER_ID, REDIRECT_URL))
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }
}
