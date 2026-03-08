package com.iam.platform.monitoring.service;

import com.iam.platform.common.constants.KafkaTopics;
import com.iam.platform.common.dto.AlertTriggerDto;
import com.iam.platform.common.dto.AuditEventDto;
import com.iam.platform.common.dto.AuditEventDto.AuditEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void logMonitoringAction(String username, String action, String resource,
                                     boolean success, Map<String, Object> metadata) {
        try {
            AuditEventDto event = AuditEventDto.builder()
                    .type(AuditEventType.ADMIN_ACTION)
                    .timestamp(Instant.now())
                    .username(username)
                    .action(action)
                    .resource(resource)
                    .success(success)
                    .metadata(metadata)
                    .build();
            kafkaTemplate.send(KafkaTopics.AUDIT_EVENTS, event);
        } catch (Exception e) {
            log.error("Failed to publish audit event: action={}", action, e);
        }
    }

    public void publishAlertTrigger(AlertTriggerDto trigger) {
        try {
            kafkaTemplate.send(KafkaTopics.ALERT_TRIGGERS, trigger);
            log.info("Published alert trigger: {}", trigger.getAlertName());
        } catch (Exception e) {
            log.error("Failed to publish alert trigger: {}", trigger.getAlertName(), e);
        }
    }
}
