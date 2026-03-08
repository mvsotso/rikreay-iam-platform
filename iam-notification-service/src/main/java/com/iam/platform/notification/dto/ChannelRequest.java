package com.iam.platform.notification.dto;

import com.iam.platform.notification.enums.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ChannelRequest(
        @NotNull(message = "Channel type is required")
        ChannelType channelType,

        @NotBlank(message = "Channel name is required")
        String channelName,

        Map<String, Object> configJson,

        boolean enabled
) {}
