package com.iam.platform.governance.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RiskScoreResponse(
        UUID id,
        String userId,
        int score,
        Map<String, Object> factors,
        Instant calculatedAt
) {}
