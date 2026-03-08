package com.iam.platform.core.repository;

import com.iam.platform.common.enums.ExternalSystem;
import com.iam.platform.core.entity.ExternalIdentityLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalIdentityLinkRepository extends JpaRepository<ExternalIdentityLink, UUID> {

    List<ExternalIdentityLink> findByOwnerTypeAndOwnerId(String ownerType, UUID ownerId);

    Optional<ExternalIdentityLink> findByExternalSystemAndExternalIdentifier(
            ExternalSystem externalSystem, String externalIdentifier);
}
