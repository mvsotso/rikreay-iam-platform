package com.iam.platform.monitoring.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.monitoring.dto.AlertRuleRequest;
import com.iam.platform.monitoring.dto.AlertRuleResponse;
import com.iam.platform.monitoring.service.AlertService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/monitoring/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert Rules", description = "Alert rule management and evaluation")
public class AlertController {

    private final AlertService alertService;

    @PostMapping
    @Operation(summary = "Create a new alert rule")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> createAlertRule(
            @Valid @RequestBody AlertRuleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(ApiResponse.ok(
                alertService.createAlertRule(request, username), "Alert rule created"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get alert rule by ID")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> getAlertRule(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.getAlertRule(id)));
    }

    @GetMapping
    @Operation(summary = "List all alert rules")
    public ResponseEntity<ApiResponse<Page<AlertRuleResponse>>> listAlertRules(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.listAlertRules(pageable)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an alert rule")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> updateAlertRule(
            @PathVariable UUID id,
            @Valid @RequestBody AlertRuleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(ApiResponse.ok(
                alertService.updateAlertRule(id, request, username), "Alert rule updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an alert rule (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteAlertRule(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        alertService.deleteAlertRule(id, username);
        return ResponseEntity.ok(ApiResponse.ok(null, "Alert rule deleted"));
    }
}
