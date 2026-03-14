package com.iam.platform.admin.controller;

import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.RbacTestSupport;
import com.iam.platform.common.test.TestConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive RBAC matrix test for iam-admin-service.
 * Tests all 13 roles against all major endpoint groups.
 *
 * For allowed roles: verifies the response is NOT 403 (may be 200, 400, or 500
 * depending on external service availability).
 * For denied roles: verifies the response IS 403.
 *
 * Endpoint groups tested:
 * - /api/v1/platform-admin/platform/** (iam-admin only)
 * - /api/v1/platform-admin/sector-admins/** (iam-admin only)
 * - /api/v1/platform-admin/sector/** (sector-admin only)
 * - /api/v1/platform-admin/org/** (tenant-admin only)
 * - /api/v1/platform-admin/users (iam-admin, tenant-admin)
 * - /api/v1/platform-admin/users/bulk-* (iam-admin only)
 * - /api/v1/platform-admin/settings (iam-admin, config-admin)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Admin Service RBAC Matrix Test -- 13 roles x all endpoint groups")
class AdminRbacMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Asserts that the response status is NOT 403 Forbidden.
     * Used for allowed roles where the actual response may vary
     * (200, 400, 500) depending on external service availability.
     */
    private void assertNotForbidden(ResultActions result) throws Exception {
        int statusCode = result.andReturn().getResponse().getStatus();
        assertThat(statusCode)
                .as("Expected role to pass authorization (not 403)")
                .isNotEqualTo(403);
    }

    // ====== Platform endpoints: iam-admin only ======

    @Nested
    @DisplayName("Platform endpoints (/api/v1/platform-admin/platform/**) -- iam-admin only")
    class PlatformEndpoints {

        static Stream<Arguments> platformDashboardMatrix() {
            return RbacTestSupport.fullMatrix("GET", "/api/v1/platform-admin/platform/dashboard",
                    TestConstants.ROLE_IAM_ADMIN);
        }

        @ParameterizedTest(name = "{2} -> {3}")
        @MethodSource("platformDashboardMatrix")
        @DisplayName("Platform dashboard access")
        void platformDashboard(String method, String endpoint, String role, boolean shouldSucceed) throws Exception {
            ResultActions result = mockMvc.perform(get(endpoint)
                    .with(JwtTestUtils.jwtWithRoles("test-" + role, role)));

            if (shouldSucceed) {
                assertNotForbidden(result);
            } else {
                result.andExpect(status().isForbidden());
            }
        }

        static Stream<Arguments> platformUsageMatrix() {
            return RbacTestSupport.fullMatrix("GET", "/api/v1/platform-admin/platform/usage",
                    TestConstants.ROLE_IAM_ADMIN);
        }

        @ParameterizedTest(name = "{2} -> {3}")
        @MethodSource("platformUsageMatrix")
        @DisplayName("Platform usage access")
        void platformUsage(String method, String endpoint, String role, boolean shouldSucceed) throws Exception {
            ResultActions result = mockMvc.perform(get(endpoint)
                    .with(JwtTestUtils.jwtWithRoles("test-" + role, role)));

            if (shouldSucceed) {
                assertNotForbidden(result);
            } else {
                result.andExpect(status().isForbidden());
            }
        }
    }

    // ====== Sector Admin Assignments: iam-admin only ======

    @Nested
    @DisplayName("Sector Admin Assignments (/api/v1/platform-admin/sector-admins/**) -- iam-admin only")
    class SectorAdminAssignmentEndpoints {

        static Stream<Arguments> sectorAdminListMatrix() {
            return RbacTestSupport.fullMatrix("GET", "/api/v1/platform-admin/sector-admins",
                    TestConstants.ROLE_IAM_ADMIN);
        }

        @ParameterizedTest(name = "{2} -> {3}")
        @MethodSource("sectorAdminListMatrix")
        @DisplayName("List sector admin assignments")
        void listSectorAdmins(String method, String endpoint, String role, boolean shouldSucceed) throws Exception {
            ResultActions result = mockMvc.perform(get(endpoint)
                    .with(JwtTestUtils.jwtWithRoles("test-" + role, role)));

            if (shouldSucceed) {
                assertNotForbidden(result);
            } else {
                result.andExpect(status().isForbidden());
            }
        }
    }

    // ====== Sector Dashboard: sector-admin only ======

    @Nested
    @DisplayName("Sector endpoints (/api/v1/platform-admin/sector/**) -- sector-admin only")
    class SectorEndpoints {

        static Stream<Arguments> sectorDashboardMatrix() {
            return RbacTestSupport.fullMatrix("GET", "/api/v1/platform-admin/sector/dashboard?memberClass=GOV",
                    TestConstants.ROLE_SECTOR_ADMIN);
        }

        @ParameterizedTest(name = "{2} -> {3}")
        @MethodSource("sectorDashboardMatrix")
        @DisplayName("Sector dashboard access")
        void sectorDashboard(String method, String endpoint, String role, boolean shouldSucceed) throws Exception {
            ResultActions result = mockMvc.perform(get(endpoint)
                    .with(JwtTestUtils.jwtWithRoles("test-" + role, role)));

            if (shouldSucceed) {
                assertNotForbidden(result);
            } else {
                result.andExpect(status().isForbidden());
            }
        }

        static Stream<Arguments> sectorOrganizationsMatrix() {
            return RbacTestSupport.fullMatrix("GET", "/api/v1/platform-admin/sector/organizations?memberClass=COM",
                    TestConstants.ROLE_SECTOR_ADMIN);
        }

        @ParameterizedTest(name = "{2} -> {3}")
        @MethodSource("sectorOrganizationsMatrix")
        @DisplayName("Sector organizations access")
        void sectorOrganizations(String method, String endpoint, String role, boolean shouldSucceed) throws Exception {
            ResultActions result = mockMvc.perform(get(endpoint)
                    .with(JwtTestUtils.jwtWithRoles("test-" + role, role)));

            if (shouldSucceed) {
                assertNotForbidden(result);
            } else {
                result.andExpect(status().isForbidden());
            }
        }
    }

    // ====== Org endpoints: tenant-admin only ======

    @Nested
    @DisplayName("Org endpoints (/api/v1/platform-admin/org/**) -- tenant-admin only")
    class OrgEndpoints {

        static Stream<Arguments> orgDashboardMatrix() {
            return RbacTestSupport.fullMatrix("GET",
                    "/api/v1/platform-admin/org/dashboard?realmName=test-realm",
                    TestConstants.ROLE_TENANT_ADMIN);
        }

        @ParameterizedTest(name = "{2} -> {3}")
        @MethodSource("orgDashboardMatrix")
        @DisplayName("Org dashboard access")
        void orgDashboard(String method, String endpoint, String role, boolean shouldSucceed) throws Exception {
            ResultActions result = mockMvc.perform(get(endpoint)
                    .with(JwtTestUtils.jwtWithRoles("test-" + role, role)));

            if (shouldSucceed) {
                assertNotForbidden(result);
            } else {
                result.andExpect(status().isForbidden());
            }
        }

        static Stream<Arguments> orgAuditMatrix() {
            return RbacTestSupport.fullMatrix("GET",
                    "/api/v1/platform-admin/org/audit?tenantId=test-tenant",
                    TestConstants.ROLE_TENANT_ADMIN);
        }

        @ParameterizedTest(name = "{2} -> {3}")
        @MethodSource("orgAuditMatrix")
        @DisplayName("Org audit access")
        void orgAudit(String method, String endpoint, String role, boolean shouldSucceed) throws Exception {
            ResultActions result = mockMvc.perform(get(endpoint)
                    .with(JwtTestUtils.jwtWithRoles("test-" + role, role)));

            if (shouldSucceed) {
                assertNotForbidden(result);
            } else {
                result.andExpect(status().isForbidden());
            }
        }

        static Stream<Arguments> orgSettingsMatrix() {
            return RbacTestSupport.fullMatrix("GET",
                    "/api/v1/platform-admin/org/settings?realmName=test-realm",
                    TestConstants.ROLE_TENANT_ADMIN);
        }

        @ParameterizedTest(name = "{2} -> {3}")
        @MethodSource("orgSettingsMatrix")
        @DisplayName("Org settings access")
        void orgSettings(String method, String endpoint, String role, boolean shouldSucceed) throws Exception {
            ResultActions result = mockMvc.perform(get(endpoint)
                    .with(JwtTestUtils.jwtWithRoles("test-" + role, role)));

            if (shouldSucceed) {
                assertNotForbidden(result);
            } else {
                result.andExpect(status().isForbidden());
            }
        }

        static Stream<Arguments> orgComplianceMatrix() {
            return RbacTestSupport.fullMatrix("GET",
                    "/api/v1/platform-admin/org/compliance?tenantId=test-tenant",
                    TestConstants.ROLE_TENANT_ADMIN);
        }

        @ParameterizedTest(name = "{2} -> {3}")
        @MethodSource("orgComplianceMatrix")
        @DisplayName("Org compliance access")
        void orgCompliance(String method, String endpoint, String role, boolean shouldSucceed) throws Exception {
            ResultActions result = mockMvc.perform(get(endpoint)
                    .with(JwtTestUtils.jwtWithRoles("test-" + role, role)));

            if (shouldSucceed) {
                assertNotForbidden(result);
            } else {
                result.andExpect(status().isForbidden());
            }
        }
    }

    // ====== User list: iam-admin OR tenant-admin ======

    @Nested
    @DisplayName("User list (/api/v1/platform-admin/users GET) -- iam-admin, tenant-admin")
    class UserListEndpoints {

        static Stream<Arguments> userListMatrix() {
            return RbacTestSupport.fullMatrix("GET", "/api/v1/platform-admin/users",
                    TestConstants.ROLE_IAM_ADMIN, TestConstants.ROLE_TENANT_ADMIN);
        }

        @ParameterizedTest(name = "{2} -> {3}")
        @MethodSource("userListMatrix")
        @DisplayName("User list access")
        void userList(String method, String endpoint, String role, boolean shouldSucceed) throws Exception {
            ResultActions result = mockMvc.perform(get(endpoint)
                    .with(JwtTestUtils.jwtWithRoles("test-" + role, role)));

            if (shouldSucceed) {
                assertNotForbidden(result);
            } else {
                result.andExpect(status().isForbidden());
            }
        }
    }

    // ====== Bulk operations: iam-admin only ======

    @Nested
    @DisplayName("Bulk operations (/api/v1/platform-admin/users/bulk-*) -- iam-admin only")
    class BulkOperationEndpoints {

        static Stream<Arguments> bulkImportMatrix() {
            return RbacTestSupport.fullMatrix("POST", "/api/v1/platform-admin/users/bulk-import",
                    TestConstants.ROLE_IAM_ADMIN);
        }

        @ParameterizedTest(name = "{2} -> {3}")
        @MethodSource("bulkImportMatrix")
        @DisplayName("Bulk import access")
        void bulkImport(String method, String endpoint, String role, boolean shouldSucceed) throws Exception {
            String validBody = """
                    {"realmName": "test-realm", "users": [{"username": "u1", "email": "u1@test.com"}]}
                    """;

            ResultActions result = mockMvc.perform(post(endpoint)
                    .with(JwtTestUtils.jwtWithRoles("test-" + role, role))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBody));

            if (shouldSucceed) {
                assertNotForbidden(result);
            } else {
                result.andExpect(status().isForbidden());
            }
        }

        static Stream<Arguments> bulkExportMatrix() {
            return RbacTestSupport.fullMatrix("GET", "/api/v1/platform-admin/users/bulk-export",
                    TestConstants.ROLE_IAM_ADMIN);
        }

        @ParameterizedTest(name = "{2} -> {3}")
        @MethodSource("bulkExportMatrix")
        @DisplayName("Bulk export access")
        void bulkExport(String method, String endpoint, String role, boolean shouldSucceed) throws Exception {
            ResultActions result = mockMvc.perform(get(endpoint)
                    .param("realmName", "test-realm")
                    .with(JwtTestUtils.jwtWithRoles("test-" + role, role)));

            if (shouldSucceed) {
                assertNotForbidden(result);
            } else {
                result.andExpect(status().isForbidden());
            }
        }

        static Stream<Arguments> bulkDisableMatrix() {
            return RbacTestSupport.fullMatrix("POST", "/api/v1/platform-admin/users/bulk-disable",
                    TestConstants.ROLE_IAM_ADMIN);
        }

        @ParameterizedTest(name = "{2} -> {3}")
        @MethodSource("bulkDisableMatrix")
        @DisplayName("Bulk disable access")
        void bulkDisable(String method, String endpoint, String role, boolean shouldSucceed) throws Exception {
            ResultActions result = mockMvc.perform(post(endpoint)
                    .param("realmName", "test-realm")
                    .with(JwtTestUtils.jwtWithRoles("test-" + role, role))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[\"user1\"]"));

            if (shouldSucceed) {
                assertNotForbidden(result);
            } else {
                result.andExpect(status().isForbidden());
            }
        }
    }

    // ====== Settings: iam-admin OR config-admin ======

    @Nested
    @DisplayName("Settings (/api/v1/platform-admin/settings) -- iam-admin, config-admin")
    class SettingsEndpoints {

        static Stream<Arguments> settingsGetMatrix() {
            return RbacTestSupport.fullMatrix("GET", "/api/v1/platform-admin/settings",
                    TestConstants.ROLE_IAM_ADMIN, TestConstants.ROLE_CONFIG_ADMIN);
        }

        @ParameterizedTest(name = "{2} -> {3}")
        @MethodSource("settingsGetMatrix")
        @DisplayName("Settings read access")
        void settingsGet(String method, String endpoint, String role, boolean shouldSucceed) throws Exception {
            ResultActions result = mockMvc.perform(get(endpoint)
                    .with(JwtTestUtils.jwtWithRoles("test-" + role, role)));

            if (shouldSucceed) {
                assertNotForbidden(result);
            } else {
                result.andExpect(status().isForbidden());
            }
        }

        static Stream<Arguments> settingsPutMatrix() {
            return RbacTestSupport.fullMatrix("PUT", "/api/v1/platform-admin/settings",
                    TestConstants.ROLE_IAM_ADMIN, TestConstants.ROLE_CONFIG_ADMIN);
        }

        @ParameterizedTest(name = "{2} -> {3}")
        @MethodSource("settingsPutMatrix")
        @DisplayName("Settings write access")
        void settingsPut(String method, String endpoint, String role, boolean shouldSucceed) throws Exception {
            String body = """
                    {"settingKey": "test.key", "settingValue": "value", "category": "general", "description": "test"}
                    """;

            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(JwtTestUtils.jwtWithRoles("test-" + role, role))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));

            if (shouldSucceed) {
                assertNotForbidden(result);
            } else {
                result.andExpect(status().isForbidden());
            }
        }
    }

    // Helper methods to use MockMvcRequestBuilders
    private static MockHttpServletRequestBuilder get(String url) {
        return MockMvcRequestBuilders.get(url);
    }

    private static MockHttpServletRequestBuilder post(String url) {
        return MockMvcRequestBuilders.post(url);
    }

    private static MockHttpServletRequestBuilder put(String url) {
        return MockMvcRequestBuilders.put(url);
    }
}
