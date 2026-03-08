package com.iam.platform.tenant.service;

import com.iam.platform.common.constants.KafkaTopics;
import com.iam.platform.common.dto.AuditEventDto;
import com.iam.platform.common.dto.AuditEventDto.AuditEventType;
import com.iam.platform.common.dto.PlatformEventDto;
import com.iam.platform.common.dto.PlatformEventDto.PlatformEventType;
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

    public void logAdminAction(String username, String action, String resource,
                                boolean success, String tenantId, Map<String, Object> metadata) {
        try {
            AuditEventDto event = AuditEventDto.builder()
                    .type(AuditEventType.ADMIN_ACTION)
                    .timestamp(Instant.now())
                    .username(username)
                    .action(action)
                    .resource(resource)
                    .success(success)
                    .tenantId(tenantId)
                    .metadata(metadata)
                    .build();
            kafkaTemplate.send(KafkaTopics.AUDIT_EVENTS, event);
            log.debug("Audit event published: action={}, resource={}", action, resource);
        } catch (Exception e) {
            log.error("Failed to publish audit event: action={}, resource={}", action, resource, e);
        }
    }

    public void publishTenantCreated(String tenantId, String realmName, String username) {
        try {
            PlatformEventDto event = PlatformEventDto.builder()
                    .eventType(PlatformEventType.TENANT_CREATED)
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .userId(username)
                    .payload(Map.of("realmName", realmName))
                    .build();
            kafkaTemplate.send(KafkaTopics.PLATFORM_EVENTS, event);
            log.debug("Platform event published: TENANT_CREATED for realm={}", realmName);
        } catch (Exception e) {
            log.error("Failed to publish platform event for tenant: {}", realmName, e);
        }
    }
}
