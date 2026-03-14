package com.iam.platform.admin.controller;

import com.iam.platform.common.test.ApiResponseAssertions;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("OrgComplianceController Tests")
class OrgComplianceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("Should return 401 when unauthenticated")
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/org/compliance")
                        .param("tenantId", TestConstants.TEST_TENANT_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 200 for tenant-admin")
    void tenantAdmin_returnsCompliance() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/org/compliance")
                        .param("tenantId", TestConstants.TEST_TENANT_ID)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pendingReviews").exists())
                .andExpect(jsonPath("$.data.riskScoreAvg").exists())
                .andExpect(jsonPath("$.data.policyViolations").exists())
                .andExpect(jsonPath("$.data.consentStats").exists())
                .andExpect(jsonPath("$.data.verificationStats").exists());
    }

    @Test
    @DisplayName("Should return compliance response with default zero values")
    void tenantAdmin_returnsDefaultValues() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/org/compliance")
                        .param("tenantId", TestConstants.TEST_TENANT_ID)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingReviews").value(0))
                .andExpect(jsonPath("$.data.riskScoreAvg").value(0.0))
                .andExpect(jsonPath("$.data.policyViolations").value(0));
    }

    @Test
    @DisplayName("Should return 403 for iam-admin")
    void iamAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/org/compliance")
                        .param("tenantId", TestConstants.TEST_TENANT_ID)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 for sector-admin")
    void sectorAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/org/compliance")
                        .param("tenantId", TestConstants.TEST_TENANT_ID)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_SECTOR_ADMIN, TestConstants.ROLE_SECTOR_ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 for governance-admin")
    void governanceAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/org/compliance")
                        .param("tenantId", TestConstants.TEST_TENANT_ID)
                        .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 for report-viewer")
    void reportViewer_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/org/compliance")
                        .param("tenantId", TestConstants.TEST_TENANT_ID)
                        .with(JwtTestUtils.jwtWithRoles("viewer", TestConstants.ROLE_REPORT_VIEWER)))
                .andExpect(status().isForbidden());
    }
}
