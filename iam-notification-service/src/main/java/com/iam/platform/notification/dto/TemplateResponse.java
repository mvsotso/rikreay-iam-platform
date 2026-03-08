package com.iam.platform.notification.dto;

import com.iam.platform.notification.enums.ChannelType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TemplateResponse(
        UUID id,
        String name,
        String subject,
        String bodyTemplate,
        ChannelType channelType,
        List<String> variableNames,
        Instant createdAt,
        Instant updatedAt
) {}
