package com.iam.platform.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
public class TelegramService {

    private final boolean enabled;
    private final String botToken;
    private final WebClient webClient;

    public TelegramService(
            @Value("${telegram.bot.enabled:false}") boolean enabled,
            @Value("${telegram.bot.token:disabled}") String botToken,
            WebClient.Builder webClientBuilder) {
        this.enabled = enabled;
        this.botToken = botToken;
        this.webClient = webClientBuilder
                .baseUrl("https://api.telegram.org")
                .build();
    }

    public void sendMessage(String chatId, String message) {
        if (!enabled || "disabled".equals(botToken)) {
            log.warn("Telegram bot is disabled. Would have sent to chatId: {}, message: {}", chatId, message);
            return;
        }

        try {
            webClient.post()
                    .uri("/bot{token}/sendMessage", botToken)
                    .bodyValue(Map.of(
                            "chat_id", chatId,
                            "text", message,
                            "parse_mode", "HTML"
                    ))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("Telegram message sent to chatId: {}", chatId);
        } catch (Exception e) {
            log.error("Failed to send Telegram message to chatId: {}", chatId, e);
            throw new RuntimeException("Telegram delivery failed: " + e.getMessage(), e);
        }
    }
}
