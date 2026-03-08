package com.iam.platform.core.service;

import com.iam.platform.common.enums.ExternalSystem;
import com.iam.platform.common.exception.ResourceNotFoundException;
import com.iam.platform.core.entity.ExternalIdentityLink;
import com.iam.platform.core.repository.ExternalIdentityLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalIdentityLinkService {

    private final ExternalIdentityLinkRepository repository;
    private final AuditService auditService;

    @Transactional
    public ExternalIdentityLink create(ExternalIdentityLink link) {
        ExternalIdentityLink saved = repository.save(link);
        auditService.logApiAccess(null, "CREATE_EXTERNAL_LINK",
                "external_identity_links/" + saved.getId(), null, true);
        return saved;
    }

    @Transactional(readOnly = true)
    public ExternalIdentityLink findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExternalIdentityLink", id.toString()));
    }

    @Transactional(readOnly = true)
    public List<ExternalIdentityLink> findByOwner(String ownerType, UUID ownerId) {
        return repository.findByOwnerTypeAndOwnerId(ownerType, ownerId);
    }

    @Transactional(readOnly = true)
    public Optional<ExternalIdentityLink> findBySystemAndIdentifier(
            ExternalSystem system, String identifier) {
        return repository.findByExternalSystemAndExternalIdentifier(system, identifier);
    }

    @Transactional
    public ExternalIdentityLink update(UUID id, ExternalIdentityLink updated) {
        ExternalIdentityLink existing = findById(id);
        existing.setVerificationStatus(updated.getVerificationStatus());
        existing.setVerifiedAt(updated.getVerifiedAt());
        existing.setVerificationMethod(updated.getVerificationMethod());
        existing.setMetadata(updated.getMetadata());
        ExternalIdentityLink saved = repository.save(existing);
        auditService.logApiAccess(null, "UPDATE_EXTERNAL_LINK",
                "external_identity_links/" + id, null, true);
        return saved;
    }

    @Transactional
    public void softDelete(UUID id) {
        ExternalIdentityLink link = findById(id);
        link.softDelete();
        repository.save(link);
        auditService.logApiAccess(null, "DELETE_EXTERNAL_LINK",
                "external_identity_links/" + id, null, true);
    }
}
