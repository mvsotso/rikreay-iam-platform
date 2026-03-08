package com.iam.platform.xroad.dto;

import java.time.Instant;
import java.util.UUID;

public record AclEntryResponse(
        UUID id,
        String consumerIdentifier,
        UUID serviceRegistrationId,
        String serviceCode,
        boolean allowed,
        String description,
        Instant createdAt
) {}
