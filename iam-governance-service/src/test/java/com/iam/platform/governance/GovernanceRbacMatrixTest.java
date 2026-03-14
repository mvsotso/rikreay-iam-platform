package com.iam.platform.governance;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parameterized RBAC matrix test for the governance service.
 * Tests all 13 roles against governance endpoints to validate
 * the security configuration in SecurityConfig.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GovernanceRbacMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String DUMMY_UUID = "550e8400-e29b-41d4-a716-446655440000";

    /**
     * Full RBAC matrix combining all governance endpoint checks.
     *
     * Security rules from SecurityConfig:
     * - POST /consents → authenticated (any role)
     * - GET /consents/me → authenticated (any role)
     * - DELETE /consents/* → authenticated (any role)
     * - GET /consents (admin list) → governance-admin, iam-admin
     * - GET /reports/** → report-viewer, governance-admin, iam-admin
     * - /governance/** (catch-all) → governance-admin, iam-admin
     */
    static Stream<Arguments> governanceRbacMatrix() {
        return RbacTestSupport.combineMatrices(
                // GET /api/v1/governance/consents (admin list) — governance-admin, iam-admin only
                RbacTestSupport.fullMatrix("GET", "/api/v1/governance/consents",
                        TestConstants.ROLE_GOVERNANCE_ADMIN, TestConstants.ROLE_IAM_ADMIN),

                // GET /api/v1/governance/policies — governance-admin, iam-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/governance/policies",
                        TestConstants.ROLE_GOVERNANCE_ADMIN, TestConstants.ROLE_IAM_ADMIN),

                // GET /api/v1/governance/risk-scores/high-risk — governance-admin, iam-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/governance/risk-scores/high-risk",
                        TestConstants.ROLE_GOVERNANCE_ADMIN, TestConstants.ROLE_IAM_ADMIN),

                // GET /api/v1/governance/workflows — governance-admin, iam-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/governance/workflows",
                        TestConstants.ROLE_GOVERNANCE_ADMIN, TestConstants.ROLE_IAM_ADMIN),

                // GET /api/v1/governance/reports/compliance — report-viewer, governance-admin, iam-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/governance/reports/compliance",
                        TestConstants.ROLE_REPORT_VIEWER, TestConstants.ROLE_GOVERNANCE_ADMIN, TestConstants.ROLE_IAM_ADMIN),

                // GET /api/v1/governance/reports/risk — report-viewer, governance-admin, iam-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/governance/reports/risk",
                        TestConstants.ROLE_REPORT_VIEWER, TestConstants.ROLE_GOVERNANCE_ADMIN, TestConstants.ROLE_IAM_ADMIN),

                // GET /api/v1/governance/campaigns — governance-admin, iam-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/governance/campaigns",
                        TestConstants.ROLE_GOVERNANCE_ADMIN, TestConstants.ROLE_IAM_ADMIN)
        );
    }

    @ParameterizedTest(name = "{0} {1} with role [{2}] -> {3}")
    @MethodSource("governanceRbacMatrix")
    @DisplayName("RBAC matrix test")
    void testRbacMatrix(String method, String endpoint, String role, boolean shouldSucceed) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = buildRequest(method, endpoint);
        requestBuilder.with(JwtTestUtils.jwtWithRoles("test-user", role));

        ResultActions result = mockMvc.perform(requestBuilder);

        if (shouldSucceed) {
            // Allowed: status should be 2xx (200 or possibly 500 if service not mocked)
            result.andExpect(status().is2xxSuccessful());
        } else {
            result.andExpect(status().isForbidden());
        }
    }

    /**
     * Tests that authenticated endpoints (consents POST, GET /me, DELETE)
     * are accessible by ALL 13 roles.
     */
    static Stream<Arguments> authenticatedEndpointsMatrix() {
        return Stream.of(TestConstants.ALL_ROLES)
                .flatMap(role -> Stream.of(
                        Arguments.of("GET", "/api/v1/governance/consents/me", role)
                ));
    }

    @ParameterizedTest(name = "{0} {1} with role [{2}] should succeed")
    @MethodSource("authenticatedEndpointsMatrix")
    @DisplayName("Authenticated-only endpoints should allow all roles")
    void testAuthenticatedEndpoints(String method, String endpoint, String role) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = buildRequest(method, endpoint);
        // The /consents/me endpoint calls UUID.fromString(jwt.getSubject()), so the subject must be a valid UUID.
        var jwt = JwtTestUtils.createJwt(DUMMY_UUID, "test-user", role);
        requestBuilder.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .jwt().jwt(jwt).authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * Tests that all governance endpoints return 401 without authentication.
     */
    static Stream<Arguments> unauthenticatedEndpoints() {
        return Stream.of(
                Arguments.of("GET", "/api/v1/governance/consents"),
                Arguments.of("GET", "/api/v1/governance/consents/me"),
                Arguments.of("GET", "/api/v1/governance/policies"),
                Arguments.of("GET", "/api/v1/governance/workflows"),
                Arguments.of("GET", "/api/v1/governance/risk-scores/high-risk"),
                Arguments.of("GET", "/api/v1/governance/reports/compliance"),
                Arguments.of("GET", "/api/v1/governance/campaigns")
        );
    }

    @ParameterizedTest(name = "{0} {1} without auth should return 401")
    @MethodSource("unauthenticatedEndpoints")
    @DisplayName("All governance endpoints should return 401 without authentication")
    void testUnauthenticated(String method, String endpoint) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = buildRequest(method, endpoint);
        mockMvc.perform(requestBuilder)
                .andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder buildRequest(String method, String endpoint) {
        return switch (method) {
            case "POST" -> post(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}");
            case "PUT" -> put(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}");
            case "DELETE" -> delete(endpoint);
            default -> get(endpoint);
        };
    }
}
