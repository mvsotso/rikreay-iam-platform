package com.iam.platform.developer.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RedirectUrisRequest(
    @NotNull List<String> redirectUris
) {}
