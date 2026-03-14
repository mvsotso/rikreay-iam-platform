package com.iam.platform.notification;

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
class NotificationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Health endpoint should be public")
    void healthEndpointPermitAll() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Notification logs should return 401 without auth")
    void logsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Notification logs should return 403 for external-user")
    void logsExternalUserForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .with(JwtTestUtils.jwtWithRoles("user", "external-user")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Notification logs should return 200 for iam-admin")
    void logsIamAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Notification logs should return 200 for ops-admin")
    void logsOpsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .with(JwtTestUtils.jwtWithRoles("ops", "ops-admin")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Templates GET should be accessible to ops-admin (GET wildcard rule matches first)")
    void templatesOpsAdminAllowed() throws Exception {
        // SecurityConfig: GET /api/v1/notifications/** allows ops-admin before templates/** rule
        int status = mockMvc.perform(get("/api/v1/notifications/templates")
                        .with(JwtTestUtils.jwtWithRoles("ops", "ops-admin")))
                .andReturn().getResponse().getStatus();
        assertThat(status).isNotIn(401, 403);
    }

    @Test
    @DisplayName("Templates GET should pass security for iam-admin")
    void templatesIamAdmin() throws Exception {
        // SecurityConfig allows iam-admin; may get 500 from H2 array type incompatibility
        int status = mockMvc.perform(get("/api/v1/notifications/templates")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andReturn().getResponse().getStatus();
        assertThat(status).isNotIn(401, 403);
    }

    @Test
    @DisplayName("Channels GET should be accessible to ops-admin (GET wildcard rule matches first)")
    void channelsOpsAdminAllowed() throws Exception {
        // SecurityConfig: GET /api/v1/notifications/** allows ops-admin before channels/** rule
        mockMvc.perform(get("/api/v1/notifications/channels")
                        .with(JwtTestUtils.jwtWithRoles("ops", "ops-admin")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Channels should return 200 for iam-admin")
    void channelsIamAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/channels")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Schedules should pass security for ops-admin")
    void schedulesOpsAdmin() throws Exception {
        // SecurityConfig allows ops-admin for schedules; may get 500 from H2 array type incompatibility
        int status = mockMvc.perform(get("/api/v1/notifications/schedules")
                        .with(JwtTestUtils.jwtWithRoles("ops", "ops-admin")))
                .andReturn().getResponse().getStatus();
        assertThat(status).isNotIn(401, 403);
    }

    @Test
    @DisplayName("Notification send should return 403 for developer role")
    void sendDeveloperForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/send")
                        .with(JwtTestUtils.jwtWithRoles("dev", "developer"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
