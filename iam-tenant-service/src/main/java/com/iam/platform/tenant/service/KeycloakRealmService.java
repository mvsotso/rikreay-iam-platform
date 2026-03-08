package com.iam.platform.tenant.service;

import com.iam.platform.common.constants.IamRoles;
import com.iam.platform.common.exception.TenantProvisioningException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Handles all Keycloak Admin API operations for realm provisioning.
 * Only iam-tenant-service should use keycloak-admin-client for realm management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakRealmService {

    private final Keycloak keycloak;

    private static final List<String> DEFAULT_REALM_ROLES = List.of(
            IamRoles.ROLE_TENANT_ADMIN,
            IamRoles.ROLE_INTERNAL_USER,
            IamRoles.ROLE_EXTERNAL_USER,
            IamRoles.ROLE_API_ACCESS,
            IamRoles.ROLE_AUDITOR
    );

    /**
     * Creates a new Keycloak realm with default configuration.
     */
    public void createRealm(String realmName, String displayName) {
        log.info("Creating Keycloak realm: {}", realmName);

        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(realmName);
        realm.setDisplayName(displayName);
        realm.setEnabled(true);
        realm.setRegistrationAllowed(false);
        realm.setResetPasswordAllowed(true);
        realm.setRememberMe(true);
        realm.setLoginWithEmailAllowed(true);
        realm.setDuplicateEmailsAllowed(false);
        realm.setVerifyEmail(true);
        realm.setSslRequired("external");
        realm.setAccessTokenLifespan(300); // 5 minutes
        realm.setSsoSessionIdleTimeout(1800); // 30 minutes
        realm.setSsoSessionMaxLifespan(36000); // 10 hours

        try {
            keycloak.realms().create(realm);
            log.info("Keycloak realm created: {}", realmName);
        } catch (Exception e) {
            throw new TenantProvisioningException(
                    "Failed to create Keycloak realm: " + realmName, e);
        }
    }

    /**
     * Creates default RBAC roles in the tenant realm.
     */
    public void createDefaultRoles(String realmName) {
        log.info("Creating default roles in realm: {}", realmName);
        RealmResource realmResource = keycloak.realm(realmName);

        for (String roleName : DEFAULT_REALM_ROLES) {
            RoleRepresentation role = new RoleRepresentation();
            role.setName(roleName);
            role.setDescription("Default role: " + roleName);
            try {
                realmResource.roles().create(role);
                log.debug("Created role '{}' in realm '{}'", roleName, realmName);
            } catch (Exception e) {
                log.warn("Role '{}' may already exist in realm '{}': {}",
                        roleName, realmName, e.getMessage());
            }
        }
    }

    /**
     * Creates a default confidential client for the tenant realm.
     */
    public void createDefaultClient(String realmName) {
        log.info("Creating default client in realm: {}", realmName);
        RealmResource realmResource = keycloak.realm(realmName);

        String clientId = realmName + "-api";

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setName(realmName + " API Client");
        client.setEnabled(true);
        client.setProtocol("openid-connect");
        client.setPublicClient(false);
        client.setServiceAccountsEnabled(true);
        client.setStandardFlowEnabled(true);
        client.setDirectAccessGrantsEnabled(false);
        client.setAttributes(Map.of(
                "pkce.code.challenge.method", "S256",
                "access.token.lifespan", "300"
        ));

        try {
            realmResource.clients().create(client);
            log.info("Created client '{}' in realm '{}'", clientId, realmName);
        } catch (Exception e) {
            log.warn("Client '{}' may already exist in realm '{}': {}",
                    clientId, realmName, e.getMessage());
        }
    }

    /**
     * Creates the tenant admin user in the realm with tenant-admin role.
     */
    public void createAdminUser(String realmName, String username, String email,
                                 String temporaryPassword) {
        log.info("Creating admin user '{}' in realm '{}'", username, realmName);
        RealmResource realmResource = keycloak.realm(realmName);

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setRequiredActions(List.of("UPDATE_PASSWORD"));

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(temporaryPassword);
        credential.setTemporary(true);
        user.setCredentials(Collections.singletonList(credential));

        try (Response response = realmResource.users().create(user)) {
            if (response.getStatus() == 201) {
                String userId = extractUserId(response);
                assignTenantAdminRole(realmResource, userId);
                log.info("Admin user '{}' created in realm '{}' with tenant-admin role",
                        username, realmName);
            } else {
                throw new TenantProvisioningException(
                        "Failed to create admin user. Status: " + response.getStatus());
            }
        } catch (TenantProvisioningException e) {
            throw e;
        } catch (Exception e) {
            throw new TenantProvisioningException(
                    "Failed to create admin user in realm: " + realmName, e);
        }
    }

    /**
     * Disables a Keycloak realm (suspend).
     */
    public void disableRealm(String realmName) {
        log.info("Disabling Keycloak realm: {}", realmName);
        try {
            RealmResource realmResource = keycloak.realm(realmName);
            RealmRepresentation realm = realmResource.toRepresentation();
            realm.setEnabled(false);
            realmResource.update(realm);
            log.info("Keycloak realm disabled: {}", realmName);
        } catch (Exception e) {
            throw new TenantProvisioningException(
                    "Failed to disable Keycloak realm: " + realmName, e);
        }
    }

    /**
     * Re-enables a Keycloak realm (activate).
     */
    public void enableRealm(String realmName) {
        log.info("Enabling Keycloak realm: {}", realmName);
        try {
            RealmResource realmResource = keycloak.realm(realmName);
            RealmRepresentation realm = realmResource.toRepresentation();
            realm.setEnabled(true);
            realmResource.update(realm);
            log.info("Keycloak realm enabled: {}", realmName);
        } catch (Exception e) {
            throw new TenantProvisioningException(
                    "Failed to enable Keycloak realm: " + realmName, e);
        }
    }

    /**
     * Checks if a realm exists in Keycloak.
     */
    public boolean realmExists(String realmName) {
        try {
            keycloak.realm(realmName).toRepresentation();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void assignTenantAdminRole(RealmResource realmResource, String userId) {
        try {
            RoleRepresentation tenantAdminRole = realmResource.roles()
                    .get(IamRoles.ROLE_TENANT_ADMIN)
                    .toRepresentation();
            realmResource.users().get(userId).roles().realmLevel()
                    .add(Collections.singletonList(tenantAdminRole));
        } catch (Exception e) {
            log.warn("Could not assign tenant-admin role to user {}: {}", userId, e.getMessage());
        }
    }

    private String extractUserId(Response response) {
        String locationHeader = response.getHeaderString("Location");
        if (locationHeader != null) {
            return locationHeader.substring(locationHeader.lastIndexOf('/') + 1);
        }
        throw new TenantProvisioningException("Could not extract user ID from response");
    }
}
