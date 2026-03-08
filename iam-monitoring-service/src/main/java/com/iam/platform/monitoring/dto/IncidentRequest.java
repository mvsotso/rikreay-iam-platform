package com.iam.platform.monitoring.dto;

import com.iam.platform.monitoring.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IncidentRequest(
        @NotBlank String title,
        @NotNull Severity severity,
        String description,
        String serviceAffected,
        String assignedTo
) {}
