package com.example.waitingservice.adapter.web.controller;

import com.example.waitingservice.application.JwtTokenProvider;
import com.example.waitingservice.application.usecase.QueryWaitingPositionUseCase;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.util.Map;

@Controller
public class WaitingRoomController {
    private final JwtTokenProvider jwtTokenProvider;
    private final QueryWaitingPositionUseCase queryWaitingPositionUseCase;

    public WaitingRoomController(JwtTokenProvider jwtTokenProvider, QueryWaitingPositionUseCase queryWaitingPositionUseCase) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.queryWaitingPositionUseCase = queryWaitingPositionUseCase;
    }


    @GetMapping("/waiting")
    public Mono<Rendering> waitingPage(@RequestParam String token) {
        Map<String, ?> claims = jwtTokenProvider.getClaims(token);
        Object queueName = claims.get("queueName");
        Object userId = claims.get("userId");

        return queryWaitingPositionUseCase.getPosition((String) queueName, (String) userId)
                .flatMap(it -> Mono.just(Rendering.view("waiting.html")
                        .modelAttribute("claims", claims)
                        .modelAttribute("id", userId)
                        .modelAttribute("queueName", queueName)
                        .modelAttribute("token", token)
                        .modelAttribute("position", it.position())
                        .build()));
    }

    @GetMapping
    @ResponseBody
    public Mono<String> hello() {
        return Mono.just("hello updated");
    }
}
