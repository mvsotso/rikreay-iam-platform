package com.iam.platform.admin.service;

import com.iam.platform.admin.dto.BulkUserImportRequest;
import com.iam.platform.common.dto.NotificationCommandDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkUserService {

    private final Keycloak keycloak;
    private final AuditService auditService;

    @CircuitBreaker(name = "keycloak")
    public Map<String, Object> bulkImportUsers(BulkUserImportRequest request, String performedBy) {
        int created = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (BulkUserImportRequest.UserEntry entry : request.users()) {
            try {
                UserRepresentation user = new UserRepresentation();
                user.setUsername(entry.username());
                user.setEmail(entry.email());
                user.setFirstName(entry.firstName());
                user.setLastName(entry.lastName());
                user.setEnabled(true);

                if (entry.temporaryPassword() != null) {
                    CredentialRepresentation cred = new CredentialRepresentation();
                    cred.setType(CredentialRepresentation.PASSWORD);
                    cred.setValue(entry.temporaryPassword());
                    cred.setTemporary(true);
                    user.setCredentials(List.of(cred));
                }

                keycloak.realm(request.realmName()).users().create(user);
                created++;
            } catch (Exception e) {
                failed++;
                errors.add(entry.username() + ": " + e.getMessage());
                log.warn("Failed to create user: {} in realm: {}", entry.username(), request.realmName(), e);
            }
        }

        auditService.logAdminAction(performedBy, "BULK_IMPORT",
                "users/" + request.realmName(), failed == 0,
                Map.of("created", created, "failed", failed, "total", request.users().size()));

        if (created > 0) {
            auditService.sendNotification(NotificationCommandDto.builder()
                    .channelType(NotificationCommandDto.ChannelType.EMAIL)
                    .recipient(performedBy)
                    .subject("Bulk User Import Complete")
                    .body("Imported " + created + " users, " + failed + " failed in realm " + request.realmName())
                    .priority(NotificationCommandDto.Priority.NORMAL)
                    .build());
        }

        return Map.of("created", created, "failed", failed, "errors", errors);
    }

    @CircuitBreaker(name = "keycloak")
    public List<Map<String, String>> bulkExportUsers(String realmName) {
        List<UserRepresentation> users = keycloak.realm(realmName).users().list(0, 1000);
        return users.stream()
                .map(u -> Map.of(
                        "username", u.getUsername() != null ? u.getUsername() : "",
                        "email", u.getEmail() != null ? u.getEmail() : "",
                        "firstName", u.getFirstName() != null ? u.getFirstName() : "",
                        "lastName", u.getLastName() != null ? u.getLastName() : "",
                        "enabled", String.valueOf(u.isEnabled())
                ))
                .toList();
    }

    @CircuitBreaker(name = "keycloak")
    public Map<String, Object> bulkDisableUsers(String realmName, List<String> usernames, String performedBy) {
        int disabled = 0;
        int failed = 0;

        for (String username : usernames) {
            try {
                var users = keycloak.realm(realmName).users().searchByUsername(username, true);
                if (!users.isEmpty()) {
                    UserRepresentation user = users.get(0);
                    user.setEnabled(false);
                    keycloak.realm(realmName).users().get(user.getId()).update(user);
                    disabled++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("Failed to disable user: {} in realm: {}", username, realmName, e);
            }
        }

        auditService.logAdminAction(performedBy, "BULK_DISABLE",
                "users/" + realmName, failed == 0,
                Map.of("disabled", disabled, "failed", failed));

        return Map.of("disabled", disabled, "failed", failed);
    }
}
