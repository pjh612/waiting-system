package com.example.waitingservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserPosition {
    private String userId;
    private long position;
}
