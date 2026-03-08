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

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "notification_templates")
public class NotificationTemplate extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 500)
    private String subject;

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 50)
    private ChannelType channelType;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "variable_names", columnDefinition = "TEXT[]")
    private List<String> variableNames;
}
