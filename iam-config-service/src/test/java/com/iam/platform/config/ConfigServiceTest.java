package com.iam.platform.config;

import com.iam.platform.config.dto.FeatureFlagRequest;
import com.iam.platform.config.dto.FeatureFlagResponse;
import com.iam.platform.config.entity.FeatureFlag;
import com.iam.platform.config.repository.FeatureFlagRepository;
import com.iam.platform.config.service.FeatureFlagService;
import com.iam.platform.config.service.AuditService;
import com.iam.platform.config.service.ConfigVersionService;
import com.iam.platform.config.dto.ConfigChangeLogResponse;
import com.iam.platform.config.entity.ConfigChangeLog;
import com.iam.platform.config.repository.ConfigChangeLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class ConfigServiceTest {

    @Autowired
    private FeatureFlagService featureFlagService;

    @MockitoBean
    private FeatureFlagRepository featureFlagRepository;

    @MockitoBean
    private ConfigChangeLogRepository changeLogRepository;

    @MockitoBean
    private AuditService auditService;

    @Test
    @DisplayName("Create feature flag should persist and return response")
    void createFeatureFlag() {
        FeatureFlagRequest request = new FeatureFlagRequest(
                "new-feature", "true", "A new feature", true, "dev");

        FeatureFlag saved = FeatureFlag.builder()
                .flagKey("new-feature")
                .flagValue("true")
                .description("A new feature")
                .enabled(true)
                .environment("dev")
                .build();

        when(featureFlagRepository.save(any())).thenReturn(saved);

        FeatureFlagResponse response = featureFlagService.createFlag(request);

        assertThat(response.flagKey()).isEqualTo("new-feature");
        assertThat(response.enabled()).isTrue();
        verify(featureFlagRepository).save(any());
    }

    @Test
    @DisplayName("Toggle feature flag should flip enabled state")
    void toggleFeatureFlag() {
        FeatureFlag flag = FeatureFlag.builder()
                .flagKey("toggle-me")
                .flagValue("true")
                .enabled(true)
                .environment("dev")
                .build();

        when(featureFlagRepository.findByFlagKeyAndEnvironment("toggle-me", "dev"))
                .thenReturn(Optional.of(flag));
        when(featureFlagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FeatureFlagResponse response = featureFlagService.toggleFlag("toggle-me", "dev");

        assertThat(response.enabled()).isFalse();
    }

    @Test
    @DisplayName("Get non-existent flag should throw exception")
    void getNonExistentFlag() {
        when(featureFlagRepository.findByFlagKeyAndEnvironment("missing", "dev"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> featureFlagService.getFlag("missing", "dev"))
                .isInstanceOf(RuntimeException.class);
    }
}
