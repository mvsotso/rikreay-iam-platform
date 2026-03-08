package com.iam.platform.core.config;

import com.iam.platform.common.constants.IamRoles;
import com.iam.platform.common.security.KeycloakJwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                // X-Road endpoints — authenticated by X-Road Security Server, not JWT
                .requestMatchers("/xroad/**").permitAll()
                // Self-service endpoints — any authenticated user
                .requestMatchers("/api/v1/persons/me", "/api/v1/users/me").authenticated()
                // Identity model endpoints — RBAC
                .requestMatchers("/api/v1/persons/**").hasAnyRole(
                    IamRoles.ROLE_INTERNAL_USER, IamRoles.ROLE_TENANT_ADMIN, IamRoles.ROLE_IAM_ADMIN)
                .requestMatchers("/api/v1/entities/**").hasAnyRole(
                    IamRoles.ROLE_INTERNAL_USER, IamRoles.ROLE_TENANT_ADMIN, IamRoles.ROLE_IAM_ADMIN)
                .requestMatchers("/api/v1/representations/**").hasAnyRole(
                    IamRoles.ROLE_TENANT_ADMIN, IamRoles.ROLE_IAM_ADMIN)
                .requestMatchers("/api/v1/users/**").hasAnyRole(
                    IamRoles.ROLE_INTERNAL_USER, IamRoles.ROLE_TENANT_ADMIN, IamRoles.ROLE_IAM_ADMIN)
                // All other requests require api-access
                .anyRequest().hasRole(IamRoles.ROLE_API_ACCESS)
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
            );

        return http.build();
    }
}
