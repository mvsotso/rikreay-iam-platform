package com.iam.platform.tenant.dto;

import com.iam.platform.common.enums.EntityType;
import com.iam.platform.common.enums.MemberClass;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreateTenantRequest(
        @NotBlank(message = "Tenant name is required")
        @Size(max = 255, message = "Tenant name must not exceed 255 characters")
        String tenantName,

        @NotBlank(message = "Realm name is required")
        @Size(max = 255, message = "Realm name must not exceed 255 characters")
        String realmName,

        String description,

        @NotNull(message = "Member class is required")
        MemberClass memberClass,

        EntityType entityType,

        String registrationNumber,

        String tin,

        String memberCode,

        String xroadSubsystem,

        @NotBlank(message = "Admin email is required")
        @Email(message = "Admin email must be valid")
        String adminEmail,

        @NotBlank(message = "Admin username is required")
        @Size(min = 3, max = 100, message = "Admin username must be between 3 and 100 characters")
        String adminUsername,

        @NotBlank(message = "Admin temporary password is required")
        @Size(min = 8, message = "Admin password must be at least 8 characters")
        String adminTempPassword,

        Map<String, Object> settings
) {}
