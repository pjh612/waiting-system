package com.example.waitingservice.application.usecase.impl;

import com.example.waitingservice.application.usecase.RegisterClientUseCase;
import com.example.waitingservice.application.dto.RegisterClientRequest;
import com.example.waitingservice.application.dto.RegisterClientResponse;
import com.example.waitingservice.domain.model.Client;
import com.example.waitingservice.domain.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterClientService implements RegisterClientUseCase {
    private final ClientRepository clientRepository;

    @Override
    public Mono<RegisterClientResponse> register(RegisterClientRequest request) {
        UUID uuid = UUID.randomUUID();
        Client client = new Client(uuid, request.name(), request.secret());

        return clientRepository.save(client)
                .map(it -> new RegisterClientResponse(it.getClientId()));
    }
}
