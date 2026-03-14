package com.iam.platform.config;

import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.RbacTestSupport;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.config.service.ConfigVersionService;
import com.iam.platform.config.service.FeatureFlagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parameterized RBAC matrix test for iam-config-service.
 * Validates that:
 *   - /{app}/{profile} endpoints are permitAll (Spring Cloud Config native)
 *   - /api/v1/config/** endpoints require config-admin or iam-admin
 *   - All other roles are denied
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConfigRbacMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfigVersionService configVersionService;

    @MockitoBean
    private FeatureFlagService featureFlagService;

    private static final String CONFIG_UPDATE_JSON = """
            {
                "properties": {
                    "server.port": 8082,
                    "logging.level.root": "INFO"
                }
            }
            """;

    private static final String FLAG_JSON = """
            {
                "flagKey": "new-feature",
                "flagValue": "true",
                "description": "New feature flag",
                "enabled": true,
                "environment": "dev"
            }
            """;

    // ====== Spring Cloud Config native endpoints (permitAll) ======

    @Nested
    @DisplayName("Spring Cloud Config native endpoints (permitAll)")
    class NativeConfigEndpoints {

        @Test
        @DisplayName("GET /{application}/{profile} should be accessible without JWT")
        void configNativeEndpointPermitAll() throws Exception {
            mockMvc.perform(get("/iam-core-service/dev"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Health endpoint should be accessible without JWT")
        void healthEndpointPublic() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk());
        }
    }

    // ====== Unauthenticated requests to /api/v1/config/** ======

    @Nested
    @DisplayName("Unauthenticated requests to /api/v1/config/**")
    class UnauthenticatedRequests {

        @Test
        @DisplayName("GET /api/v1/config/history without JWT returns 401")
        void getHistoryNoAuth() throws Exception {
            mockMvc.perform(get("/api/v1/config/history"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("PUT /api/v1/config/{app}/{profile} without JWT returns 401")
        void updateConfigNoAuth() throws Exception {
            mockMvc.perform(put("/api/v1/config/iam-core-service/dev")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CONFIG_UPDATE_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/v1/config/flags without JWT returns 401")
        void createFlagNoAuth() throws Exception {
            mockMvc.perform(post("/api/v1/config/flags")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(FLAG_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ====== Parameterized RBAC matrix ======

    /**
     * Provides a matrix of (role, shouldBeAllowed) for /api/v1/config/** endpoints.
     * Only config-admin and iam-admin should have access.
     */
    static Stream<Arguments> configApiRbacMatrix() {
        return RbacTestSupport.fullMatrix("GET", "/api/v1/config/history",
                TestConstants.ROLE_CONFIG_ADMIN, TestConstants.ROLE_IAM_ADMIN);
    }

    @ParameterizedTest(name = "GET /api/v1/config/history with role [{2}] -> allowed={3}")
    @MethodSource("configApiRbacMatrix")
    @DisplayName("RBAC matrix for GET /api/v1/config/history")
    void configHistoryRbacMatrix(String method, String endpoint, String role, boolean shouldSucceed) throws Exception {
        var result = mockMvc.perform(get(endpoint)
                .with(JwtTestUtils.jwtWithRoles("test-user", role)));

        if (shouldSucceed) {
            result.andExpect(status().isOk());
        } else {
            result.andExpect(status().isForbidden());
        }
    }

    // ====== Allowed roles for config management endpoints ======

    @Nested
    @DisplayName("config-admin access to /api/v1/config/**")
    class ConfigAdminAccess {

        @Test
        @DisplayName("config-admin can GET config history")
        void configAdminCanGetHistory() throws Exception {
            mockMvc.perform(get("/api/v1/config/history")
                            .with(JwtTestUtils.jwtWithRoles("config-user", TestConstants.ROLE_CONFIG_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("config-admin can PUT config update")
        void configAdminCanUpdateConfig() throws Exception {
            mockMvc.perform(put("/api/v1/config/iam-core-service/dev")
                            .with(JwtTestUtils.jwtWithRoles("config-user", TestConstants.ROLE_CONFIG_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CONFIG_UPDATE_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("config-admin can POST feature flags")
        void configAdminCanCreateFlag() throws Exception {
            mockMvc.perform(post("/api/v1/config/flags")
                            .with(JwtTestUtils.jwtWithRoles("config-user", TestConstants.ROLE_CONFIG_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(FLAG_JSON))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("iam-admin access to /api/v1/config/**")
    class IamAdminAccess {

        @Test
        @DisplayName("iam-admin can GET config history")
        void iamAdminCanGetHistory() throws Exception {
            mockMvc.perform(get("/api/v1/config/history")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("iam-admin can PUT config update")
        void iamAdminCanUpdateConfig() throws Exception {
            mockMvc.perform(put("/api/v1/config/iam-core-service/dev")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CONFIG_UPDATE_JSON))
                    .andExpect(status().isOk());
        }
    }

    // ====== Denied roles ======

    @Nested
    @DisplayName("Denied roles for /api/v1/config/**")
    class DeniedRoles {

        @ParameterizedTest
        @ValueSource(strings = {
                TestConstants.ROLE_TENANT_ADMIN,
                TestConstants.ROLE_SECTOR_ADMIN,
                TestConstants.ROLE_OPS_ADMIN,
                TestConstants.ROLE_AUDITOR,
                TestConstants.ROLE_DEVELOPER,
                TestConstants.ROLE_EXTERNAL_USER,
                TestConstants.ROLE_INTERNAL_USER,
                TestConstants.ROLE_API_ACCESS,
                TestConstants.ROLE_SERVICE_MANAGER,
                TestConstants.ROLE_GOVERNANCE_ADMIN,
                TestConstants.ROLE_REPORT_VIEWER
        })
        @DisplayName("Non-config roles should be denied access to config management endpoints")
        void nonConfigRolesDenied(String role) throws Exception {
            mockMvc.perform(get("/api/v1/config/history")
                            .with(JwtTestUtils.jwtWithRoles("denied-user", role)))
                    .andExpect(status().isForbidden());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                TestConstants.ROLE_TENANT_ADMIN,
                TestConstants.ROLE_DEVELOPER,
                TestConstants.ROLE_EXTERNAL_USER
        })
        @DisplayName("Non-config roles should be denied PUT to config update")
        void nonConfigRolesDeniedUpdate(String role) throws Exception {
            mockMvc.perform(put("/api/v1/config/iam-core-service/dev")
                            .with(JwtTestUtils.jwtWithRoles("denied-user", role))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CONFIG_UPDATE_JSON))
                    .andExpect(status().isForbidden());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                TestConstants.ROLE_TENANT_ADMIN,
                TestConstants.ROLE_DEVELOPER,
                TestConstants.ROLE_EXTERNAL_USER
        })
        @DisplayName("Non-config roles should be denied POST to feature flags")
        void nonConfigRolesDeniedCreateFlag(String role) throws Exception {
            mockMvc.perform(post("/api/v1/config/flags")
                            .with(JwtTestUtils.jwtWithRoles("denied-user", role))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(FLAG_JSON))
                    .andExpect(status().isForbidden());
        }
    }
}
