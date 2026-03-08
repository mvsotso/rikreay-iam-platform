package com.iam.platform.common.security;

import com.iam.platform.common.test.JwtTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtAuthenticationConverterTest {

    private KeycloakJwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new KeycloakJwtAuthenticationConverter();
    }

    @Test
    @DisplayName("Should extract realm roles as ROLE_ prefixed authorities")
    void shouldExtractRealmRoles() {
        Jwt jwt = JwtTestUtils.createJwt("user-1", "admin.user", "iam-admin", "auditor");

        AbstractAuthenticationToken token = converter.convert(jwt);

        Set<String> authorities = token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(authorities).contains("ROLE_iam-admin", "ROLE_auditor");
    }

    @Test
    @DisplayName("Should extract client roles as ROLE_clientId_roleName authorities")
    void shouldExtractClientRoles() {
        Jwt jwt = JwtTestUtils.createJwtWithClientRoles(
                "user-2", "service.user",
                List.of("api-access"),
                "iam-core-service", List.of("read", "write"));

        AbstractAuthenticationToken token = converter.convert(jwt);

        Set<String> authorities = token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(authorities).contains(
                "ROLE_api-access",
                "ROLE_iam-core-service_read",
                "ROLE_iam-core-service_write");
    }

    @Test
    @DisplayName("Should return empty authorities for JWT with no realm_access claim")
    void shouldHandleEmptyClaims() {
        Jwt jwt = JwtTestUtils.createJwtNoRoles("user-3");

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("Should use preferred_username as principal name")
    void shouldUsePreferredUsernameAsPrincipal() {
        Jwt jwt = JwtTestUtils.createJwt("sub-123", "preferred.user", "api-access");

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getName()).isEqualTo("preferred.user");
    }

    @Test
    @DisplayName("Should fall back to subject when preferred_username is null")
    void shouldFallBackToSubject() {
        Jwt jwt = JwtTestUtils.createJwtNoRoles("sub-456");

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getName()).isEqualTo("sub-456");
    }

    @Test
    @DisplayName("Should handle multiple realm roles")
    void shouldHandleMultipleRealmRoles() {
        Jwt jwt = JwtTestUtils.createJwt("user-5", "multi.role",
                "iam-admin", "tenant-admin", "auditor", "ops-admin");

        AbstractAuthenticationToken token = converter.convert(jwt);

        Collection<GrantedAuthority> authorities = token.getAuthorities();
        assertThat(authorities).hasSize(4);
    }
}
