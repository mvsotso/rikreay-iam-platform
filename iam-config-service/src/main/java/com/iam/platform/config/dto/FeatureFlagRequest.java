package com.iam.platform.config.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeatureFlagRequest(
        @NotBlank(message = "Flag key is required")
        @Size(max = 255, message = "Flag key must be at most 255 characters")
        String flagKey,

        @Size(max = 1000, message = "Flag value must be at most 1000 characters")
        String flagValue,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        boolean enabled,

        @NotBlank(message = "Environment is required")
        @Size(max = 50, message = "Environment must be at most 50 characters")
        String environment
) {}
