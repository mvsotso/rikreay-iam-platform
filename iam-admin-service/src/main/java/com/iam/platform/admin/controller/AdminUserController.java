package com.iam.platform.admin.controller;

import com.iam.platform.common.dto.ApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/platform-admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Management", description = "Unified user list from Keycloak")
public class AdminUserController {

    private final Keycloak keycloak;

    @GetMapping
    @Operation(summary = "List users from Keycloak (cross-realm for iam-admin, single realm for tenant-admin)")
    @CircuitBreaker(name = "keycloak")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> listUsers(
            @RequestParam(defaultValue = "iam-platform") String realmName,
            @RequestParam(defaultValue = "0") int first,
            @RequestParam(defaultValue = "50") int max,
            @RequestParam(required = false) String search) {

        List<UserRepresentation> users;
        if (search != null && !search.isBlank()) {
            users = keycloak.realm(realmName).users().search(search, first, max);
        } else {
            users = keycloak.realm(realmName).users().list(first, max);
        }

        List<Map<String, String>> result = users.stream()
                .map(u -> Map.of(
                        "id", u.getId() != null ? u.getId() : "",
                        "username", u.getUsername() != null ? u.getUsername() : "",
                        "email", u.getEmail() != null ? u.getEmail() : "",
                        "firstName", u.getFirstName() != null ? u.getFirstName() : "",
                        "lastName", u.getLastName() != null ? u.getLastName() : "",
                        "enabled", String.valueOf(u.isEnabled())
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
