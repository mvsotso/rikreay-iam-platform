package com.iam.platform.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ScheduledReportRequest(
        @NotBlank(message = "Report name is required")
        String name,

        @NotBlank(message = "Cron expression is required")
        String cronExpression,

        @NotNull(message = "Template ID is required")
        UUID templateId,

        @NotEmpty(message = "Recipient list cannot be empty")
        List<String> recipientList,

        boolean enabled
) {}
