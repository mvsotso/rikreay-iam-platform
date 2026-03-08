package com.iam.platform.developer.dto;

import java.time.Instant;
import java.util.UUID;

public record WebhookDeliveryResponse(
    UUID id,
    UUID webhookId,
    String eventType,
    Integer httpStatus,
    Long responseTime,
    String error,
    Instant sentAt
) {}
