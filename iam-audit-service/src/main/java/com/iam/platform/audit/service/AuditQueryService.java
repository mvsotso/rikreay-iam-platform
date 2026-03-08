package com.iam.platform.audit.service;

import com.iam.platform.audit.config.AuditProperties;
import com.iam.platform.audit.document.AuditEventDocument;
import com.iam.platform.audit.dto.AuditEventResponse;
import com.iam.platform.audit.dto.AuditStatsResponse;
import com.iam.platform.audit.entity.AuditEvent;
import com.iam.platform.audit.repository.AuditEventRepository;
import com.iam.platform.audit.repository.ElasticsearchAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Query service supporting both Elasticsearch (primary) and PostgreSQL (fallback).
 * Supports tenant-scoped queries for org-admin and sector-admin proxied through admin-service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final ElasticsearchAuditRepository esRepository;
    private final AuditEventRepository pgRepository;
    private final AuditProperties auditProperties;

    /**
     * Searches audit events with filters. Tries ES first, falls back to PG.
     */
    public Page<AuditEventResponse> searchEvents(
            String eventType, String username, String action,
            String tenantId, String memberClass,
            Instant from, Instant to, Pageable pageable) {

        if (auditProperties.isElasticsearchEnabled()) {
            try {
                PageImpl<AuditEventDocument> results = esRepository.searchAuditEvents(
                        eventType, username, action, tenantId, memberClass, from, to, pageable);
                List<AuditEventResponse> responses = results.getContent().stream()
                        .map(this::toResponse)
                        .toList();
                return new PageImpl<>(responses, pageable, results.getTotalElements());
            } catch (Exception e) {
                log.warn("ES query failed, falling back to PostgreSQL: {}", e.getMessage());
            }
        }

        return searchEventsFromPostgres(eventType, username, action, tenantId, memberClass,
                from, to, pageable);
    }

    /**
     * Searches X-Road events.
     */
    public Page<AuditEventResponse> searchXroadEvents(
            String tenantId, String memberClass,
            Instant from, Instant to, Pageable pageable) {

        if (auditProperties.isElasticsearchEnabled()) {
            try {
                PageImpl<AuditEventDocument> results = esRepository.searchXroadEvents(
                        tenantId, memberClass, from, to, pageable);
                List<AuditEventResponse> responses = results.getContent().stream()
                        .map(this::toResponse)
                        .toList();
                return new PageImpl<>(responses, pageable, results.getTotalElements());
            } catch (Exception e) {
                log.warn("ES X-Road query failed, falling back to PostgreSQL: {}", e.getMessage());
            }
        }

        return searchEventsFromPostgres("XROAD_EXCHANGE", null, null, tenantId, memberClass,
                from, to, pageable);
    }

    /**
     * Gets aggregated statistics.
     */
    @Transactional(readOnly = true)
    public AuditStatsResponse getStats(String tenantId, String memberClass,
                                        Instant from, Instant to) {
        if (from == null) from = Instant.now().minus(30, ChronoUnit.DAYS);
        if (to == null) to = Instant.now();

        List<Object[]> stats = pgRepository.getStatsByTypeAndDateRange(
                tenantId, memberClass, from, to);

        Map<String, Long> eventsByType = new HashMap<>();
        Map<String, Long> successByType = new HashMap<>();
        long totalEvents = 0;
        long totalSuccess = 0;

        for (Object[] row : stats) {
            String type = (String) row[0];
            long count = (Long) row[1];
            long success = (Long) row[2];
            eventsByType.put(type, count);
            successByType.put(type, success);
            totalEvents += count;
            totalSuccess += success;
        }

        return new AuditStatsResponse(
                totalEvents,
                totalSuccess,
                totalEvents - totalSuccess,
                eventsByType,
                successByType
        );
    }

    /**
     * Searches login history (AUTH_EVENT type).
     */
    public Page<AuditEventResponse> searchLoginHistory(
            String username, String tenantId,
            Instant from, Instant to, Boolean successOnly,
            Pageable pageable) {

        if (auditProperties.isElasticsearchEnabled()) {
            try {
                PageImpl<AuditEventDocument> results = esRepository.searchLoginHistory(
                        username, tenantId, from, to, successOnly, pageable);
                List<AuditEventResponse> responses = results.getContent().stream()
                        .map(this::toResponse)
                        .toList();
                return new PageImpl<>(responses, pageable, results.getTotalElements());
            } catch (Exception e) {
                log.warn("ES login history query failed, falling back to PostgreSQL: {}",
                        e.getMessage());
            }
        }

        // Fallback to PostgreSQL
        String successAction = successOnly != null && !successOnly ? null : null;
        return searchEventsFromPostgres("AUTH_EVENT", username, successAction,
                tenantId, null, from, to, pageable);
    }

    /**
     * Exports audit events (returns all matching events without pagination limit).
     */
    public List<AuditEventResponse> exportEvents(
            String eventType, String username, String tenantId, String memberClass,
            Instant from, Instant to) {

        Pageable exportPage = Pageable.ofSize(10000);
        Page<AuditEventResponse> results = searchEvents(
                eventType, username, null, tenantId, memberClass, from, to, exportPage);
        return results.getContent();
    }

    @Transactional(readOnly = true)
    private Page<AuditEventResponse> searchEventsFromPostgres(
            String eventType, String username, String action,
            String tenantId, String memberClass,
            Instant from, Instant to, Pageable pageable) {

        Page<AuditEvent> results = pgRepository.findByFilters(
                eventType, username, action, tenantId, memberClass, from, to, pageable);

        return results.map(this::toResponse);
    }

    private AuditEventResponse toResponse(AuditEventDocument doc) {
        return new AuditEventResponse(
                doc.getId(),
                doc.getEventType(),
                doc.getTimestamp(),
                doc.getUsername(),
                doc.getAction(),
                doc.getResource(),
                doc.getSourceIp(),
                doc.isSuccess(),
                doc.getMetadata(),
                doc.getTenantId(),
                doc.getMemberClass()
        );
    }

    private AuditEventResponse toResponse(AuditEvent entity) {
        return new AuditEventResponse(
                entity.getId().toString(),
                entity.getEventType(),
                entity.getTimestamp(),
                entity.getUsername(),
                entity.getAction(),
                entity.getResource(),
                entity.getSourceIp(),
                entity.isSuccess(),
                entity.getMetadata(),
                entity.getTenantId(),
                entity.getMemberClass()
        );
    }
}
