package com.iam.platform.xroad;

import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.RbacTestSupport;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.xroad.service.XRoadRegistryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parameterized RBAC matrix test for iam-xroad-adapter.
 * Tests all 13 roles against key endpoints to verify access control.
 *
 * Security rules:
 * - /xroad/** — permitAll (authenticated by X-Road Security Server)
 * - /api/v1/xroad/** — service-manager, iam-admin
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class XRoadRbacMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private XRoadRegistryService registryService;

    /**
     * Generates the full RBAC matrix for X-Road admin endpoints.
     * /api/v1/xroad/** requires service-manager or iam-admin.
     */
    static Stream<Arguments> rbacMatrix() {
        return RbacTestSupport.combineMatrices(
                // Services list — service-manager + iam-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/xroad/services",
                        TestConstants.ROLE_SERVICE_MANAGER, TestConstants.ROLE_IAM_ADMIN),

                // ACL list — service-manager + iam-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/xroad/acl",
                        TestConstants.ROLE_SERVICE_MANAGER, TestConstants.ROLE_IAM_ADMIN),

                // Member info — service-manager + iam-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/xroad/members",
                        TestConstants.ROLE_SERVICE_MANAGER, TestConstants.ROLE_IAM_ADMIN)
        );
    }

    @ParameterizedTest(name = "{0} {1} as [{2}] → {3}")
    @MethodSource("rbacMatrix")
    @DisplayName("RBAC matrix — X-Road admin endpoints")
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
     * Tests that X-Road admin endpoints return 401 when no token is provided.
     */
    static Stream<Arguments> protectedEndpoints() {
        return Stream.of(
                Arguments.of("GET", "/api/v1/xroad/services"),
                Arguments.of("GET", "/api/v1/xroad/acl"),
                Arguments.of("GET", "/api/v1/xroad/members")
        );
    }

    @ParameterizedTest(name = "Unauthenticated: {0} {1} → 401")
    @MethodSource("protectedEndpoints")
    @DisplayName("RBAC — X-Road admin endpoints require authentication")
    void testUnauthenticatedAccess(String method, String endpoint) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method), endpoint))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Tests that /xroad/** proxy endpoints are permitAll (no JWT needed).
     * They may return 400 due to missing X-Road headers, but NOT 401/403.
     */
    static Stream<Arguments> xroadProxyEndpoints() {
        return Stream.of(
                Arguments.of("GET", "/xroad/someService"),
                Arguments.of("POST", "/xroad/anotherService")
        );
    }

    @ParameterizedTest(name = "X-Road proxy: {0} {1} — permitAll (no JWT)")
    @MethodSource("xroadProxyEndpoints")
    @DisplayName("RBAC — /xroad/** proxy endpoints are permitAll")
    void testXRoadProxyPermitAll(String method, String endpoint) throws Exception {
        // Should NOT return 401 or 403 — may return 400 due to missing X-Road headers
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method), endpoint)
                        .contentType(MediaType.APPLICATION_JSON));

        int statusCode = result.andReturn().getResponse().getStatus();
        // Allowed: 200, 400 (missing headers) — NOT 401 or 403
        org.assertj.core.api.Assertions.assertThat(statusCode).isNotIn(401, 403);
    }

    /**
     * Tests that specific denied roles cannot create services (POST).
     */
    static Stream<Arguments> deniedWriteRoles() {
        return Stream.of(
                Arguments.of(TestConstants.ROLE_DEVELOPER),
                Arguments.of(TestConstants.ROLE_TENANT_ADMIN),
                Arguments.of(TestConstants.ROLE_OPS_ADMIN),
                Arguments.of(TestConstants.ROLE_AUDITOR),
                Arguments.of(TestConstants.ROLE_EXTERNAL_USER),
                Arguments.of(TestConstants.ROLE_GOVERNANCE_ADMIN)
        );
    }

    @ParameterizedTest(name = "POST /services as [{0}] → 403")
    @MethodSource("deniedWriteRoles")
    @DisplayName("RBAC — service registration requires service-manager or iam-admin")
    void testServiceRegistrationDenied(String role) throws Exception {
        String body = "{\"serviceCode\":\"test\",\"targetService\":\"svc\",\"targetPath\":\"/test\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/xroad/services")
                        .with(JwtTestUtils.jwtWithRoles("testuser", role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }
}
