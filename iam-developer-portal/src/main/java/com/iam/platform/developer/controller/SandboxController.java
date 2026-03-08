package com.iam.platform.developer.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.developer.dto.SandboxRequest;
import com.iam.platform.developer.dto.SandboxResponse;
import com.iam.platform.developer.service.SandboxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sandbox/realms")
@RequiredArgsConstructor
@Tag(name = "Sandbox", description = "Sandbox realm management for testing")
public class SandboxController {

    private final SandboxService sandboxService;

    @PostMapping
    @Operation(summary = "Create a sandbox realm")
    public ApiResponse<SandboxResponse> createSandbox(
            @RequestBody(required = false) SandboxRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String ownerId = jwt.getClaimAsString("preferred_username");
        SandboxRequest req = request != null ? request : new SandboxRequest(null);
        return ApiResponse.ok(sandboxService.createSandbox(req, ownerId));
    }

    @GetMapping
    @Operation(summary = "List developer's active sandbox realms")
    public ApiResponse<List<SandboxResponse>> listSandboxes(@AuthenticationPrincipal Jwt jwt) {
        String ownerId = jwt.getClaimAsString("preferred_username");
        return ApiResponse.ok(sandboxService.getSandboxesByOwner(ownerId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a sandbox realm")
    public ApiResponse<Void> deleteSandbox(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        sandboxService.deleteSandbox(id, jwt.getClaimAsString("preferred_username"));
        return ApiResponse.ok(null);
    }
}
