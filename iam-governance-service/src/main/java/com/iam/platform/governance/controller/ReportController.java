package com.iam.platform.governance.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.governance.dto.ComplianceReportDto;
import com.iam.platform.governance.service.ComplianceReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/governance/reports")
@RequiredArgsConstructor
@Tag(name = "Compliance Reports", description = "Compliance and governance reports")
public class ReportController {

    private final ComplianceReportService reportService;

    @GetMapping("/compliance")
    @Operation(summary = "Generate compliance overview report")
    public ResponseEntity<ApiResponse<ComplianceReportDto>> getComplianceReport() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.generateComplianceReport()));
    }

    @GetMapping("/access")
    @Operation(summary = "Generate access review report")
    public ResponseEntity<ApiResponse<ComplianceReportDto>> getAccessReport() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.generateAccessReport()));
    }

    @GetMapping("/risk")
    @Operation(summary = "Generate risk assessment report")
    public ResponseEntity<ApiResponse<ComplianceReportDto>> getRiskReport() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.generateRiskReport()));
    }

    @GetMapping("/consent")
    @Operation(summary = "Generate consent compliance report")
    public ResponseEntity<ApiResponse<ComplianceReportDto>> getConsentReport() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.generateConsentReport()));
    }
}
