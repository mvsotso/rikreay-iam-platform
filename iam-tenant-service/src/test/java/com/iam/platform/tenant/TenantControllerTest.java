package com.iam.platform.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.tenant.dto.CreateTenantRequest;
import com.iam.platform.tenant.dto.TenantResponse;
import com.iam.platform.tenant.enums.TenantStatus;
import com.iam.platform.tenant.service.TenantProvisioningService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TenantProvisioningService tenantService;

    @Test
    @DisplayName("POST /api/v1/tenants should return 401 without authentication")
    void createTenantUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/tenants should return 403 for tenant-admin")
    void createTenantForbiddenForTenantAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/tenants")
                        .with(JwtTestUtils.jwtWithRoles("org-admin", "tenant-admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/tenants should return 201 for iam-admin")
    void createTenantSuccessForIamAdmin() throws Exception {
        TenantResponse response = new TenantResponse(
                UUID.randomUUID(), "Test Org", "test-org", "Test",
                com.iam.platform.common.enums.MemberClass.GOV, null,
                null, null, null, null,
                TenantStatus.ACTIVE, "admin@test.org", "admin",
                null, Instant.now(), Instant.now());

        when(tenantService.createTenant(any())).thenReturn(response);

        CreateTenantRequest request = new CreateTenantRequest(
                "Test Org", "test-org", "Test",
                com.iam.platform.common.enums.MemberClass.GOV, null,
                null, null, null, null,
                "admin@test.org", "admin", "TempPass123!", null);

        mockMvc.perform(post("/api/v1/tenants")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/tenants should return 200 for iam-admin")
    void listTenantsIamAdmin() throws Exception {
        Page<TenantResponse> page = new PageImpl<>(List.of());
        when(tenantService.listTenants(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/tenants")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/tenants should return 200 for tenant-admin")
    void listTenantsTenantAdmin() throws Exception {
        Page<TenantResponse> page = new PageImpl<>(List.of());
        when(tenantService.listTenants(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/tenants")
                        .with(JwtTestUtils.jwtWithRoles("org-admin", "tenant-admin")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/tenants/{realm} should return 403 for tenant-admin")
    void deleteTenantForbiddenForTenantAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/tenants/test-realm")
                        .with(JwtTestUtils.jwtWithRoles("org-admin", "tenant-admin")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/tenants should return 401 without authentication")
    void listTenantsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/tenants"))
                .andExpect(status().isUnauthorized());
    }
}
