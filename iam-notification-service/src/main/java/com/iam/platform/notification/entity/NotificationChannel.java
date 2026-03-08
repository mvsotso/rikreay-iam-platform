package com.iam.platform.notification.entity;

import com.iam.platform.common.entity.BaseEntity;
import com.iam.platform.notification.enums.ChannelType;
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
@Table(name = "notification_channels")
public class NotificationChannel extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 50)
    private ChannelType channelType;

    @Column(name = "channel_name", nullable = false, unique = true)
    private String channelName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> configJson;

    @Column(nullable = false)
    private boolean enabled;
}
