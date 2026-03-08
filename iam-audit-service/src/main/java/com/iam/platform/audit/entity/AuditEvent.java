package com.iam.platform.audit.entity;

import com.iam.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * PostgreSQL fallback entity for audit events when Elasticsearch is unavailable.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "audit_events")
public class AuditEvent extends BaseEntity {

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "username")
    private String username;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "resource", length = 500)
    private String resource;

    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    @Column(name = "success", nullable = false)
    private boolean success;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "member_class", length = 10)
    private String memberClass;

    @Column(name = "indexed_to_es", nullable = false)
    private boolean indexedToEs;
}
