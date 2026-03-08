package com.iam.platform.config.entity;

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

import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "config_change_logs")
public class ConfigChangeLog extends BaseEntity {

    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private String application;

    @Column(nullable = false, length = 100)
    private String profile;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changes_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> changesJson;

    @Column(nullable = false)
    private String author;

    @Column(name = "change_type", nullable = false, length = 50)
    private String changeType;
}
