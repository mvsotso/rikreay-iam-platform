package com.iam.platform.audit.config;

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
                // Audit events — auditor, iam-admin, report-viewer
                .requestMatchers(HttpMethod.GET, "/api/v1/audit/events/**").hasAnyRole(
                    IamRoles.ROLE_AUDITOR, IamRoles.ROLE_IAM_ADMIN, IamRoles.ROLE_REPORT_VIEWER)
                // X-Road audit — auditor, iam-admin, service-manager
                .requestMatchers(HttpMethod.GET, "/api/v1/audit/xroad/**").hasAnyRole(
                    IamRoles.ROLE_AUDITOR, IamRoles.ROLE_IAM_ADMIN, IamRoles.ROLE_SERVICE_MANAGER)
                // Stats and export — auditor, iam-admin
                .requestMatchers(HttpMethod.GET, "/api/v1/audit/stats/**").hasAnyRole(
                    IamRoles.ROLE_AUDITOR, IamRoles.ROLE_IAM_ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/v1/audit/login-history/**").hasAnyRole(
                    IamRoles.ROLE_AUDITOR, IamRoles.ROLE_IAM_ADMIN)
                // All other audit endpoints
                .requestMatchers("/api/v1/audit/**").hasAnyRole(
                    IamRoles.ROLE_AUDITOR, IamRoles.ROLE_IAM_ADMIN, IamRoles.ROLE_REPORT_VIEWER)
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
            );

        return http.build();
    }
}
