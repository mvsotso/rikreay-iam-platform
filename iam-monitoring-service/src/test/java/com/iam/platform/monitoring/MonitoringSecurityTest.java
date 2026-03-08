package com.iam.platform.monitoring;

import com.iam.platform.common.test.JwtTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MonitoringSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Health endpoint should be public")
    void healthEndpointPermitAll() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Monitoring API should return 401 without auth")
    void monitoringUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/health"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Monitoring API should return 403 for external-user")
    void monitoringExternalUserForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/health")
                        .with(JwtTestUtils.jwtWithRoles("user", "external-user")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Monitoring API should return 200 for ops-admin")
    void monitoringOpsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/health")
                        .with(JwtTestUtils.jwtWithRoles("ops", "ops-admin")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Monitoring API should return 200 for iam-admin")
    void monitoringIamAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/health")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Auth analytics for tenant should be accessible for tenant-admin")
    void authAnalyticsTenantAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/auth-analytics/tenant/test-tenant")
                        .with(JwtTestUtils.jwtWithRoles("org-admin", "tenant-admin")))
                .andExpect(status().is(s -> s != 401 && s != 403));
    }

    @Test
    @DisplayName("Auth analytics for tenant should be forbidden for external-user")
    void authAnalyticsTenantExternalForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/auth-analytics/tenant/test-tenant")
                        .with(JwtTestUtils.jwtWithRoles("user", "external-user")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("X-Road metrics should be accessible for service-manager")
    void xroadMetricsServiceManager() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/xroad-metrics")
                        .with(JwtTestUtils.jwtWithRoles("svc", "service-manager")))
                .andExpect(status().is(s -> s != 401 && s != 403));
    }

    @Test
    @DisplayName("X-Road metrics should be forbidden for developer")
    void xroadMetricsDeveloperForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/xroad-metrics")
                        .with(JwtTestUtils.jwtWithRoles("dev", "developer")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Incidents should return 403 for tenant-admin")
    void incidentsTenantAdminForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/incidents")
                        .with(JwtTestUtils.jwtWithRoles("user", "tenant-admin")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Incidents should return 200 for ops-admin")
    void incidentsOpsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/incidents")
                        .with(JwtTestUtils.jwtWithRoles("ops", "ops-admin")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Alerts should return 200 for iam-admin")
    void alertsIamAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/alerts")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isOk());
    }
}
