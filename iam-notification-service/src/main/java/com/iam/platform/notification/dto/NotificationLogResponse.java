package com.iam.platform.notification.dto;

import com.iam.platform.notification.enums.ChannelType;
import com.iam.platform.notification.enums.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationLogResponse(
        UUID id,
        UUID templateId,
        ChannelType channelType,
        String recipient,
        String subject,
        NotificationStatus status,
        Instant sentAt,
        String errorMessage,
        Instant createdAt
) {}
