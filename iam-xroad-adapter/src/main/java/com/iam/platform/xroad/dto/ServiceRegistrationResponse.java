package com.iam.platform.xroad.dto;

import java.time.Instant;
import java.util.UUID;

public record ServiceRegistrationResponse(
        UUID id,
        String serviceCode,
        String serviceVersion,
        String targetService,
        String targetPath,
        String description,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {}
