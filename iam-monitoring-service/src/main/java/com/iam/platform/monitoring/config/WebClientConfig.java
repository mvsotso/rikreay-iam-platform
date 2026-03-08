package com.iam.platform.monitoring.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient healthCheckWebClient() {
        return WebClient.builder()
                .build();
    }

    @Bean
    public WebClient prometheusWebClient(@Value("${app.services.prometheus-url}") String prometheusUrl) {
        return WebClient.builder()
                .baseUrl(prometheusUrl)
                .build();
    }
}
