package com.iam.platform.tenant.config;

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
                // POST tenants — iam-admin only
                .requestMatchers(HttpMethod.POST, "/api/v1/tenants").hasRole(IamRoles.ROLE_IAM_ADMIN)
                // PUT suspend — iam-admin only
                .requestMatchers(HttpMethod.PUT, "/api/v1/tenants/*/suspend").hasRole(IamRoles.ROLE_IAM_ADMIN)
                .requestMatchers(HttpMethod.PUT, "/api/v1/tenants/*/activate").hasRole(IamRoles.ROLE_IAM_ADMIN)
                // DELETE tenants — iam-admin only
                .requestMatchers(HttpMethod.DELETE, "/api/v1/tenants/*").hasRole(IamRoles.ROLE_IAM_ADMIN)
                // GET tenants — iam-admin or tenant-admin
                .requestMatchers(HttpMethod.GET, "/api/v1/tenants/**").hasAnyRole(
                    IamRoles.ROLE_IAM_ADMIN, IamRoles.ROLE_TENANT_ADMIN)
                // PUT update — iam-admin or tenant-admin
                .requestMatchers(HttpMethod.PUT, "/api/v1/tenants/*").hasAnyRole(
                    IamRoles.ROLE_IAM_ADMIN, IamRoles.ROLE_TENANT_ADMIN)
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
            );

        return http.build();
    }
}
