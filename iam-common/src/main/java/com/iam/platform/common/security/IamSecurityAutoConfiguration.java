package com.iam.platform.common.security;

import com.iam.platform.common.constants.XRoadHeaders;
import com.iam.platform.common.filter.XRoadRequestFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Shared security auto-configuration for all IAM Platform servlet services.
 * <p>
 * Provides:
 * - KeycloakJwtAuthenticationConverter (RBAC-only JWT → Spring Security authorities)
 * - CorsConfigurationSource (reads app.cors.allowed-origins)
 * - GlobalExceptionHandler (unified error responses)
 * - XRoadRequestFilter (servlet-only X-Road header extraction)
 * <p>
 * Each service still defines its own SecurityFilterChain with endpoint-specific access rules.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class IamSecurityAutoConfiguration {

    @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost:3000,http://localhost:5173}")
    private List<String> allowedOrigins;

    @Bean
    public KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter() {
        return new KeycloakJwtAuthenticationConverter();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                XRoadHeaders.CLIENT,
                XRoadHeaders.ID,
                XRoadHeaders.USER_ID,
                XRoadHeaders.REQUEST_HASH
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    public XRoadRequestFilter xRoadRequestFilter() {
        return new XRoadRequestFilter();
    }
}
