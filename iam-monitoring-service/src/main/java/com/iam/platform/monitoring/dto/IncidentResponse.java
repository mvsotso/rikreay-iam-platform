package com.iam.platform.monitoring.dto;

import com.iam.platform.monitoring.enums.IncidentStatus;
import com.iam.platform.monitoring.enums.Severity;

import java.time.Instant;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        String title,
        Severity severity,
        IncidentStatus status,
        String description,
        String serviceAffected,
        String assignedTo,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt
) {}
