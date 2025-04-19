package com.example.waitingservice.adapter.security;

import com.example.waitingservice.domain.model.WaitingQueue;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.ArrayList;

@Getter
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {
    private final String apiKey;
    private final WaitingQueue queue;

    public ApiKeyAuthenticationToken(String apiKey, WaitingQueue queue, ArrayList<GrantedAuthority> grantedAuthorities) {
        super(grantedAuthorities);
        this.apiKey = apiKey;
        this.queue = queue;
        setAuthenticated(queue != null);
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return queue;
    }

}