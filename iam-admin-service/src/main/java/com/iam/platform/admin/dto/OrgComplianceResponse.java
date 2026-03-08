package com.iam.platform.admin.dto;

public record OrgComplianceResponse(
        long pendingReviews,
        double riskScoreAvg,
        long policyViolations,
        long consentStats,
        long verificationStats
) {}
