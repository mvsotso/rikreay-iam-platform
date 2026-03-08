package com.iam.platform.admin.dto;

import java.util.Map;

public record UsageResponse(
        Map<String, Long> apiCallsByDay,
        Map<String, Long> loginsByDay,
        Map<String, Long> xroadTransactionsByDay,
        long activeUsersThisMonth
) {}
