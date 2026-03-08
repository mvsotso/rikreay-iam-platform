package com.iam.platform.governance.repository;

import com.iam.platform.governance.entity.CertificationReview;
import com.iam.platform.governance.enums.ReviewDecision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<CertificationReview, UUID> {

    Page<CertificationReview> findByCampaignId(UUID campaignId, Pageable pageable);

    long countByCampaignIdAndDecision(UUID campaignId, ReviewDecision decision);
}
