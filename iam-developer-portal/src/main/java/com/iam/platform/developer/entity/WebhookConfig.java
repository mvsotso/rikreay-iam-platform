package com.iam.platform.developer.entity;

import com.iam.platform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "webhook_configs")
@SQLRestriction("deleted = false")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WebhookConfig extends BaseEntity {

    @Column(name = "app_id", nullable = false)
    private java.util.UUID appId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "target_url", nullable = false, length = 1024)
    private String targetUrl;

    @Column(name = "secret_hash")
    private String secretHash;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;
}
