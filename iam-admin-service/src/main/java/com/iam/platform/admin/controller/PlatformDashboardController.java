package com.iam.platform.admin.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.admin.dto.PlatformDashboardResponse;
import com.iam.platform.admin.dto.UsageResponse;
import com.iam.platform.admin.service.AdminDashboardService;
import com.iam.platform.admin.service.UsageTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform-admin/platform")
@RequiredArgsConstructor
@Tag(name = "Platform Dashboard", description = "Platform-wide admin dashboard (Tier 0/1)")
public class PlatformDashboardController {

    private final AdminDashboardService dashboardService;
    private final UsageTrackingService usageTrackingService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get platform-wide dashboard stats")
    public ResponseEntity<ApiResponse<PlatformDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getPlatformDashboard()));
    }

    @GetMapping("/usage")
    @Operation(summary = "Get platform usage stats")
    public ResponseEntity<ApiResponse<UsageResponse>> getUsage(
            @RequestParam(defaultValue = "platform") String tenantId) {
        return ResponseEntity.ok(ApiResponse.ok(usageTrackingService.getUsageForTenant(tenantId)));
    }

    @GetMapping("/usage/by-tenant")
    @Operation(summary = "Get usage stats grouped by tenant")
    public ResponseEntity<ApiResponse<UsageResponse>> getUsageByTenant(
            @RequestParam String tenantId) {
        return ResponseEntity.ok(ApiResponse.ok(usageTrackingService.getUsageForTenant(tenantId)));
    }
}
