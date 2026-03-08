package com.iam.platform.monitoring.dto;

import java.util.Map;

public record XRoadMetricsDto(
        long totalExchanges,
        long successfulExchanges,
        long failedExchanges,
        double averageLatencyMs,
        Map<String, Long> exchangesByMemberClass,
        Map<String, Long> exchangesByService,
        Map<String, Double> latencyByService
) {}
