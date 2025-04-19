package com.example.waitingservice.adapter.persistence.r2dbc.entity;

import com.example.waitingservice.domain.model.WaitingQueue;

public class WaitingQueueMapper {

    public static WaitingQueueEntity toEntity(WaitingQueue domain) {
        if (domain == null) {
            return null;
        }
        return new WaitingQueueEntity(
                domain.getId(),
                domain.getQueueId(),
                domain.getClientId(),
                domain.getName(),
                domain.getRedirectUrl(),
                domain.getSize(),
                domain.getApiKey(),
                domain.getSecret(),
                domain.getCreatedAt()
        );
    }

    public static WaitingQueue toDomain(WaitingQueueEntity entity) {
        if (entity == null) {
            return null;
        }
        return new WaitingQueue(
                entity.getId(),
                entity.getQueueId(),
                entity.getClientId(),
                entity.getName(),
                entity.getRedirectUrl(),
                entity.getSize(),
                entity.getApiKey(),
                entity.getSecret(),
                entity.getCreatedAt()
        );
    }
}
