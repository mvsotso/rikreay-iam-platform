package com.iam.platform.monitoring.service;

import com.iam.platform.monitoring.dto.AuthAnalyticsDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAnalyticsService {

    private final ElasticsearchTemplate elasticsearchTemplate;

    @CircuitBreaker(name = "elasticsearch", fallbackMethod = "analyticsCountFallback")
    public AuthAnalyticsDto getAuthAnalytics(String tenantId) {
        try {
            long totalLogins = countAuditEvents("LOGIN", tenantId, null);
            long successfulLogins = countAuditEvents("LOGIN", tenantId, true);
            long failedLogins = totalLogins - successfulLogins;
            double successRate = totalLogins > 0 ? (double) successfulLogins / totalLogins * 100 : 0;

            long mfaLogins = countAuditEvents("MFA_LOGIN", tenantId, true);
            double mfaRate = successfulLogins > 0 ? (double) mfaLogins / successfulLogins * 100 : 0;

            return new AuthAnalyticsDto(
                    totalLogins, successfulLogins, failedLogins,
                    Math.round(successRate * 100.0) / 100.0,
                    Math.round(mfaRate * 100.0) / 100.0,
                    Map.of(), Map.of(), Map.of()
            );
        } catch (Exception e) {
            log.error("Failed to query auth analytics from Elasticsearch", e);
            return emptyAnalytics();
        }
    }

    @CircuitBreaker(name = "elasticsearch", fallbackMethod = "analyticsCountFallback")
    public AuthAnalyticsDto getTenantAuthAnalytics(String tenantId) {
        return getAuthAnalytics(tenantId);
    }

    private long countAuditEvents(String action, String tenantId, Boolean success) {
        try {
            // Query Elasticsearch for audit events matching criteria
            // Using the elasticsearchTemplate to count documents
            // In a full implementation, this would build proper ES queries
            log.debug("Counting audit events: action={}, tenant={}, success={}", action, tenantId, success);
            return 0L;
        } catch (Exception e) {
            log.warn("Elasticsearch query failed for action={}: {}", action, e.getMessage());
            return 0L;
        }
    }

    @SuppressWarnings("unused")
    private AuthAnalyticsDto analyticsCountFallback(String tenantId, Throwable t) {
        log.error("Auth analytics circuit breaker open: {}", t.getMessage());
        return emptyAnalytics();
    }

    private AuthAnalyticsDto emptyAnalytics() {
        return new AuthAnalyticsDto(0, 0, 0, 0.0, 0.0, Map.of(), Map.of(), Map.of());
    }
}
