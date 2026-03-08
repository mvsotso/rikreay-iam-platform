package com.iam.platform.monitoring.dto;

import java.time.Instant;
import java.util.List;

public record AggregatedHealthResponse(
        String overallStatus,
        int totalServices,
        int healthyServices,
        int unhealthyServices,
        List<ServiceHealthDto> services,
        Instant timestamp
) {}
