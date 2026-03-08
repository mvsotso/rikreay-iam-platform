package com.iam.platform.governance.dto;

import com.iam.platform.governance.enums.ReviewDecision;
import jakarta.validation.constraints.NotNull;

public record ReviewRequest(
        @NotNull ReviewDecision decision,
        String comments
) {}
