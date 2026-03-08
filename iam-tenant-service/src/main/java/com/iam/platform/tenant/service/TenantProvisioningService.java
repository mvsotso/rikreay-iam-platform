package com.iam.platform.tenant.service;

import com.iam.platform.common.exception.ResourceNotFoundException;
import com.iam.platform.common.exception.TenantProvisioningException;
import com.iam.platform.tenant.dto.CreateTenantRequest;
import com.iam.platform.tenant.dto.TenantResponse;
import com.iam.platform.tenant.dto.UpdateTenantRequest;
import com.iam.platform.tenant.entity.Tenant;
import com.iam.platform.tenant.enums.TenantStatus;
import com.iam.platform.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantProvisioningService {

    private final TenantRepository tenantRepository;
    private final KeycloakRealmService keycloakRealmService;
    private final AuditService auditService;

    /**
     * Creates a new tenant: provisions Keycloak realm + DB record.
     * Steps: validate → create DB record (PROVISIONING) → create Keycloak realm →
     * create roles → create client → create admin user → update status to ACTIVE.
     */
    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {
        String currentUser = getCurrentUsername();
        log.info("User '{}' creating tenant: name={}, realm={}", currentUser,
                request.tenantName(), request.realmName());

        // Validate uniqueness
        if (tenantRepository.existsByTenantName(request.tenantName())) {
            throw new TenantProvisioningException(
                    "Tenant with name '" + request.tenantName() + "' already exists");
        }
        if (tenantRepository.existsByRealmName(request.realmName())) {
            throw new TenantProvisioningException(
                    "Tenant with realm '" + request.realmName() + "' already exists");
        }
        if (keycloakRealmService.realmExists(request.realmName())) {
            throw new TenantProvisioningException(
                    "Keycloak realm '" + request.realmName() + "' already exists");
        }

        // Create DB record in PROVISIONING status
        Tenant tenant = Tenant.builder()
                .tenantName(request.tenantName())
                .realmName(request.realmName())
                .description(request.description())
                .memberClass(request.memberClass())
                .entityType(request.entityType())
                .registrationNumber(request.registrationNumber())
                .tin(request.tin())
                .memberCode(request.memberCode())
                .xroadSubsystem(request.xroadSubsystem())
                .status(TenantStatus.PROVISIONING)
                .adminEmail(request.adminEmail())
                .adminUsername(request.adminUsername())
                .settings(request.settings())
                .build();
        tenant = tenantRepository.save(tenant);

        try {
            // Provision Keycloak realm
            keycloakRealmService.createRealm(request.realmName(), request.tenantName());
            keycloakRealmService.createDefaultRoles(request.realmName());
            keycloakRealmService.createDefaultClient(request.realmName());
            keycloakRealmService.createAdminUser(
                    request.realmName(),
                    request.adminUsername(),
                    request.adminEmail(),
                    request.adminTempPassword());

            // Update status to ACTIVE
            tenant.setStatus(TenantStatus.ACTIVE);
            tenant = tenantRepository.save(tenant);

            log.info("Tenant '{}' provisioned successfully with realm '{}'",
                    request.tenantName(), request.realmName());

            // Publish events (non-blocking)
            String tenantId = tenant.getId().toString();
            auditService.logAdminAction(currentUser, "TENANT_CREATED",
                    "tenant:" + request.realmName(), true, tenantId,
                    Map.of("tenantName", request.tenantName(),
                            "memberClass", request.memberClass().name()));
            auditService.publishTenantCreated(tenantId, request.realmName(), currentUser);

            return toResponse(tenant);

        } catch (Exception e) {
            log.error("Failed to provision tenant '{}', marking as DECOMMISSIONED",
                    request.tenantName(), e);
            tenant.setStatus(TenantStatus.DECOMMISSIONED);
            tenantRepository.save(tenant);

            auditService.logAdminAction(currentUser, "TENANT_CREATION_FAILED",
                    "tenant:" + request.realmName(), false, tenant.getId().toString(),
                    Map.of("error", e.getMessage()));

            throw new TenantProvisioningException(
                    "Failed to provision tenant: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public TenantResponse getTenant(String realmName) {
        Tenant tenant = findByRealmName(realmName);
        return toResponse(tenant);
    }

    @Transactional(readOnly = true)
    public Page<TenantResponse> listTenants(Pageable pageable) {
        return tenantRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public TenantResponse updateTenant(String realmName, UpdateTenantRequest request) {
        String currentUser = getCurrentUsername();
        Tenant tenant = findByRealmName(realmName);

        if (request.tenantName() != null) {
            if (!tenant.getTenantName().equals(request.tenantName())
                    && tenantRepository.existsByTenantName(request.tenantName())) {
                throw new TenantProvisioningException(
                        "Tenant with name '" + request.tenantName() + "' already exists");
            }
            tenant.setTenantName(request.tenantName());
        }
        if (request.description() != null) tenant.setDescription(request.description());
        if (request.entityType() != null) tenant.setEntityType(request.entityType());
        if (request.registrationNumber() != null) tenant.setRegistrationNumber(request.registrationNumber());
        if (request.tin() != null) tenant.setTin(request.tin());
        if (request.memberCode() != null) tenant.setMemberCode(request.memberCode());
        if (request.xroadSubsystem() != null) tenant.setXroadSubsystem(request.xroadSubsystem());
        if (request.settings() != null) tenant.setSettings(request.settings());

        tenant = tenantRepository.save(tenant);

        auditService.logAdminAction(currentUser, "TENANT_UPDATED",
                "tenant:" + realmName, true, tenant.getId().toString(), null);

        return toResponse(tenant);
    }

    @Transactional
    public TenantResponse suspendTenant(String realmName) {
        String currentUser = getCurrentUsername();
        Tenant tenant = findByRealmName(realmName);

        if (tenant.getStatus() == TenantStatus.DECOMMISSIONED) {
            throw new TenantProvisioningException(
                    "Cannot suspend a decommissioned tenant");
        }

        keycloakRealmService.disableRealm(realmName);
        tenant.setStatus(TenantStatus.SUSPENDED);
        tenant = tenantRepository.save(tenant);

        auditService.logAdminAction(currentUser, "TENANT_SUSPENDED",
                "tenant:" + realmName, true, tenant.getId().toString(), null);

        log.info("Tenant '{}' suspended by '{}'", realmName, currentUser);
        return toResponse(tenant);
    }

    @Transactional
    public TenantResponse activateTenant(String realmName) {
        String currentUser = getCurrentUsername();
        Tenant tenant = findByRealmName(realmName);

        if (tenant.getStatus() == TenantStatus.DECOMMISSIONED) {
            throw new TenantProvisioningException(
                    "Cannot activate a decommissioned tenant");
        }

        keycloakRealmService.enableRealm(realmName);
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant = tenantRepository.save(tenant);

        auditService.logAdminAction(currentUser, "TENANT_ACTIVATED",
                "tenant:" + realmName, true, tenant.getId().toString(), null);

        log.info("Tenant '{}' activated by '{}'", realmName, currentUser);
        return toResponse(tenant);
    }

    @Transactional
    public void deleteTenant(String realmName) {
        String currentUser = getCurrentUsername();
        Tenant tenant = findByRealmName(realmName);

        // Soft delete — disable realm but don't destroy it
        if (tenant.getStatus() == TenantStatus.ACTIVE) {
            keycloakRealmService.disableRealm(realmName);
        }
        tenant.softDelete();
        tenant.setStatus(TenantStatus.DECOMMISSIONED);
        tenantRepository.save(tenant);

        auditService.logAdminAction(currentUser, "TENANT_DELETED",
                "tenant:" + realmName, true, tenant.getId().toString(), null);

        log.info("Tenant '{}' soft-deleted by '{}'", realmName, currentUser);
    }

    private Tenant findByRealmName(String realmName) {
        return tenantRepository.findByRealmName(realmName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tenant not found with realm: " + realmName));
    }

    private TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getTenantName(),
                tenant.getRealmName(),
                tenant.getDescription(),
                tenant.getMemberClass(),
                tenant.getEntityType(),
                tenant.getRegistrationNumber(),
                tenant.getTin(),
                tenant.getMemberCode(),
                tenant.getXroadSubsystem(),
                tenant.getStatus(),
                tenant.getAdminEmail(),
                tenant.getAdminUsername(),
                tenant.getSettings(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }
}
