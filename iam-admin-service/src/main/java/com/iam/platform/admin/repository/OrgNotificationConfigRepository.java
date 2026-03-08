package com.iam.platform.admin.repository;

import com.iam.platform.admin.entity.OrgNotificationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrgNotificationConfigRepository extends JpaRepository<OrgNotificationConfig, UUID> {

    Optional<OrgNotificationConfig> findByTenantId(String tenantId);
}
