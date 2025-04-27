package com.example.waitingservice.application.usecase;

import reactor.core.publisher.Mono;

public interface TouchWaitingUserUseCase {
    Mono<?> touch(String queueName, String userId);
}
