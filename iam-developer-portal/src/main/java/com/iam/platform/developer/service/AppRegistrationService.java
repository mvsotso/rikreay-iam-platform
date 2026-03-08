package com.iam.platform.developer.service;

import com.iam.platform.developer.dto.AppCredentialsResponse;
import com.iam.platform.developer.dto.AppRegistrationRequest;
import com.iam.platform.developer.dto.AppResponse;
import com.iam.platform.developer.entity.RegisteredApp;
import com.iam.platform.developer.enums.AppStatus;
import com.iam.platform.developer.repository.RegisteredAppRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppRegistrationService {

    private final RegisteredAppRepository appRepository;
    private final Keycloak keycloakAdmin;
    private final AuditService auditService;

    @Value("${keycloak.admin.realm:master}")
    private String adminRealm;

    private static final String IAM_PLATFORM_REALM = "iam-platform";

    @Transactional
    @CircuitBreaker(name = "keycloak", fallbackMethod = "registerAppFallback")
    public AppResponse registerApp(AppRegistrationRequest request, String ownerId) {
        String clientId = "dev-" + UUID.randomUUID().toString().substring(0, 8);

        // Create Keycloak client
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setName(request.name());
        client.setDescription(request.description());
        client.setEnabled(true);
        client.setPublicClient(false);
        client.setServiceAccountsEnabled(true);
        client.setDirectAccessGrantsEnabled(false);
        client.setStandardFlowEnabled(true);
        client.setRedirectUris(request.redirectUris() != null ? request.redirectUris() : List.of());

        keycloakAdmin.realm(IAM_PLATFORM_REALM).clients().create(client);

        // Get the generated secret
        String secret = getClientSecret(clientId);

        RegisteredApp app = RegisteredApp.builder()
                .name(request.name())
                .description(request.description())
                .clientId(clientId)
                .clientSecretEncrypted(secret)
                .redirectUrisJson(request.redirectUris() != null ? request.redirectUris() : List.of())
                .ownerId(ownerId)
                .status(AppStatus.ACTIVE)
                .build();

        app = appRepository.save(app);

        auditService.logDeveloperAction(ownerId, "APP_REGISTERED", app.getClientId(),
                true, Map.of("appName", request.name()));
        auditService.publishPlatformEvent("APP_REGISTERED", Map.of(
                "appId", app.getId().toString(), "clientId", clientId, "owner", ownerId));

        log.info("Registered app: clientId={}, owner={}", clientId, ownerId);
        return toResponse(app);
    }

    public AppResponse registerAppFallback(AppRegistrationRequest request, String ownerId, Throwable t) {
        log.error("Failed to register app in Keycloak: {}", t.getMessage());
        throw new RuntimeException("Keycloak is unavailable. Please try again later.");
    }

    @Transactional(readOnly = true)
    public AppResponse getApp(UUID id) {
        return toResponse(appRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("App not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<AppResponse> getAppsByOwner(String ownerId, Pageable pageable) {
        return appRepository.findByOwnerId(ownerId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<AppResponse> getAllApps() {
        return appRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    @CircuitBreaker(name = "keycloak", fallbackMethod = "regenerateCredentialsFallback")
    public AppCredentialsResponse regenerateCredentials(UUID id, String ownerId) {
        RegisteredApp app = appRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("App not found: " + id));

        // Regenerate secret in Keycloak
        List<ClientRepresentation> clients = keycloakAdmin.realm(IAM_PLATFORM_REALM)
                .clients().findByClientId(app.getClientId());
        if (!clients.isEmpty()) {
            String keycloakId = clients.get(0).getId();
            CredentialRepresentation cred = keycloakAdmin.realm(IAM_PLATFORM_REALM)
                    .clients().get(keycloakId).generateNewSecret();

            app.setClientSecretEncrypted(cred.getValue());
            appRepository.save(app);

            auditService.logDeveloperAction(ownerId, "CREDENTIALS_REGENERATED", app.getClientId(),
                    true, Map.of());

            return new AppCredentialsResponse(app.getClientId(), cred.getValue());
        }

        throw new RuntimeException("Keycloak client not found: " + app.getClientId());
    }

    public AppCredentialsResponse regenerateCredentialsFallback(UUID id, String ownerId, Throwable t) {
        log.error("Failed to regenerate credentials in Keycloak: {}", t.getMessage());
        throw new RuntimeException("Keycloak is unavailable. Please try again later.");
    }

    @Transactional
    @CircuitBreaker(name = "keycloak", fallbackMethod = "updateRedirectUrisFallback")
    public AppResponse updateRedirectUris(UUID id, List<String> redirectUris, String ownerId) {
        RegisteredApp app = appRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("App not found: " + id));

        // Update in Keycloak
        List<ClientRepresentation> clients = keycloakAdmin.realm(IAM_PLATFORM_REALM)
                .clients().findByClientId(app.getClientId());
        if (!clients.isEmpty()) {
            ClientRepresentation client = clients.get(0);
            client.setRedirectUris(redirectUris);
            keycloakAdmin.realm(IAM_PLATFORM_REALM)
                    .clients().get(client.getId()).update(client);
        }

        app.setRedirectUrisJson(redirectUris);
        app = appRepository.save(app);

        auditService.logDeveloperAction(ownerId, "REDIRECT_URIS_UPDATED", app.getClientId(),
                true, Map.of("uris", redirectUris));

        return toResponse(app);
    }

    public AppResponse updateRedirectUrisFallback(UUID id, List<String> redirectUris, String ownerId, Throwable t) {
        log.error("Failed to update redirect URIs in Keycloak: {}", t.getMessage());
        throw new RuntimeException("Keycloak is unavailable. Please try again later.");
    }

    @Transactional
    public void suspendApp(UUID id, String username) {
        RegisteredApp app = appRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("App not found: " + id));
        app.setStatus(AppStatus.SUSPENDED);
        appRepository.save(app);
        auditService.logDeveloperAction(username, "APP_SUSPENDED", app.getClientId(), true, Map.of());
    }

    @Transactional
    public void deleteApp(UUID id, String username) {
        RegisteredApp app = appRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("App not found: " + id));
        app.softDelete();
        appRepository.save(app);
        auditService.logDeveloperAction(username, "APP_DELETED", app.getClientId(), true, Map.of());
    }

    private String getClientSecret(String clientId) {
        try {
            List<ClientRepresentation> clients = keycloakAdmin.realm(IAM_PLATFORM_REALM)
                    .clients().findByClientId(clientId);
            if (!clients.isEmpty()) {
                String id = clients.get(0).getId();
                return keycloakAdmin.realm(IAM_PLATFORM_REALM)
                        .clients().get(id).getSecret().getValue();
            }
        } catch (Exception e) {
            log.warn("Could not retrieve client secret for {}", clientId, e);
        }
        return null;
    }

    private AppResponse toResponse(RegisteredApp app) {
        return new AppResponse(
                app.getId(),
                app.getName(),
                app.getDescription(),
                app.getClientId(),
                app.getRedirectUrisJson(),
                app.getOwnerId(),
                app.getStatus(),
                app.getCreatedAt()
        );
    }
}
