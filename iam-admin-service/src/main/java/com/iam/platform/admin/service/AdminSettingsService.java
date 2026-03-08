package com.iam.platform.admin.service;

import com.iam.platform.admin.dto.PlatformSettingsRequest;
import com.iam.platform.admin.dto.PlatformSettingsResponse;
import com.iam.platform.admin.entity.PlatformSettings;
import com.iam.platform.admin.repository.PlatformSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSettingsService {

    private final PlatformSettingsRepository settingsRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<PlatformSettingsResponse> getAllSettings() {
        return settingsRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PlatformSettingsResponse> getSettingsByCategory(String category) {
        return settingsRepository.findByCategory(category).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PlatformSettingsResponse getSetting(String key) {
        PlatformSettings settings = settingsRepository.findBySettingKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Setting not found: " + key));
        return toResponse(settings);
    }

    @Transactional
    public PlatformSettingsResponse saveSetting(PlatformSettingsRequest request) {
        String username = getCurrentUsername();

        PlatformSettings settings = settingsRepository.findBySettingKey(request.settingKey())
                .orElse(PlatformSettings.builder()
                        .settingKey(request.settingKey())
                        .build());

        settings.setSettingValue(request.settingValue());
        settings.setCategory(request.category());
        settings.setDescription(request.description());
        settings.setUpdatedBy(username);

        settings = settingsRepository.save(settings);
        log.info("Platform setting saved: key={}, by={}", request.settingKey(), username);

        auditService.logAdminAction(username, "UPDATE_SETTING", "settings/" + request.settingKey(),
                true, Map.of("key", request.settingKey(), "category", request.category()));

        return toResponse(settings);
    }

    private PlatformSettingsResponse toResponse(PlatformSettings s) {
        return new PlatformSettingsResponse(
                s.getId(), s.getSettingKey(), s.getSettingValue(),
                s.getCategory(), s.getDescription(), s.getUpdatedBy(), s.getUpdatedAt()
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
