package com.iam.platform.monitoring;

import com.iam.platform.common.test.ApiResponseAssertions;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.monitoring.dto.XRoadMetricsDto;
import com.iam.platform.monitoring.service.XRoadMetricsService;
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
class XRoadMetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private XRoadMetricsService xRoadMetricsService;

    private XRoadMetricsDto sampleMetrics() {
        return new XRoadMetricsDto(
                5000L, 4950L, 50L, 125.5,
                Map.of("GOV", 3000L, "COM", 1500L, "NGO", 400L, "MUN", 100L),
                Map.of("person-lookup", 2000L, "entity-verify", 1500L, "tax-check", 1500L),
                Map.of("person-lookup", 80.5, "entity-verify", 120.3, "tax-check", 175.2)
        );
    }

    @Nested
    @DisplayName("GET /api/v1/monitoring/xroad-metrics — X-Road exchange metrics")
    class XRoadMetrics {

        @Test
        @DisplayName("Should return X-Road metrics for ops-admin")
        void getMetrics_opsAdmin_shouldSucceed() throws Exception {
            when(xRoadMetricsService.getXRoadMetrics(isNull(), isNull())).thenReturn(sampleMetrics());

            mockMvc.perform(get("/api/v1/monitoring/xroad-metrics")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_OPS_ADMIN, TestConstants.ROLE_OPS_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalExchanges").value(5000))
                    .andExpect(jsonPath("$.data.successfulExchanges").value(4950))
                    .andExpect(jsonPath("$.data.failedExchanges").value(50))
                    .andExpect(jsonPath("$.data.averageLatencyMs").value(125.5))
                    .andExpect(jsonPath("$.data.exchangesByMemberClass.GOV").value(3000));
        }

        @Test
        @DisplayName("Should return X-Road metrics for iam-admin")
        void getMetrics_iamAdmin_shouldSucceed() throws Exception {
            when(xRoadMetricsService.getXRoadMetrics(isNull(), isNull())).thenReturn(sampleMetrics());

            ApiResponseAssertions.assertApiSuccess(
                    mockMvc.perform(get("/api/v1/monitoring/xroad-metrics")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))));
        }

        @Test
        @DisplayName("Should return X-Road metrics for service-manager")
        void getMetrics_serviceManager_shouldSucceed() throws Exception {
            when(xRoadMetricsService.getXRoadMetrics(isNull(), isNull())).thenReturn(sampleMetrics());

            mockMvc.perform(get("/api/v1/monitoring/xroad-metrics")
                            .with(JwtTestUtils.jwtWithRoles("svc", TestConstants.ROLE_SERVICE_MANAGER)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.exchangesByService").isMap());
        }

        @Test
        @DisplayName("Should accept tenantId filter")
        void getMetrics_withTenantFilter_shouldSucceed() throws Exception {
            when(xRoadMetricsService.getXRoadMetrics(eq("tenant-gdt"), isNull())).thenReturn(sampleMetrics());

            mockMvc.perform(get("/api/v1/monitoring/xroad-metrics")
                            .param("tenantId", "tenant-gdt")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_OPS_ADMIN, TestConstants.ROLE_OPS_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should accept memberClass filter for Cambodia member classes")
        void getMetrics_withMemberClassFilter_shouldSucceed() throws Exception {
            when(xRoadMetricsService.getXRoadMetrics(isNull(), eq("GOV"))).thenReturn(sampleMetrics());

            mockMvc.perform(get("/api/v1/monitoring/xroad-metrics")
                            .param("memberClass", "GOV")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_OPS_ADMIN, TestConstants.ROLE_OPS_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 403 for tenant-admin")
        void getMetrics_tenantAdmin_shouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/monitoring/xroad-metrics")
                            .with(JwtTestUtils.jwtWithRoles("user", TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 for developer")
        void getMetrics_developer_shouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/monitoring/xroad-metrics")
                            .with(JwtTestUtils.jwtWithRoles("dev", TestConstants.ROLE_DEVELOPER)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 for external-user")
        void getMetrics_externalUser_shouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/monitoring/xroad-metrics")
                            .with(JwtTestUtils.jwtWithRoles("citizen", TestConstants.ROLE_EXTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void getMetrics_unauthenticated_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/v1/monitoring/xroad-metrics"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
