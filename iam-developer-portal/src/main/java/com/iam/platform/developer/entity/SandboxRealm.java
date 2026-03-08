package com.iam.platform.developer.entity;

import com.iam.platform.developer.enums.SandboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sandbox_realms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SandboxRealm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "realm_name", nullable = false, unique = true)
    private String realmName;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SandboxStatus status = SandboxStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
