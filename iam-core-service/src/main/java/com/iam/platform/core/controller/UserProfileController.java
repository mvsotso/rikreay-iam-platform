package com.iam.platform.core.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.common.dto.UserProfileDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Profiles", description = "JWT-derived user profile endpoints")
public class UserProfileController {

    @GetMapping("/me")
    @Operation(summary = "Get lightweight profile from JWT token")
    public ApiResponse<UserProfileDto> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        UserProfileDto profile = UserProfileDto.builder()
                .username(jwt.getClaimAsString("preferred_username"))
                .email(jwt.getClaimAsString("email"))
                .firstName(jwt.getClaimAsString("given_name"))
                .lastName(jwt.getClaimAsString("family_name"))
                .realmRoles(extractRealmRoles(jwt))
                .clientRoles(extractClientRoles(jwt))
                .build();
        return ApiResponse.ok(profile);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('internal-user', 'tenant-admin', 'iam-admin')")
    @Operation(summary = "List users (placeholder — delegates to Keycloak Admin API)")
    public ApiResponse<String> listUsers() {
        return ApiResponse.ok("Use Keycloak Admin API or iam-admin-service for user listing");
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return Collections.emptyList();
        Object roles = realmAccess.get("roles");
        if (roles instanceof List<?> list) {
            return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> extractClientRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null) return Collections.emptyMap();
        return resourceAccess.entrySet().stream()
                .filter(e -> e.getValue() instanceof Map)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            Map<String, Object> client = (Map<String, Object>) e.getValue();
                            Object roles = client.get("roles");
                            if (roles instanceof List<?> list) {
                                return list.stream().filter(String.class::isInstance)
                                        .map(String.class::cast).toList();
                            }
                            return Collections.<String>emptyList();
                        }
                ));
    }
}
