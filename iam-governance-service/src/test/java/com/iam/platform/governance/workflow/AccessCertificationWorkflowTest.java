package com.iam.platform.governance.workflow;

import com.iam.platform.governance.dto.CampaignRequest;
import com.iam.platform.governance.dto.CampaignResponse;
import com.iam.platform.governance.dto.ReviewRequest;
import com.iam.platform.governance.dto.ReviewResponse;
import com.iam.platform.governance.entity.CertificationReview;
import com.iam.platform.governance.enums.CampaignStatus;
import com.iam.platform.governance.enums.ReviewDecision;
import com.iam.platform.governance.repository.CampaignRepository;
import com.iam.platform.governance.repository.ReviewRepository;
import com.iam.platform.governance.service.AuditService;
import com.iam.platform.governance.service.CampaignService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Workflow: Access Certification Campaign")
class AccessCertificationWorkflowTest {

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @MockitoBean
    private Keycloak keycloakAdmin;

    @MockitoBean
    private AuditService auditService;

    @Test
    @DisplayName("E2E: Create campaign → generate reviews → submit decisions → verify audit trail")
    void fullCertificationWorkflow() {
        // Arrange — mock Keycloak to return 3 test users
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        when(keycloakAdmin.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        UserRepresentation user1 = new UserRepresentation();
        user1.setId(UUID.randomUUID().toString());
        user1.setUsername("user1");
        UserRepresentation user2 = new UserRepresentation();
        user2.setId(UUID.randomUUID().toString());
        user2.setUsername("user2");
        UserRepresentation user3 = new UserRepresentation();
        user3.setId(UUID.randomUUID().toString());
        user3.setUsername("user3");

        when(usersResource.list(0, 100)).thenReturn(List.of(user1, user2, user3));

        // Step 1: Create campaign (scope set to null to avoid H2 jsonb deserialization issues)
        CampaignRequest request = new CampaignRequest(
                "Q1 2026 Access Review",
                "Quarterly access certification for GOV sector",
                Instant.now(),
                Instant.now().plus(30, ChronoUnit.DAYS),
                null);

        CampaignResponse campaign = campaignService.createCampaign(request, "governance-admin");

        // Assert — campaign created in DRAFT status
        assertThat(campaign).isNotNull();
        assertThat(campaign.name()).isEqualTo("Q1 2026 Access Review");
        assertThat(campaign.status()).isEqualTo(CampaignStatus.DRAFT);

        // Verify audit event for campaign creation
        verify(auditService).logGovernanceAction(
                eq("governance-admin"), eq("CREATE_CAMPAIGN"), anyString(), eq(true), any());

        // Step 2: Verify reviews were generated for all 3 users
        Page<ReviewResponse> reviews = campaignService.getReviews(campaign.id(), PageRequest.of(0, 10));
        assertThat(reviews.getTotalElements()).isEqualTo(3);
        assertThat(reviews.getContent()).allSatisfy(review -> {
            assertThat(review.decision()).isEqualTo(ReviewDecision.PENDING);
            assertThat(review.campaignId()).isEqualTo(campaign.id());
        });

        // Step 3: Activate campaign
        CampaignResponse activated = campaignService.activateCampaign(campaign.id(), "governance-admin");
        assertThat(activated.status()).isEqualTo(CampaignStatus.ACTIVE);

        verify(auditService).logGovernanceAction(
                eq("governance-admin"), eq("ACTIVATE_CAMPAIGN"), anyString(), eq(true), any());

        // Step 4: Submit review decisions
        ReviewResponse firstReview = reviews.getContent().get(0);
        ReviewResponse approved = campaignService.submitReview(
                campaign.id(), firstReview.id(),
                new ReviewRequest(ReviewDecision.APPROVE, "Access confirmed"),
                "tenant-admin");

        assertThat(approved.decision()).isEqualTo(ReviewDecision.APPROVE);
        assertThat(approved.comments()).isEqualTo("Access confirmed");

        // Verify audit event for review submission
        verify(auditService).logGovernanceAction(
                eq("tenant-admin"), eq("SUBMIT_REVIEW"), anyString(), eq(true), any());
    }

    @Test
    @DisplayName("E2E: Submit REVOKE decision → triggers notification")
    void revokeDecisionTriggersNotification() {
        // Arrange
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        when(keycloakAdmin.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        UserRepresentation user = new UserRepresentation();
        user.setId(UUID.randomUUID().toString());
        user.setUsername("revoke-target");
        when(usersResource.list(0, 100)).thenReturn(List.of(user));

        CampaignRequest request = new CampaignRequest(
                "Revocation Review", "Test revocation flow",
                Instant.now(), Instant.now().plus(7, ChronoUnit.DAYS), null);
        CampaignResponse campaign = campaignService.createCampaign(request, "governance-admin");

        Page<ReviewResponse> reviews = campaignService.getReviews(campaign.id(), PageRequest.of(0, 10));
        ReviewResponse review = reviews.getContent().get(0);

        // Act — submit REVOKE decision
        ReviewResponse revoked = campaignService.submitReview(
                campaign.id(), review.id(),
                new ReviewRequest(ReviewDecision.REVOKE, "Access no longer needed"),
                "governance-admin");

        // Assert
        assertThat(revoked.decision()).isEqualTo(ReviewDecision.REVOKE);

        // Verify notification sent for revocation
        verify(auditService).sendNotification(any());
    }

    @Test
    @DisplayName("E2E: Campaign with no Keycloak users → empty reviews generated")
    void campaignWithNoUsers() {
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        when(keycloakAdmin.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list(0, 100)).thenReturn(List.of());

        CampaignRequest request = new CampaignRequest(
                "Empty Campaign", "No users to review",
                Instant.now(), Instant.now().plus(7, ChronoUnit.DAYS), null);

        CampaignResponse campaign = campaignService.createCampaign(request, "governance-admin");

        Page<ReviewResponse> reviews = campaignService.getReviews(campaign.id(), PageRequest.of(0, 10));
        assertThat(reviews.getTotalElements()).isZero();
    }
}
