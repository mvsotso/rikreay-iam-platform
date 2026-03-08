package com.iam.platform.tenant.entity;

import com.iam.platform.common.entity.BaseEntity;
import com.iam.platform.common.enums.EntityType;
import com.iam.platform.common.enums.MemberClass;
import com.iam.platform.tenant.enums.TenantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tenants")
public class Tenant extends BaseEntity {

    @Column(name = "tenant_name", nullable = false, unique = true)
    private String tenantName;

    @Column(name = "realm_name", nullable = false, unique = true)
    private String realmName;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_class", nullable = false, length = 10)
    private MemberClass memberClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", length = 50)
    private EntityType entityType;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(name = "tin", length = 50)
    private String tin;

    @Column(name = "member_code", length = 100)
    private String memberCode;

    @Column(name = "xroad_subsystem")
    private String xroadSubsystem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TenantStatus status;

    @Column(name = "admin_email", nullable = false)
    private String adminEmail;

    @Column(name = "admin_username", nullable = false)
    private String adminUsername;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "jsonb")
    private Map<String, Object> settings;
}
