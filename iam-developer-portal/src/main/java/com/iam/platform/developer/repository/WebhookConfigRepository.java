package com.iam.platform.developer.repository;

import com.iam.platform.developer.entity.WebhookConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, UUID> {

    List<WebhookConfig> findByAppId(UUID appId);

    List<WebhookConfig> findByEventTypeAndEnabledTrue(String eventType);

    List<WebhookConfig> findByAppIdAndEnabledTrue(UUID appId);
}
