package com.iam.platform.developer;

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
class DeveloperSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Health endpoint should be public")
    void healthEndpointPermitAll() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    // Public endpoints
    @Test
    @DisplayName("API docs should be accessible without authentication")
    void apiDocsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("SDK info should be accessible without authentication")
    void sdksPublic() throws Exception {
        mockMvc.perform(get("/api/v1/sdks"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Service docs should be accessible without authentication")
    void serviceDocsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/docs/services"))
                .andExpect(status().isOk());
    }

    // App endpoints — developer, iam-admin
    @Test
    @DisplayName("Apps should return 401 without auth")
    void appsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/apps"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Apps should return 403 for external-user")
    void appsExternalUserForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/apps")
                        .with(JwtTestUtils.jwtWithRoles("user", "external-user")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Apps should return 200 for developer")
    void appsDeveloper() throws Exception {
        mockMvc.perform(get("/api/v1/apps")
                        .with(JwtTestUtils.jwtWithRoles("dev", "developer")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Apps should return 200 for iam-admin")
    void appsIamAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/apps")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Apps should return 403 for ops-admin")
    void appsOpsAdminForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/apps")
                        .with(JwtTestUtils.jwtWithRoles("ops", "ops-admin")))
                .andExpect(status().isForbidden());
    }

    // Webhook endpoints — developer, iam-admin
    @Test
    @DisplayName("Webhooks should return 401 without auth")
    void webhooksUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Webhooks should return 403 for tenant-admin")
    void webhooksTenantAdminForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks")
                        .with(JwtTestUtils.jwtWithRoles("user", "tenant-admin"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // Sandbox endpoints — developer, iam-admin
    @Test
    @DisplayName("Sandbox should return 401 without auth")
    void sandboxUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/sandbox/realms"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Sandbox should return 403 for auditor")
    void sandboxAuditorForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/sandbox/realms")
                        .with(JwtTestUtils.jwtWithRoles("auditor", "auditor")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sandbox should succeed for developer")
    void sandboxDeveloper() throws Exception {
        mockMvc.perform(get("/api/v1/sandbox/realms")
                        .with(JwtTestUtils.jwtWithRoles("dev", "developer")))
                .andExpect(status().isOk());
    }
}
