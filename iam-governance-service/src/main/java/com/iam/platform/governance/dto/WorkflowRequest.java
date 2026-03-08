package com.iam.platform.governance.dto;

import com.iam.platform.governance.enums.WorkflowType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record WorkflowRequest(
        @NotBlank String name,
        @NotNull WorkflowType type,
        List<Map<String, Object>> steps,
        List<String> approvalChain,
        boolean enabled
) {}
