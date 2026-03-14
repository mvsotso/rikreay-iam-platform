package com.iam.platform.common.test;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test utility for building mock JWTs with Keycloak role structure.
 * Used across all service integration tests.
 */
public final class JwtTestUtils {

    private JwtTestUtils() {}

    /**
     * Creates a mock JWT with Keycloak-structured realm roles.
     */
    public static Jwt createJwt(String subject, String preferredUsername, String... realmRoles) {
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", Arrays.asList(realmRoles));

        return Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .header("typ", "JWT")
                .subject(subject)
                .claim("preferred_username", preferredUsername)
                .claim("realm_access", realmAccess)
                .claim("resource_access", Map.of())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .issuer("http://localhost:8080/realms/iam-platform")
                .build();
    }

    /**
     * Creates a mock JWT with both realm roles and client roles.
     */
    public static Jwt createJwtWithClientRoles(String subject, String preferredUsername,
                                                 List<String> realmRoles,
                                                 String clientId, List<String> clientRoles) {
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", realmRoles);

        Map<String, Object> clientAccess = new HashMap<>();
        clientAccess.put("roles", clientRoles);

        Map<String, Object> resourceAccess = new HashMap<>();
        resourceAccess.put(clientId, clientAccess);

        return Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .header("typ", "JWT")
                .subject(subject)
                .claim("preferred_username", preferredUsername)
                .claim("realm_access", realmAccess)
                .claim("resource_access", resourceAccess)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .issuer("http://localhost:8080/realms/iam-platform")
                .build();
    }

    /**
     * Creates a mock JWT with no roles (empty claims).
     */
    public static Jwt createJwtNoRoles(String subject) {
        return Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .header("typ", "JWT")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .issuer("http://localhost:8080/realms/iam-platform")
                .build();
    }

    /**
     * Creates a RequestPostProcessor for MockMvc with realm roles.
     * Explicitly sets authorities matching KeycloakJwtAuthenticationConverter output
     * (ROLE_ prefix + role name) so that hasAnyRole() checks work correctly.
     */
    public static RequestPostProcessor jwtWithRoles(String username, String... roles) {
        Jwt jwt = createJwt("user-id-" + username, username, roles);
        var authorities = Arrays.stream(roles)
                .map(role -> (org.springframework.security.core.GrantedAuthority)
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt).authorities(authorities);
    }

    /**
     * Creates a RequestPostProcessor for MockMvc with no authentication.
     */
    public static RequestPostProcessor noAuth() {
        return SecurityMockMvcRequestPostProcessors.anonymous();
    }

    /**
     * Mutates a WebTestClient with a mock JWT bearing realm roles.
     * For use with reactive services (iam-gateway).
     */
    public static WebTestClient mutateWithJwt(WebTestClient client, String username, String... roles) {
        Jwt jwt = createJwt("user-id-" + username, username, roles);
        return client.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(jwt));
    }

    /**
     * Creates an expired JWT for testing token expiry handling.
     */
    public static Jwt createExpiredJwt(String subject, String preferredUsername, String... realmRoles) {
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", Arrays.asList(realmRoles));

        return Jwt.withTokenValue("expired-token")
                .header("alg", "RS256")
                .header("typ", "JWT")
                .subject(subject)
                .claim("preferred_username", preferredUsername)
                .claim("realm_access", realmAccess)
                .claim("resource_access", Map.of())
                .issuedAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(3600))
                .issuer("http://localhost:8080/realms/iam-platform")
                .build();
    }

    /**
     * Creates a JWT with a tenant (realm) claim for multi-tenant testing.
     */
    public static Jwt createJwtWithTenantClaim(String subject, String preferredUsername,
                                                  String tenantRealm, String... realmRoles) {
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", Arrays.asList(realmRoles));

        return Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .header("typ", "JWT")
                .subject(subject)
                .claim("preferred_username", preferredUsername)
                .claim("realm_access", realmAccess)
                .claim("resource_access", Map.of())
                .claim("azp", tenantRealm)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .issuer("http://localhost:8080/realms/" + tenantRealm)
                .build();
    }
}
