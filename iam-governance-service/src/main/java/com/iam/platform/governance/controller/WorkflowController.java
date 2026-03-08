package com.iam.platform.governance.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.governance.dto.ExecutionResponse;
import com.iam.platform.governance.dto.WorkflowRequest;
import com.iam.platform.governance.dto.WorkflowResponse;
import com.iam.platform.governance.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/governance/workflows")
@RequiredArgsConstructor
@Tag(name = "Lifecycle Workflows", description = "Onboarding, offboarding, and role change workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping
    @Operation(summary = "Create a new lifecycle workflow")
    public ResponseEntity<ApiResponse<WorkflowResponse>> createWorkflow(
            @Valid @RequestBody WorkflowRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(ApiResponse.ok(
                workflowService.createWorkflow(request, username), "Workflow created"));
    }

    @GetMapping
    @Operation(summary = "List all workflows")
    public ResponseEntity<ApiResponse<Page<WorkflowResponse>>> listWorkflows(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(workflowService.listWorkflows(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get workflow by ID")
    public ResponseEntity<ApiResponse<WorkflowResponse>> getWorkflow(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(workflowService.getWorkflow(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a workflow")
    public ResponseEntity<ApiResponse<WorkflowResponse>> updateWorkflow(
            @PathVariable UUID id,
            @Valid @RequestBody WorkflowRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(ApiResponse.ok(
                workflowService.updateWorkflow(id, request, username), "Workflow updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a workflow (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteWorkflow(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        workflowService.deleteWorkflow(id, username);
        return ResponseEntity.ok(ApiResponse.ok(null, "Workflow deleted"));
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "Execute a workflow for a target user")
    public ResponseEntity<ApiResponse<ExecutionResponse>> executeWorkflow(
            @PathVariable UUID id,
            @RequestParam String targetUserId,
            @AuthenticationPrincipal Jwt jwt) {
        String initiatedBy = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(ApiResponse.ok(
                workflowService.executeWorkflow(id, targetUserId, initiatedBy), "Workflow execution started"));
    }
}
