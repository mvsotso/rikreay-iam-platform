package com.iam.platform.config.dto;

import java.time.Instant;
import java.util.UUID;

public record FeatureFlagResponse(
        UUID id,
        String flagKey,
        String flagValue,
        String description,
        boolean enabled,
        String environment,
        Instant createdAt,
        Instant updatedAt
) {}
