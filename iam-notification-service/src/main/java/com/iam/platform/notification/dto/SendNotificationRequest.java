package com.iam.platform.notification.dto;

import com.iam.platform.notification.enums.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record SendNotificationRequest(
        @NotNull(message = "Channel type is required")
        ChannelType channelType,

        @NotBlank(message = "Recipient is required")
        String recipient,

        String subject,

        String body,

        String templateName,

        Map<String, String> variables
) {}
