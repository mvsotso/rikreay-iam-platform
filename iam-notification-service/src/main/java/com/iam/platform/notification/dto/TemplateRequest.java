package com.iam.platform.notification.dto;

import com.iam.platform.notification.enums.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TemplateRequest(
        @NotBlank(message = "Template name is required")
        String name,

        String subject,

        @NotBlank(message = "Body template is required")
        String bodyTemplate,

        @NotNull(message = "Channel type is required")
        ChannelType channelType,

        List<String> variableNames
) {}
