package com.iam.platform.config.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.config.dto.FeatureFlagRequest;
import com.iam.platform.config.dto.FeatureFlagResponse;
import com.iam.platform.config.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/config/flags")
@RequiredArgsConstructor
@Tag(name = "Feature Flags", description = "Feature flag management APIs")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    @PostMapping
    @Operation(summary = "Create a new feature flag")
    public ResponseEntity<ApiResponse<FeatureFlagResponse>> createFlag(
            @Valid @RequestBody FeatureFlagRequest request) {
        FeatureFlagResponse response = featureFlagService.createFlag(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Feature flag created"));
    }

    @GetMapping
    @Operation(summary = "List feature flags with optional environment filter")
    public ResponseEntity<ApiResponse<Page<FeatureFlagResponse>>> listFlags(
            @RequestParam(required = false) String environment,
            Pageable pageable) {
        Page<FeatureFlagResponse> flags = featureFlagService.listFlags(environment, pageable);
        return ResponseEntity.ok(ApiResponse.ok(flags));
    }

    @GetMapping("/{flagKey}")
    @Operation(summary = "Get a feature flag by key and environment")
    public ResponseEntity<ApiResponse<FeatureFlagResponse>> getFlag(
            @PathVariable String flagKey,
            @RequestParam(defaultValue = "all") String environment) {
        FeatureFlagResponse response = featureFlagService.getFlag(flagKey, environment);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/enabled")
    @Operation(summary = "Get all enabled feature flags for an environment")
    public ResponseEntity<ApiResponse<List<FeatureFlagResponse>>> getEnabledFlags(
            @RequestParam(defaultValue = "all") String environment) {
        List<FeatureFlagResponse> flags = featureFlagService.getEnabledFlags(environment);
        return ResponseEntity.ok(ApiResponse.ok(flags));
    }

    @PutMapping("/{flagKey}")
    @Operation(summary = "Update a feature flag")
    public ResponseEntity<ApiResponse<FeatureFlagResponse>> updateFlag(
            @PathVariable String flagKey,
            @Valid @RequestBody FeatureFlagRequest request) {
        FeatureFlagResponse response = featureFlagService.updateFlag(flagKey, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Feature flag updated"));
    }

    @PutMapping("/{flagKey}/toggle")
    @Operation(summary = "Toggle a feature flag on/off")
    public ResponseEntity<ApiResponse<FeatureFlagResponse>> toggleFlag(
            @PathVariable String flagKey,
            @RequestParam(defaultValue = "all") String environment) {
        FeatureFlagResponse response = featureFlagService.toggleFlag(flagKey, environment);
        return ResponseEntity.ok(ApiResponse.ok(response, "Feature flag toggled"));
    }

    @DeleteMapping("/{flagKey}")
    @Operation(summary = "Delete a feature flag (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteFlag(
            @PathVariable String flagKey,
            @RequestParam(defaultValue = "all") String environment) {
        featureFlagService.deleteFlag(flagKey, environment);
        return ResponseEntity.ok(ApiResponse.ok(null, "Feature flag deleted"));
    }
}
