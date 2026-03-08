package com.iam.platform.admin.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.admin.dto.RealmSettingsRequest;
import com.iam.platform.admin.dto.RealmSettingsResponse;
import com.iam.platform.admin.service.RealmSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform-admin/org/settings")
@RequiredArgsConstructor
@Tag(name = "Org Settings", description = "Organization realm settings management")
public class OrgSettingsController {

    private final RealmSettingsService realmSettingsService;

    @GetMapping
    @Operation(summary = "Get organization realm settings")
    public ResponseEntity<ApiResponse<RealmSettingsResponse>> getSettings(
            @RequestParam String realmName) {
        return ResponseEntity.ok(ApiResponse.ok(realmSettingsService.getRealmSettings(realmName)));
    }

    @PutMapping
    @Operation(summary = "Update organization realm settings")
    public ResponseEntity<ApiResponse<RealmSettingsResponse>> updateSettings(
            @RequestParam String realmName,
            @RequestBody RealmSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                realmSettingsService.updateRealmSettings(realmName, request), "Settings updated"));
    }
}
