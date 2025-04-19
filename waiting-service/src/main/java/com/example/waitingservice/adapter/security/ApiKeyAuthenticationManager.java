package com.example.waitingservice.adapter.security;

import com.example.waitingservice.application.dto.WaitingQueueResponse;
import com.example.waitingservice.application.usecase.QueryWaitingQueueUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationManager implements ReactiveAuthenticationManager {
    private final QueryWaitingQueueUseCase queryWaitingQueueUseCase;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) throws AuthenticationException {
        String apiKey = (String) authentication.getCredentials();
        ArrayList<GrantedAuthority> authorities = new ArrayList<>();

        return queryWaitingQueueUseCase.getWaitingQueue(apiKey)
                .map(WaitingQueueResponse::toWaitingQueue)
                .flatMap(waitingQueue -> {
                    if (apiKey == null) {
                        return Mono.error(new RuntimeException("Invalid API Key"));
                    }
                    authorities.add(new SimpleGrantedAuthority("WAITING_QUEUE"));
                    return Mono.just(new ApiKeyAuthenticationToken(apiKey, waitingQueue, authorities));
                });
    }
}
