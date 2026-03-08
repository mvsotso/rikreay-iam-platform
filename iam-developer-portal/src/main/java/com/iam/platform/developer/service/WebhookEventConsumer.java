package com.iam.platform.developer.service;

import com.iam.platform.common.constants.KafkaTopics;
import com.iam.platform.developer.entity.WebhookConfig;
import com.iam.platform.developer.repository.WebhookConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookEventConsumer {

    private final WebhookConfigRepository webhookRepository;
    private final WebhookDispatcher webhookDispatcher;

    @KafkaListener(topics = KafkaTopics.PLATFORM_EVENTS, groupId = "developer-portal-webhook-group")
    @SuppressWarnings("unchecked")
    public void consumePlatformEvent(Map<String, Object> event) {
        try {
            String eventType = (String) event.get("eventType");
            if (eventType == null) {
                log.debug("Received platform event without eventType, skipping");
                return;
            }

            log.debug("Processing platform event: type={}", eventType);

            List<WebhookConfig> matchingWebhooks = webhookRepository
                    .findByEventTypeAndEnabledTrue(eventType);

            if (matchingWebhooks.isEmpty()) {
                log.debug("No webhooks registered for event type: {}", eventType);
                return;
            }

            for (WebhookConfig webhook : matchingWebhooks) {
                webhookDispatcher.dispatch(webhook, eventType, event);
            }

            log.info("Dispatched event {} to {} webhooks", eventType, matchingWebhooks.size());

        } catch (Exception e) {
            log.error("Error processing platform event for webhook dispatch", e);
        }
    }
}
