package com.iam.platform.monitoring.entity;

import com.iam.platform.common.entity.BaseEntity;
import com.iam.platform.monitoring.enums.ChannelType;
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

import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "alert_rules")
public class AlertRule extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 500)
    private String condition;

    @Column(nullable = false, length = 100)
    private String threshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private ChannelType channelType;

    @Column(name = "service_target", length = 100)
    private String serviceTarget;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "last_triggered_at")
    private Instant lastTriggeredAt;
}
