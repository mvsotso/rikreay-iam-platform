package com.iam.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventDto {

    private AuditEventType type;
    private Instant timestamp;
    private String username;
    private String action;
    private String resource;
    private String sourceIp;
    private boolean success;
    private Map<String, Object> metadata;
    private String tenantId;

    public enum AuditEventType {
        AUTH_EVENT,
        API_ACCESS,
        XROAD_EXCHANGE,
        ADMIN_ACTION,
        CONFIG_CHANGE,
        GOVERNANCE_ACTION
    }
}
