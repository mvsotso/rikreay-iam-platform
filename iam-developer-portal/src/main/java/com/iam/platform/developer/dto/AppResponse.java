package com.iam.platform.developer.dto;

import com.iam.platform.developer.enums.AppStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AppResponse(
    UUID id,
    String name,
    String description,
    String clientId,
    List<String> redirectUris,
    String ownerId,
    AppStatus status,
    Instant createdAt
) {}
