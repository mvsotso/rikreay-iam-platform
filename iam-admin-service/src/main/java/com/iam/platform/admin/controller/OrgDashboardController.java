package com.iam.platform.admin.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.admin.dto.OrgDashboardResponse;
import com.iam.platform.admin.service.OrgDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform-admin/org")
@RequiredArgsConstructor
@Tag(name = "Org Dashboard", description = "Organization admin dashboard (Tier 3)")
public class OrgDashboardController {

    private final OrgDashboardService orgDashboardService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get organization dashboard stats")
    public ResponseEntity<ApiResponse<OrgDashboardResponse>> getDashboard(
            @RequestParam String realmName) {
        return ResponseEntity.ok(ApiResponse.ok(orgDashboardService.getOrgDashboard(realmName)));
    }
}
