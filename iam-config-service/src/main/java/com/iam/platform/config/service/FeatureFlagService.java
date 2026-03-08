package com.iam.platform.config.service;

import com.iam.platform.config.dto.FeatureFlagRequest;
import com.iam.platform.config.dto.FeatureFlagResponse;
import com.iam.platform.config.entity.FeatureFlag;
import com.iam.platform.config.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;
    private final AuditService auditService;

    @Transactional
    public FeatureFlagResponse createFlag(FeatureFlagRequest request) {
        if (featureFlagRepository.existsByFlagKeyAndEnvironment(request.flagKey(), request.environment())) {
            throw new IllegalArgumentException(
                    "Feature flag '" + request.flagKey() + "' already exists for environment '" + request.environment() + "'");
        }

        FeatureFlag flag = FeatureFlag.builder()
                .flagKey(request.flagKey())
                .flagValue(request.flagValue())
                .description(request.description())
                .enabled(request.enabled())
                .environment(request.environment())
                .build();

        flag = featureFlagRepository.save(flag);
        log.info("Feature flag created: key={}, environment={}", request.flagKey(), request.environment());

        auditService.logConfigChange(getCurrentUsername(), "CREATE_FLAG", "feature-flag/" + request.flagKey(),
                true, Map.of("flagKey", request.flagKey(), "environment", request.environment(), "enabled", request.enabled()));

        return toResponse(flag);
    }

    @Transactional(readOnly = true)
    public Page<FeatureFlagResponse> listFlags(String environment, Pageable pageable) {
        Page<FeatureFlag> flags;
        if (environment != null && !environment.isBlank()) {
            flags = featureFlagRepository.findByEnvironment(environment, pageable);
        } else {
            flags = featureFlagRepository.findAll(pageable);
        }
        return flags.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public FeatureFlagResponse getFlag(String flagKey, String environment) {
        FeatureFlag flag = featureFlagRepository.findByFlagKeyAndEnvironment(flagKey, environment)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Feature flag '" + flagKey + "' not found for environment '" + environment + "'"));
        return toResponse(flag);
    }

    @Transactional(readOnly = true)
    public List<FeatureFlagResponse> getEnabledFlags(String environment) {
        return featureFlagRepository.findByEnabledAndEnvironment(true, environment)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FeatureFlagResponse updateFlag(String flagKey, FeatureFlagRequest request) {
        FeatureFlag flag = featureFlagRepository.findByFlagKeyAndEnvironment(flagKey, request.environment())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Feature flag '" + flagKey + "' not found for environment '" + request.environment() + "'"));

        if (request.flagValue() != null) {
            flag.setFlagValue(request.flagValue());
        }
        if (request.description() != null) {
            flag.setDescription(request.description());
        }
        flag.setEnabled(request.enabled());

        flag = featureFlagRepository.save(flag);
        log.info("Feature flag updated: key={}, environment={}", flagKey, request.environment());

        auditService.logConfigChange(getCurrentUsername(), "UPDATE_FLAG", "feature-flag/" + flagKey,
                true, Map.of("flagKey", flagKey, "environment", request.environment(), "enabled", request.enabled()));

        return toResponse(flag);
    }

    @Transactional
    public FeatureFlagResponse toggleFlag(String flagKey, String environment) {
        FeatureFlag flag = featureFlagRepository.findByFlagKeyAndEnvironment(flagKey, environment)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Feature flag '" + flagKey + "' not found for environment '" + environment + "'"));

        boolean newState = !flag.isEnabled();
        flag.setEnabled(newState);
        flag = featureFlagRepository.save(flag);
        log.info("Feature flag toggled: key={}, environment={}, enabled={}", flagKey, environment, newState);

        auditService.logConfigChange(getCurrentUsername(), "TOGGLE_FLAG", "feature-flag/" + flagKey,
                true, Map.of("flagKey", flagKey, "environment", environment, "enabled", newState));

        return toResponse(flag);
    }

    @Transactional
    public void deleteFlag(String flagKey, String environment) {
        FeatureFlag flag = featureFlagRepository.findByFlagKeyAndEnvironment(flagKey, environment)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Feature flag '" + flagKey + "' not found for environment '" + environment + "'"));

        flag.softDelete();
        featureFlagRepository.save(flag);
        log.info("Feature flag deleted: key={}, environment={}", flagKey, environment);

        auditService.logConfigChange(getCurrentUsername(), "DELETE_FLAG", "feature-flag/" + flagKey,
                true, Map.of("flagKey", flagKey, "environment", environment));
    }

    private FeatureFlagResponse toResponse(FeatureFlag flag) {
        return new FeatureFlagResponse(
                flag.getId(),
                flag.getFlagKey(),
                flag.getFlagValue(),
                flag.getDescription(),
                flag.isEnabled(),
                flag.getEnvironment(),
                flag.getCreatedAt(),
                flag.getUpdatedAt()
        );
    }

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }
}
