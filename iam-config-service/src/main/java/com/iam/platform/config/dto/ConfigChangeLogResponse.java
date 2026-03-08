package com.iam.platform.config.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ConfigChangeLogResponse(
        UUID id,
        Long version,
        String application,
        String profile,
        Map<String, Object> changesJson,
        String author,
        String changeType,
        Instant createdAt
) {}
