package com.iam.platform.config.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record ConfigUpdateRequest(
        @NotEmpty(message = "Config properties cannot be empty")
        Map<String, Object> properties
) {}
