package com.iam.platform.monitoring.dto;

import com.iam.platform.monitoring.enums.ChannelType;

import java.time.Instant;
import java.util.UUID;

public record AlertRuleResponse(
        UUID id,
        String name,
        String condition,
        String threshold,
        ChannelType channelType,
        String serviceTarget,
        boolean enabled,
        Instant lastTriggeredAt,
        Instant createdAt
) {}
