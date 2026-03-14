package com.iam.platform.admin;

import com.iam.platform.common.test.JwtTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // Platform endpoints — iam-admin only
    @Test
    @DisplayName("Platform dashboard should return 401 without auth")
    void platformDashboardUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/platform/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Platform dashboard should return 403 for tenant-admin")
    void platformDashboardTenantAdminForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/platform/dashboard")
                        .with(JwtTestUtils.jwtWithRoles("user", "tenant-admin")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Platform dashboard should return 200 for iam-admin")
    void platformDashboardIamAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/platform/dashboard")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isOk());
    }

    // Org endpoints — tenant-admin only
    @Test
    @DisplayName("Org dashboard should return 403 for iam-admin")
    void orgDashboardIamAdminForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/org/dashboard")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Org dashboard should pass authorization for tenant-admin (not 403)")
    void orgDashboardTenantAdmin() throws Exception {
        int statusCode = mockMvc.perform(get("/api/v1/platform-admin/org/dashboard")
                        .param("realmName", "test-realm")
                        .with(JwtTestUtils.jwtWithRoles("org-admin", "tenant-admin")))
                .andReturn().getResponse().getStatus();
        // May be 200 or 500 (Keycloak not available) but should not be 403
        assertThat(statusCode).isNotEqualTo(403);
    }

    // Sector endpoints — sector-admin only
    @Test
    @DisplayName("Sector dashboard should return 403 for tenant-admin")
    void sectorDashboardTenantAdminForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/sector/dashboard")
                        .with(JwtTestUtils.jwtWithRoles("user", "tenant-admin")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sector dashboard should pass authorization for sector-admin (not 403)")
    void sectorDashboardSectorAdmin() throws Exception {
        int statusCode = mockMvc.perform(get("/api/v1/platform-admin/sector/dashboard")
                        .param("memberClass", "GOV")
                        .with(JwtTestUtils.jwtWithRoles("sector", "sector-admin")))
                .andReturn().getResponse().getStatus();
        // Should not be 403 — sector-admin is authorized
        assertThat(statusCode).isNotEqualTo(403);
    }

    // Bulk import — iam-admin only
    @Test
    @DisplayName("Bulk import should return 403 for tenant-admin")
    void bulkImportTenantAdminForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/platform-admin/users/bulk-import")
                        .with(JwtTestUtils.jwtWithRoles("user", "tenant-admin"))
                        .contentType("application/json")
                        .content("[]"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Bulk import should pass authorization for iam-admin (not 403)")
    void bulkImportIamAdmin() throws Exception {
        String validBody = """
                {"realmName": "test-realm", "users": [{"username": "u1", "email": "u1@test.com"}]}
                """;
        int statusCode = mockMvc.perform(post("/api/v1/platform-admin/users/bulk-import")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin"))
                        .contentType("application/json")
                        .content(validBody))
                .andReturn().getResponse().getStatus();
        // Should not be 403 — iam-admin is authorized
        assertThat(statusCode).isNotEqualTo(403);
    }

    // User list — iam-admin OR tenant-admin
    @Test
    @DisplayName("User list should return 403 for external-user")
    void userListExternalUserForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/users")
                        .with(JwtTestUtils.jwtWithRoles("user", "external-user")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User list should succeed for tenant-admin")
    void userListTenantAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/users")
                        .with(JwtTestUtils.jwtWithRoles("org-admin", "tenant-admin")))
                .andExpect(status().isOk());
    }

    // Uses /platform-admin/ path not /admin/
    @Test
    @DisplayName("Admin service uses /platform-admin/ path prefix")
    void platformAdminPathPrefix() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/platform/dashboard")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isOk());

        // Ensure /admin/ path does NOT exist (404 or 500 via GlobalExceptionHandler)
        int statusCode = mockMvc.perform(get("/api/v1/admin/platform/dashboard")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andReturn().getResponse().getStatus();
        assertThat(statusCode).isNotEqualTo(200);
    }

    @Test
    @DisplayName("Health endpoint should be public")
    void healthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
