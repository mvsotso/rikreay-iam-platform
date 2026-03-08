package com.iam.platform.notification.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ScheduledReportResponse(
        UUID id,
        String name,
        String cronExpression,
        UUID templateId,
        List<String> recipientList,
        boolean enabled,
        Instant lastRunAt,
        Instant nextRunAt,
        Instant createdAt,
        Instant updatedAt
) {}
