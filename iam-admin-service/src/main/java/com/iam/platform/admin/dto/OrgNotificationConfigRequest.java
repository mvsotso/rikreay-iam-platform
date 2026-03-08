package com.iam.platform.admin.dto;

import java.util.List;

public record OrgNotificationConfigRequest(
        List<String> eventTypes,
        List<String> channels,
        List<String> recipients,
        boolean enabled
) {}
