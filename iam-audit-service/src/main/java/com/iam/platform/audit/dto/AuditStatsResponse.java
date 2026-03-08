package com.iam.platform.audit.dto;

import java.util.Map;

public record AuditStatsResponse(
        long totalEvents,
        long successCount,
        long failureCount,
        Map<String, Long> eventsByType,
        Map<String, Long> successByType
) {}
