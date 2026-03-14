package com.iam.platform.governance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.common.test.ApiResponseAssertions;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.governance.dto.ExecutionResponse;
import com.iam.platform.governance.dto.WorkflowRequest;
import com.iam.platform.governance.dto.WorkflowResponse;
import com.iam.platform.governance.enums.ExecutionStatus;
import com.iam.platform.governance.enums.WorkflowType;
import com.iam.platform.governance.service.WorkflowService;
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
import java.util.Map;
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
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WorkflowService workflowService;

    private static final UUID TEST_WORKFLOW_ID = UUID.randomUUID();
    private static final UUID TEST_EXECUTION_ID = UUID.randomUUID();

    private WorkflowRequest validOnboardingRequest() {
        return new WorkflowRequest(
                "Employee Onboarding",
                WorkflowType.ONBOARDING,
                List.of(
                        Map.of("name", "Create account", "type", "PROVISION"),
                        Map.of("name", "Assign roles", "type", "ROLE_ASSIGN"),
                        Map.of("name", "Send welcome email", "type", "NOTIFY")
                ),
                List.of("hr-manager", "it-admin"),
                true
        );
    }

    private WorkflowResponse sampleWorkflowResponse() {
        return new WorkflowResponse(
                TEST_WORKFLOW_ID,
                "Employee Onboarding",
                WorkflowType.ONBOARDING,
                List.of(Map.of("name", "Create account", "type", "PROVISION")),
                List.of("hr-manager", "it-admin"),
                true,
                Instant.now()
        );
    }

    private ExecutionResponse sampleExecutionResponse() {
        return new ExecutionResponse(
                TEST_EXECUTION_ID,
                TEST_WORKFLOW_ID,
                "target-user-123",
                0,
                ExecutionStatus.IN_PROGRESS,
                "gov-admin",
                Instant.now()
        );
    }

    @Nested
    @DisplayName("POST /api/v1/governance/workflows — Create workflow")
    class CreateWorkflow {

        @Test
        @DisplayName("Should create onboarding workflow for governance-admin")
        void createWorkflow_governanceAdmin_shouldSucceed() throws Exception {
            when(workflowService.createWorkflow(any(WorkflowRequest.class), anyString()))
                    .thenReturn(sampleWorkflowResponse());

            mockMvc.perform(post("/api/v1/governance/workflows")
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validOnboardingRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Employee Onboarding"))
                    .andExpect(jsonPath("$.data.type").value("ONBOARDING"))
                    .andExpect(jsonPath("$.data.enabled").value(true))
                    .andExpect(jsonPath("$.message").value("Workflow created"));
        }

        @Test
        @DisplayName("Should create offboarding workflow for iam-admin")
        void createWorkflow_offboarding_iamAdmin_shouldSucceed() throws Exception {
            WorkflowRequest offboardingRequest = new WorkflowRequest(
                    "Employee Offboarding", WorkflowType.OFFBOARDING,
                    List.of(Map.of("name", "Revoke access", "type", "DEPROVISION")),
                    List.of("hr-manager"), true
            );

            WorkflowResponse response = new WorkflowResponse(
                    UUID.randomUUID(), "Employee Offboarding", WorkflowType.OFFBOARDING,
                    List.of(Map.of("name", "Revoke access", "type", "DEPROVISION")),
                    List.of("hr-manager"), true, Instant.now()
            );

            when(workflowService.createWorkflow(any(), anyString())).thenReturn(response);

            mockMvc.perform(post("/api/v1/governance/workflows")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(offboardingRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.type").value("OFFBOARDING"));
        }

        @Test
        @DisplayName("Should return 403 for tenant-admin")
        void createWorkflow_tenantAdmin_shouldBeForbidden() throws Exception {
            mockMvc.perform(post("/api/v1/governance/workflows")
                            .with(JwtTestUtils.jwtWithRoles("user", TestConstants.ROLE_TENANT_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validOnboardingRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void createWorkflow_unauthenticated_shouldReturn401() throws Exception {
            mockMvc.perform(post("/api/v1/governance/workflows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validOnboardingRequest())))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/governance/workflows — List workflows")
    class ListWorkflows {

        @Test
        @DisplayName("Should return paginated workflows for governance-admin")
        void listWorkflows_governanceAdmin_shouldSucceed() throws Exception {
            Page<WorkflowResponse> page = new PageImpl<>(List.of(sampleWorkflowResponse()));
            when(workflowService.listWorkflows(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/governance/workflows")
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/governance/workflows/{id} — Get workflow")
    class GetWorkflow {

        @Test
        @DisplayName("Should return workflow details")
        void getWorkflow_governanceAdmin_shouldSucceed() throws Exception {
            when(workflowService.getWorkflow(TEST_WORKFLOW_ID)).thenReturn(sampleWorkflowResponse());

            mockMvc.perform(get("/api/v1/governance/workflows/" + TEST_WORKFLOW_ID)
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Employee Onboarding"))
                    .andExpect(jsonPath("$.data.approvalChain").isArray());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/governance/workflows/{id} — Update workflow")
    class UpdateWorkflow {

        @Test
        @DisplayName("Should update workflow for governance-admin")
        void updateWorkflow_governanceAdmin_shouldSucceed() throws Exception {
            WorkflowRequest updateRequest = new WorkflowRequest(
                    "Updated Onboarding", WorkflowType.ONBOARDING,
                    List.of(Map.of("name", "Step 1")), List.of("manager"), true
            );

            WorkflowResponse updatedResponse = new WorkflowResponse(
                    TEST_WORKFLOW_ID, "Updated Onboarding", WorkflowType.ONBOARDING,
                    List.of(Map.of("name", "Step 1")), List.of("manager"), true, Instant.now()
            );

            when(workflowService.updateWorkflow(eq(TEST_WORKFLOW_ID), any(), anyString()))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put("/api/v1/governance/workflows/" + TEST_WORKFLOW_ID)
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Updated Onboarding"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/governance/workflows/{id} — Delete workflow")
    class DeleteWorkflow {

        @Test
        @DisplayName("Should soft delete workflow for governance-admin")
        void deleteWorkflow_governanceAdmin_shouldSucceed() throws Exception {
            doNothing().when(workflowService).deleteWorkflow(any(UUID.class), anyString());

            mockMvc.perform(delete("/api/v1/governance/workflows/" + TEST_WORKFLOW_ID)
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Workflow deleted"));

            verify(workflowService).deleteWorkflow(eq(TEST_WORKFLOW_ID), anyString());
        }

        @Test
        @DisplayName("Should return 403 for service-manager")
        void deleteWorkflow_serviceManager_shouldBeForbidden() throws Exception {
            mockMvc.perform(delete("/api/v1/governance/workflows/" + TEST_WORKFLOW_ID)
                            .with(JwtTestUtils.jwtWithRoles("svc", TestConstants.ROLE_SERVICE_MANAGER)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/governance/workflows/{id}/execute — Execute workflow")
    class ExecuteWorkflow {

        @Test
        @DisplayName("Should start execution with IN_PROGRESS status")
        void executeWorkflow_governanceAdmin_shouldSucceed() throws Exception {
            when(workflowService.executeWorkflow(eq(TEST_WORKFLOW_ID), eq("target-user-123"), anyString()))
                    .thenReturn(sampleExecutionResponse());

            mockMvc.perform(post("/api/v1/governance/workflows/" + TEST_WORKFLOW_ID + "/execute")
                            .param("targetUserId", "target-user-123")
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.data.targetUserId").value("target-user-123"))
                    .andExpect(jsonPath("$.data.currentStep").value(0))
                    .andExpect(jsonPath("$.message").value("Workflow execution started"));
        }

        @Test
        @DisplayName("Should return 403 for external-user")
        void executeWorkflow_externalUser_shouldBeForbidden() throws Exception {
            mockMvc.perform(post("/api/v1/governance/workflows/" + TEST_WORKFLOW_ID + "/execute")
                            .param("targetUserId", "target-user-123")
                            .with(JwtTestUtils.jwtWithRoles("citizen", TestConstants.ROLE_EXTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 for ops-admin")
        void executeWorkflow_opsAdmin_shouldBeForbidden() throws Exception {
            mockMvc.perform(post("/api/v1/governance/workflows/" + TEST_WORKFLOW_ID + "/execute")
                            .param("targetUserId", "target-user-123")
                            .with(JwtTestUtils.jwtWithRoles("ops", TestConstants.ROLE_OPS_ADMIN)))
                    .andExpect(status().isForbidden());
        }
    }
}
