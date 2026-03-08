package com.iam.platform.admin.service;

import com.iam.platform.admin.dto.OrgDashboardResponse;
import com.iam.platform.admin.enums.MetricType;
import com.iam.platform.admin.repository.UsageRecordRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgDashboardService {

    private final Keycloak keycloak;
    private final UsageRecordRepository usageRecordRepository;

    @CircuitBreaker(name = "keycloak", fallbackMethod = "getOrgDashboardFallback")
    public OrgDashboardResponse getOrgDashboard(String realmName) {
        long userCount = 0;
        long activeSessionCount = 0;

        try {
            userCount = keycloak.realm(realmName).users().count();
            var sessions = keycloak.realm(realmName).getClientSessionStats();
            activeSessionCount = sessions.stream()
                    .mapToLong(s -> Long.parseLong(s.getOrDefault("active", "0")))
                    .sum();
        } catch (Exception e) {
            log.warn("Failed to get Keycloak stats for realm: {}", realmName, e);
        }

        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate today = LocalDate.now();

        Long apiCalls = usageRecordRepository.sumByTenantAndMetric(
                realmName, MetricType.API_CALLS, monthStart, today);
        Long xroadTx = usageRecordRepository.sumByTenantAndMetric(
                realmName, MetricType.XROAD_TRANSACTIONS, monthStart, today);

        return new OrgDashboardResponse(
                userCount, activeSessionCount, 0, 0,
                apiCalls != null ? apiCalls : 0,
                xroadTx != null ? xroadTx : 0,
                0, 0.0
        );
    }

    @SuppressWarnings("unused")
    private OrgDashboardResponse getOrgDashboardFallback(String realmName, Throwable t) {
        log.warn("Keycloak circuit breaker open for realm: {}", realmName);
        return new OrgDashboardResponse(0, 0, 0, 0, 0, 0, 0, 0.0);
    }
}
