package com.pizzanet.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
    
    @Bean
    public WebClient menuServiceClient(WebClient.Builder builder) {
        return builder.baseUrl("http://menu-service:8081").build();
    }
    
    @Bean
    public WebClient deliveryServiceClient(WebClient.Builder builder) {
        return builder.baseUrl("http://delivery-service:8083").build();
    }
}
