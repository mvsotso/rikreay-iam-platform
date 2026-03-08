package com.iam.platform.governance.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.Map;

public record CampaignRequest(
        @NotBlank String name,
        String description,
        Instant startDate,
        Instant endDate,
        Map<String, Object> scope
) {}
