package com.iam.platform.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkUserImportRequest(
        @NotBlank(message = "Realm name is required")
        String realmName,

        @NotEmpty(message = "Users list cannot be empty")
        List<UserEntry> users
) {
    public record UserEntry(
            @NotBlank String username,
            @NotBlank String email,
            String firstName,
            String lastName,
            String temporaryPassword,
            List<String> roles
    ) {}
}
