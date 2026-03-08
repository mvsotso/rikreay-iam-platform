package com.iam.platform.core.service;

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

    private final KafkaTemplate<String, AuditEventDto> kafkaTemplate;

    public void logApiAccess(String username, String action, String resource,
                             String sourceIp, boolean success) {
        publish(AuditEventType.API_ACCESS, username, action, resource, sourceIp, success, null, null);
    }

    public void logXRoadAccess(String username, String action, String resource,
                                String sourceIp, boolean success, Map<String, Object> metadata) {
        publishToTopic(KafkaTopics.XROAD_EVENTS,
                AuditEventType.XROAD_EXCHANGE, username, action, resource, sourceIp, success, metadata, null);
    }

    public void logAuthEvent(String username, String action, String sourceIp, boolean success) {
        publish(AuditEventType.AUTH_EVENT, username, action, "auth", sourceIp, success, null, null);
    }

    public void logIdentityVerification(String username, String action, String resource,
                                         boolean success, Map<String, Object> metadata) {
        publish(AuditEventType.GOVERNANCE_ACTION, username, action, resource, null, success, metadata, null);
    }

    private void publish(AuditEventType type, String username, String action, String resource,
                         String sourceIp, boolean success, Map<String, Object> metadata, String tenantId) {
        publishToTopic(KafkaTopics.AUDIT_EVENTS, type, username, action, resource,
                sourceIp, success, metadata, tenantId);
    }

    private void publishToTopic(String topic, AuditEventType type, String username, String action,
                                 String resource, String sourceIp, boolean success,
                                 Map<String, Object> metadata, String tenantId) {
        try {
            AuditEventDto event = AuditEventDto.builder()
                    .type(type)
                    .timestamp(Instant.now())
                    .username(username)
                    .action(action)
                    .resource(resource)
                    .sourceIp(sourceIp)
                    .success(success)
                    .metadata(metadata)
                    .tenantId(tenantId)
                    .build();
            kafkaTemplate.send(topic, event);
            log.debug("Audit event published: type={}, action={}", type, action);
        } catch (Exception e) {
            log.error("Failed to publish audit event: type={}, action={}", type, action, e);
        }
    }
}
