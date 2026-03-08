package com.iam.platform.governance.dto;

import com.iam.platform.governance.enums.PolicySeverity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SodPolicyResponse(
        UUID id,
        String name,
        List<List<String>> conflictingRoles,
        PolicySeverity severity,
        boolean enabled,
        Instant createdAt
) {}
