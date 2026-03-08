package com.iam.platform.developer.service;

import com.iam.platform.developer.dto.WebhookConfigRequest;
import com.iam.platform.developer.dto.WebhookConfigResponse;
import com.iam.platform.developer.dto.WebhookDeliveryResponse;
import com.iam.platform.developer.entity.WebhookConfig;
import com.iam.platform.developer.repository.WebhookConfigRepository;
import com.iam.platform.developer.repository.WebhookDeliveryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookConfigRepository webhookRepository;
    private final WebhookDeliveryLogRepository deliveryLogRepository;
    private final AuditService auditService;

    @Transactional
    public WebhookConfigResponse createWebhook(WebhookConfigRequest request, String username) {
        WebhookConfig config = WebhookConfig.builder()
                .appId(request.appId())
                .eventType(request.eventType())
                .targetUrl(request.targetUrl())
                .secretHash(hashSecret(request.secret()))
                .enabled(true)
                .build();

        config = webhookRepository.save(config);

        auditService.logDeveloperAction(username, "WEBHOOK_CREATED", config.getId().toString(),
                true, Map.of("eventType", request.eventType(), "targetUrl", request.targetUrl()));

        return toResponse(config);
    }

    @Transactional(readOnly = true)
    public WebhookConfigResponse getWebhook(UUID id) {
        return toResponse(webhookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Webhook not found: " + id)));
    }

    @Transactional(readOnly = true)
    public List<WebhookConfigResponse> getWebhooksByApp(UUID appId) {
        return webhookRepository.findByAppId(appId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WebhookConfigResponse updateWebhook(UUID id, WebhookConfigRequest request, String username) {
        WebhookConfig config = webhookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Webhook not found: " + id));

        config.setEventType(request.eventType());
        config.setTargetUrl(request.targetUrl());
        if (request.secret() != null) {
            config.setSecretHash(hashSecret(request.secret()));
        }

        config = webhookRepository.save(config);

        auditService.logDeveloperAction(username, "WEBHOOK_UPDATED", config.getId().toString(),
                true, Map.of());

        return toResponse(config);
    }

    @Transactional
    public void deleteWebhook(UUID id, String username) {
        WebhookConfig config = webhookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Webhook not found: " + id));
        config.softDelete();
        webhookRepository.save(config);
        auditService.logDeveloperAction(username, "WEBHOOK_DELETED", id.toString(), true, Map.of());
    }

    @Transactional(readOnly = true)
    public Page<WebhookDeliveryResponse> getDeliveryLogs(UUID webhookId, Pageable pageable) {
        return deliveryLogRepository.findByWebhookIdOrderBySentAtDesc(webhookId, pageable)
                .map(log -> new WebhookDeliveryResponse(
                        log.getId(), log.getWebhookId(), log.getEventType(),
                        log.getHttpStatus(), log.getResponseTime(), log.getError(), log.getSentAt()
                ));
    }

    private String hashSecret(String secret) {
        if (secret == null || secret.isBlank()) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("Failed to hash secret", e);
            return null;
        }
    }

    private WebhookConfigResponse toResponse(WebhookConfig config) {
        return new WebhookConfigResponse(
                config.getId(),
                config.getAppId(),
                config.getEventType(),
                config.getTargetUrl(),
                config.getEnabled(),
                config.getCreatedAt()
        );
    }
}
