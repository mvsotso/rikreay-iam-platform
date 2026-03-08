package com.iam.platform.governance.dto;

import com.iam.platform.governance.enums.ConsentMethod;
import com.iam.platform.governance.enums.DataSubjectType;
import com.iam.platform.governance.enums.LegalBasis;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConsentRequest(
        @NotNull DataSubjectType dataSubjectType,
        @NotNull UUID dataSubjectId,
        @NotBlank String purpose,
        @NotNull LegalBasis legalBasis,
        @NotNull ConsentMethod consentMethod,
        Instant expiresAt,
        List<String> dataCategories,
        boolean thirdPartySharing,
        boolean crossBorderTransfer
) {}
