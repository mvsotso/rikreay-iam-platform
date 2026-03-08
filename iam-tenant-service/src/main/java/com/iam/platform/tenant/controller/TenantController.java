package com.iam.platform.tenant.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.tenant.dto.CreateTenantRequest;
import com.iam.platform.tenant.dto.TenantResponse;
import com.iam.platform.tenant.dto.UpdateTenantRequest;
import com.iam.platform.tenant.service.TenantProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Management", description = "Tenant provisioning and lifecycle management")
public class TenantController {

    private final TenantProvisioningService tenantService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create tenant", description = "Provisions a new Keycloak realm and creates tenant record. Requires iam-admin role.")
    public ApiResponse<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        TenantResponse tenant = tenantService.createTenant(request);
        return ApiResponse.ok(tenant, "Tenant provisioned successfully");
    }

    @GetMapping
    @Operation(summary = "List tenants", description = "Lists all tenants with pagination. Requires iam-admin or tenant-admin role.")
    public ApiResponse<Page<TenantResponse>> listTenants(Pageable pageable) {
        Page<TenantResponse> tenants = tenantService.listTenants(pageable);
        return ApiResponse.ok(tenants, "Tenants retrieved successfully");
    }

    @GetMapping("/{realmName}")
    @Operation(summary = "Get tenant", description = "Gets tenant by realm name. Requires iam-admin or tenant-admin role.")
    public ApiResponse<TenantResponse> getTenant(@PathVariable String realmName) {
        TenantResponse tenant = tenantService.getTenant(realmName);
        return ApiResponse.ok(tenant);
    }

    @PutMapping("/{realmName}")
    @Operation(summary = "Update tenant", description = "Updates tenant metadata. Requires iam-admin or tenant-admin role.")
    public ApiResponse<TenantResponse> updateTenant(
            @PathVariable String realmName,
            @Valid @RequestBody UpdateTenantRequest request) {
        TenantResponse tenant = tenantService.updateTenant(realmName, request);
        return ApiResponse.ok(tenant, "Tenant updated successfully");
    }

    @PutMapping("/{realmName}/suspend")
    @Operation(summary = "Suspend tenant", description = "Disables tenant realm in Keycloak. Requires iam-admin role.")
    public ApiResponse<TenantResponse> suspendTenant(@PathVariable String realmName) {
        TenantResponse tenant = tenantService.suspendTenant(realmName);
        return ApiResponse.ok(tenant, "Tenant suspended successfully");
    }

    @PutMapping("/{realmName}/activate")
    @Operation(summary = "Activate tenant", description = "Re-enables a suspended tenant realm. Requires iam-admin role.")
    public ApiResponse<TenantResponse> activateTenant(@PathVariable String realmName) {
        TenantResponse tenant = tenantService.activateTenant(realmName);
        return ApiResponse.ok(tenant, "Tenant activated successfully");
    }

    @DeleteMapping("/{realmName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete tenant", description = "Soft-deletes tenant and disables realm. Requires iam-admin role.")
    public void deleteTenant(@PathVariable String realmName) {
        tenantService.deleteTenant(realmName);
    }
}
