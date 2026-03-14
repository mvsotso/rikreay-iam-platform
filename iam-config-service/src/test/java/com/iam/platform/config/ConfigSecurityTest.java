package com.iam.platform.config;

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
class ConfigSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Health endpoint should be accessible without authentication")
    void healthEndpointPermitAll() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Config API should return 401 without authentication")
    void configApiUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/config/flags"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Config API should return 403 with wrong role")
    void configApiWrongRole() throws Exception {
        mockMvc.perform(get("/api/v1/config/flags")
                        .with(JwtTestUtils.jwtWithRoles("user", "external-user")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Config API should return 200 with config-admin role")
    void configApiConfigAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/config/flags")
                        .with(JwtTestUtils.jwtWithRoles("admin", "config-admin")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Config API should return 200 with iam-admin role")
    void configApiIamAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/config/flags")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Config history should return 401 without auth")
    void configHistoryUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/config/history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Config rollback should return 403 for tenant-admin")
    void configRollbackWrongRole() throws Exception {
        mockMvc.perform(post("/api/v1/config/rollback/1")
                        .with(JwtTestUtils.jwtWithRoles("user", "tenant-admin")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Config rollback should not be forbidden for config-admin")
    void configRollbackConfigAdmin() throws Exception {
        // Will get 500 or similar due to missing data, but NOT 401/403
        int statusCode = mockMvc.perform(post("/api/v1/config/rollback/1")
                        .with(JwtTestUtils.jwtWithRoles("admin", "config-admin")))
                .andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(statusCode).isNotIn(401, 403);
    }

    @Test
    @DisplayName("Feature flag CRUD should be forbidden for ops-admin")
    void featureFlagOpsAdminForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/config/flags")
                        .with(JwtTestUtils.jwtWithRoles("ops", "ops-admin")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Feature flag CRUD should be forbidden for developer")
    void featureFlagDeveloperForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/config/flags")
                        .with(JwtTestUtils.jwtWithRoles("dev", "developer")))
                .andExpect(status().isForbidden());
    }
}
