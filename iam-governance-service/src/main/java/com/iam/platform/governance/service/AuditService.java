package com.iam.platform.governance.service;

import com.iam.platform.common.constants.KafkaTopics;
import com.iam.platform.common.dto.AuditEventDto;
import com.iam.platform.common.dto.AuditEventDto.AuditEventType;
import com.iam.platform.common.dto.NotificationCommandDto;
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

    public void logGovernanceAction(String username, String action, String resource,
                                     boolean success, Map<String, Object> metadata) {
        try {
            AuditEventDto event = AuditEventDto.builder()
                    .type(AuditEventType.GOVERNANCE_ACTION)
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

    public void sendNotification(NotificationCommandDto command) {
        try {
            kafkaTemplate.send(KafkaTopics.NOTIFICATION_COMMANDS, command);
        } catch (Exception e) {
            log.error("Failed to send notification command", e);
        }
    }
}
