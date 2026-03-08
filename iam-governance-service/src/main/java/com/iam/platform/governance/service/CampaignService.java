package com.iam.platform.governance.service;

import com.iam.platform.common.dto.NotificationCommandDto;
import com.iam.platform.governance.dto.CampaignRequest;
import com.iam.platform.governance.dto.CampaignResponse;
import com.iam.platform.governance.dto.ReviewRequest;
import com.iam.platform.governance.dto.ReviewResponse;
import com.iam.platform.governance.entity.CertificationCampaign;
import com.iam.platform.governance.entity.CertificationReview;
import com.iam.platform.governance.enums.CampaignStatus;
import com.iam.platform.governance.enums.ReviewDecision;
import com.iam.platform.governance.repository.CampaignRepository;
import com.iam.platform.governance.repository.ReviewRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
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
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final ReviewRepository reviewRepository;
    private final Keycloak keycloakAdmin;
    private final AuditService auditService;

    @Transactional
    public CampaignResponse createCampaign(CampaignRequest request, String username) {
        CertificationCampaign campaign = CertificationCampaign.builder()
                .name(request.name())
                .description(request.description())
                .status(CampaignStatus.DRAFT)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .scope(request.scope())
                .createdBy(username)
                .build();

        CertificationCampaign saved = campaignRepository.save(campaign);
        generateReviews(saved);

        log.info("Campaign created: id={}, name={}", saved.getId(), saved.getName());
        auditService.logGovernanceAction(username, "CREATE_CAMPAIGN", "campaigns/" + saved.getId(),
                true, Map.of("name", saved.getName()));

        return toResponse(saved);
    }

    @CircuitBreaker(name = "keycloak", fallbackMethod = "generateReviewsFallback")
    private void generateReviews(CertificationCampaign campaign) {
        try {
            String realmName = "iam-platform";
            if (campaign.getScope() != null && campaign.getScope().containsKey("tenantId")) {
                realmName = campaign.getScope().get("tenantId").toString();
            }

            List<UserRepresentation> users = keycloakAdmin.realm(realmName)
                    .users().list(0, 100);

            for (UserRepresentation user : users) {
                CertificationReview review = CertificationReview.builder()
                        .campaignId(campaign.getId())
                        .userId(user.getId())
                        .decision(ReviewDecision.PENDING)
                        .build();
                reviewRepository.save(review);
            }

            log.info("Generated {} reviews for campaign {}", users.size(), campaign.getId());
        } catch (Exception e) {
            log.error("Failed to generate reviews from Keycloak for campaign {}", campaign.getId(), e);
        }
    }

    @SuppressWarnings("unused")
    private void generateReviewsFallback(CertificationCampaign campaign, Throwable t) {
        log.error("Keycloak circuit breaker open, cannot generate reviews: {}", t.getMessage());
    }

    public Page<CampaignResponse> listCampaigns(CampaignStatus status, Pageable pageable) {
        if (status != null) {
            return campaignRepository.findByStatus(status, pageable).map(this::toResponse);
        }
        return campaignRepository.findAll(pageable).map(this::toResponse);
    }

    public CampaignResponse getCampaign(UUID id) {
        return campaignRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + id));
    }

    public Page<ReviewResponse> getReviews(UUID campaignId, Pageable pageable) {
        return reviewRepository.findByCampaignId(campaignId, pageable).map(this::toReviewResponse);
    }

    @Transactional
    public ReviewResponse submitReview(UUID campaignId, UUID reviewId, ReviewRequest request, String reviewerId) {
        CertificationReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found: " + reviewId));

        if (!review.getCampaignId().equals(campaignId)) {
            throw new RuntimeException("Review does not belong to campaign: " + campaignId);
        }

        review.setDecision(request.decision());
        review.setComments(request.comments());
        review.setReviewerId(reviewerId);
        review.setReviewedAt(Instant.now());

        CertificationReview saved = reviewRepository.save(review);
        log.info("Review submitted: id={}, decision={}", reviewId, request.decision());

        auditService.logGovernanceAction(reviewerId, "SUBMIT_REVIEW", "reviews/" + reviewId,
                true, Map.of("decision", request.decision().name(), "campaignId", campaignId.toString()));

        if (request.decision() == ReviewDecision.REVOKE) {
            auditService.sendNotification(NotificationCommandDto.builder()
                    .channelType(NotificationCommandDto.ChannelType.EMAIL)
                    .subject("Access Review: Access Revoked")
                    .body("User " + review.getUserId() + " access has been revoked in campaign review")
                    .priority(NotificationCommandDto.Priority.HIGH)
                    .build());
        }

        return toReviewResponse(saved);
    }

    @Transactional
    public CampaignResponse activateCampaign(UUID id, String username) {
        CertificationCampaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + id));
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaign.setStartDate(Instant.now());
        CertificationCampaign saved = campaignRepository.save(campaign);

        auditService.logGovernanceAction(username, "ACTIVATE_CAMPAIGN", "campaigns/" + id,
                true, Map.of("name", campaign.getName()));

        return toResponse(saved);
    }

    private CampaignResponse toResponse(CertificationCampaign campaign) {
        long totalReviews = reviewRepository.countByCampaignIdAndDecision(campaign.getId(), ReviewDecision.PENDING)
                + reviewRepository.countByCampaignIdAndDecision(campaign.getId(), ReviewDecision.APPROVE)
                + reviewRepository.countByCampaignIdAndDecision(campaign.getId(), ReviewDecision.REVOKE);
        long pendingReviews = reviewRepository.countByCampaignIdAndDecision(campaign.getId(), ReviewDecision.PENDING);

        return new CampaignResponse(
                campaign.getId(), campaign.getName(), campaign.getDescription(),
                campaign.getStatus(), campaign.getStartDate(), campaign.getEndDate(),
                campaign.getScope(), campaign.getCreatedBy(),
                totalReviews, pendingReviews, campaign.getCreatedAt());
    }

    private ReviewResponse toReviewResponse(CertificationReview review) {
        return new ReviewResponse(
                review.getId(), review.getCampaignId(), review.getUserId(),
                review.getReviewerId(), review.getDecision(), review.getComments(),
                review.getReviewedAt(), review.getCreatedAt());
    }
}
