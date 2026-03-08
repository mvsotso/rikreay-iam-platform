package com.iam.platform.developer.service;

import com.iam.platform.common.constants.KafkaTopics;
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

    public void logDeveloperAction(String username, String action, String resource,
                                    boolean success, Map<String, Object> metadata) {
        try {
            AuditEventDto event = AuditEventDto.builder()
                    .type(AuditEventType.API_ACCESS)
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

    public void publishPlatformEvent(String eventType, Map<String, Object> payload) {
        try {
            kafkaTemplate.send(KafkaTopics.PLATFORM_EVENTS, Map.of(
                    "eventType", eventType,
                    "timestamp", Instant.now().toString(),
                    "payload", payload
            ));
        } catch (Exception e) {
            log.error("Failed to publish platform event: type={}", eventType, e);
        }
    }
}
