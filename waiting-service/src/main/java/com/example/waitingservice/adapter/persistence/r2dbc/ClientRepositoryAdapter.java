package com.example.waitingservice.adapter.persistence.r2dbc;

import com.example.waitingservice.adapter.persistence.r2dbc.entity.ClientMapper;
import com.example.waitingservice.domain.model.Client;
import com.example.waitingservice.domain.repository.ClientRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class ClientRepositoryAdapter implements ClientRepository {
    private final ClientReactiveRepository repository;

    public ClientRepositoryAdapter(ClientReactiveRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Client> findById(UUID clientId) {
        return repository.findById(clientId)
                .map(ClientMapper::toDomain);
    }

    @Override
    public Mono<Client> save(Client client) {
        return repository.save(ClientMapper.toEntity(client))
                .map(ClientMapper::toDomain);
    }
}
