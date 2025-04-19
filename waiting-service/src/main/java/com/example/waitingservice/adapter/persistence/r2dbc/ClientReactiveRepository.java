package com.example.waitingservice.adapter.persistence.r2dbc;

import com.example.waitingservice.adapter.persistence.r2dbc.entity.ClientEntity;
import com.example.waitingservice.domain.model.Client;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ClientReactiveRepository extends ReactiveCrudRepository<ClientEntity, UUID> {
}
