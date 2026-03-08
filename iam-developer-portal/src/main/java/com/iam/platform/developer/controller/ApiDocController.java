package com.iam.platform.developer.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.developer.dto.ApiDocResponse;
import com.iam.platform.developer.dto.SdkInfo;
import com.iam.platform.developer.service.ApiDocService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "API Documentation", description = "Public API documentation and SDK information")
public class ApiDocController {

    private final ApiDocService apiDocService;

    @GetMapping("/api/v1/docs")
    @Operation(summary = "Get all API documentation")
    public ApiResponse<List<ApiDocResponse>> getAllDocs() {
        return ApiResponse.ok(apiDocService.getAllApiDocs());
    }

    @GetMapping("/api/v1/docs/services")
    @Operation(summary = "List available services")
    public ApiResponse<List<String>> listServices() {
        return ApiResponse.ok(apiDocService.getAvailableServices());
    }

    @GetMapping("/api/v1/docs/{serviceName}")
    @Operation(summary = "Get API documentation for a specific service")
    public ApiResponse<ApiDocResponse> getServiceDoc(@PathVariable String serviceName) {
        return ApiResponse.ok(apiDocService.getServiceApiDoc(serviceName));
    }

    @GetMapping("/api/v1/sdks")
    @Operation(summary = "Get available SDK information")
    public ApiResponse<List<SdkInfo>> getSdks() {
        return ApiResponse.ok(apiDocService.getAvailableSdks());
    }
}
