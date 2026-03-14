package com.iam.platform.gateway.config;

import com.iam.platform.gateway.security.KeycloakReactiveJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Reactive security configuration for Spring Cloud Gateway.
 * Uses ServerHttpSecurity (NOT HttpSecurity — this is WebFlux, not servlet).
 * Does NOT use ThreadLocal.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            // CSRF is disabled because this gateway uses stateless JWT authentication.
            // All state is in the Bearer token, not in cookies, so CSRF attacks are not applicable.
            // See: OWASP CSRF Prevention Cheat Sheet - Token Based Mitigation
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .headers(headers -> headers
                .contentTypeOptions(contentType -> {})
                .frameOptions(frame -> frame.mode(
                    org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                .hsts(hsts -> hsts
                    .includeSubdomains(true)
                    .maxAge(java.time.Duration.ofDays(365)))
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                .referrerPolicy(referrer -> referrer
                    .policy(org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            .authorizeExchange(exchanges -> exchanges
                // Actuator endpoints
                .pathMatchers("/actuator/**").permitAll()
                // Keycloak proxy
                .pathMatchers("/auth/**").permitAll()
                // X-Road endpoints — authenticated by X-Road Security Server, not JWT
                .pathMatchers("/xroad/**").permitAll()
                // Public API docs
                .pathMatchers("/api/v1/docs/**", "/api/v1/sdks/**").permitAll()
                // Swagger UI
                .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Fallback
                .pathMatchers("/fallback/**").permitAll()
                // All other requests require authentication
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakReactiveJwtConverter()))
            );

        return http.build();
    }

    @Bean
    public KeycloakReactiveJwtAuthenticationConverter keycloakReactiveJwtConverter() {
        return new KeycloakReactiveJwtAuthenticationConverter();
    }
}
