package com.iam.platform.governance.dto;

import com.iam.platform.governance.enums.CampaignStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CampaignResponse(
        UUID id,
        String name,
        String description,
        CampaignStatus status,
        Instant startDate,
        Instant endDate,
        Map<String, Object> scope,
        String createdBy,
        long totalReviews,
        long pendingReviews,
        Instant createdAt
) {}
