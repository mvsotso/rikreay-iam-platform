package com.iam.platform.tenant.controller;

import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.tenant.service.TenantProvisioningService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exhaustive RBAC tests for TenantController.
 * Verifies that each endpoint enforces the correct role requirements:
 *   POST   /api/v1/tenants              -> iam-admin only
 *   GET    /api/v1/tenants              -> iam-admin, tenant-admin
 *   GET    /api/v1/tenants/{realm}      -> iam-admin, tenant-admin
 *   PUT    /api/v1/tenants/{realm}      -> iam-admin, tenant-admin
 *   PUT    /api/v1/tenants/{r}/suspend  -> iam-admin only
 *   PUT    /api/v1/tenants/{r}/activate -> iam-admin only
 *   DELETE /api/v1/tenants/{realm}      -> iam-admin only
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantControllerRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantProvisioningService tenantProvisioningService;

    private static final String TENANT_JSON = """
            {
                "tenantName": "Test Corp",
                "realmName": "test-corp",
                "memberClass": "COM",
                "adminEmail": "admin@test.com",
                "adminUsername": "admin",
                "adminTempPassword": "Temp1234!"
            }
            """;

    private static final String UPDATE_JSON = """
            {
                "description": "Updated description"
            }
            """;

    // ====== No authentication ======

    @Nested
    @DisplayName("Unauthenticated requests should return 401")
    class Unauthenticated {

        @Test
        @DisplayName("POST /api/v1/tenants without JWT returns 401")
        void createTenantNoAuth() throws Exception {
            mockMvc.perform(post("/api/v1/tenants")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(TENANT_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/v1/tenants without JWT returns 401")
        void listTenantsNoAuth() throws Exception {
            mockMvc.perform(get("/api/v1/tenants"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/v1/tenants/{realm} without JWT returns 401")
        void getTenantNoAuth() throws Exception {
            mockMvc.perform(get("/api/v1/tenants/test-realm"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("PUT /api/v1/tenants/{realm} without JWT returns 401")
        void updateTenantNoAuth() throws Exception {
            mockMvc.perform(put("/api/v1/tenants/test-realm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("PUT /api/v1/tenants/{realm}/suspend without JWT returns 401")
        void suspendTenantNoAuth() throws Exception {
            mockMvc.perform(put("/api/v1/tenants/test-realm/suspend"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("PUT /api/v1/tenants/{realm}/activate without JWT returns 401")
        void activateTenantNoAuth() throws Exception {
            mockMvc.perform(put("/api/v1/tenants/test-realm/activate"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("DELETE /api/v1/tenants/{realm} without JWT returns 401")
        void deleteTenantNoAuth() throws Exception {
            mockMvc.perform(delete("/api/v1/tenants/test-realm"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ====== POST /api/v1/tenants — iam-admin only ======

    @Nested
    @DisplayName("POST /api/v1/tenants — iam-admin only")
    class CreateTenant {

        @Test
        @DisplayName("iam-admin can create tenant")
        void iamAdminCanCreate() throws Exception {
            mockMvc.perform(post("/api/v1/tenants")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(TENANT_JSON))
                    .andExpect(status().isCreated());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                TestConstants.ROLE_TENANT_ADMIN,
                TestConstants.ROLE_SECTOR_ADMIN,
                TestConstants.ROLE_OPS_ADMIN,
                TestConstants.ROLE_CONFIG_ADMIN,
                TestConstants.ROLE_AUDITOR,
                TestConstants.ROLE_DEVELOPER,
                TestConstants.ROLE_INTERNAL_USER,
                TestConstants.ROLE_EXTERNAL_USER,
                TestConstants.ROLE_API_ACCESS,
                TestConstants.ROLE_SERVICE_MANAGER,
                TestConstants.ROLE_GOVERNANCE_ADMIN,
                TestConstants.ROLE_REPORT_VIEWER
        })
        @DisplayName("Non-admin roles should be denied creating tenants")
        void nonAdminCannotCreate(String role) throws Exception {
            mockMvc.perform(post("/api/v1/tenants")
                            .with(JwtTestUtils.jwtWithRoles("denied-user", role))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(TENANT_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    // ====== GET /api/v1/tenants — iam-admin, tenant-admin ======

    @Nested
    @DisplayName("GET /api/v1/tenants — iam-admin, tenant-admin")
    class ListTenants {

        @Test
        @DisplayName("iam-admin can list tenants")
        void iamAdminCanList() throws Exception {
            mockMvc.perform(get("/api/v1/tenants")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("tenant-admin can list tenants")
        void tenantAdminCanList() throws Exception {
            mockMvc.perform(get("/api/v1/tenants")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isOk());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                TestConstants.ROLE_SECTOR_ADMIN,
                TestConstants.ROLE_OPS_ADMIN,
                TestConstants.ROLE_AUDITOR,
                TestConstants.ROLE_DEVELOPER,
                TestConstants.ROLE_EXTERNAL_USER,
                TestConstants.ROLE_API_ACCESS,
                TestConstants.ROLE_INTERNAL_USER,
                TestConstants.ROLE_REPORT_VIEWER
        })
        @DisplayName("Unauthorized roles should be denied listing tenants")
        void unauthorizedCannotList(String role) throws Exception {
            mockMvc.perform(get("/api/v1/tenants")
                            .with(JwtTestUtils.jwtWithRoles("denied-user", role)))
                    .andExpect(status().isForbidden());
        }
    }

    // ====== GET /api/v1/tenants/{realmName} — iam-admin, tenant-admin ======

    @Nested
    @DisplayName("GET /api/v1/tenants/{realmName} — iam-admin, tenant-admin")
    class GetTenant {

        @Test
        @DisplayName("iam-admin can get tenant by realm name")
        void iamAdminCanGet() throws Exception {
            mockMvc.perform(get("/api/v1/tenants/test-realm")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("tenant-admin can get tenant by realm name")
        void tenantAdminCanGet() throws Exception {
            mockMvc.perform(get("/api/v1/tenants/test-realm")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("external-user should be denied getting tenant")
        void externalUserCannotGet() throws Exception {
            mockMvc.perform(get("/api/v1/tenants/test-realm")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }
    }

    // ====== PUT /api/v1/tenants/{realmName} — iam-admin, tenant-admin ======

    @Nested
    @DisplayName("PUT /api/v1/tenants/{realmName} — iam-admin, tenant-admin")
    class UpdateTenant {

        @Test
        @DisplayName("iam-admin can update tenant")
        void iamAdminCanUpdate() throws Exception {
            mockMvc.perform(put("/api/v1/tenants/test-realm")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("tenant-admin can update tenant")
        void tenantAdminCanUpdate() throws Exception {
            mockMvc.perform(put("/api/v1/tenants/test-realm")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("developer should be denied updating tenant")
        void developerCannotUpdate() throws Exception {
            mockMvc.perform(put("/api/v1/tenants/test-realm")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_DEVELOPER, TestConstants.ROLE_DEVELOPER))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    // ====== PUT /api/v1/tenants/{realmName}/suspend — iam-admin only ======

    @Nested
    @DisplayName("PUT /api/v1/tenants/{realmName}/suspend — iam-admin only")
    class SuspendTenant {

        @Test
        @DisplayName("iam-admin can suspend tenant")
        void iamAdminCanSuspend() throws Exception {
            mockMvc.perform(put("/api/v1/tenants/test-realm/suspend")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("tenant-admin should be denied suspending tenant")
        void tenantAdminCannotSuspend() throws Exception {
            mockMvc.perform(put("/api/v1/tenants/test-realm/suspend")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ops-admin should be denied suspending tenant")
        void opsAdminCannotSuspend() throws Exception {
            mockMvc.perform(put("/api/v1/tenants/test-realm/suspend")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_OPS_ADMIN, TestConstants.ROLE_OPS_ADMIN)))
                    .andExpect(status().isForbidden());
        }
    }

    // ====== PUT /api/v1/tenants/{realmName}/activate — iam-admin only ======

    @Nested
    @DisplayName("PUT /api/v1/tenants/{realmName}/activate — iam-admin only")
    class ActivateTenant {

        @Test
        @DisplayName("iam-admin can activate tenant")
        void iamAdminCanActivate() throws Exception {
            mockMvc.perform(put("/api/v1/tenants/test-realm/activate")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("tenant-admin should be denied activating tenant")
        void tenantAdminCannotActivate() throws Exception {
            mockMvc.perform(put("/api/v1/tenants/test-realm/activate")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isForbidden());
        }
    }

    // ====== DELETE /api/v1/tenants/{realmName} — iam-admin only ======

    @Nested
    @DisplayName("DELETE /api/v1/tenants/{realmName} — iam-admin only")
    class DeleteTenant {

        @Test
        @DisplayName("iam-admin can delete tenant")
        void iamAdminCanDelete() throws Exception {
            mockMvc.perform(delete("/api/v1/tenants/test-realm")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("tenant-admin should be denied deleting tenant")
        void tenantAdminCannotDelete() throws Exception {
            mockMvc.perform(delete("/api/v1/tenants/test-realm")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isForbidden());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                TestConstants.ROLE_SECTOR_ADMIN,
                TestConstants.ROLE_OPS_ADMIN,
                TestConstants.ROLE_AUDITOR,
                TestConstants.ROLE_DEVELOPER,
                TestConstants.ROLE_EXTERNAL_USER,
                TestConstants.ROLE_API_ACCESS
        })
        @DisplayName("Non-admin roles should be denied deleting tenants")
        void nonAdminCannotDelete(String role) throws Exception {
            mockMvc.perform(delete("/api/v1/tenants/test-realm")
                            .with(JwtTestUtils.jwtWithRoles("denied-user", role)))
                    .andExpect(status().isForbidden());
        }
    }

    // ====== Health endpoint ======

    @Nested
    @DisplayName("Public endpoints")
    class PublicEndpoints {

        @Test
        @DisplayName("Health endpoint should be accessible without JWT")
        void healthEndpointPublic() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk());
        }
    }
}
