package com.iam.platform.developer.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.developer.dto.WebhookConfigRequest;
import com.iam.platform.developer.dto.WebhookConfigResponse;
import com.iam.platform.developer.dto.WebhookDeliveryResponse;
import com.iam.platform.developer.service.WebhookService;
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
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Webhook configuration and delivery management")
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping
    @Operation(summary = "Create a webhook configuration")
    public ApiResponse<WebhookConfigResponse> createWebhook(
            @Valid @RequestBody WebhookConfigRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return ApiResponse.ok(webhookService.createWebhook(request, username));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get webhook by ID")
    public ApiResponse<WebhookConfigResponse> getWebhook(@PathVariable UUID id) {
        return ApiResponse.ok(webhookService.getWebhook(id));
    }

    @GetMapping
    @Operation(summary = "List webhooks by application ID")
    public ApiResponse<List<WebhookConfigResponse>> listWebhooks(@RequestParam UUID appId) {
        return ApiResponse.ok(webhookService.getWebhooksByApp(appId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a webhook configuration")
    public ApiResponse<WebhookConfigResponse> updateWebhook(
            @PathVariable UUID id,
            @Valid @RequestBody WebhookConfigRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return ApiResponse.ok(webhookService.updateWebhook(id, request, username));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a webhook")
    public ApiResponse<Void> deleteWebhook(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        webhookService.deleteWebhook(id, jwt.getClaimAsString("preferred_username"));
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/deliveries")
    @Operation(summary = "Get webhook delivery logs")
    public ApiResponse<Page<WebhookDeliveryResponse>> getDeliveryLogs(
            @PathVariable UUID id,
            Pageable pageable) {
        return ApiResponse.ok(webhookService.getDeliveryLogs(id, pageable));
    }
}
