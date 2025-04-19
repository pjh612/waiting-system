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
@Table("client")
public class ClientEntity {
    @Id
    private Long id;
    private UUID clientId;
    private String name;
    private String secret;
    private Instant createdAt;
}
