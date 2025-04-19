package com.example.waitingservice.domain.repository;

import com.example.waitingservice.domain.model.Client;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ClientRepository {
    Mono<Client> findById(UUID clientId);

    Mono<Client> save(Client client);
}
