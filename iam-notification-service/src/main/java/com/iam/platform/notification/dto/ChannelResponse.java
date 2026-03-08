package com.iam.platform.notification.dto;

import com.iam.platform.notification.enums.ChannelType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ChannelResponse(
        UUID id,
        ChannelType channelType,
        String channelName,
        Map<String, Object> configJson,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {}
