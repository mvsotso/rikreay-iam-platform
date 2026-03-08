package com.iam.platform.governance.dto;

import com.iam.platform.governance.enums.ReviewDecision;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID campaignId,
        String userId,
        String reviewerId,
        ReviewDecision decision,
        String comments,
        Instant reviewedAt,
        Instant createdAt
) {}
