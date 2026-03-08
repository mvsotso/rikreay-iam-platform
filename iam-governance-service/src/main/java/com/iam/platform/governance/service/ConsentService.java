package com.iam.platform.governance.service;

import com.iam.platform.governance.dto.ConsentRequest;
import com.iam.platform.governance.dto.ConsentResponse;
import com.iam.platform.governance.entity.ConsentRecord;
import com.iam.platform.governance.repository.ConsentRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentService {

    private final ConsentRecordRepository consentRecordRepository;
    private final AuditService auditService;

    @Transactional
    public ConsentResponse giveConsent(ConsentRequest request, String ipAddress) {
        ConsentRecord consent = ConsentRecord.builder()
                .dataSubjectType(request.dataSubjectType())
                .dataSubjectId(request.dataSubjectId())
                .purpose(request.purpose())
                .legalBasis(request.legalBasis())
                .consentGiven(true)
                .consentTimestamp(Instant.now())
                .consentMethod(request.consentMethod())
                .expiresAt(request.expiresAt())
                .ipAddress(ipAddress)
                .dataCategories(request.dataCategories())
                .thirdPartySharing(request.thirdPartySharing())
                .crossBorderTransfer(request.crossBorderTransfer())
                .build();

        ConsentRecord saved = consentRecordRepository.save(consent);
        log.info("Consent given: id={}, subject={}, purpose={}", saved.getId(), saved.getDataSubjectId(), saved.getPurpose());

        auditService.logGovernanceAction("data-subject:" + request.dataSubjectId(),
                "GIVE_CONSENT", "consents/" + saved.getId(), true,
                Map.of("purpose", saved.getPurpose(), "legalBasis", saved.getLegalBasis().name()));

        return toResponse(saved);
    }

    @Transactional
    public void withdrawConsent(UUID consentId) {
        ConsentRecord consent = consentRecordRepository.findById(consentId)
                .orElseThrow(() -> new RuntimeException("Consent record not found: " + consentId));

        consent.setConsentGiven(false);
        consent.setWithdrawnAt(Instant.now());
        consentRecordRepository.save(consent);
        log.info("Consent withdrawn: id={}", consentId);

        auditService.logGovernanceAction("data-subject:" + consent.getDataSubjectId(),
                "WITHDRAW_CONSENT", "consents/" + consentId, true,
                Map.of("purpose", consent.getPurpose()));
    }

    public List<ConsentResponse> getActiveConsents(UUID dataSubjectId) {
        return consentRecordRepository
                .findByDataSubjectIdAndConsentGivenAndWithdrawnAtIsNull(dataSubjectId, true)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public boolean checkConsent(UUID dataSubjectId, String purpose) {
        return consentRecordRepository
                .findByDataSubjectIdAndPurposeAndConsentGivenAndWithdrawnAtIsNull(dataSubjectId, purpose, true)
                .isPresent();
    }

    public Page<ConsentResponse> getConsentsForSubject(UUID dataSubjectId, Pageable pageable) {
        return consentRecordRepository.findByDataSubjectId(dataSubjectId, pageable).map(this::toResponse);
    }

    public Page<ConsentResponse> listAllConsents(Pageable pageable) {
        return consentRecordRepository.findAll(pageable).map(this::toResponse);
    }

    public List<ConsentResponse> exportConsents(UUID dataSubjectId) {
        return consentRecordRepository.findByDataSubjectId(dataSubjectId, Pageable.unpaged())
                .map(this::toResponse)
                .getContent();
    }

    private ConsentResponse toResponse(ConsentRecord consent) {
        return new ConsentResponse(
                consent.getId(), consent.getDataSubjectType(), consent.getDataSubjectId(),
                consent.getPurpose(), consent.getLegalBasis(), consent.isConsentGiven(),
                consent.getConsentTimestamp(), consent.getConsentMethod(),
                consent.getWithdrawnAt(), consent.getExpiresAt(),
                consent.getDataCategories(), consent.isThirdPartySharing(),
                consent.isCrossBorderTransfer(), consent.getCreatedAt());
    }
}
