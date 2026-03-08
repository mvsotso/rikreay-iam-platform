package com.iam.platform.developer.config;

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
                // Public API documentation and SDKs
                .requestMatchers("/api/v1/docs/**").permitAll()
                .requestMatchers("/api/v1/sdks").permitAll()
                // App management — developer and iam-admin
                .requestMatchers("/api/v1/apps/**").hasAnyRole(
                    IamRoles.ROLE_DEVELOPER, IamRoles.ROLE_IAM_ADMIN)
                // Webhook management — developer and iam-admin
                .requestMatchers("/api/v1/webhooks/**").hasAnyRole(
                    IamRoles.ROLE_DEVELOPER, IamRoles.ROLE_IAM_ADMIN)
                // Sandbox management — developer
                .requestMatchers("/api/v1/sandbox/**").hasAnyRole(
                    IamRoles.ROLE_DEVELOPER, IamRoles.ROLE_IAM_ADMIN)
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
            );

        return http.build();
    }
}
