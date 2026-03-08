package com.iam.platform.developer.repository;

import com.iam.platform.developer.entity.WebhookDeliveryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface WebhookDeliveryLogRepository extends JpaRepository<WebhookDeliveryLog, UUID> {

    Page<WebhookDeliveryLog> findByWebhookIdOrderBySentAtDesc(UUID webhookId, Pageable pageable);

    void deleteByWebhookId(UUID webhookId);

    void deleteBySentAtBefore(Instant cutoff);
}
