package com.iam.platform.notification.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.notification.dto.TemplateRequest;
import com.iam.platform.notification.dto.TemplateResponse;
import com.iam.platform.notification.enums.ChannelType;
import com.iam.platform.notification.service.TemplateService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/templates")
@RequiredArgsConstructor
@Tag(name = "Notification Templates", description = "Template management APIs")
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    @Operation(summary = "Create a notification template")
    public ResponseEntity<ApiResponse<TemplateResponse>> createTemplate(
            @Valid @RequestBody TemplateRequest request) {
        TemplateResponse response = templateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.ok(response, "Template created"));
    }

    @GetMapping
    @Operation(summary = "List notification templates")
    public ResponseEntity<ApiResponse<Page<TemplateResponse>>> listTemplates(
            @RequestParam(required = false) ChannelType channelType,
            Pageable pageable) {
        Page<TemplateResponse> templates = templateService.listTemplates(channelType, pageable);
        return ResponseEntity.ok(ApiResponse.ok(templates));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a notification template by ID")
    public ResponseEntity<ApiResponse<TemplateResponse>> getTemplate(@PathVariable UUID id) {
        TemplateResponse response = templateService.getTemplate(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a notification template")
    public ResponseEntity<ApiResponse<TemplateResponse>> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody TemplateRequest request) {
        TemplateResponse response = templateService.updateTemplate(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Template updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification template (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable UUID id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Template deleted"));
    }
}
