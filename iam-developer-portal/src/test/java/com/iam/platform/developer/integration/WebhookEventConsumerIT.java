package com.iam.platform.developer.integration;

import com.iam.platform.common.constants.KafkaTopics;
import com.iam.platform.developer.entity.WebhookConfig;
import com.iam.platform.developer.entity.WebhookDeliveryLog;
import com.iam.platform.developer.repository.WebhookConfigRepository;
import com.iam.platform.developer.repository.WebhookDeliveryLogRepository;
import com.iam.platform.developer.service.WebhookDispatcher;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration tests for the WebhookEventConsumer.
 * Produces events to iam.platform.events and verifies that matching
 * webhook configurations are dispatched via the WebhookDispatcher.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
@EmbeddedKafka(
        topics = {KafkaTopics.PLATFORM_EVENTS, KafkaTopics.AUDIT_EVENTS},
        partitions = 1,
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"}
)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=",
        "developer.webhook.max-retries=3",
        "developer.webhook.retry-delays=1000,5000,25000"
})
class WebhookEventConsumerIT {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private WebhookConfigRepository webhookConfigRepository;

    @Autowired
    private WebhookDeliveryLogRepository deliveryLogRepository;

    @MockBean
    private WebhookDispatcher webhookDispatcher;

    @MockBean
    private Keycloak keycloak;

    private KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    @BeforeEach
    void setUp() {
        deliveryLogRepository.deleteAll();
        webhookConfigRepository.deleteAll();
        reset(webhookDispatcher);

        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, Map<String, Object>> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(producerFactory);
    }

    @Test
    @DisplayName("Should dispatch webhook when matching config exists for USER_CREATED event")
    void shouldDispatchWebhookForUserCreatedEvent() {
        WebhookConfig config = createWebhookConfig("USER_CREATED", "https://example.com/webhook");

        Map<String, Object> event = Map.of(
                "eventType", "USER_CREATED",
                "userId", "user-123",
                "tenantId", "test-realm"
        );

        kafkaTemplate.send(KafkaTopics.PLATFORM_EVENTS, event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(webhookDispatcher, times(1)).dispatch(
                    any(WebhookConfig.class),
                    eq("USER_CREATED"),
                    any()
            );
        });
    }

    @Test
    @DisplayName("Should not dispatch webhook when no matching config exists")
    void shouldNotDispatchWhenNoMatchingConfig() {
        // Register webhook for USER_CREATED, but send TENANT_CREATED
        createWebhookConfig("USER_CREATED", "https://example.com/webhook");

        Map<String, Object> event = Map.of(
                "eventType", "TENANT_CREATED",
                "tenantId", "new-realm"
        );

        kafkaTemplate.send(KafkaTopics.PLATFORM_EVENTS, event);

        // Wait a bit, then verify no dispatch happened
        await().during(3, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(webhookDispatcher, never()).dispatch(
                    any(WebhookConfig.class),
                    eq("TENANT_CREATED"),
                    any()
            );
        });
    }

    @Test
    @DisplayName("Should skip event without eventType field")
    void shouldSkipEventWithoutEventType() {
        createWebhookConfig("USER_CREATED", "https://example.com/webhook");

        Map<String, Object> event = Map.of(
                "userId", "user-123",
                "tenantId", "test-realm"
        );

        kafkaTemplate.send(KafkaTopics.PLATFORM_EVENTS, event);

        await().during(3, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(webhookDispatcher, never()).dispatch(any(), any(), any());
        });
    }

    @Test
    @DisplayName("Should dispatch to multiple webhooks for same event type")
    void shouldDispatchToMultipleWebhooks() {
        createWebhookConfig("LOGIN_SUCCESS", "https://app1.com/webhook");
        createWebhookConfig("LOGIN_SUCCESS", "https://app2.com/webhook");

        Map<String, Object> event = Map.of(
                "eventType", "LOGIN_SUCCESS",
                "userId", "user-456"
        );

        kafkaTemplate.send(KafkaTopics.PLATFORM_EVENTS, event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(webhookDispatcher, times(2)).dispatch(
                    any(WebhookConfig.class),
                    eq("LOGIN_SUCCESS"),
                    any()
            );
        });
    }

    @Test
    @DisplayName("Should not dispatch to disabled webhooks")
    void shouldNotDispatchToDisabledWebhooks() {
        WebhookConfig config = WebhookConfig.builder()
                .appId(UUID.randomUUID())
                .eventType("ROLE_CHANGED")
                .targetUrl("https://disabled.com/webhook")
                .secretHash("secret")
                .enabled(false)
                .build();
        webhookConfigRepository.save(config);

        Map<String, Object> event = Map.of(
                "eventType", "ROLE_CHANGED",
                "userId", "user-789"
        );

        kafkaTemplate.send(KafkaTopics.PLATFORM_EVENTS, event);

        await().during(3, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(webhookDispatcher, never()).dispatch(any(), eq("ROLE_CHANGED"), any());
        });
    }

    private WebhookConfig createWebhookConfig(String eventType, String targetUrl) {
        WebhookConfig config = WebhookConfig.builder()
                .appId(UUID.randomUUID())
                .eventType(eventType)
                .targetUrl(targetUrl)
                .secretHash("test-secret-hash")
                .enabled(true)
                .build();
        return webhookConfigRepository.save(config);
    }
}
