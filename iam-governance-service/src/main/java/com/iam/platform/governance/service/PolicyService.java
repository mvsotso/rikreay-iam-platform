package com.iam.platform.governance.service;

import com.iam.platform.governance.dto.PolicyViolationDto;
import com.iam.platform.governance.dto.SodPolicyRequest;
import com.iam.platform.governance.dto.SodPolicyResponse;
import com.iam.platform.governance.entity.SodPolicy;
import com.iam.platform.governance.repository.SodPolicyRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService {

    private final SodPolicyRepository sodPolicyRepository;
    private final Keycloak keycloakAdmin;
    private final AuditService auditService;

    @Transactional
    public SodPolicyResponse createPolicy(SodPolicyRequest request, String username) {
        SodPolicy policy = SodPolicy.builder()
                .name(request.name())
                .conflictingRolesJson(request.conflictingRoles())
                .severity(request.severity())
                .enabled(request.enabled())
                .build();

        SodPolicy saved = sodPolicyRepository.save(policy);
        log.info("SoD policy created: id={}, name={}", saved.getId(), saved.getName());

        auditService.logGovernanceAction(username, "CREATE_POLICY", "policies/" + saved.getId(),
                true, Map.of("name", saved.getName(), "severity", saved.getSeverity().name()));

        return toResponse(saved);
    }

    public SodPolicyResponse getPolicy(UUID id) {
        return sodPolicyRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + id));
    }

    public Page<SodPolicyResponse> listPolicies(Pageable pageable) {
        return sodPolicyRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public SodPolicyResponse updatePolicy(UUID id, SodPolicyRequest request, String username) {
        SodPolicy policy = sodPolicyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + id));

        policy.setName(request.name());
        policy.setConflictingRolesJson(request.conflictingRoles());
        policy.setSeverity(request.severity());
        policy.setEnabled(request.enabled());

        SodPolicy saved = sodPolicyRepository.save(policy);
        auditService.logGovernanceAction(username, "UPDATE_POLICY", "policies/" + id,
                true, Map.of("name", saved.getName()));

        return toResponse(saved);
    }

    @Transactional
    public void deletePolicy(UUID id, String username) {
        SodPolicy policy = sodPolicyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + id));
        policy.softDelete();
        sodPolicyRepository.save(policy);

        auditService.logGovernanceAction(username, "DELETE_POLICY", "policies/" + id,
                true, Map.of("name", policy.getName()));
    }

    @CircuitBreaker(name = "keycloak", fallbackMethod = "evaluatePolicyFallback")
    public List<PolicyViolationDto> evaluatePolicy(UUID policyId, String userId) {
        SodPolicy policy = sodPolicyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + policyId));

        Set<String> userRoles = getUserRoles(userId);
        List<PolicyViolationDto> violations = new ArrayList<>();

        for (List<String> conflictPair : policy.getConflictingRolesJson()) {
            if (conflictPair.size() >= 2) {
                List<String> matchedRoles = conflictPair.stream()
                        .filter(userRoles::contains)
                        .collect(Collectors.toList());

                if (matchedRoles.size() >= 2) {
                    violations.add(new PolicyViolationDto(
                            policy.getId(), policy.getName(), policy.getSeverity(),
                            userId, matchedRoles));
                }
            }
        }

        return violations;
    }

    @SuppressWarnings("unused")
    private List<PolicyViolationDto> evaluatePolicyFallback(UUID policyId, String userId, Throwable t) {
        log.error("Policy evaluation failed (circuit breaker): {}", t.getMessage());
        return List.of();
    }

    public List<PolicyViolationDto> checkConflicts(String userId, String newRole) {
        List<SodPolicy> enabledPolicies = sodPolicyRepository.findByEnabled(true);
        Set<String> userRoles = getUserRoles(userId);
        userRoles.add(newRole);

        List<PolicyViolationDto> violations = new ArrayList<>();
        for (SodPolicy policy : enabledPolicies) {
            for (List<String> conflictPair : policy.getConflictingRolesJson()) {
                List<String> matched = conflictPair.stream()
                        .filter(userRoles::contains)
                        .collect(Collectors.toList());
                if (matched.size() >= 2) {
                    violations.add(new PolicyViolationDto(
                            policy.getId(), policy.getName(), policy.getSeverity(),
                            userId, matched));
                }
            }
        }
        return violations;
    }

    private Set<String> getUserRoles(String userId) {
        try {
            List<RoleRepresentation> roles = keycloakAdmin.realm("iam-platform")
                    .users().get(userId).roles().realmLevel().listEffective();
            return roles.stream()
                    .map(RoleRepresentation::getName)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Failed to get user roles from Keycloak: userId={}", userId, e);
            return Set.of();
        }
    }

    private SodPolicyResponse toResponse(SodPolicy policy) {
        return new SodPolicyResponse(
                policy.getId(), policy.getName(), policy.getConflictingRolesJson(),
                policy.getSeverity(), policy.isEnabled(), policy.getCreatedAt());
    }
}
