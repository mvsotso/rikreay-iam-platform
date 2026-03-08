package com.iam.platform.admin.service;

import com.iam.platform.admin.dto.OrgNotificationConfigRequest;
import com.iam.platform.admin.dto.OrgNotificationConfigResponse;
import com.iam.platform.admin.entity.OrgNotificationConfig;
import com.iam.platform.admin.repository.OrgNotificationConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgNotificationConfigService {

    private final OrgNotificationConfigRepository configRepository;

    @Transactional(readOnly = true)
    public OrgNotificationConfigResponse getConfig(String tenantId) {
        OrgNotificationConfig config = configRepository.findByTenantId(tenantId)
                .orElse(OrgNotificationConfig.builder()
                        .tenantId(tenantId)
                        .eventTypes(List.of())
                        .channels(List.of())
                        .recipients(List.of())
                        .enabled(false)
                        .build());
        return toResponse(config);
    }

    @Transactional
    public OrgNotificationConfigResponse updateConfig(String tenantId, OrgNotificationConfigRequest request) {
        OrgNotificationConfig config = configRepository.findByTenantId(tenantId)
                .orElse(OrgNotificationConfig.builder().tenantId(tenantId).build());

        config.setEventTypes(request.eventTypes());
        config.setChannels(request.channels());
        config.setRecipients(request.recipients());
        config.setEnabled(request.enabled());

        config = configRepository.save(config);
        log.info("Org notification config updated for tenant: {}", tenantId);
        return toResponse(config);
    }

    private OrgNotificationConfigResponse toResponse(OrgNotificationConfig config) {
        return new OrgNotificationConfigResponse(
                config.getId(), config.getTenantId(),
                config.getEventTypes(), config.getChannels(),
                config.getRecipients(), config.isEnabled()
        );
    }
}
