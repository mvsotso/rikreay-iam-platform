package com.iam.platform.monitoring.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.monitoring.dto.AuthAnalyticsDto;
import com.iam.platform.monitoring.service.AuthAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitoring/auth-analytics")
@RequiredArgsConstructor
@Tag(name = "Auth Analytics", description = "Authentication analytics from audit events")
public class AuthAnalyticsController {

    private final AuthAnalyticsService authAnalyticsService;

    @GetMapping
    @Operation(summary = "Get platform-wide auth analytics with optional tenant filter")
    public ResponseEntity<ApiResponse<AuthAnalyticsDto>> getAuthAnalytics(
            @RequestParam(required = false) String tenantId) {
        return ResponseEntity.ok(ApiResponse.ok(authAnalyticsService.getAuthAnalytics(tenantId)));
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Get tenant-scoped auth analytics")
    public ResponseEntity<ApiResponse<AuthAnalyticsDto>> getTenantAuthAnalytics(
            @PathVariable String tenantId) {
        return ResponseEntity.ok(ApiResponse.ok(authAnalyticsService.getTenantAuthAnalytics(tenantId)));
    }
}
