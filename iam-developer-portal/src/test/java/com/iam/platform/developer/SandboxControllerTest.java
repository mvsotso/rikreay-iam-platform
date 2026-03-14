package com.iam.platform.developer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.common.test.ApiResponseAssertions;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.developer.dto.SandboxRequest;
import com.iam.platform.developer.dto.SandboxResponse;
import com.iam.platform.developer.enums.SandboxStatus;
import com.iam.platform.developer.service.SandboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for SandboxController — requires developer or iam-admin role.
 * Verifies CRUD operations, auth enforcement, and service delegation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SandboxControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SandboxService sandboxService;

    private static final String SANDBOX_URL = "/api/v1/sandbox/realms";

    @Test
    @DisplayName("POST /api/v1/sandbox/realms — developer should create sandbox")
    void createSandbox_asDeveloper_shouldSucceed() throws Exception {
        SandboxRequest request = new SandboxRequest("test");
        SandboxResponse response = new SandboxResponse(
                UUID.randomUUID(), "test-abcd1234", "dev-user",
                SandboxStatus.ACTIVE, Instant.now().plus(7, ChronoUnit.DAYS), Instant.now());

        when(sandboxService.createSandbox(any(SandboxRequest.class), eq("dev-user")))
                .thenReturn(response);

        ApiResponseAssertions.assertApiSuccess(
                mockMvc.perform(post(SANDBOX_URL)
                        .with(JwtTestUtils.jwtWithRoles("dev-user", TestConstants.ROLE_DEVELOPER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
        ).andExpect(jsonPath("$.data.realmName").value("test-abcd1234"))
         .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/sandbox/realms — without body should use default prefix")
    void createSandbox_noBody_shouldUseDefaultPrefix() throws Exception {
        SandboxResponse response = new SandboxResponse(
                UUID.randomUUID(), "sandbox-abcd1234", "dev-user",
                SandboxStatus.ACTIVE, Instant.now().plus(7, ChronoUnit.DAYS), Instant.now());

        when(sandboxService.createSandbox(any(SandboxRequest.class), eq("dev-user")))
                .thenReturn(response);

        mockMvc.perform(post(SANDBOX_URL)
                        .with(JwtTestUtils.jwtWithRoles("dev-user", TestConstants.ROLE_DEVELOPER))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/sandbox/realms — developer should list sandboxes")
    void listSandboxes_asDeveloper_shouldSucceed() throws Exception {
        List<SandboxResponse> sandboxes = List.of(
                new SandboxResponse(UUID.randomUUID(), "sandbox-1", "dev-user",
                        SandboxStatus.ACTIVE, Instant.now().plus(7, ChronoUnit.DAYS), Instant.now())
        );
        when(sandboxService.getSandboxesByOwner("dev-user")).thenReturn(sandboxes);

        ApiResponseAssertions.assertApiSuccess(
                mockMvc.perform(get(SANDBOX_URL)
                        .with(JwtTestUtils.jwtWithRoles("dev-user", TestConstants.ROLE_DEVELOPER)))
        ).andExpect(jsonPath("$.data").isArray())
         .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("DELETE /api/v1/sandbox/realms/{id} — developer should delete sandbox")
    void deleteSandbox_asDeveloper_shouldSucceed() throws Exception {
        UUID sandboxId = UUID.randomUUID();
        doNothing().when(sandboxService).deleteSandbox(sandboxId, "dev-user");

        mockMvc.perform(delete(SANDBOX_URL + "/" + sandboxId)
                        .with(JwtTestUtils.jwtWithRoles("dev-user", TestConstants.ROLE_DEVELOPER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(sandboxService).deleteSandbox(sandboxId, "dev-user");
    }

    @Test
    @DisplayName("POST /api/v1/sandbox/realms — unauthenticated should get 401")
    void createSandbox_noAuth_shouldReturn401() throws Exception {
        mockMvc.perform(post(SANDBOX_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/sandbox/realms — external-user should get 403")
    void listSandboxes_asExternalUser_shouldReturn403() throws Exception {
        mockMvc.perform(get(SANDBOX_URL)
                        .with(JwtTestUtils.jwtWithRoles("user", TestConstants.ROLE_EXTERNAL_USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/sandbox/realms — iam-admin should also create sandbox")
    void createSandbox_asIamAdmin_shouldSucceed() throws Exception {
        SandboxResponse response = new SandboxResponse(
                UUID.randomUUID(), "sandbox-admin", "admin-user",
                SandboxStatus.ACTIVE, Instant.now().plus(7, ChronoUnit.DAYS), Instant.now());

        when(sandboxService.createSandbox(any(SandboxRequest.class), eq("admin-user")))
                .thenReturn(response);

        mockMvc.perform(post(SANDBOX_URL)
                        .with(JwtTestUtils.jwtWithRoles("admin-user", TestConstants.ROLE_IAM_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.realmName").value("sandbox-admin"));
    }
}
