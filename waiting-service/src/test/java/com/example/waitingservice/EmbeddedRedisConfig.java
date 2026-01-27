package com.example.waitingservice;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.util.TestSocketUtils;
import redis.embedded.RedisServer;

import java.io.IOException;

@TestConfiguration
public class EmbeddedRedisConfig {

    private static RedisServer redisServer;
    private static int PORT;
    private static final String HOST = "localhost";

    static {
        PORT = TestSocketUtils.findAvailableTcpPort();
        try {
            redisServer = new RedisServer(PORT);
            redisServer.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (redisServer != null) {
                    try {
                        redisServer.stop();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }));
        } catch (IOException e) {
            throw new RuntimeException("Embedded Redis 시작 실패", e);
        }
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration(HOST, PORT));
    }
}