package com.iam.platform.governance.dto;

import java.time.Instant;
import java.util.Map;

public record ComplianceReportDto(
        String reportType,
        Instant generatedAt,
        long totalUsers,
        long usersWithRiskScore,
        long highRiskUsers,
        long activePolicies,
        long policyViolations,
        long activeCampaigns,
        long pendingReviews,
        long totalConsents,
        long activeConsents,
        Map<String, Object> details
) {}
