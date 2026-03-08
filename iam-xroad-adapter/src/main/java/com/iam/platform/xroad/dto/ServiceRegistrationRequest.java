package com.iam.platform.xroad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceRegistrationRequest(
        @NotBlank(message = "Service code is required")
        @Size(max = 255, message = "Service code must not exceed 255 characters")
        String serviceCode,

        String serviceVersion,

        @NotBlank(message = "Target service is required")
        String targetService,

        @NotBlank(message = "Target path is required")
        @Size(max = 500, message = "Target path must not exceed 500 characters")
        String targetPath,

        String description,

        Boolean enabled
) {
    public ServiceRegistrationRequest {
        if (serviceVersion == null || serviceVersion.isBlank()) serviceVersion = "v1";
        if (enabled == null) enabled = true;
    }
}
