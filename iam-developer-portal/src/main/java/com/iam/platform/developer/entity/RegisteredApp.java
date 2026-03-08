package com.iam.platform.developer.entity;

import com.iam.platform.common.entity.BaseEntity;
import com.iam.platform.developer.enums.AppStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "registered_apps")
@SQLRestriction("deleted = false")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RegisteredApp extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "client_id", nullable = false, unique = true)
    private String clientId;

    @Column(name = "client_secret_encrypted", columnDefinition = "TEXT")
    private String clientSecretEncrypted;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "redirect_uris_json", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> redirectUrisJson = List.of();

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AppStatus status = AppStatus.ACTIVE;
}
