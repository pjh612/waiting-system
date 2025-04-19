package com.example.testweb.jwt;

import java.util.Map;

public interface JwtTokenProvider {
    String generateToken(Map<String, Object> data, long expirationTimeMillis);

    boolean validateToken(String token);

    String getClaim(String token, String claimKey);

    Map<String, ?> getClaims(String token);
}
