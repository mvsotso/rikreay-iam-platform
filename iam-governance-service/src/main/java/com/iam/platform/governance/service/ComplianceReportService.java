package com.iam.platform.governance.service;

import com.iam.platform.governance.dto.ComplianceReportDto;
import com.iam.platform.governance.enums.CampaignStatus;
import com.iam.platform.governance.enums.ReviewDecision;
import com.iam.platform.governance.repository.CampaignRepository;
import com.iam.platform.governance.repository.ConsentRecordRepository;
import com.iam.platform.governance.repository.RiskScoreRepository;
import com.iam.platform.governance.repository.ReviewRepository;
import com.iam.platform.governance.repository.SodPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceReportService {

    private final CampaignRepository campaignRepository;
    private final ReviewRepository reviewRepository;
    private final SodPolicyRepository sodPolicyRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final ConsentRecordRepository consentRecordRepository;

    public ComplianceReportDto generateComplianceReport() {
        long activeCampaigns = campaignRepository.findByStatus(CampaignStatus.ACTIVE, Pageable.unpaged()).getTotalElements();
        long activePolicies = sodPolicyRepository.findByEnabled(true).size();
        long totalRiskScores = riskScoreRepository.count();
        long highRiskUsers = riskScoreRepository.findByScoreGreaterThanEqualOrderByScoreDesc(70, Pageable.unpaged()).getTotalElements();
        long totalConsents = consentRecordRepository.count();

        return new ComplianceReportDto(
                "COMPLIANCE_OVERVIEW",
                Instant.now(),
                0, // totalUsers — would need Keycloak call
                totalRiskScores,
                highRiskUsers,
                activePolicies,
                0, // policyViolations — computed on-demand
                activeCampaigns,
                0, // pendingReviews — computed per campaign
                totalConsents,
                totalConsents, // activeConsents approximation
                Map.of()
        );
    }

    public ComplianceReportDto generateAccessReport() {
        return new ComplianceReportDto(
                "ACCESS_REVIEW",
                Instant.now(),
                0, 0, 0, 0, 0,
                campaignRepository.findByStatus(CampaignStatus.ACTIVE, Pageable.unpaged()).getTotalElements(),
                0, 0, 0, Map.of()
        );
    }

    public ComplianceReportDto generateRiskReport() {
        long totalScores = riskScoreRepository.count();
        long highRisk = riskScoreRepository.findByScoreGreaterThanEqualOrderByScoreDesc(70, Pageable.unpaged()).getTotalElements();
        long mediumRisk = riskScoreRepository.findByScoreGreaterThanEqualOrderByScoreDesc(40, Pageable.unpaged()).getTotalElements() - highRisk;

        return new ComplianceReportDto(
                "RISK_ASSESSMENT",
                Instant.now(),
                0, totalScores, highRisk, 0, 0, 0, 0, 0, 0,
                Map.of("highRisk", highRisk, "mediumRisk", mediumRisk, "lowRisk", totalScores - highRisk - mediumRisk)
        );
    }

    public ComplianceReportDto generateConsentReport() {
        long totalConsents = consentRecordRepository.count();

        return new ComplianceReportDto(
                "CONSENT_COMPLIANCE",
                Instant.now(),
                0, 0, 0, 0, 0, 0, 0,
                totalConsents, totalConsents, Map.of()
        );
    }
}
