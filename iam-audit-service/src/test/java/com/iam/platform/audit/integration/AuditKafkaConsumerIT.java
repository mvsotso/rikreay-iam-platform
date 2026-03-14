package com.iam.platform.audit.integration;

import com.iam.platform.audit.config.AuditProperties;
import com.iam.platform.audit.entity.AuditEvent;
import com.iam.platform.audit.repository.AuditEventRepository;
import com.iam.platform.audit.repository.ElasticsearchAuditRepository;
import com.iam.platform.audit.service.AuditIndexingService;
import com.iam.platform.common.constants.KafkaTopics;
import com.iam.platform.common.dto.AuditEventDto;
import com.iam.platform.common.dto.AuditEventDto.AuditEventType;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

/**
 * Integration tests for the audit Kafka consumer (AuditIndexingService).
 * Uses @EmbeddedKafka to produce messages to iam.audit.events and iam.xroad.events
 * and verifies they are consumed and persisted to H2.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
@EmbeddedKafka(
        topics = {KafkaTopics.AUDIT_EVENTS, KafkaTopics.XROAD_EVENTS},
        partitions = 1,
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"}
)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration,org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration",
        "app.audit.elasticsearch-enabled=false"
})
class AuditKafkaConsumerIT {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @MockBean
    private ElasticsearchAuditRepository esRepository;

    private KafkaTemplate<String, AuditEventDto> kafkaTemplate;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();

        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, AuditEventDto> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(producerFactory);
    }

    @Test
    @DisplayName("Should consume AUTH_EVENT from iam.audit.events and persist to PostgreSQL")
    void shouldConsumeAuthEventAndPersist() {
        AuditEventDto event = AuditEventDto.builder()
                .type(AuditEventType.AUTH_EVENT)
                .timestamp(Instant.now())
                .username("test-user")
                .action("LOGIN")
                .resource("/auth/login")
                .sourceIp("192.168.1.1")
                .success(true)
                .tenantId("test-tenant")
                .build();

        kafkaTemplate.send(KafkaTopics.AUDIT_EVENTS, event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditEvent> events = auditEventRepository.findAll();
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getEventType()).isEqualTo("AUTH_EVENT");
            assertThat(events.get(0).getUsername()).isEqualTo("test-user");
            assertThat(events.get(0).getAction()).isEqualTo("LOGIN");
            assertThat(events.get(0).isSuccess()).isTrue();
            assertThat(events.get(0).getTenantId()).isEqualTo("test-tenant");
        });
    }

    @Test
    @DisplayName("Should consume API_ACCESS event and persist to PostgreSQL")
    void shouldConsumeApiAccessEventAndPersist() {
        AuditEventDto event = AuditEventDto.builder()
                .type(AuditEventType.API_ACCESS)
                .timestamp(Instant.now())
                .username("api-user")
                .action("GET")
                .resource("/api/v1/persons")
                .sourceIp("10.0.0.1")
                .success(true)
                .tenantId("gdt-realm")
                .build();

        kafkaTemplate.send(KafkaTopics.AUDIT_EVENTS, event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditEvent> events = auditEventRepository.findAll();
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getEventType()).isEqualTo("API_ACCESS");
            assertThat(events.get(0).getResource()).isEqualTo("/api/v1/persons");
        });
    }

    @Test
    @DisplayName("Should consume ADMIN_ACTION event and persist to PostgreSQL")
    void shouldConsumeAdminActionEventAndPersist() {
        AuditEventDto event = AuditEventDto.builder()
                .type(AuditEventType.ADMIN_ACTION)
                .timestamp(Instant.now())
                .username("admin")
                .action("BULK_IMPORT")
                .resource("users/test-realm")
                .success(true)
                .metadata(Map.of("created", 5, "failed", 0))
                .build();

        kafkaTemplate.send(KafkaTopics.AUDIT_EVENTS, event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditEvent> events = auditEventRepository.findAll();
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getEventType()).isEqualTo("ADMIN_ACTION");
            assertThat(events.get(0).getAction()).isEqualTo("BULK_IMPORT");
            assertThat(events.get(0).getMetadata()).containsKey("created");
        });
    }

    @Test
    @DisplayName("Should consume CONFIG_CHANGE event and persist to PostgreSQL")
    void shouldConsumeConfigChangeEvent() {
        AuditEventDto event = AuditEventDto.builder()
                .type(AuditEventType.CONFIG_CHANGE)
                .timestamp(Instant.now())
                .username("config-admin")
                .action("UPDATE_FEATURE_FLAG")
                .resource("config/feature-flags")
                .success(true)
                .build();

        kafkaTemplate.send(KafkaTopics.AUDIT_EVENTS, event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditEvent> events = auditEventRepository.findAll();
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getEventType()).isEqualTo("CONFIG_CHANGE");
        });
    }

    @Test
    @DisplayName("Should consume GOVERNANCE_ACTION event and persist to PostgreSQL")
    void shouldConsumeGovernanceActionEvent() {
        AuditEventDto event = AuditEventDto.builder()
                .type(AuditEventType.GOVERNANCE_ACTION)
                .timestamp(Instant.now())
                .username("governance-admin")
                .action("ACCESS_REVIEW_COMPLETED")
                .resource("governance/campaigns/123")
                .success(true)
                .build();

        kafkaTemplate.send(KafkaTopics.AUDIT_EVENTS, event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditEvent> events = auditEventRepository.findAll();
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getEventType()).isEqualTo("GOVERNANCE_ACTION");
        });
    }

    @Test
    @DisplayName("Should consume X-Road event from iam.xroad.events and persist to PostgreSQL")
    void shouldConsumeXroadEventAndPersist() {
        AuditEventDto event = AuditEventDto.builder()
                .type(AuditEventType.XROAD_EXCHANGE)
                .timestamp(Instant.now())
                .username("xroad-service")
                .action("XROAD_REQUEST")
                .resource("/xroad/citizens/verify")
                .sourceIp("172.16.0.1")
                .success(true)
                .tenantId("moi-realm")
                .build();

        kafkaTemplate.send(KafkaTopics.XROAD_EVENTS, event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditEvent> events = auditEventRepository.findAll();
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getEventType()).isEqualTo("XROAD_EXCHANGE");
            assertThat(events.get(0).getAction()).isEqualTo("XROAD_REQUEST");
        });
    }

    @Test
    @DisplayName("Should handle multiple events in sequence")
    void shouldHandleMultipleEventsInSequence() {
        for (int i = 0; i < 5; i++) {
            AuditEventDto event = AuditEventDto.builder()
                    .type(AuditEventType.AUTH_EVENT)
                    .timestamp(Instant.now())
                    .username("user-" + i)
                    .action("LOGIN")
                    .resource("/auth/login")
                    .success(i % 2 == 0)
                    .build();
            kafkaTemplate.send(KafkaTopics.AUDIT_EVENTS, event);
        }

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditEvent> events = auditEventRepository.findAll();
            assertThat(events).hasSize(5);
        });
    }

    @Test
    @DisplayName("Should handle event with null type gracefully and persist as UNKNOWN")
    void shouldHandleEventWithNullType() {
        AuditEventDto event = AuditEventDto.builder()
                .type(null)
                .timestamp(Instant.now())
                .username("test-user")
                .action("UNKNOWN_ACTION")
                .resource("/some/resource")
                .success(false)
                .build();

        kafkaTemplate.send(KafkaTopics.AUDIT_EVENTS, event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditEvent> events = auditEventRepository.findAll();
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getEventType()).isEqualTo("UNKNOWN");
        });
    }
}
