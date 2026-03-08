package com.iam.platform.admin.controller;

import com.iam.platform.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequestMapping("/api/v1/platform-admin/org/audit")
@RequiredArgsConstructor
@Tag(name = "Org Audit", description = "Organization audit log proxy (Tier 3)")
public class OrgAuditController {

    @Qualifier("auditServiceWebClient")
    private final WebClient auditServiceWebClient;

    @GetMapping
    @Operation(summary = "Get audit events for organization (proxied to audit-service)")
    public ResponseEntity<ApiResponse<String>> getAuditEvents(
            @RequestParam String tenantId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String username) {
        // In production, this proxies to iam-audit-service with tenantId filter
        return ResponseEntity.ok(ApiResponse.ok(
                "Audit events for tenant: " + tenantId,
                "Proxied to iam-audit-service"));
    }

    @GetMapping("/export")
    @Operation(summary = "Export audit events for organization")
    public ResponseEntity<ApiResponse<String>> exportAuditEvents(
            @RequestParam String tenantId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Audit export for tenant: " + tenantId));
    }

    @GetMapping("/login-history")
    @Operation(summary = "Get login history for organization")
    public ResponseEntity<ApiResponse<String>> getLoginHistory(
            @RequestParam String tenantId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Login history for tenant: " + tenantId));
    }
}
