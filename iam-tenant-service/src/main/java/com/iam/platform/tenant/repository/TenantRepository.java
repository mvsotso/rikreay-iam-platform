package com.iam.platform.tenant.repository;

import com.iam.platform.common.enums.MemberClass;
import com.iam.platform.tenant.entity.Tenant;
import com.iam.platform.tenant.enums.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByRealmName(String realmName);

    Optional<Tenant> findByTenantName(String tenantName);

    boolean existsByRealmName(String realmName);

    boolean existsByTenantName(String tenantName);

    Page<Tenant> findByMemberClass(MemberClass memberClass, Pageable pageable);

    Page<Tenant> findByStatus(TenantStatus status, Pageable pageable);

    Page<Tenant> findByMemberClassAndStatus(MemberClass memberClass, TenantStatus status, Pageable pageable);
}
