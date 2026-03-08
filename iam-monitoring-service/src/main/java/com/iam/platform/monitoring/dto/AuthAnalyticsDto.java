package com.iam.platform.monitoring.dto;

import java.util.Map;

public record AuthAnalyticsDto(
        long totalLogins,
        long successfulLogins,
        long failedLogins,
        double successRate,
        double mfaAdoptionRate,
        Map<String, Long> loginsByRealm,
        Map<String, Long> failedLoginsByReason,
        Map<String, Long> loginsByHour
) {}
