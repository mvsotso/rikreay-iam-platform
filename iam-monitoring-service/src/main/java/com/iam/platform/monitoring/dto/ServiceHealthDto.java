package com.iam.platform.monitoring.dto;

import java.time.Instant;

public record ServiceHealthDto(
        String serviceName,
        String url,
        String status,
        long responseTimeMs,
        Instant checkedAt,
        String details
) {}
