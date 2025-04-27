package com.example.waitingservice.application.usecase;

import com.example.waitingservice.application.QueueManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TouchWaitingUserService implements TouchWaitingUserUseCase {
    private final QueueManager queueManager;

    @Override
    public Mono<?> touch(String queueName, String userId) {
        return queueManager.touch(queueName, userId);
    }
}
