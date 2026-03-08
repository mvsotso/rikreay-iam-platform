package com.iam.platform.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
public class SmsService {

    private final boolean enabled;
    private final WebClient webClient;

    public SmsService(
            @Value("${sms.gateway.enabled:false}") boolean enabled,
            @Value("${sms.gateway.url:http://localhost:9999/sms}") String gatewayUrl,
            @Value("${sms.gateway.api-key:disabled}") String apiKey,
            WebClient.Builder webClientBuilder) {
        this.enabled = enabled;
        this.webClient = webClientBuilder
                .baseUrl(gatewayUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public void sendSms(String phoneNumber, String message) {
        if (!enabled) {
            log.warn("SMS gateway is disabled. Would have sent to: {}, message: {}", phoneNumber, message);
            return;
        }

        try {
            webClient.post()
                    .bodyValue(Map.of("to", phoneNumber, "message", message))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("SMS sent to: {}", phoneNumber);
        } catch (Exception e) {
            log.error("Failed to send SMS to: {}", phoneNumber, e);
            throw new RuntimeException("SMS delivery failed: " + e.getMessage(), e);
        }
    }
}
