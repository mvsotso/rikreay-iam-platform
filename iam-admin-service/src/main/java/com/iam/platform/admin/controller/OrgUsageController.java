package com.iam.platform.admin.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.admin.dto.UsageResponse;
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
@RequestMapping("/api/v1/platform-admin/org/usage")
@RequiredArgsConstructor
@Tag(name = "Org Usage", description = "Organization usage tracking (Tier 3)")
public class OrgUsageController {

    private final UsageTrackingService usageTrackingService;

    @GetMapping
    @Operation(summary = "Get usage stats for organization")
    public ResponseEntity<ApiResponse<UsageResponse>> getUsage(
            @RequestParam String tenantId) {
        return ResponseEntity.ok(ApiResponse.ok(usageTrackingService.getUsageForTenant(tenantId)));
    }
}
