package com.iam.platform.admin.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.admin.dto.PlatformSettingsRequest;
import com.iam.platform.admin.dto.PlatformSettingsResponse;
import com.iam.platform.admin.service.AdminSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platform-admin/settings")
@RequiredArgsConstructor
@Tag(name = "Platform Settings", description = "Platform-wide settings management")
public class AdminSettingsController {

    private final AdminSettingsService settingsService;

    @GetMapping
    @Operation(summary = "Get platform settings")
    public ResponseEntity<ApiResponse<List<PlatformSettingsResponse>>> getSettings(
            @RequestParam(required = false) String category) {
        List<PlatformSettingsResponse> settings;
        if (category != null) {
            settings = settingsService.getSettingsByCategory(category);
        } else {
            settings = settingsService.getAllSettings();
        }
        return ResponseEntity.ok(ApiResponse.ok(settings));
    }

    @PutMapping
    @Operation(summary = "Update a platform setting")
    public ResponseEntity<ApiResponse<PlatformSettingsResponse>> updateSetting(
            @Valid @RequestBody PlatformSettingsRequest request) {
        PlatformSettingsResponse response = settingsService.saveSetting(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Setting updated"));
    }
}
