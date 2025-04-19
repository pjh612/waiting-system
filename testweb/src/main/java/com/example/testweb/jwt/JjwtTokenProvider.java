package com.example.testweb.jwt;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;


public class JjwtTokenProvider implements JwtTokenProvider {
    private final SecretKey key;

    public JjwtTokenProvider(String secretKey) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    public String generateToken(Map<String, Object> data, long expirationTimeMillis) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject("QueueToken")
                .claims(data)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationTimeMillis))
                .signWith(key)
                .compact();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String getClaim(String token, String claimKey) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload()
                .get(claimKey, String.class);
    }

    @Override
    public Map<String,?> getClaims(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
