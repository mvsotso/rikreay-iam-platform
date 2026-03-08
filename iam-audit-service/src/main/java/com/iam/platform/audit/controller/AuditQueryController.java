package com.iam.platform.audit.controller;

import com.iam.platform.audit.dto.AuditEventResponse;
import com.iam.platform.audit.dto.AuditStatsResponse;
import com.iam.platform.audit.service.AuditQueryService;
import com.iam.platform.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Queries", description = "Audit event search and analytics")
public class AuditQueryController {

    private final AuditQueryService auditQueryService;

    @GetMapping("/events")
    @Operation(summary = "Search audit events",
            description = "Paginated search with filters. Supports tenant-scoped queries. " +
                    "Requires auditor, iam-admin, or report-viewer role.")
    public ApiResponse<Page<AuditEventResponse>> searchEvents(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String memberClass,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            Pageable pageable) {

        Page<AuditEventResponse> events = auditQueryService.searchEvents(
                type, username, action, tenantId, memberClass, dateFrom, dateTo, pageable);
        return ApiResponse.ok(events, "Audit events retrieved successfully");
    }

    @GetMapping("/xroad")
    @Operation(summary = "Search X-Road events",
            description = "Paginated X-Road exchange events. Requires auditor, iam-admin, or service-manager role.")
    public ApiResponse<Page<AuditEventResponse>> searchXroadEvents(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String memberClass,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            Pageable pageable) {

        Page<AuditEventResponse> events = auditQueryService.searchXroadEvents(
                tenantId, memberClass, dateFrom, dateTo, pageable);
        return ApiResponse.ok(events, "X-Road events retrieved successfully");
    }

    @GetMapping("/stats")
    @Operation(summary = "Get audit statistics",
            description = "Aggregated event statistics by type. Requires auditor or iam-admin role.")
    public ApiResponse<AuditStatsResponse> getStats(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String memberClass,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo) {

        AuditStatsResponse stats = auditQueryService.getStats(
                tenantId, memberClass, dateFrom, dateTo);
        return ApiResponse.ok(stats, "Audit statistics retrieved successfully");
    }

    @GetMapping("/events/export")
    @Operation(summary = "Export audit events",
            description = "Export audit events as JSON. Limited to 10,000 records. " +
                    "Requires auditor or iam-admin role.")
    public ApiResponse<List<AuditEventResponse>> exportEvents(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String memberClass,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo) {

        List<AuditEventResponse> events = auditQueryService.exportEvents(
                type, username, tenantId, memberClass, dateFrom, dateTo);
        return ApiResponse.ok(events, "Audit events exported successfully");
    }

    @GetMapping("/login-history")
    @Operation(summary = "Search login history",
            description = "Dedicated login/logout/failed-login history. " +
                    "Requires auditor or iam-admin role.")
    public ApiResponse<Page<AuditEventResponse>> searchLoginHistory(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @RequestParam(required = false) Boolean success,
            Pageable pageable) {

        Page<AuditEventResponse> events = auditQueryService.searchLoginHistory(
                username, tenantId, dateFrom, dateTo, success, pageable);
        return ApiResponse.ok(events, "Login history retrieved successfully");
    }
}
