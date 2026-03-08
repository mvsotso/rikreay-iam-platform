package com.iam.platform.audit.repository;

import com.iam.platform.audit.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * PostgreSQL repository for audit events (fallback storage).
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    Page<AuditEvent> findByEventType(String eventType, Pageable pageable);

    Page<AuditEvent> findByUsername(String username, Pageable pageable);

    Page<AuditEvent> findByTenantId(String tenantId, Pageable pageable);

    Page<AuditEvent> findByMemberClass(String memberClass, Pageable pageable);

    @Query("SELECT a FROM AuditEvent a WHERE a.timestamp BETWEEN :from AND :to")
    Page<AuditEvent> findByDateRange(
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("SELECT a FROM AuditEvent a WHERE " +
            "(:eventType IS NULL OR a.eventType = :eventType) AND " +
            "(:username IS NULL OR a.username = :username) AND " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:tenantId IS NULL OR a.tenantId = :tenantId) AND " +
            "(:memberClass IS NULL OR a.memberClass = :memberClass) AND " +
            "(:from IS NULL OR a.timestamp >= :from) AND " +
            "(:to IS NULL OR a.timestamp <= :to)")
    Page<AuditEvent> findByFilters(
            @Param("eventType") String eventType,
            @Param("username") String username,
            @Param("action") String action,
            @Param("tenantId") String tenantId,
            @Param("memberClass") String memberClass,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    List<AuditEvent> findByIndexedToEsFalse();

    @Query("SELECT a.eventType, COUNT(a), SUM(CASE WHEN a.success = true THEN 1 ELSE 0 END) " +
            "FROM AuditEvent a WHERE " +
            "(:tenantId IS NULL OR a.tenantId = :tenantId) AND " +
            "(:memberClass IS NULL OR a.memberClass = :memberClass) AND " +
            "a.timestamp BETWEEN :from AND :to " +
            "GROUP BY a.eventType")
    List<Object[]> getStatsByTypeAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("memberClass") String memberClass,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
