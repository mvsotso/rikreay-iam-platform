package com.iam.platform.monitoring.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.monitoring.dto.AggregatedHealthResponse;
import com.iam.platform.monitoring.dto.ServiceHealthDto;
import com.iam.platform.monitoring.service.HealthAggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/monitoring")
@RequiredArgsConstructor
@Tag(name = "Health Monitoring", description = "Service health aggregation")
public class HealthController {

    private final HealthAggregationService healthAggregationService;

    @GetMapping("/health")
    @Operation(summary = "Get aggregated health status of all services")
    public ResponseEntity<ApiResponse<AggregatedHealthResponse>> getAggregatedHealth() {
        return ResponseEntity.ok(ApiResponse.ok(healthAggregationService.getAggregatedHealth()));
    }

    @GetMapping("/services")
    @Operation(summary = "Get detailed service health list")
    public ResponseEntity<ApiResponse<List<ServiceHealthDto>>> getServiceDetails() {
        return ResponseEntity.ok(ApiResponse.ok(healthAggregationService.getServiceDetails()));
    }
}
