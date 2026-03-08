package com.iam.platform.governance.entity;

import com.iam.platform.common.entity.BaseEntity;
import com.iam.platform.governance.enums.WorkflowType;
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
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "lifecycle_workflows")
public class LifecycleWorkflow extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkflowType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "steps_json", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> stepsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "approval_chain_json", columnDefinition = "jsonb", nullable = false)
    private List<String> approvalChainJson;

    @Column(nullable = false)
    private boolean enabled;
}
