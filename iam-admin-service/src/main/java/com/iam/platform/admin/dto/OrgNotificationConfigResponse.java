package com.iam.platform.admin.dto;

import java.util.List;
import java.util.UUID;

public record OrgNotificationConfigResponse(
        UUID id,
        String tenantId,
        List<String> eventTypes,
        List<String> channels,
        List<String> recipients,
        boolean enabled
) {}
