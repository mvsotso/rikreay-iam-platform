package com.iam.platform.core.repository;

import com.iam.platform.common.enums.EntityType;
import com.iam.platform.common.enums.MemberClass;
import com.iam.platform.core.entity.LegalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegalEntityRepository extends JpaRepository<LegalEntity, UUID> {

    Optional<LegalEntity> findByRegistrationNumber(String registrationNumber);

    Optional<LegalEntity> findByTaxIdentificationNumber(String tin);

    Optional<LegalEntity> findByRealmName(String realmName);

    Page<LegalEntity> findByMemberClass(MemberClass memberClass, Pageable pageable);

    Page<LegalEntity> findByEntityType(EntityType entityType, Pageable pageable);

    Page<LegalEntity> findByStatus(String status, Pageable pageable);
}
