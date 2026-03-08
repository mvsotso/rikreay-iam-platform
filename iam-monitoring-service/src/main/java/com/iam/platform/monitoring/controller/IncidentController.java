package com.iam.platform.monitoring.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.monitoring.dto.IncidentRequest;
import com.iam.platform.monitoring.dto.IncidentResponse;
import com.iam.platform.monitoring.enums.IncidentStatus;
import com.iam.platform.monitoring.enums.Severity;
import com.iam.platform.monitoring.service.IncidentService;
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
@RequestMapping("/api/v1/monitoring/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents", description = "Incident tracking and management")
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    @Operation(summary = "Create a new incident")
    public ResponseEntity<ApiResponse<IncidentResponse>> createIncident(
            @Valid @RequestBody IncidentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(ApiResponse.ok(
                incidentService.createIncident(request, username), "Incident created"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get incident by ID")
    public ResponseEntity<ApiResponse<IncidentResponse>> getIncident(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(incidentService.getIncident(id)));
    }

    @GetMapping
    @Operation(summary = "List incidents with optional filters")
    public ResponseEntity<ApiResponse<Page<IncidentResponse>>> listIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) String serviceAffected,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                incidentService.listIncidents(status, severity, serviceAffected, pageable)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update incident status")
    public ResponseEntity<ApiResponse<IncidentResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestParam IncidentStatus status,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(ApiResponse.ok(
                incidentService.updateIncidentStatus(id, status, username), "Incident status updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an incident (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteIncident(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        incidentService.deleteIncident(id, username);
        return ResponseEntity.ok(ApiResponse.ok(null, "Incident deleted"));
    }
}
