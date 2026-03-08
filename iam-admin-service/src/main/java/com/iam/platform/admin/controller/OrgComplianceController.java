package com.iam.platform.admin.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.admin.dto.OrgComplianceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform-admin/org/compliance")
@RequiredArgsConstructor
@Tag(name = "Org Compliance", description = "Organization compliance status (Tier 3)")
public class OrgComplianceController {

    @GetMapping
    @Operation(summary = "Get compliance status for organization")
    public ResponseEntity<ApiResponse<OrgComplianceResponse>> getCompliance(
            @RequestParam String tenantId) {
        // In production, this would query iam-governance-service
        OrgComplianceResponse response = new OrgComplianceResponse(
                0, 0.0, 0, 0, 0);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
