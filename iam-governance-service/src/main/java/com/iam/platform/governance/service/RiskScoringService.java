package com.iam.platform.governance.service;

import com.iam.platform.governance.dto.RiskScoreResponse;
import com.iam.platform.governance.entity.RiskScore;
import com.iam.platform.governance.repository.RiskScoreRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskScoringService {

    private final RiskScoreRepository riskScoreRepository;
    private final Keycloak keycloakAdmin;
    private final AuditService auditService;

    @CircuitBreaker(name = "keycloak", fallbackMethod = "calculateRiskFallback")
    @Transactional
    public RiskScoreResponse calculateRiskScore(String userId) {
        Map<String, Object> factors = new HashMap<>();
        int score = 0;

        try {
            UserRepresentation user = keycloakAdmin.realm("iam-platform")
                    .users().get(userId).toRepresentation();

            // Factor: Role count (more roles = higher risk)
            List<RoleRepresentation> roles = keycloakAdmin.realm("iam-platform")
                    .users().get(userId).roles().realmLevel().listEffective();
            int roleCount = roles.size();
            factors.put("roleCount", roleCount);
            if (roleCount > 5) score += 20;
            else if (roleCount > 3) score += 10;

            // Factor: Has admin role
            boolean hasAdmin = roles.stream()
                    .anyMatch(r -> r.getName().contains("admin"));
            factors.put("hasAdminRole", hasAdmin);
            if (hasAdmin) score += 30;

            // Factor: Account enabled
            factors.put("accountEnabled", user.isEnabled());
            if (!user.isEnabled()) score += 15;

            // Factor: Email verified
            boolean emailVerified = Boolean.TRUE.equals(user.isEmailVerified());
            factors.put("emailVerified", emailVerified);
            if (!emailVerified) score += 10;

        } catch (Exception e) {
            log.error("Failed to calculate risk factors from Keycloak for user {}", userId, e);
            factors.put("error", "keycloak_unavailable");
            score = 50; // Default medium risk when cannot evaluate
        }

        // Clamp score to 0-100
        score = Math.min(100, Math.max(0, score));

        RiskScore riskScore = RiskScore.builder()
                .userId(userId)
                .score(score)
                .factorsJson(factors)
                .calculatedAt(Instant.now())
                .build();

        RiskScore saved = riskScoreRepository.save(riskScore);
        return toResponse(saved);
    }

    @SuppressWarnings("unused")
    private RiskScoreResponse calculateRiskFallback(String userId, Throwable t) {
        log.error("Risk scoring circuit breaker open for user {}: {}", userId, t.getMessage());
        return new RiskScoreResponse(null, userId, 50, Map.of("error", "unavailable"), Instant.now());
    }

    public RiskScoreResponse getLatestRiskScore(String userId) {
        return riskScoreRepository.findTopByUserIdOrderByCalculatedAtDesc(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("No risk score found for user: " + userId));
    }

    public Page<RiskScoreResponse> getHighRiskUsers(int threshold, Pageable pageable) {
        return riskScoreRepository.findByScoreGreaterThanEqualOrderByScoreDesc(threshold, pageable)
                .map(this::toResponse);
    }

    @Scheduled(cron = "0 0 2 * * *") // 2 AM daily
    @Transactional
    public void batchCalculateAll() {
        log.info("Starting nightly batch risk score calculation");
        try {
            List<UserRepresentation> users = keycloakAdmin.realm("iam-platform")
                    .users().list(0, 1000);

            int calculated = 0;
            for (UserRepresentation user : users) {
                try {
                    calculateRiskScore(user.getId());
                    calculated++;
                } catch (Exception e) {
                    log.error("Failed to calculate risk score for user {}", user.getId(), e);
                }
            }
            log.info("Batch risk scoring completed: {}/{} users", calculated, users.size());

            auditService.logGovernanceAction("system", "BATCH_RISK_SCORING", "risk-scores",
                    true, Map.of("totalUsers", users.size(), "calculated", calculated));

        } catch (Exception e) {
            log.error("Batch risk scoring failed", e);
        }
    }

    private RiskScoreResponse toResponse(RiskScore riskScore) {
        return new RiskScoreResponse(
                riskScore.getId(), riskScore.getUserId(), riskScore.getScore(),
                riskScore.getFactorsJson(), riskScore.getCalculatedAt());
    }
}
