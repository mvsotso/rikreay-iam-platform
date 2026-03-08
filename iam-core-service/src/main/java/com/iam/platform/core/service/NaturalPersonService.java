package com.iam.platform.core.service;

import com.iam.platform.common.exception.ResourceNotFoundException;
import com.iam.platform.core.entity.NaturalPerson;
import com.iam.platform.core.repository.NaturalPersonRepository;
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
public class NaturalPersonService {

    private final NaturalPersonRepository repository;
    private final AuditService auditService;

    @Transactional
    public NaturalPerson create(NaturalPerson person) {
        NaturalPerson saved = repository.save(person);
        auditService.logApiAccess(person.getKeycloakUserId(), "CREATE_PERSON",
                "natural_persons/" + saved.getId(), null, true);
        log.info("Natural person created: id={}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public NaturalPerson findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NaturalPerson", id.toString()));
    }

    @Transactional(readOnly = true)
    public NaturalPerson findByKeycloakUserId(String keycloakUserId) {
        return repository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("NaturalPerson", keycloakUserId));
    }

    @Transactional(readOnly = true)
    public NaturalPerson findByPersonalIdCode(String personalIdCode) {
        return repository.findByPersonalIdCode(personalIdCode)
                .orElseThrow(() -> new ResourceNotFoundException("NaturalPerson", personalIdCode));
    }

    @Transactional(readOnly = true)
    public Page<NaturalPerson> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional
    public NaturalPerson update(UUID id, NaturalPerson updated) {
        NaturalPerson existing = findById(id);
        existing.setFirstNameKh(updated.getFirstNameKh());
        existing.setLastNameKh(updated.getLastNameKh());
        existing.setFirstNameEn(updated.getFirstNameEn());
        existing.setLastNameEn(updated.getLastNameEn());
        existing.setDateOfBirth(updated.getDateOfBirth());
        existing.setGender(updated.getGender());
        existing.setNationality(updated.getNationality());
        existing.setNationalIdNumber(updated.getNationalIdNumber());
        existing.setCamDigiKeyId(updated.getCamDigiKeyId());
        existing.setStatus(updated.getStatus());
        NaturalPerson saved = repository.save(existing);
        auditService.logApiAccess(null, "UPDATE_PERSON",
                "natural_persons/" + id, null, true);
        return saved;
    }

    @Transactional
    public void softDelete(UUID id) {
        NaturalPerson person = findById(id);
        person.softDelete();
        repository.save(person);
        auditService.logApiAccess(null, "DELETE_PERSON",
                "natural_persons/" + id, null, true);
        log.info("Natural person soft-deleted: id={}", id);
    }
}
