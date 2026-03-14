package com.iam.platform.gateway.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Tests for FallbackController.
 * Validates that the fallback endpoint returns the correct 503 response
 * with proper error structure when downstream services are unavailable.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class FallbackControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Fallback /unavailable should return 503 SERVICE_UNAVAILABLE")
    void fallbackReturns503() {
        webTestClient.get().uri("/fallback/unavailable")
                .exchange()
                .expectStatus().isEqualTo(503);
    }

    @Test
    @DisplayName("Fallback response should be JSON")
    void fallbackReturnsJson() {
        webTestClient.get().uri("/fallback/unavailable")
                .exchange()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON);
    }

    @Test
    @DisplayName("Fallback response body should contain success=false")
    void fallbackBodyContainsSuccessFalse() {
        webTestClient.get().uri("/fallback/unavailable")
                .exchange()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    @DisplayName("Fallback response body should contain SERVICE_UNAVAILABLE error code")
    void fallbackBodyContainsErrorCode() {
        webTestClient.get().uri("/fallback/unavailable")
                .exchange()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("SERVICE_UNAVAILABLE");
    }

    @Test
    @DisplayName("Fallback response body should contain user-friendly message")
    void fallbackBodyContainsMessage() {
        webTestClient.get().uri("/fallback/unavailable")
                .exchange()
                .expectBody()
                .jsonPath("$.message").isNotEmpty();
    }

    @Test
    @DisplayName("Fallback response body should contain timestamp")
    void fallbackBodyContainsTimestamp() {
        webTestClient.get().uri("/fallback/unavailable")
                .exchange()
                .expectBody()
                .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    @DisplayName("Fallback should be accessible without authentication (permitAll)")
    void fallbackDoesNotRequireAuth() {
        // No JWT provided — should still return 503, not 401
        webTestClient.get().uri("/fallback/unavailable")
                .exchange()
                .expectStatus().isEqualTo(503);
    }
}
