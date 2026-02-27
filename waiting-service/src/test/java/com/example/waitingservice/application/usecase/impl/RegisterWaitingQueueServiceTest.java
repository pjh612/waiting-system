package com.example.waitingservice.application.usecase.impl;

import com.example.waitingservice.application.dto.RegisterWaitingQueueResponse;
import com.example.waitingservice.domain.model.WaitingQueue;
import com.example.waitingservice.domain.repository.WaitingQueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RegisterWaitingQueueServiceTest {

    private WaitingQueueRepository waitingQueueRepository;
    private PasswordEncoder passwordEncoder;
    private RegisterWaitingQueueService registerWaitingQueueService;

    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final String QUEUE_NAME = "test-queue";
    private static final String REDIRECT_URL = "http://example.com/redirect";
    private static final Long QUEUE_SIZE = 100L;
    private static final String SECRET = "test-secret";
    private static final String ENCODED_KEY = "encoded-key-12345";

    @BeforeEach
    void setUp() {
        waitingQueueRepository = mock(WaitingQueueRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        registerWaitingQueueService = new RegisterWaitingQueueService(waitingQueueRepository, passwordEncoder);
    }

    @Nested
    @DisplayName("정상 케이스")
    class HappyCases {

        @Test
        void 새_대기열이_정상적으로_생성된다() {
            UUID expectedQueueId = UUID.randomUUID();
            WaitingQueue savedQueue = new WaitingQueue(
                    expectedQueueId, CLIENT_ID, QUEUE_NAME, REDIRECT_URL, QUEUE_SIZE, ENCODED_KEY, SECRET
            );

            when(waitingQueueRepository.findByClientIdAndQueueName(CLIENT_ID, QUEUE_NAME))
                    .thenReturn(Mono.empty());
            when(passwordEncoder.encode(anyString())).thenReturn(ENCODED_KEY);
            when(waitingQueueRepository.save(any(WaitingQueue.class))).thenReturn(Mono.just(savedQueue));

            StepVerifier.create(registerWaitingQueueService.register(
                            CLIENT_ID, QUEUE_NAME, REDIRECT_URL, QUEUE_SIZE, SECRET))
                    .assertNext(response -> {
                        assertThat(response.id()).isNotNull();
                        assertThat(response.clientId()).isEqualTo(CLIENT_ID);
                        assertThat(response.name()).isEqualTo(QUEUE_NAME);
                        assertThat(response.redirectUrl()).isEqualTo(REDIRECT_URL);
                        assertThat(response.size()).isEqualTo(QUEUE_SIZE);
                    })
                    .verifyComplete();

            verify(waitingQueueRepository).save(any(WaitingQueue.class));
        }

        @Test
        void UUID로부터_API_키가_올바르게_인코딩된다() {
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            WaitingQueue savedQueue = new WaitingQueue(
                    UUID.randomUUID(), CLIENT_ID, QUEUE_NAME, REDIRECT_URL, QUEUE_SIZE, ENCODED_KEY, SECRET
            );

            when(waitingQueueRepository.findByClientIdAndQueueName(CLIENT_ID, QUEUE_NAME))
                    .thenReturn(Mono.empty());
            when(passwordEncoder.encode(keyCaptor.capture())).thenReturn(ENCODED_KEY);
            when(waitingQueueRepository.save(any(WaitingQueue.class))).thenReturn(Mono.just(savedQueue));

            registerWaitingQueueService.register(
                    CLIENT_ID, QUEUE_NAME, REDIRECT_URL, QUEUE_SIZE, SECRET).block();

            String capturedKey = keyCaptor.getValue();
            assertThat(capturedKey).isNotNull();
            assertThat(capturedKey).isNotEmpty();
        }

        @Test
        void 반환된_응답에_API_키가_포함된다() {
            WaitingQueue savedQueue = new WaitingQueue(
                    UUID.randomUUID(), CLIENT_ID, QUEUE_NAME, REDIRECT_URL, QUEUE_SIZE, ENCODED_KEY, SECRET
            );

            when(waitingQueueRepository.findByClientIdAndQueueName(CLIENT_ID, QUEUE_NAME))
                    .thenReturn(Mono.empty());
            when(passwordEncoder.encode(anyString())).thenReturn(ENCODED_KEY);
            when(waitingQueueRepository.save(any(WaitingQueue.class))).thenReturn(Mono.just(savedQueue));

            StepVerifier.create(registerWaitingQueueService.register(
                            CLIENT_ID, QUEUE_NAME, REDIRECT_URL, QUEUE_SIZE, SECRET))
                    .assertNext(response -> {
                        assertThat(response.key()).isEqualTo(ENCODED_KEY);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("오류 케이스")
    class ErrorCases {

        @Test
        void 레포지토리_저장_실패_시_에러를_전파한다() {
            when(waitingQueueRepository.findByClientIdAndQueueName(CLIENT_ID, QUEUE_NAME))
                    .thenReturn(Mono.empty());
            when(passwordEncoder.encode(anyString())).thenReturn(ENCODED_KEY);
            when(waitingQueueRepository.save(any(WaitingQueue.class)))
                    .thenReturn(Mono.error(new RuntimeException("Database connection failed")));

            StepVerifier.create(registerWaitingQueueService.register(
                            CLIENT_ID, QUEUE_NAME, REDIRECT_URL, QUEUE_SIZE, SECRET))
                    .expectErrorMatches(throwable ->
                            throwable instanceof RuntimeException
                            && throwable.getMessage().equals("Database connection failed"))
                    .verify();
        }

        @Test
        void 중복된_queueName으로_등록_시도_시_예외를_발생시킨다() {
            WaitingQueue existingQueue = new WaitingQueue(
                    UUID.randomUUID(), CLIENT_ID, QUEUE_NAME, REDIRECT_URL, QUEUE_SIZE, ENCODED_KEY, SECRET
            );

            when(waitingQueueRepository.findByClientIdAndQueueName(CLIENT_ID, QUEUE_NAME))
                    .thenReturn(Mono.just(existingQueue));

            StepVerifier.create(registerWaitingQueueService.register(
                            CLIENT_ID, QUEUE_NAME, REDIRECT_URL, QUEUE_SIZE, SECRET))
                    .expectErrorMatches(throwable ->
                            throwable instanceof IllegalArgumentException
                            && throwable.getMessage().contains("already exists"))
                    .verify();
        }
    }
}
