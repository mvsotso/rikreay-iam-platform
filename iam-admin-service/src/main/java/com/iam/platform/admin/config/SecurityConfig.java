package com.iam.platform.admin.config;

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
                // Tier 0/1: Platform admin
                .requestMatchers("/api/v1/platform-admin/platform/**").hasRole(IamRoles.ROLE_IAM_ADMIN)
                .requestMatchers("/api/v1/platform-admin/sector-admins/**").hasRole(IamRoles.ROLE_IAM_ADMIN)
                // Bulk user operations — iam-admin only
                .requestMatchers(HttpMethod.POST, "/api/v1/platform-admin/users/bulk-import").hasRole(IamRoles.ROLE_IAM_ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/v1/platform-admin/users/bulk-export").hasRole(IamRoles.ROLE_IAM_ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/v1/platform-admin/users/bulk-disable").hasRole(IamRoles.ROLE_IAM_ADMIN)
                // User list — iam-admin or tenant-admin
                .requestMatchers(HttpMethod.GET, "/api/v1/platform-admin/users").hasAnyRole(
                    IamRoles.ROLE_IAM_ADMIN, IamRoles.ROLE_TENANT_ADMIN)
                // Settings — iam-admin or config-admin
                .requestMatchers("/api/v1/platform-admin/settings/**").hasAnyRole(
                    IamRoles.ROLE_IAM_ADMIN, IamRoles.ROLE_CONFIG_ADMIN)
                // Tier 2: Sector admin
                .requestMatchers("/api/v1/platform-admin/sector/**").hasRole(IamRoles.ROLE_SECTOR_ADMIN)
                // Tier 3: Org admin
                .requestMatchers("/api/v1/platform-admin/org/**").hasRole(IamRoles.ROLE_TENANT_ADMIN)
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
            );

        return http.build();
    }
}
