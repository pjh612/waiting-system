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