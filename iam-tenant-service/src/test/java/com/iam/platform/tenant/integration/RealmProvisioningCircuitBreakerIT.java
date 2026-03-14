package com.iam.platform.tenant.integration;

import com.iam.platform.common.enums.MemberClass;
import com.iam.platform.common.exception.TenantProvisioningException;
import com.iam.platform.tenant.dto.CreateTenantRequest;
import com.iam.platform.tenant.dto.TenantResponse;
import com.iam.platform.tenant.entity.Tenant;
import com.iam.platform.tenant.enums.TenantStatus;
import com.iam.platform.tenant.repository.TenantRepository;
import com.iam.platform.tenant.service.AuditService;
import com.iam.platform.tenant.service.KeycloakRealmService;
import com.iam.platform.tenant.service.TenantProvisioningService;
import jakarta.ws.rs.ProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integration tests for tenant realm provisioning when Keycloak is unavailable.
 * Verifies that the TenantProvisioningService handles Keycloak failures gracefully,
 * marks tenants as DECOMMISSIONED on failure, and publishes appropriate audit events.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
class RealmProvisioningCircuitBreakerIT {

    @Autowired
    private TenantProvisioningService tenantProvisioningService;

    @Autowired
    private TenantRepository tenantRepository;

    @MockBean
    private Keycloak keycloak;

    @MockBean
    private AuditService auditService;

    private RealmsResource mockRealmsResource;
    private RealmResource mockRealmResource;
    private UsersResource mockUsersResource;
    private RolesResource mockRolesResource;
    private ClientsResource mockClientsResource;

    @BeforeEach
    void setUp() {
        tenantRepository.deleteAll();

        mockRealmsResource = mock(RealmsResource.class);
        mockRealmResource = mock(RealmResource.class);
        mockUsersResource = mock(UsersResource.class);
        mockRolesResource = mock(RolesResource.class);
        mockClientsResource = mock(ClientsResource.class);

        when(keycloak.realms()).thenReturn(mockRealmsResource);
        when(keycloak.realm(anyString())).thenReturn(mockRealmResource);
        when(mockRealmResource.users()).thenReturn(mockUsersResource);
        when(mockRealmResource.roles()).thenReturn(mockRolesResource);
        when(mockRealmResource.clients()).thenReturn(mockClientsResource);
    }

    @Test
    @DisplayName("Should mark tenant as DECOMMISSIONED when Keycloak realm creation fails")
    @WithMockUser(username = "platform-admin", roles = {"iam-admin"})
    void shouldMarkTenantDecommissionedWhenRealmCreationFails() {
        // Keycloak realm creation fails
        doThrow(new ProcessingException("Connection refused"))
                .when(mockRealmsResource).create(any(RealmRepresentation.class));

        // realmExists check returns false
        when(mockRealmResource.toRepresentation())
                .thenThrow(new ProcessingException("Not found"));

        CreateTenantRequest request = createTenantRequest("Test Org", "test-org-realm");

        assertThatThrownBy(() -> tenantProvisioningService.createTenant(request))
                .isInstanceOf(TenantProvisioningException.class)
                .hasMessageContaining("Failed to provision tenant");

        // Verify tenant was created but marked as DECOMMISSIONED
        Optional<Tenant> tenant = tenantRepository.findByRealmName("test-org-realm");
        assertThat(tenant).isPresent();
        assertThat(tenant.get().getStatus()).isEqualTo(TenantStatus.DECOMMISSIONED);
    }

    @Test
    @DisplayName("Should mark tenant as DECOMMISSIONED when role creation fails")
    @WithMockUser(username = "platform-admin", roles = {"iam-admin"})
    void shouldMarkTenantDecommissionedWhenRoleCreationFails() {
        // Realm creation succeeds
        doNothing().when(mockRealmsResource).create(any(RealmRepresentation.class));
        // realmExists check returns false
        when(mockRealmResource.toRepresentation())
                .thenThrow(new ProcessingException("Not found"));

        // Role creation fails
        doThrow(new ProcessingException("Connection lost"))
                .when(mockRolesResource).create(any());

        CreateTenantRequest request = createTenantRequest("Test Org 2", "test-org-realm-2");

        assertThatThrownBy(() -> tenantProvisioningService.createTenant(request))
                .isInstanceOf(TenantProvisioningException.class);

        Optional<Tenant> tenant = tenantRepository.findByRealmName("test-org-realm-2");
        assertThat(tenant).isPresent();
        assertThat(tenant.get().getStatus()).isEqualTo(TenantStatus.DECOMMISSIONED);
    }

    @Test
    @DisplayName("Should publish audit event on tenant creation failure")
    @WithMockUser(username = "platform-admin", roles = {"iam-admin"})
    void shouldPublishAuditEventOnFailure() {
        when(mockRealmResource.toRepresentation())
                .thenThrow(new ProcessingException("Not found"));
        doThrow(new ProcessingException("Connection refused"))
                .when(mockRealmsResource).create(any(RealmRepresentation.class));

        CreateTenantRequest request = createTenantRequest("Audit Test Org", "audit-test-realm");

        assertThatThrownBy(() -> tenantProvisioningService.createTenant(request))
                .isInstanceOf(TenantProvisioningException.class);

        // Verify audit event was published for the failure
        verify(auditService, atLeastOnce()).logAdminAction(
                anyString(), eq("TENANT_CREATION_FAILED"),
                anyString(), eq(false), anyString(), any());
    }

    @Test
    @DisplayName("Should reject duplicate tenant name")
    @WithMockUser(username = "platform-admin", roles = {"iam-admin"})
    void shouldRejectDuplicateTenantName() {
        // Pre-create a tenant
        Tenant existing = Tenant.builder()
                .tenantName("Existing Org")
                .realmName("existing-realm")
                .memberClass(MemberClass.GOV)
                .status(TenantStatus.ACTIVE)
                .adminEmail("admin@existing.com")
                .adminUsername("admin")
                .build();
        tenantRepository.save(existing);

        CreateTenantRequest request = createTenantRequest("Existing Org", "new-realm");

        assertThatThrownBy(() -> tenantProvisioningService.createTenant(request))
                .isInstanceOf(TenantProvisioningException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should reject duplicate realm name")
    @WithMockUser(username = "platform-admin", roles = {"iam-admin"})
    void shouldRejectDuplicateRealmName() {
        Tenant existing = Tenant.builder()
                .tenantName("Some Org")
                .realmName("duplicate-realm")
                .memberClass(MemberClass.COM)
                .status(TenantStatus.ACTIVE)
                .adminEmail("admin@some.com")
                .adminUsername("admin")
                .build();
        tenantRepository.save(existing);

        CreateTenantRequest request = createTenantRequest("New Org", "duplicate-realm");

        assertThatThrownBy(() -> tenantProvisioningService.createTenant(request))
                .isInstanceOf(TenantProvisioningException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should reject when Keycloak realm already exists")
    @WithMockUser(username = "platform-admin", roles = {"iam-admin"})
    void shouldRejectWhenKeycloakRealmAlreadyExists() {
        // Keycloak says realm exists
        RealmRepresentation existingRealm = new RealmRepresentation();
        existingRealm.setRealm("existing-kc-realm");
        when(mockRealmResource.toRepresentation()).thenReturn(existingRealm);

        CreateTenantRequest request = createTenantRequest("New Org 2", "existing-kc-realm");

        assertThatThrownBy(() -> tenantProvisioningService.createTenant(request))
                .isInstanceOf(TenantProvisioningException.class)
                .hasMessageContaining("already exists");
    }

    private CreateTenantRequest createTenantRequest(String name, String realmName) {
        return new CreateTenantRequest(
                name,
                realmName,
                "Test organization",
                MemberClass.GOV,
                null,
                null,
                null,
                null,
                null,
                "admin@test.com",
                "admin-user",
                "TempPass123!",
                null
        );
    }
}
