package com.example.waitingservice.adapter.security;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class ApiKeyAuthenticationFilter implements ServerAuthenticationConverter {
    private final ReactiveAuthenticationManager authenticationManager;

    public ApiKeyAuthenticationFilter(ReactiveAuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest()
                        .getHeaders()
                        .getFirst("Authorization"))
                .map(apiKey -> new ApiKeyAuthenticationToken(apiKey, null, null))
                .flatMap(authenticationManager::authenticate);
    }
}
