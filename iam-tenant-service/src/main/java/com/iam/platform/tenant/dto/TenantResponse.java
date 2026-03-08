package com.iam.platform.tenant.dto;

import com.iam.platform.common.enums.EntityType;
import com.iam.platform.common.enums.MemberClass;
import com.iam.platform.tenant.enums.TenantStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String tenantName,
        String realmName,
        String description,
        MemberClass memberClass,
        EntityType entityType,
        String registrationNumber,
        String tin,
        String memberCode,
        String xroadSubsystem,
        TenantStatus status,
        String adminEmail,
        String adminUsername,
        Map<String, Object> settings,
        Instant createdAt,
        Instant updatedAt
) {}
