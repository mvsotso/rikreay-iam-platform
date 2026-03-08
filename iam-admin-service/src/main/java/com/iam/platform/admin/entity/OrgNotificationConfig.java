package com.iam.platform.admin.entity;

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

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "org_notification_configs")
public class OrgNotificationConfig extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, unique = true)
    private String tenantId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "event_types", columnDefinition = "TEXT[]", nullable = false)
    private List<String> eventTypes;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "TEXT[]", nullable = false)
    private List<String> channels;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "TEXT[]", nullable = false)
    private List<String> recipients;

    @Column(nullable = false)
    private boolean enabled;
}
