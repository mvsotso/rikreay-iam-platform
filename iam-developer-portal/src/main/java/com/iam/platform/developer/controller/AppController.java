package com.iam.platform.developer.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.developer.dto.AppCredentialsResponse;
import com.iam.platform.developer.dto.AppRegistrationRequest;
import com.iam.platform.developer.dto.AppResponse;
import com.iam.platform.developer.dto.RedirectUrisRequest;
import com.iam.platform.developer.service.AppRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/apps")
@RequiredArgsConstructor
@Tag(name = "App Registration", description = "Self-service application registration APIs")
public class AppController {

    private final AppRegistrationService appService;

    @PostMapping
    @Operation(summary = "Register a new application")
    public ApiResponse<AppResponse> registerApp(
            @Valid @RequestBody AppRegistrationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String ownerId = jwt.getClaimAsString("preferred_username");
        return ApiResponse.ok(appService.registerApp(request, ownerId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get application by ID")
    public ApiResponse<AppResponse> getApp(@PathVariable UUID id) {
        return ApiResponse.ok(appService.getApp(id));
    }

    @GetMapping
    @Operation(summary = "List all applications for current user")
    public ApiResponse<Page<AppResponse>> listApps(
            @AuthenticationPrincipal Jwt jwt,
            Pageable pageable) {
        String ownerId = jwt.getClaimAsString("preferred_username");
        return ApiResponse.ok(appService.getAppsByOwner(ownerId, pageable));
    }

    @PostMapping("/{id}/credentials")
    @Operation(summary = "Regenerate application credentials")
    public ApiResponse<AppCredentialsResponse> regenerateCredentials(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String ownerId = jwt.getClaimAsString("preferred_username");
        return ApiResponse.ok(appService.regenerateCredentials(id, ownerId));
    }

    @PutMapping("/{id}/redirect-uris")
    @Operation(summary = "Update application redirect URIs")
    public ApiResponse<AppResponse> updateRedirectUris(
            @PathVariable UUID id,
            @Valid @RequestBody RedirectUrisRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String ownerId = jwt.getClaimAsString("preferred_username");
        return ApiResponse.ok(appService.updateRedirectUris(id, request.redirectUris(), ownerId));
    }

    @PutMapping("/{id}/suspend")
    @Operation(summary = "Suspend an application")
    public ApiResponse<Void> suspendApp(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        appService.suspendApp(id, jwt.getClaimAsString("preferred_username"));
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an application")
    public ApiResponse<Void> deleteApp(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        appService.deleteApp(id, jwt.getClaimAsString("preferred_username"));
        return ApiResponse.ok(null);
    }
}
