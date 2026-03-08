package com.iam.platform.admin.dto;

public record OrgDashboardResponse(
        long userCount,
        long activeSessionCount,
        long representativeCount,
        long pendingVerificationsCount,
        long apiCallsThisMonth,
        long xroadTransactionsThisMonth,
        long recentEventsCount,
        double mfaAdoptionRate
) {}
