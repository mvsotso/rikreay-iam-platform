package com.iam.platform.developer.repository;

import com.iam.platform.developer.entity.SandboxRealm;
import com.iam.platform.developer.enums.SandboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SandboxRealmRepository extends JpaRepository<SandboxRealm, UUID> {

    List<SandboxRealm> findByOwnerId(String ownerId);

    List<SandboxRealm> findByOwnerIdAndStatus(String ownerId, SandboxStatus status);

    List<SandboxRealm> findByStatusAndExpiresAtBefore(SandboxStatus status, Instant now);

    long countByOwnerIdAndStatus(String ownerId, SandboxStatus status);
}
