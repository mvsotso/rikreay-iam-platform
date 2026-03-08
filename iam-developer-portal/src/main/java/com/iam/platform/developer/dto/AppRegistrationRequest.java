package com.iam.platform.developer.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record AppRegistrationRequest(
    @NotBlank String name,
    String description,
    List<String> redirectUris
) {}
