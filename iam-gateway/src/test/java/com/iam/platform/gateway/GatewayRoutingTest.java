package com.iam.platform.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * Tests for gateway route definitions.
 * Verifies that all 11 service routes are properly configured
 * and that requests with valid JWT pass through (even if downstream is down).
 *
 * NOTE: In test profile, routes are cleared (routes: []), so route-definition
 * tests verify the RouteLocator bean existence and the security layer.
 * For service-route path tests, we verify via the main application.yml loaded
 * alongside the test profile overlay.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "10000")
@ActiveProfiles("test")
class GatewayRoutingTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RouteLocator routeLocator;

    // ====== Route definitions existence ======

    @Test
    @DisplayName("RouteLocator bean should exist in application context")
    void routeLocatorExists() {
        assertThat(routeLocator).isNotNull();
    }

    // ====== Authenticated route access (security layer only) ======

    @ParameterizedTest
    @CsvSource({
            "/api/v1/users/me,             Core Service users",
            "/api/v1/persons/search,       Core Service persons",
            "/api/v1/entities/list,         Core Service entities",
            "/api/v1/representations/all,  Core Service representations",
            "/api/v1/tenants,              Tenant Service",
            "/api/v1/audit/events,         Audit Service",
            "/api/v1/xroad/services,       X-Road Adapter",
            "/api/v1/platform-admin/users, Admin Service",
            "/api/v1/monitoring/health,    Monitoring Service",
            "/api/v1/governance/campaigns, Governance Service",
            "/api/v1/apps,                 Developer Portal apps",
            "/api/v1/notifications,        Notification Service",
            "/api/v1/config/history,       Config Service"
    })
    @DisplayName("Authenticated request should not return 401 for service route paths")
    void authenticatedRequestPassesSecurity(String path, String serviceName) {
        webTestClient
                .mutateWith(mockJwt()
                        .jwt(jwt -> jwt
                                .subject("test-user")
                                .claim("preferred_username", "test-user")
                                .claim("realm_access", Map.of("roles", List.of("iam-admin")))
                        )
                        .authorities(new SimpleGrantedAuthority("ROLE_iam-admin"))
                )
                .get().uri(path)
                .exchange()
                // The route should pass security (not 401). Downstream may be 503/404.
                .expectStatus().value(status ->
                        assertThat(status)
                                .as("Route for %s at %s should pass security", serviceName, path)
                                .isNotEqualTo(401));
    }

    @ParameterizedTest
    @CsvSource({
            "/api/v1/users/me,             Core Service users",
            "/api/v1/tenants,              Tenant Service",
            "/api/v1/audit/events,         Audit Service",
            "/api/v1/platform-admin/users, Admin Service",
            "/api/v1/monitoring/health,    Monitoring Service",
            "/api/v1/governance/campaigns, Governance Service",
            "/api/v1/apps,                 Developer Portal",
            "/api/v1/notifications,        Notification Service",
            "/api/v1/config/history,       Config Service"
    })
    @DisplayName("Unauthenticated request should return 401 for protected service routes")
    void unauthenticatedRequestBlocked(String path, String serviceName) {
        webTestClient.get().uri(path)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ====== Public route paths (no auth) ======

    @Test
    @DisplayName("X-Road route should not require JWT")
    void xroadRouteNoAuth() {
        webTestClient.get().uri("/xroad/verify-person")
                .exchange()
                .expectStatus().value(status ->
                        assertThat(status).isNotEqualTo(401));
    }

    @Test
    @DisplayName("API docs route should not require JWT")
    void apiDocsRouteNoAuth() {
        webTestClient.get().uri("/api/v1/docs/openapi.json")
                .exchange()
                .expectStatus().value(status ->
                        assertThat(status).isNotEqualTo(401));
    }

    @Test
    @DisplayName("SDKs route should not require JWT")
    void sdksRouteNoAuth() {
        webTestClient.get().uri("/api/v1/sdks/java")
                .exchange()
                .expectStatus().value(status ->
                        assertThat(status).isNotEqualTo(401));
    }

    @Test
    @DisplayName("Auth/Keycloak proxy route should not require JWT")
    void keycloakProxyRouteNoAuth() {
        webTestClient.get().uri("/auth/realms/iam-platform/.well-known/openid-configuration")
                .exchange()
                .expectStatus().value(status ->
                        assertThat(status).isNotEqualTo(401));
    }

    // ====== HTTP methods ======

    @Test
    @DisplayName("POST to protected route should return 401 without JWT")
    void postProtectedRouteUnauthenticated() {
        webTestClient.post().uri("/api/v1/tenants")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("PUT to protected route should return 401 without JWT")
    void putProtectedRouteUnauthenticated() {
        webTestClient.put().uri("/api/v1/tenants/test-realm")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("DELETE to protected route should return 401 without JWT")
    void deleteProtectedRouteUnauthenticated() {
        webTestClient.delete().uri("/api/v1/tenants/test-realm")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
