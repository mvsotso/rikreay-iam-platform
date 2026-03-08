package com.iam.platform.monitoring.dto;

import com.iam.platform.monitoring.enums.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AlertRuleRequest(
        @NotBlank String name,
        @NotBlank String condition,
        @NotBlank String threshold,
        @NotNull ChannelType channelType,
        String serviceTarget,
        boolean enabled
) {}
