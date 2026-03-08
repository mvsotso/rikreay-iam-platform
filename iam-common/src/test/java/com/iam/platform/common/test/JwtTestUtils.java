package com.iam.platform.common.test;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
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
     */
    public static RequestPostProcessor jwtWithRoles(String username, String... roles) {
        Jwt jwt = createJwt("user-id-" + username, username, roles);
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt);
    }

    /**
     * Creates a RequestPostProcessor for MockMvc with no authentication.
     */
    public static RequestPostProcessor noAuth() {
        return SecurityMockMvcRequestPostProcessors.anonymous();
    }
}
