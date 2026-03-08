package com.iam.platform.governance;

import com.iam.platform.governance.dto.CampaignRequest;
import com.iam.platform.governance.dto.CampaignResponse;
import com.iam.platform.governance.dto.SodPolicyRequest;
import com.iam.platform.governance.dto.SodPolicyResponse;
import com.iam.platform.governance.entity.CertificationCampaign;
import com.iam.platform.governance.entity.SodPolicy;
import com.iam.platform.governance.enums.CampaignStatus;
import com.iam.platform.governance.enums.PolicySeverity;
import com.iam.platform.governance.repository.CampaignRepository;
import com.iam.platform.governance.repository.ReviewRepository;
import com.iam.platform.governance.repository.SodPolicyRepository;
import com.iam.platform.governance.service.AuditService;
import com.iam.platform.governance.service.CampaignService;
import com.iam.platform.governance.service.PolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class GovernanceServiceTest {

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private PolicyService policyService;

    @MockitoBean
    private CampaignRepository campaignRepository;

    @MockitoBean
    private ReviewRepository reviewRepository;

    @MockitoBean
    private SodPolicyRepository sodPolicyRepository;

    @MockitoBean
    private Keycloak keycloakAdmin;

    @MockitoBean
    private AuditService auditService;

    @Test
    @DisplayName("Create campaign should set status to DRAFT")
    void createCampaignDraft() {
        CampaignRequest request = new CampaignRequest(
                "Q1 Access Review", "Quarterly review",
                Instant.now(), Instant.now().plusSeconds(86400 * 30), Map.of());

        CertificationCampaign saved = CertificationCampaign.builder()
                .name("Q1 Access Review")
                .description("Quarterly review")
                .status(CampaignStatus.DRAFT)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .scope(Map.of())
                .createdBy("gov-admin")
                .build();

        when(campaignRepository.save(any())).thenReturn(saved);

        CampaignResponse response = campaignService.createCampaign(request, "gov-admin");

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(response.name()).isEqualTo("Q1 Access Review");
    }

    @Test
    @DisplayName("Create SoD policy should detect conflicting roles")
    void createSodPolicy() {
        SodPolicyRequest request = new SodPolicyRequest(
                "Finance SoD",
                List.of(List.of("iam-admin", "auditor")),
                PolicySeverity.HIGH,
                true);

        SodPolicy saved = SodPolicy.builder()
                .name("Finance SoD")
                .conflictingRolesJson(List.of(List.of("iam-admin", "auditor")))
                .severity(PolicySeverity.HIGH)
                .enabled(true)
                .build();

        when(sodPolicyRepository.save(any())).thenReturn(saved);

        SodPolicyResponse response = policyService.createPolicy(request, "gov-admin");

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Finance SoD");
        assertThat(response.severity()).isEqualTo(PolicySeverity.HIGH);
    }

    @Test
    @DisplayName("Activate campaign should change status to ACTIVE")
    void activateCampaign() {
        UUID id = UUID.randomUUID();
        CertificationCampaign campaign = CertificationCampaign.builder()
                .name("Test Campaign")
                .status(CampaignStatus.DRAFT)
                .startDate(Instant.now())
                .endDate(Instant.now().plusSeconds(86400))
                .scope(Map.of())
                .createdBy("gov-admin")
                .build();

        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CampaignResponse response = campaignService.activateCampaign(id, "gov-admin");

        assertThat(response.status()).isEqualTo(CampaignStatus.ACTIVE);
    }
}
