package com.iam.platform.monitoring;

import com.iam.platform.common.test.ApiResponseAssertions;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.monitoring.dto.AggregatedHealthResponse;
import com.iam.platform.monitoring.dto.ServiceHealthDto;
import com.iam.platform.monitoring.service.HealthAggregationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthAggregationService healthAggregationService;

    private AggregatedHealthResponse allHealthyResponse() {
        List<ServiceHealthDto> services = List.of(
                new ServiceHealthDto("iam-core-service", "http://localhost:8082", "UP", 45, Instant.now(), "Healthy"),
                new ServiceHealthDto("iam-tenant-service", "http://localhost:8083", "UP", 32, Instant.now(), "Healthy"),
                new ServiceHealthDto("iam-gateway", "http://localhost:8081", "UP", 28, Instant.now(), "Healthy")
        );
        return new AggregatedHealthResponse("UP", 3, 3, 0, services, Instant.now());
    }

    private AggregatedHealthResponse degradedResponse() {
        List<ServiceHealthDto> services = List.of(
                new ServiceHealthDto("iam-core-service", "http://localhost:8082", "UP", 45, Instant.now(), "Healthy"),
                new ServiceHealthDto("iam-audit-service", "http://localhost:8084", "DOWN", 5000, Instant.now(), "Connection refused")
        );
        return new AggregatedHealthResponse("DEGRADED", 2, 1, 1, services, Instant.now());
    }

    @Nested
    @DisplayName("GET /api/v1/monitoring/health — Aggregated health")
    class AggregatedHealth {

        @Test
        @DisplayName("Should return aggregated health for ops-admin")
        void getHealth_opsAdmin_shouldSucceed() throws Exception {
            when(healthAggregationService.getAggregatedHealth()).thenReturn(allHealthyResponse());

            mockMvc.perform(get("/api/v1/monitoring/health")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_OPS_ADMIN, TestConstants.ROLE_OPS_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.overallStatus").value("UP"))
                    .andExpect(jsonPath("$.data.totalServices").value(3))
                    .andExpect(jsonPath("$.data.healthyServices").value(3))
                    .andExpect(jsonPath("$.data.unhealthyServices").value(0))
                    .andExpect(jsonPath("$.data.services").isArray());
        }

        @Test
        @DisplayName("Should return aggregated health for iam-admin")
        void getHealth_iamAdmin_shouldSucceed() throws Exception {
            when(healthAggregationService.getAggregatedHealth()).thenReturn(allHealthyResponse());

            ApiResponseAssertions.assertApiSuccess(
                    mockMvc.perform(get("/api/v1/monitoring/health")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))));
        }

        @Test
        @DisplayName("Should show DEGRADED status when some services are down")
        void getHealth_degraded_shouldShowDegradedStatus() throws Exception {
            when(healthAggregationService.getAggregatedHealth()).thenReturn(degradedResponse());

            mockMvc.perform(get("/api/v1/monitoring/health")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_OPS_ADMIN, TestConstants.ROLE_OPS_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.overallStatus").value("DEGRADED"))
                    .andExpect(jsonPath("$.data.unhealthyServices").value(1))
                    .andExpect(jsonPath("$.data.services[1].status").value("DOWN"));
        }

        @Test
        @DisplayName("Should return 403 for external-user")
        void getHealth_externalUser_shouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/monitoring/health")
                            .with(JwtTestUtils.jwtWithRoles("citizen", TestConstants.ROLE_EXTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 for tenant-admin")
        void getHealth_tenantAdmin_shouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/monitoring/health")
                            .with(JwtTestUtils.jwtWithRoles("user", TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void getHealth_unauthenticated_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/v1/monitoring/health"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/monitoring/services — Service details")
    class ServiceDetails {

        @Test
        @DisplayName("Should return service detail list for ops-admin")
        void getServices_opsAdmin_shouldSucceed() throws Exception {
            when(healthAggregationService.getServiceDetails())
                    .thenReturn(allHealthyResponse().services());

            mockMvc.perform(get("/api/v1/monitoring/services")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_OPS_ADMIN, TestConstants.ROLE_OPS_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].serviceName").value("iam-core-service"))
                    .andExpect(jsonPath("$.data[0].status").value("UP"));
        }

        @Test
        @DisplayName("Should return 403 for governance-admin")
        void getServices_governanceAdmin_shouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/monitoring/services")
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN)))
                    .andExpect(status().isForbidden());
        }
    }
}
