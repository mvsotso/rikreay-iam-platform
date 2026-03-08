package com.iam.platform.admin.dto;

import com.iam.platform.common.enums.MemberClass;

import java.time.Instant;
import java.util.UUID;

public record SectorAdminResponse(
        UUID id,
        UUID naturalPersonId,
        MemberClass memberClass,
        String assignedByUserId,
        Instant validFrom,
        Instant validUntil,
        String status,
        Instant createdAt
) {}
