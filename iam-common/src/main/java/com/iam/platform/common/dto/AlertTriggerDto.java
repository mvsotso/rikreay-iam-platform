package com.iam.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertTriggerDto {

    private UUID alertRuleId;
    private String alertName;
    private AlertSeverity severity;
    private String condition;
    private String currentValue;
    private String threshold;
    private String serviceAffected;
    private Instant timestamp;
    private Map<String, Object> metadata;

    public enum AlertSeverity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }
}
