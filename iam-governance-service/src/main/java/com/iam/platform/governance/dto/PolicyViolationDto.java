package com.iam.platform.governance.dto;

import com.iam.platform.governance.enums.PolicySeverity;

import java.util.List;
import java.util.UUID;

public record PolicyViolationDto(
        UUID policyId,
        String policyName,
        PolicySeverity severity,
        String userId,
        List<String> conflictingRoles
) {}
