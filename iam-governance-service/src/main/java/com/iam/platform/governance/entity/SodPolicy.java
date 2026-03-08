package com.iam.platform.governance.entity;

import com.iam.platform.common.entity.BaseEntity;
import com.iam.platform.governance.enums.PolicySeverity;
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

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sod_policies")
public class SodPolicy extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conflicting_roles_json", columnDefinition = "jsonb", nullable = false)
    private List<List<String>> conflictingRolesJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PolicySeverity severity;

    @Column(nullable = false)
    private boolean enabled;
}
