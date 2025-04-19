package com.example.testweb.controller;

import com.example.testweb.client.WaitingClient;
import com.example.testweb.dto.RegisterWaitingResponse;
import com.example.testweb.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

@Controller
public class TestController {
    private final WaitingClient waitingClient;
    private final JwtTokenProvider jwtTokenProvider;

    public TestController(WaitingClient waitingClient, JwtTokenProvider jwtTokenProvider) {
        this.waitingClient = waitingClient;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping("/test")
    public String testPage(HttpServletRequest request, @AuthenticationPrincipal User user) {
        Cookie cookie = Arrays.stream(request.getCookies()).filter(it -> it.getName().equalsIgnoreCase("abc")).findFirst()
                .orElse(null);

        String token = cookie != null ? cookie.getValue() : null;
        if (cookie != null && jwtTokenProvider.validateToken(token)) {
            return "target";
        }

        RegisterWaitingResponse register = waitingClient.register(user.getUsername());

        String waitingPageUrl = "http://localhost:8081/waiting?token=" + register.token();
        return "redirect:" + waitingPageUrl;
    }

    @GetMapping("/entering")
    public String enteringPage(@RequestParam("token") String token, HttpServletResponse response) {
        Map<String, ?> claims = jwtTokenProvider.getClaims(token);
        String userId = claims.get("userId").toString();
        String queueName = claims.get("queueName").toString();

        String tk = jwtTokenProvider.generateToken(Map.of("userId", userId, "queueName", queueName), Duration.ofMinutes(30).toMillis());

        response.addCookie(new Cookie("abc",tk));

        return "redirect:" + "http://localhost:8080/test";
    }
}
