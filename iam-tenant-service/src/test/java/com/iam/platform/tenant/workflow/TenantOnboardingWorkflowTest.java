package com.iam.platform.tenant.workflow;

import com.iam.platform.tenant.dto.CreateTenantRequest;
import com.iam.platform.tenant.dto.TenantResponse;
import com.iam.platform.tenant.enums.EntityType;
import com.iam.platform.tenant.enums.MemberClass;
import com.iam.platform.tenant.enums.TenantStatus;
import com.iam.platform.tenant.repository.TenantRepository;
import com.iam.platform.tenant.service.AuditService;
import com.iam.platform.tenant.service.KeycloakRealmService;
import com.iam.platform.tenant.service.TenantProvisioningService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Workflow: User Onboarding — Tenant Creation")
class TenantOnboardingWorkflowTest {

    @Autowired
    private TenantProvisioningService tenantService;

    @Autowired
    private TenantRepository tenantRepository;

    @MockitoBean
    private KeycloakRealmService keycloakRealmService;

    @MockitoBean
    private AuditService auditService;

    @Test
    @DisplayName("E2E: Create tenant → persist → publish audit event → publish tenant-created event")
    void createTenantFullWorkflow() {
        // Arrange — KeycloakRealmService is mocked (no-op for realm creation)
        when(keycloakRealmService.realmExists(anyString())).thenReturn(false);

        CreateTenantRequest request = new CreateTenantRequest(
                "Ministry of Finance",
                "mof-realm",
                "Cambodia Ministry of Finance",
                MemberClass.GOV,
                EntityType.GOVERNMENT_MINISTRY,
                "REG-MOF-001",
                "TIN-MOF-2026",
                "MOF",
                "mof-subsystem",
                "admin@mof.gov.kh",
                "mof.admin",
                "TempPass@2026",
                Map.of("sector", "finance")
        );

        // Act
        TenantResponse response = tenantService.createTenant(request);

        // Assert — tenant persisted correctly
        assertThat(response).isNotNull();
        assertThat(response.tenantName()).isEqualTo("Ministry of Finance");
        assertThat(response.realmName()).isEqualTo("mof-realm");
        assertThat(response.memberClass()).isEqualTo(MemberClass.GOV);
        assertThat(response.entityType()).isEqualTo(EntityType.GOVERNMENT_MINISTRY);
        assertThat(response.status()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(response.adminEmail()).isEqualTo("admin@mof.gov.kh");
        assertThat(response.adminUsername()).isEqualTo("mof.admin");

        // Assert — Keycloak realm provisioning was called
        verify(keycloakRealmService).createRealm(eq("mof-realm"), eq("Ministry of Finance"));
        verify(keycloakRealmService).createDefaultRoles(eq("mof-realm"));
        verify(keycloakRealmService).createDefaultClient(eq("mof-realm"));
        verify(keycloakRealmService).createAdminUser(eq("mof-realm"), eq("mof.admin"), eq("admin@mof.gov.kh"), eq("TempPass@2026"));

        // Assert — audit event published
        verify(auditService).logAdminAction(
                anyString(), eq("TENANT_CREATED"), anyString(), eq(true), anyString(), any());

        // Assert — tenant-created platform event published
        verify(auditService).publishTenantCreated(anyString(), eq("mof-realm"), anyString());
    }

    @Test
    @DisplayName("E2E: Create tenant → suspend → verify status change and audit")
    void createAndSuspendTenantWorkflow() {
        when(keycloakRealmService.realmExists(anyString())).thenReturn(false);

        CreateTenantRequest request = new CreateTenantRequest(
                "Wing Money", "wing-realm", "Wing Money Cambodia",
                MemberClass.COM, EntityType.PRIVATE_LLC,
                "REG-WING-001", "TIN-WING-2026", "WING", "wing-subsystem",
                "admin@wing.com.kh", "wing.admin", "TempPass@2026", null);

        TenantResponse created = tenantService.createTenant(request);
        assertThat(created.status()).isEqualTo(TenantStatus.ACTIVE);

        // Act — suspend
        TenantResponse suspended = tenantService.suspendTenant("wing-realm");

        // Assert — status changed to SUSPENDED
        assertThat(suspended.status()).isEqualTo(TenantStatus.SUSPENDED);

        // Verify audit logged for both create and suspend
        verify(auditService, times(2)).logAdminAction(
                anyString(), anyString(), anyString(), eq(true), anyString(), any());
    }

    @Test
    @DisplayName("E2E: Create tenant → suspend → reactivate → verify lifecycle")
    void createSuspendAndReactivateWorkflow() {
        when(keycloakRealmService.realmExists(anyString())).thenReturn(false);

        CreateTenantRequest request = new CreateTenantRequest(
                "ABA Bank", "aba-realm", "ABA Bank Cambodia",
                MemberClass.COM, EntityType.PRIVATE_LLC,
                "REG-ABA-001", "TIN-ABA-2026", "ABA", "aba-subsystem",
                "admin@aba.com.kh", "aba.admin", "TempPass@2026", null);

        tenantService.createTenant(request);
        tenantService.suspendTenant("aba-realm");
        TenantResponse reactivated = tenantService.activateTenant("aba-realm");

        assertThat(reactivated.status()).isEqualTo(TenantStatus.ACTIVE);

        // Full lifecycle: create + suspend + activate = 3 audit events
        verify(auditService, times(3)).logAdminAction(
                anyString(), anyString(), anyString(), eq(true), anyString(), any());
    }
}
