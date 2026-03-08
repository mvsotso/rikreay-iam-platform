package com.iam.platform.admin.dto;

import com.iam.platform.common.enums.MemberClass;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record SectorAdminRequest(
        @NotNull(message = "Natural person ID is required")
        UUID naturalPersonId,

        @NotNull(message = "Member class is required")
        MemberClass memberClass,

        @NotNull(message = "Valid from date is required")
        Instant validFrom,

        Instant validUntil
) {}
