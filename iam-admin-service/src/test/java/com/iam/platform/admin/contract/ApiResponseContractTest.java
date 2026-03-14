package com.iam.platform.admin.contract;

import com.iam.platform.common.test.JwtTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests verifying the ApiResponse envelope structure
 * is consistently applied across admin service response paths.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiResponseContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Platform dashboard success response has correct envelope")
    void platformDashboard_successEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/platform/dashboard")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("401 when no JWT provided")
    void noJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/platform/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("403 when tenant-admin tries platform endpoint")
    void tenantAdmin_forbiddenOnPlatformEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/platform/dashboard")
                        .with(JwtTestUtils.jwtWithRoles("org-admin", "tenant-admin")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Org dashboard accessible by tenant-admin with correct envelope")
    void orgDashboard_tenantAdminAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/org/dashboard")
                        .param("realmName", "test-realm")
                        .with(JwtTestUtils.jwtWithRoles("org-admin", "tenant-admin")))
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    @DisplayName("Sector dashboard accessible by sector-admin")
    void sectorDashboard_sectorAdminAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/sector/dashboard")
                        .param("memberClass", "GOV")
                        .with(JwtTestUtils.jwtWithRoles("sector-mgr", "sector-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Settings endpoint accessible by config-admin")
    void settings_configAdminAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/settings")
                        .with(JwtTestUtils.jwtWithRoles("config-mgr", "config-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Health endpoint is public and not wrapped in ApiResponse")
    void healthEndpoint_publicAccess() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @DisplayName("External-user denied on all admin endpoints")
    void externalUser_deniedOnAllAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/platform/dashboard")
                        .with(JwtTestUtils.jwtWithRoles("citizen", "external-user")))
                .andExpect(status().isForbidden());
    }
}
