package com.example.waitingservice.adapter.cache.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class RedisLuaConfig {
    @Bean
    public DefaultRedisScript<Long> registerWaitingScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/lua/register_waiting.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
