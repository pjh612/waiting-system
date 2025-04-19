package com.example.waitingservice.application;

import java.util.Map;

public interface JwtTokenProvider {
    String generateToken(Map<String, Object> data, long expirationTimeMillis);


    String generateToken(Map<String, Object> data, long expirationTimeMillis, String secret);

    boolean validateToken(String token);


    String getClaim(String token, String claimKey);

    Map<String,?> getClaims(String token);
}
