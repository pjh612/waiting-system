package com.example.waitingservice.adapter.metric;

import com.alert.sse.TagBasedAlertSessionRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CustomMetricConfig {
    @Bean
    public Gauge activeSseSessionsGauge(MeterRegistry registry, TagBasedAlertSessionRepository<?> repository) {
        return Gauge.builder("sse.active.sessions", repository, repo -> (double) repository.size())
                .description("현재 활성화된 SSE 세션(Emitter)의 총 개수")
                .tag("module", "alert-sse")
                .register(registry);
    }
}
