package com.iam.platform.gateway.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Reactive JWT converter for Spring Cloud Gateway.
 * Extracts Keycloak realm and client roles as Spring Security authorities (RBAC only).
 * <p>
 * Does NOT use ThreadLocal — safe for reactive/WebFlux context.
 */
public class KeycloakReactiveJwtAuthenticationConverter
        implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_KEY = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        String principalName = jwt.getClaimAsString("preferred_username");
        if (principalName == null) {
            principalName = jwt.getSubject();
        }
        return Mono.just(new JwtAuthenticationToken(jwt, authorities, principalName));
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.addAll(extractRealmRoles(jwt));
        authorities.addAll(extractClientRoles(jwt));
        return authorities;
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccess == null) return Collections.emptyList();

        Object rolesObj = realmAccess.get(ROLES_KEY);
        if (!(rolesObj instanceof List<?> roles)) return Collections.emptyList();

        return roles.stream()
                .filter(String.class::isInstance)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(ROLE_PREFIX + role))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractClientRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS_CLAIM);
        if (resourceAccess == null) return Collections.emptyList();

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Map.Entry<String, Object> entry : resourceAccess.entrySet()) {
            String clientId = entry.getKey();
            if (!(entry.getValue() instanceof Map<?, ?> clientAccess)) continue;

            Object rolesObj = clientAccess.get(ROLES_KEY);
            if (!(rolesObj instanceof List<?> roles)) continue;

            for (Object role : roles) {
                if (role instanceof String roleName) {
                    authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + clientId + "_" + roleName));
                }
            }
        }
        return authorities;
    }
}
