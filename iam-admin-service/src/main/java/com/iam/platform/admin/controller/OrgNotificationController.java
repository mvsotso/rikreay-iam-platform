package com.iam.platform.admin.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.admin.dto.OrgNotificationConfigRequest;
import com.iam.platform.admin.dto.OrgNotificationConfigResponse;
import com.iam.platform.admin.service.OrgNotificationConfigService;
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
@RequestMapping("/api/v1/platform-admin/org/notifications")
@RequiredArgsConstructor
@Tag(name = "Org Notifications", description = "Organization notification preferences (Tier 3)")
public class OrgNotificationController {

    private final OrgNotificationConfigService configService;

    @GetMapping
    @Operation(summary = "Get organization notification config")
    public ResponseEntity<ApiResponse<OrgNotificationConfigResponse>> getConfig(
            @RequestParam String tenantId) {
        return ResponseEntity.ok(ApiResponse.ok(configService.getConfig(tenantId)));
    }

    @PutMapping
    @Operation(summary = "Update organization notification config")
    public ResponseEntity<ApiResponse<OrgNotificationConfigResponse>> updateConfig(
            @RequestParam String tenantId,
            @RequestBody OrgNotificationConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                configService.updateConfig(tenantId, request), "Notification config updated"));
    }
}
