package com.iam.platform.xroad.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final XRoadProperties xroadProperties;

    @Bean
    public WebClient xroadWebClient() {
        return WebClient.builder()
                .baseUrl(xroadProperties.getRouting().getCoreServiceUrl())
                .build();
    }
}
