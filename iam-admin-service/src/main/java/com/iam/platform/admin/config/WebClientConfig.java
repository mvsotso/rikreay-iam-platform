package com.iam.platform.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient auditServiceWebClient(
            @Value("${app.services.audit-service-url:http://localhost:8084}") String auditServiceUrl) {
        return WebClient.builder()
                .baseUrl(auditServiceUrl)
                .build();
    }
}
