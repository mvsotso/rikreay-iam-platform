package com.iam.platform.core.entity;

import com.iam.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "contact_channels")
@SQLRestriction("deleted = false")
public class ContactChannel extends BaseEntity {

    @Column(name = "owner_type", nullable = false)
    private String ownerType;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "channel_type", nullable = false)
    private String channelType;

    @Column(nullable = false)
    private String value;

    @Column(name = "is_primary")
    private Boolean isPrimary;

    @Column(name = "is_verified")
    private Boolean isVerified;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "notification_enabled")
    private Boolean notificationEnabled;
}
