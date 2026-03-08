package com.iam.platform.governance.service;

import com.iam.platform.governance.dto.ExecutionResponse;
import com.iam.platform.governance.dto.WorkflowRequest;
import com.iam.platform.governance.dto.WorkflowResponse;
import com.iam.platform.governance.entity.LifecycleWorkflow;
import com.iam.platform.governance.entity.WorkflowExecution;
import com.iam.platform.governance.enums.ExecutionStatus;
import com.iam.platform.governance.repository.ExecutionRepository;
import com.iam.platform.governance.repository.WorkflowRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final ExecutionRepository executionRepository;
    private final Keycloak keycloakAdmin;
    private final AuditService auditService;

    @Transactional
    public WorkflowResponse createWorkflow(WorkflowRequest request, String username) {
        LifecycleWorkflow workflow = LifecycleWorkflow.builder()
                .name(request.name())
                .type(request.type())
                .stepsJson(request.steps() != null ? request.steps() : List.of())
                .approvalChainJson(request.approvalChain() != null ? request.approvalChain() : List.of())
                .enabled(request.enabled())
                .build();

        LifecycleWorkflow saved = workflowRepository.save(workflow);
        log.info("Workflow created: id={}, name={}, type={}", saved.getId(), saved.getName(), saved.getType());

        auditService.logGovernanceAction(username, "CREATE_WORKFLOW", "workflows/" + saved.getId(),
                true, Map.of("name", saved.getName(), "type", saved.getType().name()));

        return toResponse(saved);
    }

    public WorkflowResponse getWorkflow(UUID id) {
        return workflowRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + id));
    }

    public Page<WorkflowResponse> listWorkflows(Pageable pageable) {
        return workflowRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public WorkflowResponse updateWorkflow(UUID id, WorkflowRequest request, String username) {
        LifecycleWorkflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + id));

        workflow.setName(request.name());
        workflow.setType(request.type());
        workflow.setStepsJson(request.steps() != null ? request.steps() : List.of());
        workflow.setApprovalChainJson(request.approvalChain() != null ? request.approvalChain() : List.of());
        workflow.setEnabled(request.enabled());

        LifecycleWorkflow saved = workflowRepository.save(workflow);
        auditService.logGovernanceAction(username, "UPDATE_WORKFLOW", "workflows/" + id,
                true, Map.of("name", saved.getName()));

        return toResponse(saved);
    }

    @Transactional
    public void deleteWorkflow(UUID id, String username) {
        LifecycleWorkflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + id));
        workflow.softDelete();
        workflowRepository.save(workflow);

        auditService.logGovernanceAction(username, "DELETE_WORKFLOW", "workflows/" + id,
                true, Map.of("name", workflow.getName()));
    }

    @Transactional
    @CircuitBreaker(name = "keycloak", fallbackMethod = "executeWorkflowFallback")
    public ExecutionResponse executeWorkflow(UUID workflowId, String targetUserId, String initiatedBy) {
        LifecycleWorkflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowId));

        WorkflowExecution execution = WorkflowExecution.builder()
                .workflowId(workflowId)
                .targetUserId(targetUserId)
                .currentStep(0)
                .status(ExecutionStatus.IN_PROGRESS)
                .initiatedBy(initiatedBy)
                .build();

        WorkflowExecution saved = executionRepository.save(execution);
        log.info("Workflow execution started: id={}, workflow={}, target={}", saved.getId(), workflowId, targetUserId);

        auditService.logGovernanceAction(initiatedBy, "EXECUTE_WORKFLOW", "executions/" + saved.getId(),
                true, Map.of("workflowId", workflowId.toString(), "targetUserId", targetUserId,
                        "type", workflow.getType().name()));

        return toExecutionResponse(saved);
    }

    @SuppressWarnings("unused")
    private ExecutionResponse executeWorkflowFallback(UUID workflowId, String targetUserId, String initiatedBy, Throwable t) {
        log.error("Workflow execution failed (circuit breaker): {}", t.getMessage());
        throw new RuntimeException("Workflow execution unavailable: " + t.getMessage());
    }

    @Transactional
    public ExecutionResponse advanceExecution(UUID executionId, String username) {
        WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Execution not found: " + executionId));

        LifecycleWorkflow workflow = workflowRepository.findById(execution.getWorkflowId())
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + execution.getWorkflowId()));

        int totalSteps = workflow.getStepsJson().size();
        int nextStep = execution.getCurrentStep() + 1;

        if (nextStep >= totalSteps) {
            execution.setStatus(ExecutionStatus.COMPLETED);
            execution.setCurrentStep(totalSteps);
            log.info("Workflow execution completed: id={}", executionId);
        } else {
            execution.setCurrentStep(nextStep);
        }

        WorkflowExecution saved = executionRepository.save(execution);
        auditService.logGovernanceAction(username, "ADVANCE_EXECUTION", "executions/" + executionId,
                true, Map.of("step", nextStep, "status", saved.getStatus().name()));

        return toExecutionResponse(saved);
    }

    private WorkflowResponse toResponse(LifecycleWorkflow workflow) {
        return new WorkflowResponse(
                workflow.getId(), workflow.getName(), workflow.getType(),
                workflow.getStepsJson(), workflow.getApprovalChainJson(),
                workflow.isEnabled(), workflow.getCreatedAt());
    }

    private ExecutionResponse toExecutionResponse(WorkflowExecution execution) {
        return new ExecutionResponse(
                execution.getId(), execution.getWorkflowId(), execution.getTargetUserId(),
                execution.getCurrentStep(), execution.getStatus(),
                execution.getInitiatedBy(), execution.getCreatedAt());
    }
}
