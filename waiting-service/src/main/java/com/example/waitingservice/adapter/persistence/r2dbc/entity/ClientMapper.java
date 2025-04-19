package com.example.waitingservice.adapter.persistence.r2dbc.entity;

import com.example.waitingservice.domain.model.Client;

public class ClientMapper {

    public static ClientEntity toEntity(Client domain) {
        if (domain == null) {
            return null;
        }
        return new ClientEntity(
                domain.getId(),
                domain.getClientId(),
                domain.getName(),
                domain.getSecret(),
                domain.getCreatedAt()
        );
    }

    public static Client toDomain(ClientEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Client(
                entity.getId(),
                entity.getClientId(),
                entity.getName(),
                entity.getSecret(),
                entity.getCreatedAt()
        );
    }
}
