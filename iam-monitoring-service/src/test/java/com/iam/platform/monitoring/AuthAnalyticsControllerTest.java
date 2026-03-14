package com.iam.platform.monitoring;

import com.iam.platform.common.test.ApiResponseAssertions;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.monitoring.dto.AuthAnalyticsDto;
import com.iam.platform.monitoring.service.AuthAnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthAnalyticsService authAnalyticsService;

    private AuthAnalyticsDto sampleAnalytics() {
        return new AuthAnalyticsDto(
                1000L, 950L, 50L,
                95.0, 72.5,
                Map.of("iam-platform", 800L, "tenant-a", 200L),
                Map.of("invalid_password", 30L, "account_locked", 20L),
                Map.of("09:00", 150L, "10:00", 200L)
        );
    }

    @Nested
    @DisplayName("GET /api/v1/monitoring/auth-analytics — Platform-wide analytics")
    class PlatformAnalytics {

        @Test
        @DisplayName("Should return auth analytics for ops-admin")
        void getAnalytics_opsAdmin_shouldSucceed() throws Exception {
            when(authAnalyticsService.getAuthAnalytics(isNull())).thenReturn(sampleAnalytics());

            mockMvc.perform(get("/api/v1/monitoring/auth-analytics")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_OPS_ADMIN, TestConstants.ROLE_OPS_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalLogins").value(1000))
                    .andExpect(jsonPath("$.data.successfulLogins").value(950))
                    .andExpect(jsonPath("$.data.failedLogins").value(50))
                    .andExpect(jsonPath("$.data.successRate").value(95.0))
                    .andExpect(jsonPath("$.data.mfaAdoptionRate").value(72.5));
        }

        @Test
        @DisplayName("Should return auth analytics for iam-admin")
        void getAnalytics_iamAdmin_shouldSucceed() throws Exception {
            when(authAnalyticsService.getAuthAnalytics(isNull())).thenReturn(sampleAnalytics());

            ApiResponseAssertions.assertApiSuccess(
                    mockMvc.perform(get("/api/v1/monitoring/auth-analytics")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))));
        }

        @Test
        @DisplayName("Should accept optional tenantId filter")
        void getAnalytics_withTenantFilter_shouldSucceed() throws Exception {
            when(authAnalyticsService.getAuthAnalytics(eq("tenant-a"))).thenReturn(sampleAnalytics());

            mockMvc.perform(get("/api/v1/monitoring/auth-analytics")
                            .param("tenantId", "tenant-a")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_OPS_ADMIN, TestConstants.ROLE_OPS_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 403 for external-user")
        void getAnalytics_externalUser_shouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/monitoring/auth-analytics")
                            .with(JwtTestUtils.jwtWithRoles("citizen", TestConstants.ROLE_EXTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 for governance-admin")
        void getAnalytics_governanceAdmin_shouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/monitoring/auth-analytics")
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void getAnalytics_unauthenticated_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/v1/monitoring/auth-analytics"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/monitoring/auth-analytics/tenant/{tenantId} — Tenant analytics")
    class TenantAnalytics {

        @Test
        @DisplayName("Should return tenant analytics for ops-admin")
        void getTenantAnalytics_opsAdmin_shouldSucceed() throws Exception {
            when(authAnalyticsService.getTenantAuthAnalytics("test-tenant")).thenReturn(sampleAnalytics());

            mockMvc.perform(get("/api/v1/monitoring/auth-analytics/tenant/test-tenant")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_OPS_ADMIN, TestConstants.ROLE_OPS_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalLogins").value(1000));
        }

        @Test
        @DisplayName("Should return tenant analytics for iam-admin")
        void getTenantAnalytics_iamAdmin_shouldSucceed() throws Exception {
            when(authAnalyticsService.getTenantAuthAnalytics("test-tenant")).thenReturn(sampleAnalytics());

            mockMvc.perform(get("/api/v1/monitoring/auth-analytics/tenant/test-tenant")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should allow tenant-admin to view their own tenant analytics")
        void getTenantAnalytics_tenantAdmin_shouldSucceed() throws Exception {
            when(authAnalyticsService.getTenantAuthAnalytics("my-tenant")).thenReturn(sampleAnalytics());

            mockMvc.perform(get("/api/v1/monitoring/auth-analytics/tenant/my-tenant")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 403 for external-user on tenant analytics")
        void getTenantAnalytics_externalUser_shouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/monitoring/auth-analytics/tenant/test-tenant")
                            .with(JwtTestUtils.jwtWithRoles("citizen", TestConstants.ROLE_EXTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 for developer on tenant analytics")
        void getTenantAnalytics_developer_shouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/monitoring/auth-analytics/tenant/test-tenant")
                            .with(JwtTestUtils.jwtWithRoles("dev", TestConstants.ROLE_DEVELOPER)))
                    .andExpect(status().isForbidden());
        }
    }
}
