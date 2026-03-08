package com.iam.platform.tenant.dto;

import com.iam.platform.common.enums.EntityType;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record UpdateTenantRequest(
        @Size(max = 255, message = "Tenant name must not exceed 255 characters")
        String tenantName,

        String description,

        EntityType entityType,

        String registrationNumber,

        String tin,

        String memberCode,

        String xroadSubsystem,

        Map<String, Object> settings
) {}
