package com.iam.platform.config.entity;

import com.iam.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "feature_flags", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"flag_key", "environment"})
})
public class FeatureFlag extends BaseEntity {

    @Column(name = "flag_key", nullable = false)
    private String flagKey;

    @Column(name = "flag_value", length = 1000)
    private String flagValue;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false, length = 50)
    private String environment;
}
