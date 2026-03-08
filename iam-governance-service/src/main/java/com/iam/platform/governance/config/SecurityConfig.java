package com.iam.platform.governance.config;

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
                // Consent endpoints — authenticated users can manage their own
                .requestMatchers(HttpMethod.POST, "/api/v1/governance/consents").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/governance/consents/me").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/governance/consents/*").authenticated()
                // Admin consent view
                .requestMatchers(HttpMethod.GET, "/api/v1/governance/consents").hasAnyRole(
                    IamRoles.ROLE_GOVERNANCE_ADMIN, IamRoles.ROLE_IAM_ADMIN)
                // Campaign reviews — tenant-admin can participate
                .requestMatchers(HttpMethod.POST, "/api/v1/governance/campaigns/*/reviews").hasAnyRole(
                    IamRoles.ROLE_TENANT_ADMIN, IamRoles.ROLE_GOVERNANCE_ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/v1/governance/campaigns/*/reviews").hasAnyRole(
                    IamRoles.ROLE_GOVERNANCE_ADMIN, IamRoles.ROLE_IAM_ADMIN, IamRoles.ROLE_TENANT_ADMIN)
                // Reports — report-viewer access
                .requestMatchers("/api/v1/governance/reports/**").hasAnyRole(
                    IamRoles.ROLE_REPORT_VIEWER, IamRoles.ROLE_GOVERNANCE_ADMIN, IamRoles.ROLE_IAM_ADMIN)
                // All other governance endpoints
                .requestMatchers("/api/v1/governance/**").hasAnyRole(
                    IamRoles.ROLE_GOVERNANCE_ADMIN, IamRoles.ROLE_IAM_ADMIN)
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
            );

        return http.build();
    }
}
