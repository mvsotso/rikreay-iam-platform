package com.iam.platform.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Redis-backed rate limiter with role-based key resolution.
 * Rate limits per role:
 *   iam-admin: 1000 req/min (~17 req/sec)
 *   internal-user: 300 req/min (~5 req/sec)
 *   developer: 200 req/min (~3 req/sec)
 *   external-user: 60 req/min (1 req/sec)
 *   api-access: 30 req/min (~0.5 req/sec)
 *   anonymous: 10 req/min
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Resolves rate limit key by combining user identity with their highest role.
     * Format: "role:username" — allows per-role rate limiting.
     */
    @Bean
    public KeyResolver roleBasedKeyResolver() {
        return exchange -> ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(jwt -> {
                    String username = jwt.getName();
                    String role = resolveHighestRole(jwt);
                    return role + ":" + username;
                })
                .defaultIfEmpty("anonymous:" + resolveClientIp(exchange));
    }

    /**
     * Default rate limiter — used for routes without specific role-based limiting.
     * 10 requests/second replenish, burst of 20.
     */
    @Bean
    public RedisRateLimiter defaultRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }

    @SuppressWarnings("unchecked")
    private String resolveHighestRole(JwtAuthenticationToken jwt) {
        Map<String, Object> realmAccess = jwt.getToken().getClaimAsMap("realm_access");
        if (realmAccess == null) return "api-access";

        List<String> roles = (List<String>) realmAccess.get("roles");
        if (roles == null || roles.isEmpty()) return "api-access";

        // Priority order: highest privilege first
        if (roles.contains("iam-admin")) return "iam-admin";
        if (roles.contains("ops-admin")) return "ops-admin";
        if (roles.contains("config-admin")) return "config-admin";
        if (roles.contains("governance-admin")) return "governance-admin";
        if (roles.contains("sector-admin")) return "sector-admin";
        if (roles.contains("tenant-admin")) return "tenant-admin";
        if (roles.contains("service-manager")) return "service-manager";
        if (roles.contains("internal-user")) return "internal-user";
        if (roles.contains("developer")) return "developer";
        if (roles.contains("auditor")) return "auditor";
        if (roles.contains("external-user")) return "external-user";
        if (roles.contains("api-access")) return "api-access";

        return "api-access";
    }

    private String resolveClientIp(org.springframework.web.server.ServerWebExchange exchange) {
        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }
}
