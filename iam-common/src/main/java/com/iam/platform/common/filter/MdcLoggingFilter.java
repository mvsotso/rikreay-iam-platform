package com.iam.platform.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Sets MDC context from JWT claims and X-Road headers for structured logging.
 * MDC fields: traceId, userId, tenantId, service, clientIp.
 * Servlet-only — iam-gateway uses its own reactive logging filter.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class MdcLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Trace ID from header or generate new
            String traceId = request.getHeader("X-Request-Id");
            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString().substring(0, 8);
            }
            MDC.put("traceId", traceId);

            // Client IP
            String clientIp = request.getHeader("X-Forwarded-For");
            if (clientIp == null) {
                clientIp = request.getRemoteAddr();
            }
            MDC.put("clientIp", clientIp);

            // Service name from application context
            String serviceName = request.getServletContext().getServletContextName();
            if (serviceName != null) {
                MDC.put("service", serviceName);
            }

            // Extract userId and tenantId from JWT if available
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                String userId = jwt.getClaimAsString("preferred_username");
                if (userId != null) {
                    MDC.put("userId", userId);
                }
                String issuer = jwt.getClaimAsString("iss");
                if (issuer != null && issuer.contains("/realms/")) {
                    String tenantId = issuer.substring(issuer.lastIndexOf("/realms/") + 8);
                    MDC.put("tenantId", tenantId);
                }
            }

            // Extract from X-Road headers if present
            String xroadClient = request.getHeader("X-Road-Client");
            if (xroadClient != null) {
                MDC.put("xroadClient", xroadClient);
            }

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
