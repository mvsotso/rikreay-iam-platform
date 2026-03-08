package com.iam.platform.config.workflow;

import com.iam.platform.config.dto.ConfigChangeLogResponse;
import com.iam.platform.config.dto.ConfigUpdateRequest;
import com.iam.platform.config.dto.FeatureFlagRequest;
import com.iam.platform.config.dto.FeatureFlagResponse;
import com.iam.platform.config.repository.ConfigChangeLogRepository;
import com.iam.platform.config.repository.FeatureFlagRepository;
import com.iam.platform.config.service.AuditService;
import com.iam.platform.config.service.ConfigVersionService;
import com.iam.platform.config.service.FeatureFlagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Workflow: Config Change with Feature Flags & Rollback")
class ConfigChangeWorkflowTest {

    @Autowired
    private FeatureFlagService featureFlagService;

    @Autowired
    private ConfigVersionService configVersionService;

    @Autowired
    private FeatureFlagRepository featureFlagRepository;

    @Autowired
    private ConfigChangeLogRepository changeLogRepository;

    @MockitoBean
    private AuditService auditService;

    @Test
    @DisplayName("E2E: Create feature flag → toggle → verify audit event published")
    void createAndToggleFeatureFlag() {
        // Step 1: Create feature flag
        FeatureFlagRequest createRequest = new FeatureFlagRequest(
                "camdigikey.enabled",
                "true",
                "Enable CamDigiKey authentication integration",
                true,
                "dev");

        FeatureFlagResponse created = featureFlagService.createFlag(createRequest);

        assertThat(created).isNotNull();
        assertThat(created.flagKey()).isEqualTo("camdigikey.enabled");
        assertThat(created.enabled()).isTrue();
        assertThat(created.environment()).isEqualTo("dev");

        // Verify audit event for creation (CONFIG_CHANGE type)
        verify(auditService).logConfigChange(
                anyString(), eq("CREATE_FLAG"), anyString(), eq(true), any());

        // Step 2: Toggle the flag off
        FeatureFlagResponse toggled = featureFlagService.toggleFlag("camdigikey.enabled", "dev");

        assertThat(toggled.enabled()).isFalse();

        // Verify audit event for toggle
        verify(auditService).logConfigChange(
                anyString(), eq("TOGGLE_FLAG"), anyString(), eq(true), any());
    }

    @Test
    @DisplayName("E2E: Record config change → verify history → rollback → verify new version")
    void configChangeAndRollbackWorkflow() {
        // Step 1: Record a config change
        ConfigUpdateRequest updateRequest = new ConfigUpdateRequest(
                Map.of("server.port", 8082, "spring.jpa.show-sql", true));

        ConfigChangeLogResponse change = configVersionService.recordChange(
                "iam-core-service", "dev", updateRequest, "UPDATE");

        assertThat(change).isNotNull();
        assertThat(change.application()).isEqualTo("iam-core-service");
        assertThat(change.profile()).isEqualTo("dev");
        assertThat(change.changeType()).isEqualTo("UPDATE");
        assertThat(change.changesJson()).containsKey("server.port");

        // Verify audit published
        verify(auditService).logConfigChange(
                anyString(), eq("CONFIG_UPDATE"), anyString(), eq(true), any());

        // Step 2: Verify history
        Page<ConfigChangeLogResponse> history = configVersionService.getHistory(
                "iam-core-service", "dev", PageRequest.of(0, 10));
        assertThat(history.getTotalElements()).isGreaterThanOrEqualTo(1);

        // Step 3: Get the version and verify
        ConfigChangeLogResponse version = configVersionService.getVersion(change.version());
        assertThat(version.changesJson()).isEqualTo(change.changesJson());

        // Step 4: Rollback to the recorded version
        ConfigChangeLogResponse rollback = configVersionService.rollback(change.version());

        assertThat(rollback).isNotNull();
        assertThat(rollback.changeType()).isEqualTo("ROLLBACK");
        assertThat(rollback.application()).isEqualTo("iam-core-service");
        assertThat(rollback.profile()).isEqualTo("dev");
        assertThat(rollback.changesJson()).isEqualTo(change.changesJson());
        assertThat(rollback.version()).isGreaterThan(change.version());

        // Verify audit for rollback
        verify(auditService).logConfigChange(
                anyString(), eq("CONFIG_ROLLBACK"), anyString(), eq(true), any());
    }

    @Test
    @DisplayName("E2E: Create flags for multiple environments → toggle independently")
    void multiEnvironmentFeatureFlags() {
        // Create same flag for dev and uat
        featureFlagService.createFlag(new FeatureFlagRequest(
                "xroad.proxy.enabled", "true", "X-Road proxy feature", true, "dev"));
        featureFlagService.createFlag(new FeatureFlagRequest(
                "xroad.proxy.enabled", "true", "X-Road proxy feature", true, "uat"));

        // Toggle only dev off
        FeatureFlagResponse devToggled = featureFlagService.toggleFlag("xroad.proxy.enabled", "dev");
        assertThat(devToggled.enabled()).isFalse();

        // UAT should still be enabled
        FeatureFlagResponse uatFlag = featureFlagService.getFlag("xroad.proxy.enabled", "uat");
        assertThat(uatFlag.enabled()).isTrue();
    }

    @Test
    @DisplayName("E2E: Multiple config changes → verify version ordering")
    void multipleConfigChangesVersioning() {
        ConfigChangeLogResponse v1 = configVersionService.recordChange(
                "iam-gateway", "prod",
                new ConfigUpdateRequest(Map.of("rate-limit", 100)),
                "UPDATE");

        ConfigChangeLogResponse v2 = configVersionService.recordChange(
                "iam-gateway", "prod",
                new ConfigUpdateRequest(Map.of("rate-limit", 200)),
                "UPDATE");

        ConfigChangeLogResponse v3 = configVersionService.recordChange(
                "iam-gateway", "prod",
                new ConfigUpdateRequest(Map.of("rate-limit", 300)),
                "UPDATE");

        // Versions should be sequential
        assertThat(v2.version()).isGreaterThan(v1.version());
        assertThat(v3.version()).isGreaterThan(v2.version());

        // Latest version should be v3
        ConfigChangeLogResponse latest = configVersionService.getLatestVersion("iam-gateway", "prod");
        assertThat(latest.version()).isEqualTo(v3.version());

        // Rollback to v1
        ConfigChangeLogResponse rollback = configVersionService.rollback(v1.version());
        assertThat(rollback.changesJson()).containsEntry("rate-limit", 100);
        assertThat(rollback.version()).isGreaterThan(v3.version());

        // Verify multiple audit events logged
        verify(auditService, atLeast(4)).logConfigChange(
                anyString(), anyString(), anyString(), eq(true), any());
    }
}
