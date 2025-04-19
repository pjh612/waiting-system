package com.example.waitingservice.adapter.persistence.r2dbc.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("waiting_queue")
public class WaitingQueueEntity {

    @Id
    private Long id;
    private UUID queueId;
    private UUID clientId;
    private String name;
    private String redirectUrl;
    private Long size;
    private String apiKey;
    private String secret;
    private Instant createdAt;
}
