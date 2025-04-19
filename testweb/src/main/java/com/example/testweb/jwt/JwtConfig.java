package com.example.testweb.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Bean
    public JwtTokenProvider jwtTokenProvider(@Value("${secret}") String secret) {
        return new JjwtTokenProvider(secret);
    }
}
