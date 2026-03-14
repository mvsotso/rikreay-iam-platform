package com.iam.platform.notification;

import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.RbacTestSupport;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.notification.service.ScheduleService;
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
 * Parameterized RBAC matrix test for iam-notification-service.
 * Tests all 13 roles against key endpoints to verify access control.
 *
 * Security rules:
 * - GET /api/v1/notifications/** — iam-admin, ops-admin
 * - POST /api/v1/notifications/send — iam-admin, ops-admin
 * - /api/v1/notifications/channels/** — iam-admin only
 * - /api/v1/notifications/templates/** — iam-admin only
 * - /api/v1/notifications/schedules/** — iam-admin, ops-admin
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationRbacMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleService scheduleService;

    /**
     * Generates the full RBAC matrix for notification service endpoints.
     */
    static Stream<Arguments> rbacMatrix() {
        return RbacTestSupport.combineMatrices(
                // Notification logs — iam-admin + ops-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/notifications",
                        TestConstants.ROLE_IAM_ADMIN, TestConstants.ROLE_OPS_ADMIN),

                // Notification stats — iam-admin + ops-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/notifications/stats",
                        TestConstants.ROLE_IAM_ADMIN, TestConstants.ROLE_OPS_ADMIN),

                // Channels GET — matched by the GET /api/v1/notifications/** rule, so iam-admin + ops-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/notifications/channels",
                        TestConstants.ROLE_IAM_ADMIN, TestConstants.ROLE_OPS_ADMIN),

                // Schedules — iam-admin + ops-admin
                RbacTestSupport.fullMatrix("GET", "/api/v1/notifications/schedules",
                        TestConstants.ROLE_IAM_ADMIN, TestConstants.ROLE_OPS_ADMIN)
        );
    }

    @ParameterizedTest(name = "{0} {1} as [{2}] → {3}")
    @MethodSource("rbacMatrix")
    @DisplayName("RBAC matrix — notification service endpoints")
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
     * Tests that all protected endpoints return 401 when no token is provided.
     */
    static Stream<Arguments> protectedEndpoints() {
        return Stream.of(
                Arguments.of("GET", "/api/v1/notifications"),
                Arguments.of("GET", "/api/v1/notifications/stats"),
                Arguments.of("GET", "/api/v1/notifications/channels"),
                Arguments.of("GET", "/api/v1/notifications/templates"),
                Arguments.of("GET", "/api/v1/notifications/schedules")
        );
    }

    @ParameterizedTest(name = "Unauthenticated: {0} {1} → 401")
    @MethodSource("protectedEndpoints")
    @DisplayName("RBAC — all notification endpoints require authentication")
    void testUnauthenticatedAccess(String method, String endpoint) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method), endpoint))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Tests channel-specific RBAC — channels are iam-admin only, not ops-admin.
     */
    static Stream<Arguments> channelWriteEndpoints() {
        return Stream.of(
                Arguments.of(TestConstants.ROLE_OPS_ADMIN, false),
                Arguments.of(TestConstants.ROLE_DEVELOPER, false),
                Arguments.of(TestConstants.ROLE_TENANT_ADMIN, false),
                Arguments.of(TestConstants.ROLE_AUDITOR, false),
                Arguments.of(TestConstants.ROLE_IAM_ADMIN, true)
        );
    }

    @ParameterizedTest(name = "POST /channels as [{0}] → allowed={1}")
    @MethodSource("channelWriteEndpoints")
    @DisplayName("RBAC — channel write operations require iam-admin only")
    void testChannelWriteRbac(String role, boolean shouldSucceed) throws Exception {
        String body = "{\"channelType\":\"EMAIL\",\"channelName\":\"test-ch-" + role + "\",\"enabled\":true}";

        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/v1/notifications/channels")
                        .with(JwtTestUtils.jwtWithRoles("testuser", role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));

        int statusCode = result.andReturn().getResponse().getStatus();
        if (shouldSucceed) {
            // Auth passed — may get 201, 400 (validation), or 500 (unmocked repo) but NOT 403
            org.assertj.core.api.Assertions.assertThat(statusCode).isNotEqualTo(403);
        } else {
            org.assertj.core.api.Assertions.assertThat(statusCode).isEqualTo(403);
        }
    }
}
