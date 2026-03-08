package com.iam.platform.developer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record WebhookConfigRequest(
    @NotNull UUID appId,
    @NotBlank String eventType,
    @NotBlank String targetUrl,
    String secret
) {}
