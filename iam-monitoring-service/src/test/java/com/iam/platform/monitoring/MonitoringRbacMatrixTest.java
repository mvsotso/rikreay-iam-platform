package com.iam.platform.monitoring;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parameterized RBAC matrix test for the monitoring service.
 * Tests all 13 roles against monitoring endpoints to validate
 * the security configuration.
 *
 * Security rules from SecurityConfig:
 * - /auth-analytics/tenant/** → ops-admin, iam-admin, tenant-admin
 * - /xroad-metrics → ops-admin, iam-admin, service-manager
 * - /monitoring/** (catch-all) → ops-admin, iam-admin
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MonitoringRbacMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Full RBAC matrix for monitoring endpoints.
     */
    static Stream<Arguments> monitoringRbacMatrix() {
        return RbacTestSupport.combineMatrices(
                // GET /api/v1/monitoring/health — ops-admin, iam-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/monitoring/health",
                        TestConstants.ROLE_OPS_ADMIN, TestConstants.ROLE_IAM_ADMIN),

                // GET /api/v1/monitoring/services — ops-admin, iam-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/monitoring/services",
                        TestConstants.ROLE_OPS_ADMIN, TestConstants.ROLE_IAM_ADMIN),

                // GET /api/v1/monitoring/auth-analytics — ops-admin, iam-admin (catch-all)
                RbacTestSupport.fullMatrix("GET", "/api/v1/monitoring/auth-analytics",
                        TestConstants.ROLE_OPS_ADMIN, TestConstants.ROLE_IAM_ADMIN),

                // GET /api/v1/monitoring/auth-analytics/tenant/test — ops-admin, iam-admin, tenant-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/monitoring/auth-analytics/tenant/test-tenant",
                        TestConstants.ROLE_OPS_ADMIN, TestConstants.ROLE_IAM_ADMIN, TestConstants.ROLE_TENANT_ADMIN),

                // GET /api/v1/monitoring/xroad-metrics — ops-admin, iam-admin, service-manager
                RbacTestSupport.fullMatrix("GET", "/api/v1/monitoring/xroad-metrics",
                        TestConstants.ROLE_OPS_ADMIN, TestConstants.ROLE_IAM_ADMIN, TestConstants.ROLE_SERVICE_MANAGER),

                // GET /api/v1/monitoring/incidents — ops-admin, iam-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/monitoring/incidents",
                        TestConstants.ROLE_OPS_ADMIN, TestConstants.ROLE_IAM_ADMIN),

                // GET /api/v1/monitoring/alerts — ops-admin, iam-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/monitoring/alerts",
                        TestConstants.ROLE_OPS_ADMIN, TestConstants.ROLE_IAM_ADMIN)
        );
    }

    @ParameterizedTest(name = "{0} {1} with role [{2}] -> {3}")
    @MethodSource("monitoringRbacMatrix")
    @DisplayName("RBAC matrix test")
    void testRbacMatrix(String method, String endpoint, String role, boolean shouldSucceed) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(endpoint)
                .with(JwtTestUtils.jwtWithRoles("test-user", role));

        ResultActions result = mockMvc.perform(requestBuilder);

        if (shouldSucceed) {
            result.andExpect(status().is2xxSuccessful());
        } else {
            result.andExpect(status().isForbidden());
        }
    }

    /**
     * Tests that all monitoring endpoints return 401 without authentication.
     */
    static Stream<Arguments> unauthenticatedEndpoints() {
        return Stream.of(
                Arguments.of("GET", "/api/v1/monitoring/health"),
                Arguments.of("GET", "/api/v1/monitoring/services"),
                Arguments.of("GET", "/api/v1/monitoring/auth-analytics"),
                Arguments.of("GET", "/api/v1/monitoring/auth-analytics/tenant/test"),
                Arguments.of("GET", "/api/v1/monitoring/xroad-metrics"),
                Arguments.of("GET", "/api/v1/monitoring/incidents"),
                Arguments.of("GET", "/api/v1/monitoring/alerts")
        );
    }

    @ParameterizedTest(name = "{0} {1} without auth should return 401")
    @MethodSource("unauthenticatedEndpoints")
    @DisplayName("All monitoring endpoints should return 401 without authentication")
    void testUnauthenticated(String method, String endpoint) throws Exception {
        mockMvc.perform(get(endpoint))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Tests that actuator health is publicly accessible (permitAll).
     */
    static Stream<Arguments> publicEndpoints() {
        return Stream.of(
                Arguments.of("/actuator/health"),
                Arguments.of("/actuator/info")
        );
    }

    @ParameterizedTest(name = "{0} should be publicly accessible")
    @MethodSource("publicEndpoints")
    @DisplayName("Public endpoints should not require authentication")
    void testPublicEndpoints(String endpoint) throws Exception {
        mockMvc.perform(get(endpoint))
                .andExpect(status().isOk());
    }
}
