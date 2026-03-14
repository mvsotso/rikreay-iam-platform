package com.iam.platform.governance;

import com.iam.platform.common.test.ApiResponseAssertions;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.governance.dto.RiskScoreResponse;
import com.iam.platform.governance.service.RiskScoringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RiskScoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RiskScoringService riskScoringService;

    private static final String TEST_USER_ID = "user-abc-123";

    private RiskScoreResponse sampleRiskScore(int score) {
        return new RiskScoreResponse(
                UUID.randomUUID(),
                TEST_USER_ID,
                score,
                Map.of("roleCount", 5, "hasAdminRole", true, "emailVerified", true),
                Instant.now()
        );
    }

    @Nested
    @DisplayName("POST /api/v1/governance/risk-scores/{userId}/calculate — Calculate risk score")
    class CalculateRiskScore {

        @Test
        @DisplayName("Should calculate risk score for governance-admin")
        void calculateRisk_governanceAdmin_shouldSucceed() throws Exception {
            when(riskScoringService.calculateRiskScore(TEST_USER_ID))
                    .thenReturn(sampleRiskScore(45));

            mockMvc.perform(post("/api/v1/governance/risk-scores/" + TEST_USER_ID + "/calculate")
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID))
                    .andExpect(jsonPath("$.data.score").value(45))
                    .andExpect(jsonPath("$.data.factors.roleCount").value(5))
                    .andExpect(jsonPath("$.message").value("Risk score calculated"));
        }

        @Test
        @DisplayName("Should calculate risk score for iam-admin")
        void calculateRisk_iamAdmin_shouldSucceed() throws Exception {
            when(riskScoringService.calculateRiskScore(TEST_USER_ID))
                    .thenReturn(sampleRiskScore(80));

            ApiResponseAssertions.assertApiSuccess(
                    mockMvc.perform(post("/api/v1/governance/risk-scores/" + TEST_USER_ID + "/calculate")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))));
        }

        @Test
        @DisplayName("Should return 403 for tenant-admin")
        void calculateRisk_tenantAdmin_shouldBeForbidden() throws Exception {
            mockMvc.perform(post("/api/v1/governance/risk-scores/" + TEST_USER_ID + "/calculate")
                            .with(JwtTestUtils.jwtWithRoles("user", TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 for external-user")
        void calculateRisk_externalUser_shouldBeForbidden() throws Exception {
            mockMvc.perform(post("/api/v1/governance/risk-scores/" + TEST_USER_ID + "/calculate")
                            .with(JwtTestUtils.jwtWithRoles("citizen", TestConstants.ROLE_EXTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void calculateRisk_unauthenticated_shouldReturn401() throws Exception {
            mockMvc.perform(post("/api/v1/governance/risk-scores/" + TEST_USER_ID + "/calculate"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/governance/risk-scores/{userId} — Get latest risk score")
    class GetLatestRiskScore {

        @Test
        @DisplayName("Should return latest risk score for governance-admin")
        void getLatestRisk_governanceAdmin_shouldSucceed() throws Exception {
            when(riskScoringService.getLatestRiskScore(TEST_USER_ID))
                    .thenReturn(sampleRiskScore(72));

            mockMvc.perform(get("/api/v1/governance/risk-scores/" + TEST_USER_ID)
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.score").value(72))
                    .andExpect(jsonPath("$.data.factors").isMap());
        }

        @Test
        @DisplayName("Should return 403 for auditor")
        void getLatestRisk_auditor_shouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/governance/risk-scores/" + TEST_USER_ID)
                            .with(JwtTestUtils.jwtWithRoles("auditor", TestConstants.ROLE_AUDITOR)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/governance/risk-scores/high-risk — Get high risk users")
    class GetHighRiskUsers {

        @Test
        @DisplayName("Should return high risk users above threshold for governance-admin")
        void getHighRisk_governanceAdmin_shouldSucceed() throws Exception {
            Page<RiskScoreResponse> page = new PageImpl<>(List.of(
                    sampleRiskScore(85),
                    sampleRiskScore(92)
            ));
            when(riskScoringService.getHighRiskUsers(anyInt(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/governance/risk-scores/high-risk")
                            .param("threshold", "70")
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].score").value(85));
        }

        @Test
        @DisplayName("Should use default threshold of 70 when not specified")
        void getHighRisk_defaultThreshold_shouldSucceed() throws Exception {
            Page<RiskScoreResponse> page = new PageImpl<>(List.of());
            when(riskScoringService.getHighRiskUsers(eq(70), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/governance/risk-scores/high-risk")
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 403 for developer")
        void getHighRisk_developer_shouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/governance/risk-scores/high-risk")
                            .with(JwtTestUtils.jwtWithRoles("dev", TestConstants.ROLE_DEVELOPER)))
                    .andExpect(status().isForbidden());
        }
    }
}
