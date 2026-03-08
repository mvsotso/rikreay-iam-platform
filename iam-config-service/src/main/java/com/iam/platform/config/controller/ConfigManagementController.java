package com.iam.platform.config.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.config.dto.ConfigChangeLogResponse;
import com.iam.platform.config.dto.ConfigUpdateRequest;
import com.iam.platform.config.service.ConfigVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
@Tag(name = "Config Management", description = "Configuration versioning and management APIs")
public class ConfigManagementController {

    private final ConfigVersionService configVersionService;

    @PutMapping("/{application}/{profile}")
    @Operation(summary = "Update configuration for an application and profile")
    public ResponseEntity<ApiResponse<ConfigChangeLogResponse>> updateConfig(
            @PathVariable String application,
            @PathVariable String profile,
            @Valid @RequestBody ConfigUpdateRequest request) {
        ConfigChangeLogResponse response = configVersionService.recordChange(
                application, profile, request, "UPDATE");
        return ResponseEntity.ok(ApiResponse.ok(response, "Configuration updated"));
    }

    @GetMapping("/{application}/{profile}/latest")
    @Operation(summary = "Get the latest config version for an application and profile")
    public ResponseEntity<ApiResponse<ConfigChangeLogResponse>> getLatestConfig(
            @PathVariable String application,
            @PathVariable String profile) {
        ConfigChangeLogResponse response = configVersionService.getLatestVersion(application, profile);
        if (response == null) {
            return ResponseEntity.ok(ApiResponse.ok(null, "No configuration found"));
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/history")
    @Operation(summary = "Get configuration change history")
    public ResponseEntity<ApiResponse<Page<ConfigChangeLogResponse>>> getHistory(
            @RequestParam(required = false) String application,
            @RequestParam(required = false) String profile,
            Pageable pageable) {
        Page<ConfigChangeLogResponse> history = configVersionService.getHistory(
                application, profile, pageable);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @GetMapping("/history/{version}")
    @Operation(summary = "Get a specific config version")
    public ResponseEntity<ApiResponse<ConfigChangeLogResponse>> getVersion(
            @PathVariable Long version) {
        ConfigChangeLogResponse response = configVersionService.getVersion(version);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/rollback/{version}")
    @Operation(summary = "Rollback configuration to a specific version")
    public ResponseEntity<ApiResponse<ConfigChangeLogResponse>> rollback(
            @PathVariable Long version) {
        ConfigChangeLogResponse response = configVersionService.rollback(version);
        return ResponseEntity.ok(ApiResponse.ok(response, "Configuration rolled back to version " + version));
    }
}
