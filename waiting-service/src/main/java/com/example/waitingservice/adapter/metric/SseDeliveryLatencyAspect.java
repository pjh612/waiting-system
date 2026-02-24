package com.example.waitingservice.adapter.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@RequiredArgsConstructor
public class SseDeliveryLatencyAspect {

    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    @Around("execution(* com.example.waitingservice.application.usecase.SubscribeWaitingResultUseCase.subscribe(..))")
    @SuppressWarnings("unchecked")
    public Object measureDeliveryLatency(ProceedingJoinPoint joinPoint) throws Throwable {
        String queueName = (String) joinPoint.getArgs()[0];
        Flux<ServerSentEvent<Object>> flux = (Flux<ServerSentEvent<Object>>) joinPoint.proceed();
        return flux.doOnNext(event -> recordLatencyIfPresent(event, queueName));
    }

    private void recordLatencyIfPresent(ServerSentEvent<Object> event, String queueName) {
        Long sentAt = extractTimestamp(event.data());
        if (sentAt == null) {
            return;
        }
        long latencyMs = System.currentTimeMillis() - sentAt;
        Timer.builder("waiting.allow.delivery.latency")
                .description("allow 명령 후 SSE 최종 전달까지의 지연 시간 (kafka → redis pub/sub → sse)")
                .tag("queue", queueName)
                .register(meterRegistry)
                .record(latencyMs, TimeUnit.MILLISECONDS);
    }

    private Long extractTimestamp(Object data) {
        try {
            Map<String, Object> payload = objectMapper.convertValue(data, new TypeReference<>() {
            });
            Object ts = payload.get("timestamp");
            if (ts instanceof Number n) {
                return n.longValue();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
