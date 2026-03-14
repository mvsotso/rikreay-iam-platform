package com.iam.platform.admin.controller;

import com.iam.platform.admin.dto.OrgDashboardResponse;
import com.iam.platform.admin.service.OrgDashboardService;
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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("OrgDashboardController Tests")
class OrgDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrgDashboardService orgDashboardService;

    @SuppressWarnings("unused")
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final OrgDashboardResponse SAMPLE_DASHBOARD = new OrgDashboardResponse(
            25, 5, 3, 2, 1500, 200, 10, 0.75
    );

    @Test
    @DisplayName("Should return 401 when unauthenticated")
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/org/dashboard")
                        .param("realmName", TestConstants.TEST_REALM_NAME))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 200 for tenant-admin")
    void tenantAdmin_returnsDashboard() throws Exception {
        when(orgDashboardService.getOrgDashboard(TestConstants.TEST_REALM_NAME)).thenReturn(SAMPLE_DASHBOARD);

        ApiResponseAssertions.assertApiSuccess(
                mockMvc.perform(get("/api/v1/platform-admin/org/dashboard")
                        .param("realmName", TestConstants.TEST_REALM_NAME)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
        );
    }

    @Test
    @DisplayName("Should return dashboard data fields correctly")
    void tenantAdmin_returnsCorrectFields() throws Exception {
        when(orgDashboardService.getOrgDashboard(TestConstants.TEST_REALM_NAME)).thenReturn(SAMPLE_DASHBOARD);

        mockMvc.perform(get("/api/v1/platform-admin/org/dashboard")
                        .param("realmName", TestConstants.TEST_REALM_NAME)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userCount").value(25))
                .andExpect(jsonPath("$.data.activeSessionCount").value(5))
                .andExpect(jsonPath("$.data.apiCallsThisMonth").value(1500))
                .andExpect(jsonPath("$.data.xroadTransactionsThisMonth").value(200))
                .andExpect(jsonPath("$.data.mfaAdoptionRate").value(0.75));
    }

    @Test
    @DisplayName("Should return 403 for iam-admin (org endpoints are tenant-admin only)")
    void iamAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/org/dashboard")
                        .param("realmName", TestConstants.TEST_REALM_NAME)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                .andExpect(status().isForbidden());

        verify(orgDashboardService, never()).getOrgDashboard(TestConstants.TEST_REALM_NAME);
    }

    @Test
    @DisplayName("Should return 403 for sector-admin")
    void sectorAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/org/dashboard")
                        .param("realmName", TestConstants.TEST_REALM_NAME)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_SECTOR_ADMIN, TestConstants.ROLE_SECTOR_ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 for external-user")
    void externalUser_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/org/dashboard")
                        .param("realmName", TestConstants.TEST_REALM_NAME)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 for ops-admin")
    void opsAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/org/dashboard")
                        .param("realmName", TestConstants.TEST_REALM_NAME)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_OPS_ADMIN, TestConstants.ROLE_OPS_ADMIN)))
                .andExpect(status().isForbidden());
    }
}
