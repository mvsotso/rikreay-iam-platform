package com.iam.platform.core.repository;

import com.iam.platform.core.entity.IdentityDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IdentityDocumentRepository extends JpaRepository<IdentityDocument, UUID> {

    List<IdentityDocument> findByOwnerTypeAndOwnerId(String ownerType, UUID ownerId);
}
