package com.iam.platform.xroad.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.xroad.config.XRoadProperties;
import com.iam.platform.xroad.dto.AclEntryRequest;
import com.iam.platform.xroad.dto.AclEntryResponse;
import com.iam.platform.xroad.dto.ServiceRegistrationRequest;
import com.iam.platform.xroad.dto.ServiceRegistrationResponse;
import com.iam.platform.xroad.dto.XRoadMemberResponse;
import com.iam.platform.xroad.service.XRoadRegistryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/xroad")
@RequiredArgsConstructor
@Tag(name = "X-Road Administration", description = "Service registration and ACL management")
public class XRoadAdminController {

    private final XRoadRegistryService registryService;
    private final XRoadProperties xroadProperties;

    // --- Service Registrations ---

    @GetMapping("/services")
    @Operation(summary = "List registered services",
            description = "Lists all X-Road service registrations. Requires service-manager or iam-admin role.")
    public ApiResponse<List<ServiceRegistrationResponse>> listServices() {
        return ApiResponse.ok(registryService.listServices(), "Services retrieved successfully");
    }

    @GetMapping("/services/{id}")
    @Operation(summary = "Get service registration")
    public ApiResponse<ServiceRegistrationResponse> getService(@PathVariable UUID id) {
        return ApiResponse.ok(registryService.getService(id));
    }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register X-Road service",
            description = "Creates a new service registration for X-Road routing. Requires service-manager or iam-admin role.")
    public ApiResponse<ServiceRegistrationResponse> createService(
            @Valid @RequestBody ServiceRegistrationRequest request) {
        return ApiResponse.ok(registryService.createService(request), "Service registered successfully");
    }

    @DeleteMapping("/services/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete service registration")
    public void deleteService(@PathVariable UUID id) {
        registryService.deleteService(id);
    }

    // --- ACL Entries ---

    @GetMapping("/acl")
    @Operation(summary = "List ACL entries",
            description = "Lists all X-Road ACL entries. Requires service-manager or iam-admin role.")
    public ApiResponse<List<AclEntryResponse>> listAclEntries() {
        return ApiResponse.ok(registryService.listAclEntries(), "ACL entries retrieved successfully");
    }

    @GetMapping("/acl/service/{serviceRegistrationId}")
    @Operation(summary = "Get ACL for service")
    public ApiResponse<List<AclEntryResponse>> getAclForService(@PathVariable UUID serviceRegistrationId) {
        return ApiResponse.ok(registryService.getAclForService(serviceRegistrationId));
    }

    @PostMapping("/acl")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create ACL entry",
            description = "Grants a consumer access to an X-Road service. Requires service-manager or iam-admin role.")
    public ApiResponse<AclEntryResponse> createAclEntry(@Valid @RequestBody AclEntryRequest request) {
        return ApiResponse.ok(registryService.createAclEntry(request), "ACL entry created successfully");
    }

    @DeleteMapping("/acl/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete ACL entry")
    public void deleteAclEntry(@PathVariable UUID id) {
        registryService.deleteAclEntry(id);
    }

    // --- Member Info ---

    @GetMapping("/members")
    @Operation(summary = "Get X-Road member info",
            description = "Returns this adapter's X-Road member identity. Requires service-manager or iam-admin role.")
    public ApiResponse<XRoadMemberResponse> getMemberInfo() {
        XRoadProperties.Member member = xroadProperties.getMember();
        XRoadMemberResponse response = new XRoadMemberResponse(
                member.getInstance(),
                member.getMemberClass(),
                member.getMemberCode(),
                member.getSubsystem(),
                member.getFullIdentifier()
        );
        return ApiResponse.ok(response, "X-Road member info retrieved successfully");
    }
}
