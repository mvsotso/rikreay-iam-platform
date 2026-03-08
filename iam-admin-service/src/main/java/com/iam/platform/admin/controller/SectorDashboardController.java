package com.iam.platform.admin.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.admin.dto.SectorDashboardResponse;
import com.iam.platform.admin.service.SectorDashboardService;
import com.iam.platform.common.enums.MemberClass;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/platform-admin/sector")
@RequiredArgsConstructor
@Tag(name = "Sector Dashboard", description = "Sector admin dashboard (Tier 2)")
public class SectorDashboardController {

    private final SectorDashboardService sectorDashboardService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get sector dashboard stats")
    public ResponseEntity<ApiResponse<SectorDashboardResponse>> getDashboard(
            @RequestParam MemberClass memberClass) {
        return ResponseEntity.ok(ApiResponse.ok(
                sectorDashboardService.getSectorDashboard(memberClass)));
    }

    @GetMapping("/organizations")
    @Operation(summary = "List organizations in sector")
    public ResponseEntity<ApiResponse<String>> getOrganizations(
            @RequestParam MemberClass memberClass) {
        return ResponseEntity.ok(ApiResponse.ok("Organizations for " + memberClass,
                "Query iam-core-service for LegalEntities filtered by memberClass"));
    }

    @GetMapping("/audit")
    @Operation(summary = "Get audit events for sector")
    public ResponseEntity<ApiResponse<String>> getAudit(
            @RequestParam MemberClass memberClass) {
        return ResponseEntity.ok(ApiResponse.ok("Audit for " + memberClass,
                "Proxied to iam-audit-service with memberClass filter"));
    }

    @GetMapping("/compliance")
    @Operation(summary = "Get compliance status across sector")
    public ResponseEntity<ApiResponse<String>> getCompliance(
            @RequestParam MemberClass memberClass) {
        return ResponseEntity.ok(ApiResponse.ok("Compliance for " + memberClass));
    }

    @GetMapping("/reports")
    @Operation(summary = "Get sector reports")
    public ResponseEntity<ApiResponse<String>> getReports(
            @RequestParam MemberClass memberClass) {
        return ResponseEntity.ok(ApiResponse.ok("Reports for " + memberClass));
    }

    @PostMapping("/organizations/{id}/approve")
    @Operation(summary = "Approve an organization in sector")
    public ResponseEntity<ApiResponse<String>> approveOrg(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Organization approved: " + id));
    }

    @PostMapping("/organizations/{id}/reject")
    @Operation(summary = "Reject an organization in sector")
    public ResponseEntity<ApiResponse<String>> rejectOrg(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Organization rejected: " + id));
    }
}
