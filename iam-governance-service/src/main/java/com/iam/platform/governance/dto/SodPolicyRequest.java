package com.iam.platform.governance.dto;

import com.iam.platform.governance.enums.PolicySeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SodPolicyRequest(
        @NotBlank String name,
        @NotNull List<List<String>> conflictingRoles,
        @NotNull PolicySeverity severity,
        boolean enabled
) {}
