package com.iam.platform.tenant.service;

import com.iam.platform.common.constants.IamRoles;
import com.iam.platform.common.exception.TenantProvisioningException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for KeycloakRealmService.
 * All Keycloak admin client interactions are mocked.
 */
@ExtendWith(MockitoExtension.class)
class KeycloakRealmServiceTest {

    @Mock
    private Keycloak keycloak;

    @InjectMocks
    private KeycloakRealmService keycloakRealmService;

    private RealmsResource realmsResource;
    private RealmResource realmResource;
    private RolesResource rolesResource;
    private ClientsResource clientsResource;
    private UsersResource usersResource;

    @BeforeEach
    void setUp() {
        realmsResource = mock(RealmsResource.class);
        realmResource = mock(RealmResource.class);
        rolesResource = mock(RolesResource.class);
        clientsResource = mock(ClientsResource.class);
        usersResource = mock(UsersResource.class);
    }

    // ====== createRealm ======

    @Nested
    @DisplayName("createRealm")
    class CreateRealm {

        @Test
        @DisplayName("Should create realm with correct properties")
        void createRealmSuccess() {
            when(keycloak.realms()).thenReturn(realmsResource);

            keycloakRealmService.createRealm("test-corp", "Test Corporation");

            verify(realmsResource).create(argThat(realm -> {
                assertThat(realm.getRealm()).isEqualTo("test-corp");
                assertThat(realm.getDisplayName()).isEqualTo("Test Corporation");
                assertThat(realm.isEnabled()).isTrue();
                assertThat(realm.isRegistrationAllowed()).isFalse();
                assertThat(realm.isVerifyEmail()).isTrue();
                assertThat(realm.getSslRequired()).isEqualTo("external");
                assertThat(realm.getAccessTokenLifespan()).isEqualTo(300);
                assertThat(realm.getSsoSessionIdleTimeout()).isEqualTo(1800);
                assertThat(realm.getSsoSessionMaxLifespan()).isEqualTo(36000);
                return true;
            }));
        }

        @Test
        @DisplayName("Should throw TenantProvisioningException on failure")
        void createRealmFailure() {
            when(keycloak.realms()).thenReturn(realmsResource);
            doThrow(new RuntimeException("Keycloak unavailable"))
                    .when(realmsResource).create(any());

            assertThatThrownBy(() -> keycloakRealmService.createRealm("bad-realm", "Bad"))
                    .isInstanceOf(TenantProvisioningException.class)
                    .hasMessageContaining("Failed to create Keycloak realm: bad-realm");
        }
    }

    // ====== createDefaultRoles ======

    @Nested
    @DisplayName("createDefaultRoles")
    class CreateDefaultRoles {

        @Test
        @DisplayName("Should create 5 default roles in the realm")
        void createDefaultRolesSuccess() {
            when(keycloak.realm("test-corp")).thenReturn(realmResource);
            when(realmResource.roles()).thenReturn(rolesResource);

            keycloakRealmService.createDefaultRoles("test-corp");

            // 5 default roles: tenant-admin, internal-user, external-user, api-access, auditor
            verify(rolesResource, times(5)).create(any(RoleRepresentation.class));
        }

        @Test
        @DisplayName("Should create tenant-admin role")
        void createsTenantAdminRole() {
            when(keycloak.realm("test-corp")).thenReturn(realmResource);
            when(realmResource.roles()).thenReturn(rolesResource);

            keycloakRealmService.createDefaultRoles("test-corp");

            verify(rolesResource).create(argThat(role ->
                    IamRoles.ROLE_TENANT_ADMIN.equals(role.getName())));
        }

        @Test
        @DisplayName("Should not throw if role already exists")
        void createDefaultRolesIdempotent() {
            when(keycloak.realm("test-corp")).thenReturn(realmResource);
            when(realmResource.roles()).thenReturn(rolesResource);
            doThrow(new RuntimeException("Role exists"))
                    .when(rolesResource).create(any());

            // Should not throw — logs warning instead
            keycloakRealmService.createDefaultRoles("test-corp");
        }
    }

    // ====== createDefaultClient ======

    @Nested
    @DisplayName("createDefaultClient")
    class CreateDefaultClient {

        @Test
        @DisplayName("Should create client with realm-name-api clientId")
        void createDefaultClientSuccess() {
            when(keycloak.realm("test-corp")).thenReturn(realmResource);
            when(realmResource.clients()).thenReturn(clientsResource);

            keycloakRealmService.createDefaultClient("test-corp");

            verify(clientsResource).create(argThat(client -> {
                assertThat(client.getClientId()).isEqualTo("test-corp-api");
                assertThat(client.isPublicClient()).isFalse();
                assertThat(client.isServiceAccountsEnabled()).isTrue();
                assertThat(client.isStandardFlowEnabled()).isTrue();
                assertThat(client.isDirectAccessGrantsEnabled()).isFalse();
                assertThat(client.getAttributes()).containsEntry("pkce.code.challenge.method", "S256");
                return true;
            }));
        }

        @Test
        @DisplayName("Should not throw if client already exists")
        void createDefaultClientIdempotent() {
            when(keycloak.realm("test-corp")).thenReturn(realmResource);
            when(realmResource.clients()).thenReturn(clientsResource);
            doThrow(new RuntimeException("Client exists"))
                    .when(clientsResource).create(any());

            // Should not throw
            keycloakRealmService.createDefaultClient("test-corp");
        }
    }

    // ====== createAdminUser ======

    @Nested
    @DisplayName("createAdminUser")
    class CreateAdminUser {

        @Test
        @DisplayName("Should create user and assign tenant-admin role on 201")
        void createAdminUserSuccess() {
            when(keycloak.realm("test-corp")).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);

            Response mockResponse = mock(Response.class);
            when(mockResponse.getStatus()).thenReturn(201);
            when(mockResponse.getHeaderString("Location"))
                    .thenReturn("http://keycloak/admin/realms/test-corp/users/user-uuid-123");
            when(usersResource.create(any())).thenReturn(mockResponse);

            // Mock role assignment chain
            UserResource userResource = mock(UserResource.class);
            RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
            RoleScopeResource roleScopeResource = mock(RoleScopeResource.class);
            RoleResource roleResource = mock(RoleResource.class);
            RoleRepresentation roleRep = new RoleRepresentation();
            roleRep.setName(IamRoles.ROLE_TENANT_ADMIN);

            when(usersResource.get("user-uuid-123")).thenReturn(userResource);
            when(userResource.roles()).thenReturn(roleMappingResource);
            when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
            when(realmResource.roles()).thenReturn(rolesResource);
            when(rolesResource.get(IamRoles.ROLE_TENANT_ADMIN)).thenReturn(roleResource);
            when(roleResource.toRepresentation()).thenReturn(roleRep);

            keycloakRealmService.createAdminUser("test-corp", "admin", "admin@test.com", "Temp123!");

            verify(usersResource).create(argThat(user -> {
                assertThat(user.getUsername()).isEqualTo("admin");
                assertThat(user.getEmail()).isEqualTo("admin@test.com");
                assertThat(user.isEnabled()).isTrue();
                assertThat(user.isEmailVerified()).isTrue();
                assertThat(user.getRequiredActions()).contains("UPDATE_PASSWORD");
                return true;
            }));
        }

        @Test
        @DisplayName("Should throw if user creation returns non-201")
        void createAdminUserFailure() {
            when(keycloak.realm("test-corp")).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);

            Response mockResponse = mock(Response.class);
            when(mockResponse.getStatus()).thenReturn(409); // Conflict
            when(usersResource.create(any())).thenReturn(mockResponse);

            assertThatThrownBy(() ->
                    keycloakRealmService.createAdminUser("test-corp", "admin", "admin@test.com", "Temp123!"))
                    .isInstanceOf(TenantProvisioningException.class)
                    .hasMessageContaining("Failed to create admin user");
        }
    }

    // ====== disableRealm / enableRealm ======

    @Nested
    @DisplayName("disableRealm and enableRealm")
    class RealmEnableDisable {

        @Test
        @DisplayName("disableRealm should set enabled=false")
        void disableRealm() {
            when(keycloak.realm("test-corp")).thenReturn(realmResource);
            RealmRepresentation realmRep = new RealmRepresentation();
            realmRep.setEnabled(true);
            when(realmResource.toRepresentation()).thenReturn(realmRep);

            keycloakRealmService.disableRealm("test-corp");

            verify(realmResource).update(argThat(realm -> {
                assertThat(realm.isEnabled()).isFalse();
                return true;
            }));
        }

        @Test
        @DisplayName("enableRealm should set enabled=true")
        void enableRealm() {
            when(keycloak.realm("test-corp")).thenReturn(realmResource);
            RealmRepresentation realmRep = new RealmRepresentation();
            realmRep.setEnabled(false);
            when(realmResource.toRepresentation()).thenReturn(realmRep);

            keycloakRealmService.enableRealm("test-corp");

            verify(realmResource).update(argThat(realm -> {
                assertThat(realm.isEnabled()).isTrue();
                return true;
            }));
        }

        @Test
        @DisplayName("disableRealm should throw TenantProvisioningException on failure")
        void disableRealmFailure() {
            when(keycloak.realm("bad-realm")).thenReturn(realmResource);
            when(realmResource.toRepresentation()).thenThrow(new RuntimeException("Not found"));

            assertThatThrownBy(() -> keycloakRealmService.disableRealm("bad-realm"))
                    .isInstanceOf(TenantProvisioningException.class)
                    .hasMessageContaining("Failed to disable Keycloak realm: bad-realm");
        }

        @Test
        @DisplayName("enableRealm should throw TenantProvisioningException on failure")
        void enableRealmFailure() {
            when(keycloak.realm("bad-realm")).thenReturn(realmResource);
            when(realmResource.toRepresentation()).thenThrow(new RuntimeException("Not found"));

            assertThatThrownBy(() -> keycloakRealmService.enableRealm("bad-realm"))
                    .isInstanceOf(TenantProvisioningException.class)
                    .hasMessageContaining("Failed to enable Keycloak realm: bad-realm");
        }
    }

    // ====== realmExists ======

    @Nested
    @DisplayName("realmExists")
    class RealmExists {

        @Test
        @DisplayName("Should return true when realm exists")
        void realmExistsTrue() {
            when(keycloak.realm("existing-realm")).thenReturn(realmResource);
            when(realmResource.toRepresentation()).thenReturn(new RealmRepresentation());

            assertThat(keycloakRealmService.realmExists("existing-realm")).isTrue();
        }

        @Test
        @DisplayName("Should return false when realm does not exist")
        void realmExistsFalse() {
            when(keycloak.realm("nonexistent")).thenReturn(realmResource);
            when(realmResource.toRepresentation()).thenThrow(new RuntimeException("Not found"));

            assertThat(keycloakRealmService.realmExists("nonexistent")).isFalse();
        }
    }
}
