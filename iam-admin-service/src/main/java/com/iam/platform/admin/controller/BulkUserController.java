package com.iam.platform.admin.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.admin.dto.BulkUserImportRequest;
import com.iam.platform.admin.service.BulkUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/platform-admin/users")
@RequiredArgsConstructor
@Tag(name = "Bulk User Operations", description = "Bulk import/export/disable users")
public class BulkUserController {

    private final BulkUserService bulkUserService;

    @PostMapping("/bulk-import")
    @Operation(summary = "Bulk import users into a realm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkImport(
            @Valid @RequestBody BulkUserImportRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Map<String, Object> result = bulkUserService.bulkImportUsers(request, username);
        return ResponseEntity.ok(ApiResponse.ok(result, "Bulk import completed"));
    }

    @GetMapping("/bulk-export")
    @Operation(summary = "Export users from a realm")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> bulkExport(
            @RequestParam String realmName) {
        List<Map<String, String>> users = bulkUserService.bulkExportUsers(realmName);
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @PostMapping("/bulk-disable")
    @Operation(summary = "Bulk disable users in a realm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkDisable(
            @RequestParam String realmName,
            @RequestBody List<String> usernames) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Map<String, Object> result = bulkUserService.bulkDisableUsers(realmName, usernames, username);
        return ResponseEntity.ok(ApiResponse.ok(result, "Bulk disable completed"));
    }
}
