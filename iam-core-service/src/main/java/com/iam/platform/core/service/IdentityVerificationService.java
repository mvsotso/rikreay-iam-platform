package com.iam.platform.core.service;

import com.iam.platform.common.enums.VerificationStatus;
import com.iam.platform.common.exception.IdentityVerificationException;
import com.iam.platform.core.entity.NaturalPerson;
import com.iam.platform.core.entity.Representation;
import com.iam.platform.core.repository.NaturalPersonRepository;
import com.iam.platform.core.repository.RepresentationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityVerificationService {

    private final NaturalPersonRepository personRepository;
    private final RepresentationRepository representationRepository;
    private final AuditService auditService;

    @Transactional
    public NaturalPerson verifyNaturalPerson(UUID personId, String method) {
        NaturalPerson person = personRepository.findById(personId)
                .orElseThrow(() -> new IdentityVerificationException("Person not found: " + personId));

        person.setIdentityVerificationStatus(VerificationStatus.VERIFIED);
        person.setIdentityVerificationMethod(method);
        person.setIdentityVerifiedAt(Instant.now());
        NaturalPerson saved = personRepository.save(person);

        auditService.logIdentityVerification(
                person.getKeycloakUserId(), "VERIFY_PERSON",
                "natural_persons/" + personId, true,
                Map.of("method", method, "personId", personId.toString()));

        log.info("Natural person verified: id={}, method={}", personId, method);
        return saved;
    }

    @Transactional
    public Representation verifyRepresentation(UUID representationId) {
        Representation rep = representationRepository.findById(representationId)
                .orElseThrow(() -> new IdentityVerificationException(
                        "Representation not found: " + representationId));

        rep.setVerificationStatus(VerificationStatus.VERIFIED);
        Representation saved = representationRepository.save(rep);

        auditService.logIdentityVerification(
                null, "VERIFY_REPRESENTATION",
                "representations/" + representationId, true,
                Map.of("representationId", representationId.toString()));

        log.info("Representation verified: id={}", representationId);
        return saved;
    }
}
