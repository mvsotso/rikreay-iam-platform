package com.iam.platform.admin.dto;

import java.util.Map;

public record PlatformDashboardResponse(
        long totalOrgs,
        long totalUsers,
        Map<String, Long> orgsBySector,
        Map<String, Long> usersBySector,
        long totalActiveSessions,
        long totalApiCalls
) {}
