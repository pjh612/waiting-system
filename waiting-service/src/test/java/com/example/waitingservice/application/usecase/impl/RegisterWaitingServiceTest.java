package com.example.waitingservice.application.usecase.impl;

import com.example.waitingservice.application.JwtTokenProvider;
import com.example.waitingservice.application.QueueManager;
import com.example.waitingservice.application.dto.RegisterWaitingResponse;
import com.example.waitingservice.application.usecase.RegisterWaitingUseCase;
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

    private QueueManager queuePort;
    private JwtTokenProvider jwtTokenProvider;

    private RegisterWaitingUseCase registerWaitingService;

    private static final String QUEUE_NAME = "test-queue";
    private static final String USER_ID = "test-user-id";
    private static final String REDIRECT_URL = "http://example.com/redirect";
    private static final String MOCK_TOKEN = "mock.jwt.token";
    private static final Long MOCK_ORDER = 1L;

    @BeforeEach
    void setUp() {
        queuePort = mock(QueueManager.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);

        registerWaitingService = new RegisterWaitingService(queuePort, jwtTokenProvider);
    }

    @Nested
    @DisplayName("정상 케이스")
    class HappyCases {

        @Test
        void 사용자가_대기열에_정상_등록된다() {
            when(queuePort.registerWaiting(eq(QUEUE_NAME), eq(USER_ID), any(Instant.class)))
                    .thenReturn(Mono.just(MOCK_ORDER));
            when(jwtTokenProvider.generateToken(anyMap(), anyLong()))
                    .thenReturn(MOCK_TOKEN);

            StepVerifier.create(registerWaitingService.register(QUEUE_NAME, USER_ID, REDIRECT_URL))
                    .assertNext(response -> {
                        assertThat(response.order()).isEqualTo(MOCK_ORDER);
                    })
                    .verifyComplete();

            verify(queuePort).registerWaiting(eq(QUEUE_NAME), eq(USER_ID), any(Instant.class));
        }

        @Test
        void 등록_후_올바른_순번이_반환된다() {
            Long expectedOrder = 5L;

            when(queuePort.registerWaiting(eq(QUEUE_NAME), eq(USER_ID), any(Instant.class)))
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
        void JWT_토큰_5분_유효에_userId_redirectUrl_queueName_order가_포함된다() {
            when(queuePort.registerWaiting(eq(QUEUE_NAME), eq(USER_ID), any(Instant.class)))
                    .thenReturn(Mono.just(MOCK_ORDER));
            when(jwtTokenProvider.generateToken(anyMap(), anyLong()))
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
            assertThat(capturedClaims).containsEntry("order", MOCK_ORDER);
        }

        @Test
        void RegisterWaitingResponse에_순번_토큰_타임스탬프가_포함된다() {
            when(queuePort.registerWaiting(eq(QUEUE_NAME), eq(USER_ID), any(Instant.class)))
                    .thenReturn(Mono.just(MOCK_ORDER));
            when(jwtTokenProvider.generateToken(anyMap(), anyLong()))
                    .thenReturn(MOCK_TOKEN);

            StepVerifier.create(registerWaitingService.register(QUEUE_NAME, USER_ID, REDIRECT_URL))
                    .assertNext(response -> {
                        assertThat(response.order()).isEqualTo(MOCK_ORDER);
                        assertThat(response.token()).isEqualTo(MOCK_TOKEN);
                        assertThat(response.eventId()).isGreaterThan(0);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    class ExceptionCases {

        @Test
        void 이미_등록된_사용자를_다시_등록하려는_경우_에러를_전파한다() {
            when(queuePort.registerWaiting(eq(QUEUE_NAME), eq(USER_ID), any(Instant.class)))
                    .thenReturn(Mono.error(new IllegalStateException("User already registered")));

            StepVerifier.create(registerWaitingService.register(QUEUE_NAME, USER_ID, REDIRECT_URL))
                    .expectErrorMatches(throwable ->
                            throwable instanceof IllegalStateException
                            && throwable.getMessage().equals("User already registered"))
                    .verify();
        }
    }
}
