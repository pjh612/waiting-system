package com.example.waitingservice;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

import java.io.IOException;

@TestConfiguration
public class EmbeddedRedisConfig {

    private final RedisServer redisServer;

    public EmbeddedRedisConfig() throws IOException {
        this.redisServer = new RedisServer(63790);
    }

    @PostConstruct
    public void start() throws IOException {
        // 2️. EmbeddedRedis 생성 후 → start() 호출 (Redis 서버 시작)
        this.redisServer.start();
    }

    // 3. 테스트 실행

    @PreDestroy
    public void stop() throws IOException {
        // 4. EmbeddedRedis 소멸 전 → stop() 호출 (Redis 서버 종료)
        this.redisServer.stop();
    }
}

