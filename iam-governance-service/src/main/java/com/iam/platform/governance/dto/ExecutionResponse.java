package com.iam.platform.governance.dto;

import com.iam.platform.governance.enums.ExecutionStatus;

import java.time.Instant;
import java.util.UUID;

public record ExecutionResponse(
        UUID id,
        UUID workflowId,
        String targetUserId,
        int currentStep,
        ExecutionStatus status,
        String initiatedBy,
        Instant createdAt
) {}
