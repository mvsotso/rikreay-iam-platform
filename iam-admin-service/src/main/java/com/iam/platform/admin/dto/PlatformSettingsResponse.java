package com.iam.platform.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record PlatformSettingsResponse(
        UUID id,
        String settingKey,
        String settingValue,
        String category,
        String description,
        String updatedBy,
        Instant updatedAt
) {}
