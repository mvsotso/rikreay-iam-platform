package com.iam.platform.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * Security tests for the reactive API Gateway.
 * Verifies that public endpoints are accessible without JWT and
 * that protected routes return 401 without authentication.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class GatewaySecurityTests {

    @Autowired
    private WebTestClient webTestClient;

    // ====== Public Endpoints ======

    @Nested
    @DisplayName("Public endpoints (no JWT required)")
    class PublicEndpoints {

        @Test
        @DisplayName("Health endpoint should be accessible without authentication")
        void healthEndpointPermitAll() {
            webTestClient.get().uri("/actuator/health")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("Fallback endpoint should return 503 without authentication")
        void fallbackEndpointPermitAll() {
            webTestClient.get().uri("/fallback/unavailable")
                    .exchange()
                    .expectStatus().is5xxServerError();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "/actuator/health",
                "/actuator/info"
        })
        @DisplayName("Actuator endpoints should be accessible without JWT")
        void actuatorEndpointsPermitAll(String path) {
            webTestClient.get().uri(path)
                    .exchange()
                    .expectStatus().is2xxSuccessful();
        }

        @Test
        @DisplayName("Swagger UI should be accessible without JWT")
        void swaggerUiPermitAll() {
            // Swagger may redirect, but should not return 401
            webTestClient.get().uri("/swagger-ui.html")
                    .exchange()
                    .expectStatus().value(status ->
                            org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
        }
    }

    // ====== Protected Endpoints ======

    @Nested
    @DisplayName("Protected endpoints (JWT required)")
    class ProtectedEndpoints {

        @Test
        @DisplayName("Protected API route should return 401 without authentication")
        void protectedRouteUnauthenticated() {
            webTestClient.get().uri("/api/v1/some-protected-resource")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "/api/v1/users/me",
                "/api/v1/tenants",
                "/api/v1/audit/events",
                "/api/v1/xroad/services",
                "/api/v1/platform-admin/platform/settings",
                "/api/v1/monitoring/health",
                "/api/v1/governance/campaigns",
                "/api/v1/apps",
                "/api/v1/notifications",
                "/api/v1/config/history"
        })
        @DisplayName("All service API paths should return 401 without JWT")
        void allServicePathsRequireAuth(String path) {
            webTestClient.get().uri(path)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    // ====== Authenticated requests ======

    @Nested
    @DisplayName("Authenticated requests with mock JWT")
    class AuthenticatedRequests {

        @Test
        @DisplayName("Authenticated request to protected route should not return 401")
        void authenticatedRequestNotUnauthorized() {
            webTestClient
                    .mutateWith(mockJwt()
                            .jwt(jwt -> jwt
                                    .subject("test-user")
                                    .claim("preferred_username", "test-user")
                                    .claim("realm_access", Map.of("roles", List.of("iam-admin")))
                            )
                            .authorities(new SimpleGrantedAuthority("ROLE_iam-admin"))
                    )
                    .get().uri("/api/v1/users/me")
                    .exchange()
                    // Routes to downstream service which is not running, so we get 503 (fallback)
                    // or 404/502, but NOT 401
                    .expectStatus().value(status ->
                            org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
        }

        @Test
        @DisplayName("Authenticated request with iam-admin role to any API endpoint should not return 401")
        void iamAdminShouldPassGatewayAuth() {
            webTestClient
                    .mutateWith(mockJwt()
                            .jwt(jwt -> jwt
                                    .subject("admin-user")
                                    .claim("preferred_username", "admin-user")
                                    .claim("realm_access", Map.of("roles", List.of("iam-admin")))
                            )
                            .authorities(new SimpleGrantedAuthority("ROLE_iam-admin"))
                    )
                    .get().uri("/api/v1/tenants")
                    .exchange()
                    .expectStatus().value(status ->
                            org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
        }

        @Test
        @DisplayName("Authenticated request with external-user role should not return 401")
        void externalUserShouldPassGatewayAuth() {
            webTestClient
                    .mutateWith(mockJwt()
                            .jwt(jwt -> jwt
                                    .subject("citizen-user")
                                    .claim("preferred_username", "citizen-user")
                                    .claim("realm_access", Map.of("roles", List.of("external-user")))
                            )
                            .authorities(new SimpleGrantedAuthority("ROLE_external-user"))
                    )
                    .get().uri("/api/v1/persons/search")
                    .exchange()
                    .expectStatus().value(status ->
                            org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
        }
    }

    // ====== X-Road / Auth Proxy paths ======

    @Nested
    @DisplayName("X-Road and Auth proxy paths (permitAll)")
    class PermitAllPaths {

        @Test
        @DisplayName("X-Road path should be accessible without JWT")
        void xroadPathPermitAll() {
            webTestClient.get().uri("/xroad/services")
                    .exchange()
                    .expectStatus().value(status ->
                            org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
        }

        @Test
        @DisplayName("Auth/Keycloak proxy path should be accessible without JWT")
        void authProxyPermitAll() {
            webTestClient.get().uri("/auth/realms/iam-platform")
                    .exchange()
                    .expectStatus().value(status ->
                            org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
        }

        @Test
        @DisplayName("Public API docs should be accessible without JWT")
        void apiDocsPermitAll() {
            webTestClient.get().uri("/api/v1/docs/openapi")
                    .exchange()
                    .expectStatus().value(status ->
                            org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
        }

        @Test
        @DisplayName("Public SDKs endpoint should be accessible without JWT")
        void sdksPermitAll() {
            webTestClient.get().uri("/api/v1/sdks/java")
                    .exchange()
                    .expectStatus().value(status ->
                            org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
        }
    }
}
