package com.example.testweb.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Value("${waiting-service.url}")
    private String waitingServiceUrl;


    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.baseUrl(waitingServiceUrl).build();
    }
}
