package com.iam.platform.admin.service;

import com.iam.platform.admin.dto.PlatformDashboardResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final Keycloak keycloak;

    @CircuitBreaker(name = "keycloak", fallbackMethod = "getDashboardFallback")
    public PlatformDashboardResponse getPlatformDashboard() {
        List<RealmRepresentation> realms = keycloak.realms().findAll();

        long totalOrgs = realms.size() - 1; // exclude master realm
        long totalUsers = 0;
        Map<String, Long> orgsBySector = new HashMap<>();
        Map<String, Long> usersBySector = new HashMap<>();

        for (RealmRepresentation realm : realms) {
            if ("master".equals(realm.getRealm())) continue;
            try {
                int userCount = keycloak.realm(realm.getRealm()).users().count();
                totalUsers += userCount;
            } catch (Exception e) {
                log.warn("Failed to get user count for realm: {}", realm.getRealm());
            }
        }

        return new PlatformDashboardResponse(
                totalOrgs, totalUsers, orgsBySector, usersBySector, 0, 0
        );
    }

    @SuppressWarnings("unused")
    private PlatformDashboardResponse getDashboardFallback(Throwable t) {
        log.warn("Keycloak circuit breaker open, returning cached/default dashboard", t);
        return new PlatformDashboardResponse(0, 0, Map.of(), Map.of(), 0, 0);
    }
}
