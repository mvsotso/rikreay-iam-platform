package com.iam.platform.audit.integration;

import com.iam.platform.audit.config.AuditProperties;
import com.iam.platform.audit.document.AuditEventDocument;
import com.iam.platform.audit.entity.AuditEvent;
import com.iam.platform.audit.repository.AuditEventRepository;
import com.iam.platform.audit.repository.ElasticsearchAuditRepository;
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
import static org.mockito.Mockito.*;

/**
 * Integration tests for the audit dual-write mechanism.
 * Verifies that events are persisted to PostgreSQL regardless of Elasticsearch
 * availability, and that the indexedToEs flag is set correctly.
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
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration,org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration"
})
class AuditDualWriteIT {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private AuditProperties auditProperties;

    @MockBean
    private ElasticsearchAuditRepository esRepository;

    private KafkaTemplate<String, AuditEventDto> kafkaTemplate;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        reset(esRepository);

        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, AuditEventDto> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(producerFactory);
    }

    @Test
    @DisplayName("Should persist to PG with indexedToEs=false when ES is disabled")
    void shouldPersistToPgWhenEsDisabled() {
        auditProperties.setElasticsearchEnabled(false);

        AuditEventDto event = createEvent("LOGIN", AuditEventType.AUTH_EVENT);
        kafkaTemplate.send(KafkaTopics.AUDIT_EVENTS, event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditEvent> events = auditEventRepository.findAll();
            assertThat(events).hasSize(1);
            assertThat(events.get(0).isIndexedToEs()).isFalse();
        });

        verify(esRepository, never()).indexAuditEvent(any());
    }

    @Test
    @DisplayName("Should persist to PG with indexedToEs=true when ES succeeds")
    void shouldPersistToPgWithEsFlagWhenEsSucceeds() {
        auditProperties.setElasticsearchEnabled(true);
        when(esRepository.indexAuditEvent(any())).thenReturn(new AuditEventDocument());

        AuditEventDto event = createEvent("LOGIN", AuditEventType.AUTH_EVENT);
        kafkaTemplate.send(KafkaTopics.AUDIT_EVENTS, event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditEvent> events = auditEventRepository.findAll();
            assertThat(events).hasSize(1);
            assertThat(events.get(0).isIndexedToEs()).isTrue();
        });

        verify(esRepository, times(1)).indexAuditEvent(any());
    }

    @Test
    @DisplayName("Should fallback to PG only when ES throws exception")
    void shouldFallbackToPgWhenEsFails() {
        auditProperties.setElasticsearchEnabled(true);
        when(esRepository.indexAuditEvent(any()))
                .thenThrow(new RuntimeException("Elasticsearch connection refused"));

        AuditEventDto event = createEvent("LOGIN", AuditEventType.AUTH_EVENT);
        kafkaTemplate.send(KafkaTopics.AUDIT_EVENTS, event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditEvent> events = auditEventRepository.findAll();
            assertThat(events).hasSize(1);
            assertThat(events.get(0).isIndexedToEs()).isFalse();
            assertThat(events.get(0).getAction()).isEqualTo("LOGIN");
        });
    }

    @Test
    @DisplayName("Should use X-Road index when consuming from iam.xroad.events with ES enabled")
    void shouldUseXroadIndexForXroadEvents() {
        auditProperties.setElasticsearchEnabled(true);
        when(esRepository.indexXroadEvent(any())).thenReturn(new AuditEventDocument());

        AuditEventDto event = createEvent("XROAD_REQUEST", AuditEventType.XROAD_EXCHANGE);
        kafkaTemplate.send(KafkaTopics.XROAD_EVENTS, event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditEvent> events = auditEventRepository.findAll();
            assertThat(events).hasSize(1);
            assertThat(events.get(0).isIndexedToEs()).isTrue();
        });

        verify(esRepository, times(1)).indexXroadEvent(any());
        verify(esRepository, never()).indexAuditEvent(any());
    }

    @Test
    @DisplayName("Should handle X-Road ES failure and still persist to PG")
    void shouldHandleXroadEsFailure() {
        auditProperties.setElasticsearchEnabled(true);
        when(esRepository.indexXroadEvent(any()))
                .thenThrow(new RuntimeException("ES cluster unavailable"));

        AuditEventDto event = createEvent("XROAD_REQUEST", AuditEventType.XROAD_EXCHANGE);
        kafkaTemplate.send(KafkaTopics.XROAD_EVENTS, event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditEvent> events = auditEventRepository.findAll();
            assertThat(events).hasSize(1);
            assertThat(events.get(0).isIndexedToEs()).isFalse();
        });
    }

    @Test
    @DisplayName("Should preserve all event fields through dual-write pipeline")
    void shouldPreserveAllFieldsThroughPipeline() {
        auditProperties.setElasticsearchEnabled(false);
        Instant now = Instant.now();

        AuditEventDto event = AuditEventDto.builder()
                .type(AuditEventType.ADMIN_ACTION)
                .timestamp(now)
                .username("platform-admin")
                .action("USER_DISABLED")
                .resource("users/gdt-realm/user123")
                .sourceIp("192.168.10.5")
                .success(false)
                .tenantId("gdt-realm")
                .metadata(Map.of("reason", "policy-violation", "userId", "user123"))
                .build();

        kafkaTemplate.send(KafkaTopics.AUDIT_EVENTS, event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditEvent> events = auditEventRepository.findAll();
            assertThat(events).hasSize(1);
            AuditEvent persisted = events.get(0);
            assertThat(persisted.getEventType()).isEqualTo("ADMIN_ACTION");
            assertThat(persisted.getUsername()).isEqualTo("platform-admin");
            assertThat(persisted.getAction()).isEqualTo("USER_DISABLED");
            assertThat(persisted.getResource()).isEqualTo("users/gdt-realm/user123");
            assertThat(persisted.getSourceIp()).isEqualTo("192.168.10.5");
            assertThat(persisted.isSuccess()).isFalse();
            assertThat(persisted.getTenantId()).isEqualTo("gdt-realm");
            assertThat(persisted.getMetadata()).containsEntry("reason", "policy-violation");
        });
    }

    private AuditEventDto createEvent(String action, AuditEventType type) {
        return AuditEventDto.builder()
                .type(type)
                .timestamp(Instant.now())
                .username("test-user")
                .action(action)
                .resource("/test/resource")
                .sourceIp("127.0.0.1")
                .success(true)
                .tenantId("test-tenant")
                .build();
    }
}
