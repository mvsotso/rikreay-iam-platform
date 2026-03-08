package com.iam.platform.audit.dto;

import java.time.Instant;
import java.util.Map;

public record AuditEventResponse(
        String id,
        String eventType,
        Instant timestamp,
        String username,
        String action,
        String resource,
        String sourceIp,
        boolean success,
        Map<String, Object> metadata,
        String tenantId,
        String memberClass
) {}
