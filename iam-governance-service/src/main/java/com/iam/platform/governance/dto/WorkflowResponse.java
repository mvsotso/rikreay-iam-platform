package com.iam.platform.governance.dto;

import com.iam.platform.governance.enums.WorkflowType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record WorkflowResponse(
        UUID id,
        String name,
        WorkflowType type,
        List<Map<String, Object>> steps,
        List<String> approvalChain,
        boolean enabled,
        Instant createdAt
) {}
