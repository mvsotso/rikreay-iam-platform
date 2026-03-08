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
public class PlatformEventDto {

    private PlatformEventType eventType;
    private Instant timestamp;
    private Map<String, Object> payload;
    private String tenantId;
    private String userId;

    public enum PlatformEventType {
        USER_CREATED,
        USER_UPDATED,
        USER_DELETED,
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        ROLE_CHANGED,
        APP_REGISTERED,
        TENANT_CREATED,
        CONFIG_CHANGED
    }
}
