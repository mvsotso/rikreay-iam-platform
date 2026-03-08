package com.iam.platform.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class GatewaySecurityTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Health endpoint should be accessible without authentication")
    void healthEndpointPermitAll() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Fallback endpoint should be accessible without authentication")
    void fallbackEndpointPermitAll() {
        webTestClient.get().uri("/fallback/unavailable")
                .exchange()
                .expectStatus().is5xxServerError(); // 503 by design
    }

    @Test
    @DisplayName("Protected route should return 401 without authentication")
    void protectedRouteUnauthenticated() {
        webTestClient.get().uri("/api/v1/some-protected-resource")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
