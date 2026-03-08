package com.iam.platform.developer.service;

import com.iam.platform.developer.config.DeveloperProperties;
import com.iam.platform.developer.dto.SandboxRequest;
import com.iam.platform.developer.dto.SandboxResponse;
import com.iam.platform.developer.entity.SandboxRealm;
import com.iam.platform.developer.enums.SandboxStatus;
import com.iam.platform.developer.repository.SandboxRealmRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SandboxService {

    private final SandboxRealmRepository sandboxRepository;
    private final Keycloak keycloakAdmin;
    private final DeveloperProperties properties;
    private final AuditService auditService;

    @Transactional
    @CircuitBreaker(name = "keycloak", fallbackMethod = "createSandboxFallback")
    public SandboxResponse createSandbox(SandboxRequest request, String ownerId) {
        long activeCount = sandboxRepository.countByOwnerIdAndStatus(ownerId, SandboxStatus.ACTIVE);
        if (activeCount >= properties.getSandbox().getMaxPerUser()) {
            throw new RuntimeException("Maximum sandbox limit reached: " + properties.getSandbox().getMaxPerUser());
        }

        String prefix = request.realmPrefix() != null ? request.realmPrefix() : "sandbox";
        String realmName = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        int expiryDays = properties.getSandbox().getExpiryDays();

        // Create Keycloak realm
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(realmName);
        realm.setEnabled(true);
        realm.setDisplayName("Sandbox: " + realmName);
        keycloakAdmin.realms().create(realm);

        // Pre-populate with sample roles
        createSampleRoles(realmName);
        // Pre-populate with sample users
        createSampleUsers(realmName);
        // Create a sample client
        createSampleClient(realmName);

        SandboxRealm sandbox = SandboxRealm.builder()
                .ownerId(ownerId)
                .realmName(realmName)
                .expiresAt(Instant.now().plus(expiryDays, ChronoUnit.DAYS))
                .status(SandboxStatus.ACTIVE)
                .build();

        sandbox = sandboxRepository.save(sandbox);

        auditService.logDeveloperAction(ownerId, "SANDBOX_CREATED", realmName,
                true, Map.of("expiryDays", expiryDays));

        log.info("Created sandbox realm: {}, owner={}, expires in {} days", realmName, ownerId, expiryDays);
        return toResponse(sandbox);
    }

    public SandboxResponse createSandboxFallback(SandboxRequest request, String ownerId, Throwable t) {
        log.error("Failed to create sandbox realm in Keycloak: {}", t.getMessage());
        throw new RuntimeException("Keycloak is unavailable. Please try again later.");
    }

    @Transactional(readOnly = true)
    public List<SandboxResponse> getSandboxesByOwner(String ownerId) {
        return sandboxRepository.findByOwnerIdAndStatus(ownerId, SandboxStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @CircuitBreaker(name = "keycloak", fallbackMethod = "deleteSandboxFallback")
    public void deleteSandbox(UUID id, String ownerId) {
        SandboxRealm sandbox = sandboxRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sandbox not found: " + id));

        // Delete from Keycloak
        try {
            keycloakAdmin.realm(sandbox.getRealmName()).remove();
        } catch (Exception e) {
            log.warn("Failed to remove sandbox realm from Keycloak: {}", e.getMessage());
        }

        sandbox.setStatus(SandboxStatus.DELETED);
        sandboxRepository.save(sandbox);

        auditService.logDeveloperAction(ownerId, "SANDBOX_DELETED", sandbox.getRealmName(),
                true, Map.of());

        log.info("Deleted sandbox realm: {}", sandbox.getRealmName());
    }

    public void deleteSandboxFallback(UUID id, String ownerId, Throwable t) {
        log.error("Failed to delete sandbox realm from Keycloak: {}", t.getMessage());
        throw new RuntimeException("Keycloak is unavailable. Please try again later.");
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredSandboxes() {
        log.info("Running sandbox cleanup...");
        List<SandboxRealm> expired = sandboxRepository
                .findByStatusAndExpiresAtBefore(SandboxStatus.ACTIVE, Instant.now());

        for (SandboxRealm sandbox : expired) {
            try {
                keycloakAdmin.realm(sandbox.getRealmName()).remove();
                sandbox.setStatus(SandboxStatus.EXPIRED);
                sandboxRepository.save(sandbox);
                log.info("Cleaned up expired sandbox: {}", sandbox.getRealmName());
            } catch (Exception e) {
                log.error("Failed to cleanup sandbox {}: {}", sandbox.getRealmName(), e.getMessage());
            }
        }

        log.info("Sandbox cleanup complete. Processed {} expired sandboxes.", expired.size());
    }

    private void createSampleRoles(String realmName) {
        List<String> sampleRoles = List.of("user", "admin", "manager", "viewer");
        for (String roleName : sampleRoles) {
            RoleRepresentation role = new RoleRepresentation();
            role.setName(roleName);
            role.setDescription("Sample " + roleName + " role for sandbox testing");
            try {
                keycloakAdmin.realm(realmName).roles().create(role);
            } catch (Exception e) {
                log.warn("Failed to create sample role {}: {}", roleName, e.getMessage());
            }
        }
    }

    private void createSampleUsers(String realmName) {
        List<Map<String, String>> sampleUsers = List.of(
                Map.of("username", "test-user", "email", "test-user@sandbox.local",
                        "firstName", "Test", "lastName", "User"),
                Map.of("username", "test-admin", "email", "test-admin@sandbox.local",
                        "firstName", "Test", "lastName", "Admin")
        );

        for (Map<String, String> userData : sampleUsers) {
            UserRepresentation user = new UserRepresentation();
            user.setUsername(userData.get("username"));
            user.setEmail(userData.get("email"));
            user.setFirstName(userData.get("firstName"));
            user.setLastName(userData.get("lastName"));
            user.setEnabled(true);
            user.setEmailVerified(true);

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue("sandbox123");
            credential.setTemporary(false);
            user.setCredentials(List.of(credential));

            try {
                keycloakAdmin.realm(realmName).users().create(user);
            } catch (Exception e) {
                log.warn("Failed to create sample user {}: {}", userData.get("username"), e.getMessage());
            }
        }
    }

    private void createSampleClient(String realmName) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("sandbox-app");
        client.setName("Sandbox Application");
        client.setEnabled(true);
        client.setPublicClient(true);
        client.setStandardFlowEnabled(true);
        client.setDirectAccessGrantsEnabled(true);
        client.setRedirectUris(List.of("http://localhost:*"));

        try {
            keycloakAdmin.realm(realmName).clients().create(client);
        } catch (Exception e) {
            log.warn("Failed to create sample client: {}", e.getMessage());
        }
    }

    private SandboxResponse toResponse(SandboxRealm sandbox) {
        return new SandboxResponse(
                sandbox.getId(),
                sandbox.getRealmName(),
                sandbox.getOwnerId(),
                sandbox.getStatus(),
                sandbox.getExpiresAt(),
                sandbox.getCreatedAt()
        );
    }
}
