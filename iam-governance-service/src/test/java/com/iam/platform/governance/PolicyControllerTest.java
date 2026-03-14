package com.iam.platform.governance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.common.test.ApiResponseAssertions;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.governance.dto.PolicyViolationDto;
import com.iam.platform.governance.dto.SodPolicyRequest;
import com.iam.platform.governance.dto.SodPolicyResponse;
import com.iam.platform.governance.enums.PolicySeverity;
import com.iam.platform.governance.service.PolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PolicyService policyService;

    private static final UUID TEST_POLICY_ID = UUID.randomUUID();

    private SodPolicyRequest validPolicyRequest() {
        return new SodPolicyRequest(
                "Finance SoD Policy",
                List.of(List.of("iam-admin", "auditor"), List.of("tenant-admin", "governance-admin")),
                PolicySeverity.HIGH,
                true
        );
    }

    private SodPolicyResponse samplePolicyResponse() {
        return new SodPolicyResponse(
                TEST_POLICY_ID,
                "Finance SoD Policy",
                List.of(List.of("iam-admin", "auditor")),
                PolicySeverity.HIGH,
                true,
                Instant.now()
        );
    }

    @Nested
    @DisplayName("POST /api/v1/governance/policies — Create SoD policy")
    class CreatePolicy {

        @Test
        @DisplayName("Should succeed for governance-admin")
        void createPolicy_governanceAdmin_shouldSucceed() throws Exception {
            when(policyService.createPolicy(any(SodPolicyRequest.class), anyString()))
                    .thenReturn(samplePolicyResponse());

            ApiResponseAssertions.assertApiSuccess(
                    mockMvc.perform(post("/api/v1/governance/policies")
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validPolicyRequest()))));
        }

        @Test
        @DisplayName("Should succeed for iam-admin")
        void createPolicy_iamAdmin_shouldSucceed() throws Exception {
            when(policyService.createPolicy(any(SodPolicyRequest.class), anyString()))
                    .thenReturn(samplePolicyResponse());

            mockMvc.perform(post("/api/v1/governance/policies")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validPolicyRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Finance SoD Policy"))
                    .andExpect(jsonPath("$.data.severity").value("HIGH"));
        }

        @Test
        @DisplayName("Should return 403 for tenant-admin")
        void createPolicy_tenantAdmin_shouldBeForbidden() throws Exception {
            mockMvc.perform(post("/api/v1/governance/policies")
                            .with(JwtTestUtils.jwtWithRoles("user", TestConstants.ROLE_TENANT_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validPolicyRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 for ops-admin")
        void createPolicy_opsAdmin_shouldBeForbidden() throws Exception {
            mockMvc.perform(post("/api/v1/governance/policies")
                            .with(JwtTestUtils.jwtWithRoles("ops", TestConstants.ROLE_OPS_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validPolicyRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void createPolicy_unauthenticated_shouldReturn401() throws Exception {
            mockMvc.perform(post("/api/v1/governance/policies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validPolicyRequest())))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/governance/policies — List policies")
    class ListPolicies {

        @Test
        @DisplayName("Should return paginated policies for governance-admin")
        void listPolicies_governanceAdmin_shouldSucceed() throws Exception {
            Page<SodPolicyResponse> page = new PageImpl<>(List.of(samplePolicyResponse()));
            when(policyService.listPolicies(any(Pageable.class))).thenReturn(page);

            ApiResponseAssertions.assertApiSuccess(
                    mockMvc.perform(get("/api/v1/governance/policies")
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN))));
        }

        @Test
        @DisplayName("Should return 403 for report-viewer")
        void listPolicies_reportViewer_shouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/governance/policies")
                            .with(JwtTestUtils.jwtWithRoles("viewer", TestConstants.ROLE_REPORT_VIEWER)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/governance/policies/{id} — Get policy")
    class GetPolicy {

        @Test
        @DisplayName("Should return policy details for governance-admin")
        void getPolicy_governanceAdmin_shouldSucceed() throws Exception {
            when(policyService.getPolicy(TEST_POLICY_ID)).thenReturn(samplePolicyResponse());

            mockMvc.perform(get("/api/v1/governance/policies/" + TEST_POLICY_ID)
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Finance SoD Policy"))
                    .andExpect(jsonPath("$.data.enabled").value(true));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/governance/policies/{id} — Update policy")
    class UpdatePolicy {

        @Test
        @DisplayName("Should succeed for governance-admin")
        void updatePolicy_governanceAdmin_shouldSucceed() throws Exception {
            SodPolicyRequest updateRequest = new SodPolicyRequest(
                    "Updated SoD Policy",
                    List.of(List.of("iam-admin", "auditor")),
                    PolicySeverity.CRITICAL,
                    true
            );

            SodPolicyResponse updatedResponse = new SodPolicyResponse(
                    TEST_POLICY_ID, "Updated SoD Policy",
                    List.of(List.of("iam-admin", "auditor")),
                    PolicySeverity.CRITICAL, true, Instant.now()
            );

            when(policyService.updatePolicy(eq(TEST_POLICY_ID), any(SodPolicyRequest.class), anyString()))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put("/api/v1/governance/policies/" + TEST_POLICY_ID)
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Updated SoD Policy"))
                    .andExpect(jsonPath("$.data.severity").value("CRITICAL"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/governance/policies/{id} — Delete policy")
    class DeletePolicy {

        @Test
        @DisplayName("Should succeed for governance-admin (soft delete)")
        void deletePolicy_governanceAdmin_shouldSucceed() throws Exception {
            doNothing().when(policyService).deletePolicy(any(UUID.class), anyString());

            mockMvc.perform(delete("/api/v1/governance/policies/" + TEST_POLICY_ID)
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Policy deleted"));

            verify(policyService).deletePolicy(eq(TEST_POLICY_ID), anyString());
        }

        @Test
        @DisplayName("Should return 403 for auditor")
        void deletePolicy_auditor_shouldBeForbidden() throws Exception {
            mockMvc.perform(delete("/api/v1/governance/policies/" + TEST_POLICY_ID)
                            .with(JwtTestUtils.jwtWithRoles("auditor", TestConstants.ROLE_AUDITOR)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/governance/policies/{id}/evaluate — Evaluate policy violations")
    class EvaluatePolicy {

        @Test
        @DisplayName("Should return violations for governance-admin")
        void evaluatePolicy_governanceAdmin_shouldSucceed() throws Exception {
            List<PolicyViolationDto> violations = List.of(
                    new PolicyViolationDto(TEST_POLICY_ID, "Finance SoD", PolicySeverity.HIGH,
                            "user-123", List.of("iam-admin", "auditor"))
            );
            when(policyService.evaluatePolicy(eq(TEST_POLICY_ID), eq("user-123"))).thenReturn(violations);

            mockMvc.perform(get("/api/v1/governance/policies/" + TEST_POLICY_ID + "/evaluate")
                            .param("userId", "user-123")
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].policyName").value("Finance SoD"))
                    .andExpect(jsonPath("$.data[0].conflictingRoles").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/governance/policies/check-conflicts — Check role conflicts")
    class CheckConflicts {

        @Test
        @DisplayName("Should return empty list when no conflicts")
        void checkConflicts_noViolations_shouldReturnEmpty() throws Exception {
            when(policyService.checkConflicts("user-123", "developer")).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/governance/policies/check-conflicts")
                            .param("userId", "user-123")
                            .param("role", "developer")
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should return 403 for developer")
        void checkConflicts_developer_shouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/governance/policies/check-conflicts")
                            .param("userId", "user-123")
                            .param("role", "iam-admin")
                            .with(JwtTestUtils.jwtWithRoles("dev", TestConstants.ROLE_DEVELOPER)))
                    .andExpect(status().isForbidden());
        }
    }
}
