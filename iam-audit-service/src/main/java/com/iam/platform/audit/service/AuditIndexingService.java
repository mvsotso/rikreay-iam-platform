package com.iam.platform.audit.service;

import com.iam.platform.audit.config.AuditProperties;
import com.iam.platform.audit.document.AuditEventDocument;
import com.iam.platform.audit.entity.AuditEvent;
import com.iam.platform.audit.repository.AuditEventRepository;
import com.iam.platform.audit.repository.ElasticsearchAuditRepository;
import com.iam.platform.common.constants.KafkaTopics;
import com.iam.platform.common.dto.AuditEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Kafka consumer that indexes audit events into Elasticsearch (primary)
 * and PostgreSQL (fallback).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditIndexingService {

    private final ElasticsearchAuditRepository esRepository;
    private final AuditEventRepository pgRepository;
    private final AuditProperties auditProperties;

    @KafkaListener(
            topics = KafkaTopics.AUDIT_EVENTS,
            containerFactory = "auditKafkaListenerContainerFactory")
    public void consumeAuditEvent(AuditEventDto event) {
        log.debug("Received audit event: type={}, action={}", event.getType(), event.getAction());
        indexEvent(event, false);
    }

    @KafkaListener(
            topics = KafkaTopics.XROAD_EVENTS,
            containerFactory = "auditKafkaListenerContainerFactory")
    public void consumeXroadEvent(AuditEventDto event) {
        log.debug("Received X-Road event: action={}", event.getAction());
        indexEvent(event, true);
    }

    private void indexEvent(AuditEventDto event, boolean isXroad) {
        boolean indexedToEs = false;

        // Try Elasticsearch first (primary storage)
        if (auditProperties.isElasticsearchEnabled()) {
            try {
                AuditEventDocument document = toDocument(event);
                if (isXroad) {
                    esRepository.indexXroadEvent(document);
                } else {
                    esRepository.indexAuditEvent(document);
                }
                indexedToEs = true;
                log.debug("Audit event indexed to ES: type={}, action={}",
                        event.getType(), event.getAction());
            } catch (Exception e) {
                log.warn("Failed to index to Elasticsearch, falling back to PostgreSQL: {}",
                        e.getMessage());
            }
        }

        // Always save to PostgreSQL (fallback + permanent record)
        try {
            AuditEvent entity = toEntity(event, indexedToEs);
            pgRepository.save(entity);
            log.debug("Audit event saved to PostgreSQL: type={}, action={}",
                    event.getType(), event.getAction());
        } catch (Exception e) {
            log.error("Failed to save audit event to PostgreSQL: type={}, action={}",
                    event.getType(), event.getAction(), e);
        }
    }

    private AuditEventDocument toDocument(AuditEventDto event) {
        return AuditEventDocument.builder()
                .id(UUID.randomUUID().toString())
                .eventType(event.getType() != null ? event.getType().name() : "UNKNOWN")
                .timestamp(event.getTimestamp())
                .username(event.getUsername())
                .action(event.getAction())
                .resource(event.getResource())
                .sourceIp(event.getSourceIp())
                .success(event.isSuccess())
                .metadata(event.getMetadata())
                .tenantId(event.getTenantId())
                .build();
    }

    private AuditEvent toEntity(AuditEventDto event, boolean indexedToEs) {
        return AuditEvent.builder()
                .eventType(event.getType() != null ? event.getType().name() : "UNKNOWN")
                .timestamp(event.getTimestamp())
                .username(event.getUsername())
                .action(event.getAction())
                .resource(event.getResource())
                .sourceIp(event.getSourceIp())
                .success(event.isSuccess())
                .metadata(event.getMetadata())
                .tenantId(event.getTenantId())
                .indexedToEs(indexedToEs)
                .build();
    }
}
