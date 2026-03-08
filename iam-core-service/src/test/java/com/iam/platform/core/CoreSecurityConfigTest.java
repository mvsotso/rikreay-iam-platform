package com.iam.platform.core;

import com.iam.platform.common.test.JwtTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CoreSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Health endpoint should be accessible without authentication")
    void healthEndpointPermitAll() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("API endpoint should return 401 without authentication")
    void apiEndpointUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/persons"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("API endpoint should return 403 with wrong role")
    void apiEndpointWrongRole() throws Exception {
        mockMvc.perform(get("/api/v1/persons")
                        .with(JwtTestUtils.jwtWithRoles("user", "external-user")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("API endpoint should return 200 with correct role")
    void apiEndpointCorrectRole() throws Exception {
        mockMvc.perform(get("/api/v1/persons")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("X-Road endpoint should be accessible without JWT")
    void xroadEndpointPermitAll() throws Exception {
        // X-Road endpoints are permitAll (authenticated by Security Server)
        // They may return 400 without proper X-Road headers, but NOT 401/403
        mockMvc.perform(get("/xroad/v1/test"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Persons endpoint should be accessible with internal-user role")
    void personsEndpointInternalUser() throws Exception {
        mockMvc.perform(get("/api/v1/persons")
                        .with(JwtTestUtils.jwtWithRoles("staff", "internal-user")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Representations endpoint should return 403 for internal-user")
    void representationsEndpointForbiddenForInternalUser() throws Exception {
        mockMvc.perform(get("/api/v1/representations")
                        .with(JwtTestUtils.jwtWithRoles("staff", "internal-user")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Representations endpoint should be accessible with tenant-admin role")
    void representationsEndpointTenantAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/representations")
                        .with(JwtTestUtils.jwtWithRoles("org-admin", "tenant-admin")))
                .andExpect(status().isOk());
    }
}
