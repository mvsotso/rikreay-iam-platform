package com.iam.platform.developer.dto;

import com.iam.platform.developer.enums.SandboxStatus;
import java.time.Instant;
import java.util.UUID;

public record SandboxResponse(
    UUID id,
    String realmName,
    String ownerId,
    SandboxStatus status,
    Instant expiresAt,
    Instant createdAt
) {}
