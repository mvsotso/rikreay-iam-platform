package com.iam.platform.admin.dto;

public record SectorDashboardResponse(
        String memberClass,
        long orgCount,
        long totalUsers,
        long activeOrgs,
        long suspendedOrgs,
        long aggregateApiCalls,
        long aggregateXroadTransactions
) {}
