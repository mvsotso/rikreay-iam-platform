package com.iam.platform.common.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Custom health indicator for Keycloak connectivity.
 * Checks the Keycloak realm endpoint to verify the IDP is reachable.
 * Only activates when keycloak URL is configured.
 */
@Component("keycloak")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.issuer-uri")
public class KeycloakHealthIndicator implements HealthIndicator {

    private final String issuerUri;
    private final RestClient restClient;

    public KeycloakHealthIndicator(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri) {
        this.issuerUri = issuerUri;
        this.restClient = RestClient.create();
    }

    @Override
    public Health health() {
        if (issuerUri == null || issuerUri.isBlank()) {
            return Health.unknown().withDetail("reason", "No issuer-uri configured").build();
        }

        try {
            restClient.get()
                    .uri(issuerUri)
                    .retrieve()
                    .body(String.class);
            return Health.up()
                    .withDetail("issuerUri", issuerUri)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("issuerUri", issuerUri)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
