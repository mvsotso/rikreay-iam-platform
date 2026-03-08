package com.iam.platform.admin;

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
    @DisplayName("Org dashboard should succeed for tenant-admin")
    void orgDashboardTenantAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/org/dashboard")
                        .with(JwtTestUtils.jwtWithRoles("org-admin", "tenant-admin")))
                .andExpect(status().is(s -> s != 401 && s != 403));
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
    @DisplayName("Sector dashboard should succeed for sector-admin")
    void sectorDashboardSectorAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/sector/dashboard")
                        .with(JwtTestUtils.jwtWithRoles("sector", "sector-admin")))
                .andExpect(status().is(s -> s != 401 && s != 403));
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
    @DisplayName("Bulk import should succeed for iam-admin")
    void bulkImportIamAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/platform-admin/users/bulk-import")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin"))
                        .contentType("application/json")
                        .content("[]"))
                .andExpect(status().is(s -> s != 401 && s != 403));
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
                .andExpect(status().is(s -> s != 401 && s != 403));
    }

    // Uses /platform-admin/ path not /admin/
    @Test
    @DisplayName("Admin service uses /platform-admin/ path prefix")
    void platformAdminPathPrefix() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/platform/dashboard")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isOk());

        // Ensure /admin/ path does NOT exist
        mockMvc.perform(get("/api/v1/admin/platform/dashboard")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().is(s -> s == 401 || s == 404));
    }

    @Test
    @DisplayName("Health endpoint should be public")
    void healthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
