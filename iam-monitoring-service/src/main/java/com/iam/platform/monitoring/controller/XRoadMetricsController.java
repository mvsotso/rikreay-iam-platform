package com.iam.platform.monitoring.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.monitoring.dto.XRoadMetricsDto;
import com.iam.platform.monitoring.service.XRoadMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitoring/xroad-metrics")
@RequiredArgsConstructor
@Tag(name = "X-Road Metrics", description = "X-Road exchange throughput and latency metrics")
public class XRoadMetricsController {

    private final XRoadMetricsService xRoadMetricsService;

    @GetMapping
    @Operation(summary = "Get X-Road exchange metrics with optional filters")
    public ResponseEntity<ApiResponse<XRoadMetricsDto>> getXRoadMetrics(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String memberClass) {
        return ResponseEntity.ok(ApiResponse.ok(xRoadMetricsService.getXRoadMetrics(tenantId, memberClass)));
    }
}
