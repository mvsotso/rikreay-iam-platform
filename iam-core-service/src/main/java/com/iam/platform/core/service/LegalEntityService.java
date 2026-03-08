package com.iam.platform.core.service;

import com.iam.platform.common.exception.ResourceNotFoundException;
import com.iam.platform.core.entity.LegalEntity;
import com.iam.platform.core.repository.LegalEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LegalEntityService {

    private final LegalEntityRepository repository;
    private final AuditService auditService;

    @Transactional
    public LegalEntity create(LegalEntity entity) {
        entity.setStatus("ACTIVE");
        LegalEntity saved = repository.save(entity);
        auditService.logApiAccess(null, "CREATE_ENTITY",
                "legal_entities/" + saved.getId(), null, true);
        log.info("Legal entity created: id={}, realmName={}", saved.getId(), saved.getRealmName());
        return saved;
    }

    @Transactional(readOnly = true)
    public LegalEntity findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LegalEntity", id.toString()));
    }

    @Transactional(readOnly = true)
    public LegalEntity findByRegistrationNumber(String registrationNumber) {
        return repository.findByRegistrationNumber(registrationNumber)
                .orElseThrow(() -> new ResourceNotFoundException("LegalEntity", registrationNumber));
    }

    @Transactional(readOnly = true)
    public LegalEntity findByTin(String tin) {
        return repository.findByTaxIdentificationNumber(tin)
                .orElseThrow(() -> new ResourceNotFoundException("LegalEntity", tin));
    }

    @Transactional(readOnly = true)
    public Page<LegalEntity> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional
    public LegalEntity update(UUID id, LegalEntity updated) {
        LegalEntity existing = findById(id);
        existing.setNameKh(updated.getNameKh());
        existing.setNameEn(updated.getNameEn());
        existing.setEntityType(updated.getEntityType());
        existing.setMemberClass(updated.getMemberClass());
        existing.setXroadMemberCode(updated.getXroadMemberCode());
        existing.setXroadSubsystem(updated.getXroadSubsystem());
        existing.setSectorCode(updated.getSectorCode());
        existing.setRegisteredAddress(updated.getRegisteredAddress());
        existing.setProvince(updated.getProvince());
        existing.setStatus(updated.getStatus());
        LegalEntity saved = repository.save(existing);
        auditService.logApiAccess(null, "UPDATE_ENTITY",
                "legal_entities/" + id, null, true);
        return saved;
    }

    @Transactional
    public void softDelete(UUID id) {
        LegalEntity entity = findById(id);
        entity.softDelete();
        repository.save(entity);
        auditService.logApiAccess(null, "DELETE_ENTITY",
                "legal_entities/" + id, null, true);
        log.info("Legal entity soft-deleted: id={}", id);
    }
}
