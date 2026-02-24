package com.example.waitingservice.adapter.web.controller;

import com.example.waitingservice.EmbeddedRedisConfig;
import com.example.waitingservice.application.dto.AllowWaitingUserRequest;
import com.example.waitingservice.application.dto.RegisterClientRequest;
import com.example.waitingservice.application.dto.RegisterClientResponse;
import com.example.waitingservice.application.dto.RegisterWaitingQueueResponse;
import com.example.waitingservice.application.dto.RegisterWaitingRequest;
import com.example.waitingservice.application.dto.RegisterWaitingResponse;
import com.example.waitingservice.application.usecase.RegisterClientUseCase;
import com.example.waitingservice.application.usecase.RegisterWaitingQueueUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest
@Testcontainers
@AutoConfigureWebTestClient
@Import(EmbeddedRedisConfig.class)
class UserWaitingControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    RegisterClientUseCase registerClientUseCase;

    @Autowired
    RegisterWaitingQueueUseCase registerWaitingQueueUseCase;

    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.8.0");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }


    @Test
    @WithMockUser
    void register() {
        RegisterClientResponse registerClientResponse = registerClientUseCase.register(new RegisterClientRequest("test", "waitinggnitiawwaitinggnitiawwaitinggnitiawwaitinggnitiaw"))
                .block();
        RegisterWaitingQueueResponse registerWaitingQueueResponse = registerWaitingQueueUseCase.register(registerClientResponse.id(), "test-queue", "redirect-url", 1000L, "secret").block();

        webTestClient.post()
                .uri("/api/waiting")
                .header("Authorization", registerWaitingQueueResponse.key())
                .bodyValue(new RegisterWaitingRequest("id"))
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    @WithMockUser
    void register_응답에_eventId가_포함된다() {
        RegisterClientResponse registerClientResponse = registerClientUseCase.register(new RegisterClientRequest("test", "waitinggnitiawwaitinggnitiawwaitinggnitiawwaitinggnitiaw"))
                .block();
        RegisterWaitingQueueResponse registerWaitingQueueResponse = registerWaitingQueueUseCase.register(registerClientResponse.id(), "test-queue", "redirect-url", 1000L, "secret").block();

        long beforeRegister = System.currentTimeMillis();

        RegisterWaitingResponse response = webTestClient.post()
                .uri("/api/waiting")
                .header("Authorization", registerWaitingQueueResponse.key())
                .bodyValue(new RegisterWaitingRequest("id-1"))
                .exchange()
                .expectStatus().isOk()
                .returnResult(RegisterWaitingResponse.class)
                .getResponseBody()
                .blockFirst();

        long afterRegister = System.currentTimeMillis();

        assertThat(response).isNotNull();
        assertThat(response.eventId()).isBetween(beforeRegister, afterRegister);
    }

    @Test
    @WithMockUser
    void allow가_subscribe보다_먼저_발생해도_lastEventId로_메시지를_수신한다() throws InterruptedException {
        RegisterClientResponse registerClientResponse = registerClientUseCase.register(new RegisterClientRequest("test", "waitinggnitiawwaitinggnitiawwaitinggnitiawwaitinggnitiaw"))
                .block();
        RegisterWaitingQueueResponse registerWaitingQueueResponse = registerWaitingQueueUseCase.register(registerClientResponse.id(), "test-queue", "redirect-url", 1000L, "waitinggnitiawwaitinggnitiawwaitinggnitiawwaitinggnitiaw").block();

        // 1. 사용자 등록 → eventId 수령
        RegisterWaitingResponse registerResponse = webTestClient.post()
                .uri("/api/waiting")
                .header("Authorization", registerWaitingQueueResponse.key())
                .bodyValue(new RegisterWaitingRequest("id-race"))
                .exchange()
                .expectStatus().isOk()
                .returnResult(RegisterWaitingResponse.class)
                .getResponseBody()
                .blockFirst();

        assertThat(registerResponse).isNotNull();
        String token = registerResponse.token();
        String eventId = String.valueOf(registerResponse.eventId());

        // 2. subscribe 전에 allow 먼저 실행 (race condition 재현)
        webTestClient.post()
                .uri("/api/waiting/allow")
                .header("Authorization", registerWaitingQueueResponse.key())
                .bodyValue(new AllowWaitingUserRequest(1L))
                .exchange()
                .expectStatus().isOk();

        // Kafka 메시지 전파 대기
        Thread.sleep(1000);

        // 3. Last-Event-ID를 register 시점의 eventId로 설정하여 구독
        Flux<ServerSentEvent<String>> eventStream = webTestClient.get()
                .uri("/api/waiting/subscribe")
                .header("token", token)
                .header("Last-Event-ID", eventId)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .getResponseBody();

        // 4. allow 메시지가 캐시에서 복구되어 수신되는지 검증
        StepVerifier.create(eventStream)
                .thenAwait(Duration.ofSeconds(5))
                .thenConsumeWhile(event -> {
                    System.out.println("Event: " + event.event() + ": " + event.data());
                    return !"MESSAGE".equals(event.event());
                })
                .expectNextMatches(event -> "MESSAGE".equals(event.event()))
                .thenCancel()
                .verify();
    }

    @Test
    @WithMockUser
    void subscribe() {
        // 1000개의 사용자 요청 생성
        int userCount = 1;

        RegisterClientResponse registerClientResponse = registerClientUseCase.register(new RegisterClientRequest("test", "waitinggnitiawwaitinggnitiawwaitinggnitiawwaitinggnitiaw"))
                .block();
        RegisterWaitingQueueResponse registerWaitingQueueResponse = registerWaitingQueueUseCase.register(registerClientResponse.id(), "test-queue", "redirect-url", 1000L, "waitinggnitiawwaitinggnitiawwaitinggnitiawwaitinggnitiaw").block();

        // 1000명의 사용자 등록 및 토큰 수집
        List<String> tokens = Flux.range(1, userCount)
                .flatMap(i -> webTestClient.post()
                        .uri("/api/waiting")
                        .header("Authorization", registerWaitingQueueResponse.key())
                        .bodyValue(new RegisterWaitingRequest("id-" + i))
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .returnResult(RegisterWaitingResponse.class)
                        .getResponseBody()
                )
                .map(RegisterWaitingResponse::token)
                .collectList()
                .block();

        assertThat(tokens).hasSize(userCount);

        List<Flux<ServerSentEvent<String>>> eventStreams = tokens.stream()
                .map(token -> webTestClient.get()
                        .uri("/api/waiting/subscribe")
                        .header("token", token)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .returnResult(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                        })
                        .getResponseBody()
                )
                .toList();

        AllowWaitingUserRequest allowRequest = new AllowWaitingUserRequest(1L);
        webTestClient.post()
                .uri("/api/waiting/allow")
                .header("Authorization", registerWaitingQueueResponse.key())
                .bodyValue(allowRequest)
                .exchange()
                .expectStatus()
                .isOk();

        for (Flux<ServerSentEvent<String>> eventStream : eventStreams) {
            StepVerifier.create(eventStream)
                    .thenAwait(Duration.ofSeconds(10))
                    .thenConsumeWhile(event -> {
                        System.out.println("Event Data: " + event.event() + ": " + event.data());
                        return !"MESSAGE".equals(event.event());

                    })
                    .expectNextMatches(event -> "MESSAGE".equals(event.event()))
                    .thenCancel()
                    .verify();
        }
    }
}