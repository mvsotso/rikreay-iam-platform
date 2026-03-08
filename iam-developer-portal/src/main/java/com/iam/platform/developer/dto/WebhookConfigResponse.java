package com.iam.platform.developer.dto;

import java.time.Instant;
import java.util.UUID;

public record WebhookConfigResponse(
    UUID id,
    UUID appId,
    String eventType,
    String targetUrl,
    Boolean enabled,
    Instant createdAt
) {}
