package com.iam.platform.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record PlatformSettingsRequest(
        @NotBlank(message = "Setting key is required")
        String settingKey,

        String settingValue,

        @NotBlank(message = "Category is required")
        String category,

        String description
) {}
