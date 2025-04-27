package com.example.waitingservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(EmbeddedRedisConfig.class)
class WaitingServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
