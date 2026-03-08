package com.iam.platform.monitoring.config;

import com.iam.platform.common.constants.IamRoles;
import com.iam.platform.common.security.KeycloakJwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final KeycloakJwtAuthenticationConverter jwtConverter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Tenant-scoped auth analytics — tenant-admin can access their own
                .requestMatchers("/api/v1/monitoring/auth-analytics/tenant/**").hasAnyRole(
                    IamRoles.ROLE_OPS_ADMIN, IamRoles.ROLE_IAM_ADMIN, IamRoles.ROLE_TENANT_ADMIN)
                // X-Road metrics — also accessible by service-manager
                .requestMatchers("/api/v1/monitoring/xroad-metrics").hasAnyRole(
                    IamRoles.ROLE_OPS_ADMIN, IamRoles.ROLE_IAM_ADMIN, IamRoles.ROLE_SERVICE_MANAGER)
                // All other monitoring endpoints — ops-admin or iam-admin
                .requestMatchers("/api/v1/monitoring/**").hasAnyRole(
                    IamRoles.ROLE_OPS_ADMIN, IamRoles.ROLE_IAM_ADMIN)
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
            );

        return http.build();
    }
}
