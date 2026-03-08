package com.iam.platform.governance.repository;

import com.iam.platform.governance.entity.ConsentRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, UUID> {

    List<ConsentRecord> findByDataSubjectIdAndConsentGivenAndWithdrawnAtIsNull(UUID dataSubjectId, boolean consentGiven);

    Optional<ConsentRecord> findByDataSubjectIdAndPurposeAndConsentGivenAndWithdrawnAtIsNull(
            UUID dataSubjectId, String purpose, boolean consentGiven);

    Page<ConsentRecord> findByDataSubjectId(UUID dataSubjectId, Pageable pageable);

    Page<ConsentRecord> findAll(Pageable pageable);
}
