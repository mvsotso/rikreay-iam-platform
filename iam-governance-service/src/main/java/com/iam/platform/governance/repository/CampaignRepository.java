package com.iam.platform.governance.repository;

import com.iam.platform.governance.entity.CertificationCampaign;
import com.iam.platform.governance.enums.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<CertificationCampaign, UUID> {

    Page<CertificationCampaign> findByStatus(CampaignStatus status, Pageable pageable);
}
