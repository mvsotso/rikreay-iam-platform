package com.iam.platform.governance.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.governance.dto.RiskScoreResponse;
import com.iam.platform.governance.service.RiskScoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/governance/risk-scores")
@RequiredArgsConstructor
@Tag(name = "Risk Scores", description = "User risk scoring and assessment")
public class RiskScoreController {

    private final RiskScoringService riskScoringService;

    @PostMapping("/{userId}/calculate")
    @Operation(summary = "Calculate risk score for a user")
    public ResponseEntity<ApiResponse<RiskScoreResponse>> calculateRiskScore(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(
                riskScoringService.calculateRiskScore(userId), "Risk score calculated"));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get latest risk score for a user")
    public ResponseEntity<ApiResponse<RiskScoreResponse>> getRiskScore(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(riskScoringService.getLatestRiskScore(userId)));
    }

    @GetMapping("/high-risk")
    @Operation(summary = "Get high risk users above threshold")
    public ResponseEntity<ApiResponse<Page<RiskScoreResponse>>> getHighRiskUsers(
            @RequestParam(defaultValue = "70") int threshold,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(riskScoringService.getHighRiskUsers(threshold, pageable)));
    }
}
