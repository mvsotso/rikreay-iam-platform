package com.iam.platform.governance.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.governance.dto.PolicyViolationDto;
import com.iam.platform.governance.dto.SodPolicyRequest;
import com.iam.platform.governance.dto.SodPolicyResponse;
import com.iam.platform.governance.service.PolicyService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/governance/policies")
@RequiredArgsConstructor
@Tag(name = "SoD Policies", description = "Separation of Duties policy management")
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping
    @Operation(summary = "Create a new SoD policy")
    public ResponseEntity<ApiResponse<SodPolicyResponse>> createPolicy(
            @Valid @RequestBody SodPolicyRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(ApiResponse.ok(
                policyService.createPolicy(request, username), "Policy created"));
    }

    @GetMapping
    @Operation(summary = "List all SoD policies")
    public ResponseEntity<ApiResponse<Page<SodPolicyResponse>>> listPolicies(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(policyService.listPolicies(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get policy by ID")
    public ResponseEntity<ApiResponse<SodPolicyResponse>> getPolicy(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(policyService.getPolicy(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a SoD policy")
    public ResponseEntity<ApiResponse<SodPolicyResponse>> updatePolicy(
            @PathVariable UUID id,
            @Valid @RequestBody SodPolicyRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(ApiResponse.ok(
                policyService.updatePolicy(id, request, username), "Policy updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a SoD policy (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deletePolicy(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        policyService.deletePolicy(id, username);
        return ResponseEntity.ok(ApiResponse.ok(null, "Policy deleted"));
    }

    @GetMapping("/{id}/evaluate")
    @Operation(summary = "Evaluate policy violations for a user")
    public ResponseEntity<ApiResponse<List<PolicyViolationDto>>> evaluatePolicy(
            @PathVariable UUID id,
            @RequestParam String userId) {
        return ResponseEntity.ok(ApiResponse.ok(policyService.evaluatePolicy(id, userId)));
    }

    @GetMapping("/check-conflicts")
    @Operation(summary = "Check if adding a role would create conflicts")
    public ResponseEntity<ApiResponse<List<PolicyViolationDto>>> checkConflicts(
            @RequestParam String userId,
            @RequestParam String role) {
        return ResponseEntity.ok(ApiResponse.ok(policyService.checkConflicts(userId, role)));
    }
}
