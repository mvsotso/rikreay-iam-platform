package com.iam.platform.xroad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AclEntryRequest(
        @NotBlank(message = "Consumer identifier is required (e.g., KH/GOV/MOF/BUDGET-SYSTEM)")
        String consumerIdentifier,

        @NotNull(message = "Service registration ID is required")
        UUID serviceRegistrationId,

        String description
) {}
