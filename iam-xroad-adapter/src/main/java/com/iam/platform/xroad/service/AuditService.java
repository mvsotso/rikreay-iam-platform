package com.iam.platform.xroad.service;

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

    public void logXRoadExchange(String consumerIdentifier, String action, String resource,
                                  boolean success, Map<String, Object> metadata) {
        try {
            AuditEventDto event = AuditEventDto.builder()
                    .type(AuditEventType.XROAD_EXCHANGE)
                    .timestamp(Instant.now())
                    .username(consumerIdentifier)
                    .action(action)
                    .resource(resource)
                    .success(success)
                    .metadata(metadata)
                    .build();
            kafkaTemplate.send(KafkaTopics.XROAD_EVENTS, event);
            log.debug("X-Road audit event published: action={}, consumer={}", action, consumerIdentifier);
        } catch (Exception e) {
            log.error("Failed to publish X-Road audit event: action={}", action, e);
        }
    }

    public void logAdminAction(String username, String action, String resource,
                                boolean success) {
        try {
            AuditEventDto event = AuditEventDto.builder()
                    .type(AuditEventType.ADMIN_ACTION)
                    .timestamp(Instant.now())
                    .username(username)
                    .action(action)
                    .resource(resource)
                    .success(success)
                    .build();
            kafkaTemplate.send(KafkaTopics.AUDIT_EVENTS, event);
            log.debug("Admin audit event published: action={}", action);
        } catch (Exception e) {
            log.error("Failed to publish admin audit event: action={}", action, e);
        }
    }
}
