package com.iam.platform.config.service;

import com.iam.platform.config.dto.ConfigChangeLogResponse;
import com.iam.platform.config.dto.ConfigUpdateRequest;
import com.iam.platform.config.entity.ConfigChangeLog;
import com.iam.platform.config.repository.ConfigChangeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigVersionService {

    private final ConfigChangeLogRepository changeLogRepository;
    private final AuditService auditService;

    @Transactional
    public ConfigChangeLogResponse recordChange(String application, String profile,
                                                  ConfigUpdateRequest request, String changeType) {
        Long version = changeLogRepository.getNextVersion();
        String author = getCurrentUsername();

        ConfigChangeLog changeLog = ConfigChangeLog.builder()
                .version(version)
                .application(application)
                .profile(profile)
                .changesJson(request.properties())
                .author(author)
                .changeType(changeType)
                .build();

        changeLog = changeLogRepository.save(changeLog);
        log.info("Config change recorded: app={}, profile={}, version={}, author={}",
                application, profile, version, author);

        auditService.logConfigChange(author, "CONFIG_" + changeType,
                "config/" + application + "/" + profile,
                true, Map.of("version", version, "application", application, "profile", profile));

        return toResponse(changeLog);
    }

    @Transactional(readOnly = true)
    public Page<ConfigChangeLogResponse> getHistory(String application, String profile, Pageable pageable) {
        Page<ConfigChangeLog> logs;
        if (application != null && profile != null) {
            logs = changeLogRepository.findByApplicationAndProfileOrderByVersionDesc(
                    application, profile, pageable);
        } else {
            logs = changeLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return logs.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ConfigChangeLogResponse getVersion(Long version) {
        ConfigChangeLog changeLog = changeLogRepository.findByVersion(version)
                .orElseThrow(() -> new IllegalArgumentException("Config version " + version + " not found"));
        return toResponse(changeLog);
    }

    @Transactional
    public ConfigChangeLogResponse rollback(Long version) {
        ConfigChangeLog originalChange = changeLogRepository.findByVersion(version)
                .orElseThrow(() -> new IllegalArgumentException("Config version " + version + " not found"));

        Long newVersion = changeLogRepository.getNextVersion();
        String author = getCurrentUsername();

        ConfigChangeLog rollbackLog = ConfigChangeLog.builder()
                .version(newVersion)
                .application(originalChange.getApplication())
                .profile(originalChange.getProfile())
                .changesJson(originalChange.getChangesJson())
                .author(author)
                .changeType("ROLLBACK")
                .build();

        rollbackLog = changeLogRepository.save(rollbackLog);
        log.info("Config rolled back: app={}, profile={}, from version={} to new version={}",
                originalChange.getApplication(), originalChange.getProfile(), version, newVersion);

        auditService.logConfigChange(author, "CONFIG_ROLLBACK",
                "config/" + originalChange.getApplication() + "/" + originalChange.getProfile(),
                true, Map.of("rolledBackToVersion", version, "newVersion", newVersion));

        return toResponse(rollbackLog);
    }

    @Transactional(readOnly = true)
    public ConfigChangeLogResponse getLatestVersion(String application, String profile) {
        return changeLogRepository.findTopByApplicationAndProfileOrderByVersionDesc(application, profile)
                .map(this::toResponse)
                .orElse(null);
    }

    private ConfigChangeLogResponse toResponse(ConfigChangeLog log) {
        return new ConfigChangeLogResponse(
                log.getId(),
                log.getVersion(),
                log.getApplication(),
                log.getProfile(),
                log.getChangesJson(),
                log.getAuthor(),
                log.getChangeType(),
                log.getCreatedAt()
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
