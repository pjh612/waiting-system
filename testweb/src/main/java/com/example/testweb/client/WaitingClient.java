package com.example.testweb.client;

import com.example.testweb.dto.EnterUserRequest;
import com.example.testweb.dto.EnterUserResponse;
import com.example.testweb.dto.RegisterWaitingRequest;
import com.example.testweb.dto.RegisterWaitingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class WaitingClient {
    private final RestClient restClient;
    private final String apiKey;

    public WaitingClient(RestClient restClient, @Value("${waiting-queue.key}") String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    public RegisterWaitingResponse register(String id) {
        return restClient.post()
                .uri("/api/waiting")
                .body(new RegisterWaitingRequest(id))
                .header("Authorization", apiKey)
                .retrieve()
                .body(RegisterWaitingResponse.class);
    }

    public EnterUserResponse enter(Long count) {
        return restClient.post()
                .uri("/api/waiting/allow")
                .body(new EnterUserRequest(count))
                .header("Authorization", apiKey)
                .retrieve()
                .body(EnterUserResponse.class);
    }
}
