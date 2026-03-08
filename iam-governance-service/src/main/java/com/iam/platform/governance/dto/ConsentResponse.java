package com.iam.platform.governance.dto;

import com.iam.platform.governance.enums.ConsentMethod;
import com.iam.platform.governance.enums.DataSubjectType;
import com.iam.platform.governance.enums.LegalBasis;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConsentResponse(
        UUID id,
        DataSubjectType dataSubjectType,
        UUID dataSubjectId,
        String purpose,
        LegalBasis legalBasis,
        boolean consentGiven,
        Instant consentTimestamp,
        ConsentMethod consentMethod,
        Instant withdrawnAt,
        Instant expiresAt,
        List<String> dataCategories,
        boolean thirdPartySharing,
        boolean crossBorderTransfer,
        Instant createdAt
) {}
