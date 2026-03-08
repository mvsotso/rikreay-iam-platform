package com.iam.platform.core.service;

import com.iam.platform.common.enums.RepresentativeRole;
import com.iam.platform.common.exception.IamPlatformException;
import com.iam.platform.common.exception.ResourceNotFoundException;
import com.iam.platform.core.entity.Representation;
import com.iam.platform.core.repository.RepresentationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepresentationService {

    private final RepresentationRepository repository;
    private final AuditService auditService;

    @Transactional
    public Representation create(Representation representation) {
        Representation saved = repository.save(representation);
        auditService.logApiAccess(null, "CREATE_REPRESENTATION",
                "representations/" + saved.getId(), null, true);
        log.info("Representation created: id={}, role={}", saved.getId(), saved.getRepresentativeRole());
        return saved;
    }

    @Transactional(readOnly = true)
    public Representation findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Representation", id.toString()));
    }

    @Transactional(readOnly = true)
    public Page<Representation> findByLegalEntity(UUID legalEntityId, Pageable pageable) {
        return repository.findByLegalEntityId(legalEntityId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Representation> findByNaturalPerson(UUID naturalPersonId, Pageable pageable) {
        return repository.findByNaturalPersonId(naturalPersonId, pageable);
    }

    @Transactional
    public Representation update(UUID id, Representation updated) {
        Representation existing = findById(id);
        existing.setRepresentativeRole(updated.getRepresentativeRole());
        existing.setDelegationScope(updated.getDelegationScope());
        existing.setSpecificPermissions(updated.getSpecificPermissions());
        existing.setTitle(updated.getTitle());
        existing.setValidFrom(updated.getValidFrom());
        existing.setValidUntil(updated.getValidUntil());
        existing.setAuthorizationDocument(updated.getAuthorizationDocument());
        existing.setAuthorizationDocumentType(updated.getAuthorizationDocumentType());
        existing.setIsPrimary(updated.getIsPrimary());
        existing.setStatus(updated.getStatus());
        Representation saved = repository.save(existing);
        auditService.logApiAccess(null, "UPDATE_REPRESENTATION",
                "representations/" + id, null, true);
        return saved;
    }

    @Transactional
    public void revoke(UUID id) {
        Representation rep = findById(id);

        // Prevent revoking the last LEGAL_REPRESENTATIVE
        if (rep.getRepresentativeRole() == RepresentativeRole.LEGAL_REPRESENTATIVE) {
            List<Representation> legalReps = repository.findByLegalEntityIdAndRepresentativeRole(
                    rep.getLegalEntity().getId(), RepresentativeRole.LEGAL_REPRESENTATIVE);
            if (legalReps.size() <= 1) {
                throw new IamPlatformException(
                        "Cannot revoke the last LEGAL_REPRESENTATIVE for entity " + rep.getLegalEntity().getId());
            }
        }

        rep.softDelete();
        repository.save(rep);
        auditService.logApiAccess(null, "REVOKE_REPRESENTATION",
                "representations/" + id, null, true);
        log.info("Representation revoked: id={}", id);
    }
}
