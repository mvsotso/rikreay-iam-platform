package com.iam.platform.developer;

import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.RbacTestSupport;
import com.iam.platform.common.test.TestConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parameterized RBAC matrix test for iam-developer-portal.
 * Tests all 13 roles against key endpoints to verify access control.
 *
 * Security rules:
 * - /api/v1/docs/**, /api/v1/sdks — permitAll (public)
 * - /api/v1/apps/** — developer, iam-admin
 * - /api/v1/webhooks/** — developer, iam-admin
 * - /api/v1/sandbox/** — developer, iam-admin
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeveloperRbacMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Generates the full RBAC matrix for developer portal endpoints.
     */
    static Stream<Arguments> rbacMatrix() {
        return RbacTestSupport.combineMatrices(
                // Apps — developer + iam-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/apps",
                        TestConstants.ROLE_DEVELOPER, TestConstants.ROLE_IAM_ADMIN),

                // Webhooks — developer + iam-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/webhooks?appId=00000000-0000-0000-0000-000000000001",
                        TestConstants.ROLE_DEVELOPER, TestConstants.ROLE_IAM_ADMIN),

                // Sandbox — developer + iam-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/sandbox/realms",
                        TestConstants.ROLE_DEVELOPER, TestConstants.ROLE_IAM_ADMIN)
        );
    }

    @ParameterizedTest(name = "{0} {1} as [{2}] → {3}")
    @MethodSource("rbacMatrix")
    @DisplayName("RBAC matrix — developer portal endpoints")
    void testRbacMatrix(String method, String endpoint, String role, boolean shouldSucceed) throws Exception {
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method), endpoint)
                        .with(JwtTestUtils.jwtWithRoles("testuser", role))
                        .contentType(MediaType.APPLICATION_JSON));

        if (shouldSucceed) {
            result.andExpect(status().isOk());
        } else {
            result.andExpect(status().isForbidden());
        }
    }

    /**
     * Tests that public endpoints are accessible without authentication.
     */
    static Stream<Arguments> publicEndpoints() {
        return Stream.of(
                Arguments.of("GET", "/api/v1/docs"),
                Arguments.of("GET", "/api/v1/docs/services"),
                Arguments.of("GET", "/api/v1/sdks")
        );
    }

    @ParameterizedTest(name = "Public: {0} {1} — no auth required")
    @MethodSource("publicEndpoints")
    @DisplayName("RBAC — public endpoints accessible without authentication")
    void testPublicEndpoints(String method, String endpoint) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method), endpoint))
                .andExpect(status().isOk());
    }

    /**
     * Tests that protected endpoints return 401 when no token is provided.
     */
    static Stream<Arguments> protectedEndpoints() {
        return Stream.of(
                Arguments.of("GET", "/api/v1/apps"),
                Arguments.of("GET", "/api/v1/webhooks?appId=00000000-0000-0000-0000-000000000001"),
                Arguments.of("GET", "/api/v1/sandbox/realms")
        );
    }

    @ParameterizedTest(name = "Unauthenticated: {0} {1} → 401")
    @MethodSource("protectedEndpoints")
    @DisplayName("RBAC — protected endpoints return 401 without authentication")
    void testUnauthenticatedAccess(String method, String endpoint) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method), endpoint))
                .andExpect(status().isUnauthorized());
    }
}
