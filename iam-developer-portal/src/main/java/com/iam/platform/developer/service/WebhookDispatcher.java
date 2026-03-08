package com.iam.platform.developer.service;

import com.iam.platform.developer.config.DeveloperProperties;
import com.iam.platform.developer.entity.WebhookConfig;
import com.iam.platform.developer.entity.WebhookDeliveryLog;
import com.iam.platform.developer.repository.WebhookDeliveryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDispatcher {

    private final WebClient webhookWebClient;
    private final WebhookDeliveryLogRepository deliveryLogRepository;
    private final DeveloperProperties properties;

    @Async
    public void dispatch(WebhookConfig webhook, String eventType, Map<String, Object> payload) {
        String deliveryId = UUID.randomUUID().toString();
        String body = serializePayload(payload);
        String signature = computeHmacSignature(body, webhook.getSecretHash());

        long[] delays = parseRetryDelays();
        int maxRetries = properties.getWebhook().getMaxRetries();

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (attempt > 0) {
                try {
                    long delay = attempt <= delays.length ? delays[attempt - 1] : delays[delays.length - 1];
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            long startTime = System.currentTimeMillis();
            try {
                Integer statusCode = webhookWebClient.post()
                        .uri(webhook.getTargetUrl())
                        .header("Content-Type", "application/json")
                        .header("X-Webhook-Signature", signature)
                        .header("X-Webhook-Delivery-Id", deliveryId)
                        .header("X-Webhook-Event", eventType)
                        .bodyValue(body)
                        .retrieve()
                        .toBodilessEntity()
                        .timeout(Duration.ofSeconds(10))
                        .map(response -> response.getStatusCode().value())
                        .block();

                long responseTime = System.currentTimeMillis() - startTime;

                saveDeliveryLog(webhook.getId(), eventType, statusCode, responseTime, null);

                if (statusCode != null && statusCode >= 200 && statusCode < 300) {
                    log.debug("Webhook delivered: deliveryId={}, target={}, status={}",
                            deliveryId, webhook.getTargetUrl(), statusCode);
                    return;
                }

                log.warn("Webhook delivery failed: deliveryId={}, status={}, attempt={}/{}",
                        deliveryId, statusCode, attempt + 1, maxRetries + 1);

            } catch (Exception e) {
                long responseTime = System.currentTimeMillis() - startTime;
                saveDeliveryLog(webhook.getId(), eventType, null, responseTime, e.getMessage());
                log.warn("Webhook delivery error: deliveryId={}, attempt={}/{}, error={}",
                        deliveryId, attempt + 1, maxRetries + 1, e.getMessage());
            }
        }

        log.error("Webhook delivery exhausted retries: deliveryId={}, target={}",
                deliveryId, webhook.getTargetUrl());
    }

    private String computeHmacSignature(String payload, String secretHash) {
        if (secretHash == null || secretHash.isBlank()) {
            return "sha256=unsigned";
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretHash.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(hmac);
        } catch (Exception e) {
            log.error("Failed to compute HMAC signature", e);
            return "sha256=error";
        }
    }

    private String serializePayload(Map<String, Object> payload) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.findAndRegisterModules();
            return mapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Failed to serialize webhook payload", e);
            return "{}";
        }
    }

    private void saveDeliveryLog(UUID webhookId, String eventType, Integer httpStatus,
                                  Long responseTime, String error) {
        try {
            WebhookDeliveryLog logEntry = WebhookDeliveryLog.builder()
                    .webhookId(webhookId)
                    .eventType(eventType)
                    .httpStatus(httpStatus)
                    .responseTime(responseTime)
                    .error(error)
                    .sentAt(Instant.now())
                    .build();
            deliveryLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to save delivery log", e);
        }
    }

    private long[] parseRetryDelays() {
        try {
            String[] parts = properties.getWebhook().getRetryDelays().split(",");
            long[] delays = new long[parts.length];
            for (int i = 0; i < parts.length; i++) {
                delays[i] = Long.parseLong(parts[i].trim());
            }
            return delays;
        } catch (Exception e) {
            return new long[]{1000, 5000, 25000};
        }
    }
}
