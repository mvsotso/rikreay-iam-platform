package com.iam.platform.notification.integration;

import com.iam.platform.common.constants.KafkaTopics;
import com.iam.platform.common.dto.NotificationCommandDto;
import com.iam.platform.notification.entity.NotificationLog;
import com.iam.platform.notification.enums.ChannelType;
import com.iam.platform.notification.enums.NotificationStatus;
import com.iam.platform.notification.repository.NotificationLogRepository;
import com.iam.platform.notification.service.EmailService;
import com.iam.platform.notification.service.SmsService;
import com.iam.platform.notification.service.TelegramService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration tests for the notification Kafka consumer.
 * Produces messages to iam.notification.commands and iam.alert.triggers,
 * then verifies dispatch to the correct channel service.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
@EmbeddedKafka(
        topics = {KafkaTopics.NOTIFICATION_COMMANDS, KafkaTopics.ALERT_TRIGGERS},
        partitions = 1,
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"}
)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=",
        "spring.thymeleaf.check-template-location=false"
})
class NotificationKafkaConsumerIT {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private NotificationLogRepository logRepository;

    @MockBean
    private EmailService emailService;

    @MockBean
    private SmsService smsService;

    @MockBean
    private TelegramService telegramService;

    private KafkaTemplate<String, NotificationCommandDto> kafkaTemplate;

    @BeforeEach
    void setUp() throws Exception {
        logRepository.deleteAll();
        reset(emailService, smsService, telegramService);

        // By default, channel services succeed
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());
        doNothing().when(smsService).sendSms(anyString(), anyString());
        doNothing().when(telegramService).sendMessage(anyString(), anyString());

        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, NotificationCommandDto> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(producerFactory);
    }

    @Test
    @DisplayName("Should dispatch EMAIL notification via EmailService")
    void shouldDispatchEmailNotification() throws Exception {
        NotificationCommandDto command = NotificationCommandDto.builder()
                .channelType(NotificationCommandDto.ChannelType.EMAIL)
                .recipient("admin@gdt.gov.kh")
                .subject("Test Email Subject")
                .body("<p>Test email body</p>")
                .priority(NotificationCommandDto.Priority.NORMAL)
                .build();

        kafkaTemplate.send(KafkaTopics.NOTIFICATION_COMMANDS, command);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(emailService, times(1)).sendEmail(
                    eq("admin@gdt.gov.kh"),
                    eq("Test Email Subject"),
                    eq("<p>Test email body</p>")
            );
        });

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<NotificationLog> logs = logRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getChannelType()).isEqualTo(ChannelType.EMAIL);
            assertThat(logs.get(0).getRecipient()).isEqualTo("admin@gdt.gov.kh");
            assertThat(logs.get(0).getStatus()).isEqualTo(NotificationStatus.SENT);
        });
    }

    @Test
    @DisplayName("Should dispatch SMS notification via SmsService")
    void shouldDispatchSmsNotification() {
        NotificationCommandDto command = NotificationCommandDto.builder()
                .channelType(NotificationCommandDto.ChannelType.SMS)
                .recipient("+85512345678")
                .subject("SMS Alert")
                .body("Your verification code is 123456")
                .priority(NotificationCommandDto.Priority.HIGH)
                .build();

        kafkaTemplate.send(KafkaTopics.NOTIFICATION_COMMANDS, command);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(smsService, times(1)).sendSms(
                    eq("+85512345678"),
                    eq("Your verification code is 123456")
            );
        });

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<NotificationLog> logs = logRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getChannelType()).isEqualTo(ChannelType.SMS);
            assertThat(logs.get(0).getStatus()).isEqualTo(NotificationStatus.SENT);
        });
    }

    @Test
    @DisplayName("Should dispatch Telegram notification via TelegramService")
    void shouldDispatchTelegramNotification() {
        NotificationCommandDto command = NotificationCommandDto.builder()
                .channelType(NotificationCommandDto.ChannelType.TELEGRAM)
                .recipient("123456789")
                .subject("Telegram Alert")
                .body("System health check failed")
                .priority(NotificationCommandDto.Priority.URGENT)
                .build();

        kafkaTemplate.send(KafkaTopics.NOTIFICATION_COMMANDS, command);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(telegramService, times(1)).sendMessage(
                    eq("123456789"),
                    eq("System health check failed")
            );
        });

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<NotificationLog> logs = logRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getChannelType()).isEqualTo(ChannelType.TELEGRAM);
            assertThat(logs.get(0).getStatus()).isEqualTo(NotificationStatus.SENT);
        });
    }

    @Test
    @DisplayName("Should log FAILED status when email dispatch throws exception")
    void shouldLogFailedStatusWhenEmailFails() throws Exception {
        doThrow(new RuntimeException("SMTP connection refused"))
                .when(emailService).sendEmail(anyString(), anyString(), anyString());

        NotificationCommandDto command = NotificationCommandDto.builder()
                .channelType(NotificationCommandDto.ChannelType.EMAIL)
                .recipient("failing@test.com")
                .subject("Will Fail")
                .body("This will fail")
                .build();

        kafkaTemplate.send(KafkaTopics.NOTIFICATION_COMMANDS, command);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<NotificationLog> logs = logRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getStatus()).isEqualTo(NotificationStatus.FAILED);
            assertThat(logs.get(0).getErrorMessage()).contains("SMTP connection refused");
        });
    }

    @Test
    @DisplayName("Should handle alert trigger from iam.alert.triggers topic")
    void shouldHandleAlertTrigger() throws Exception {
        NotificationCommandDto alert = NotificationCommandDto.builder()
                .channelType(NotificationCommandDto.ChannelType.EMAIL)
                .recipient("ops@rikreay.gov.kh")
                .subject("High CPU Usage")
                .body("Service iam-core-service CPU at 95%")
                .priority(NotificationCommandDto.Priority.URGENT)
                .build();

        kafkaTemplate.send(KafkaTopics.ALERT_TRIGGERS, alert);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(emailService, times(1)).sendEmail(
                    eq("ops@rikreay.gov.kh"),
                    eq("[ALERT] High CPU Usage"),
                    eq("Service iam-core-service CPU at 95%")
            );
        });
    }

    @Test
    @DisplayName("Should use default subject when command has null subject")
    void shouldUseDefaultSubjectWhenNull() throws Exception {
        NotificationCommandDto command = NotificationCommandDto.builder()
                .channelType(NotificationCommandDto.ChannelType.EMAIL)
                .recipient("user@test.com")
                .subject(null)
                .body("Body without subject")
                .build();

        kafkaTemplate.send(KafkaTopics.NOTIFICATION_COMMANDS, command);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(emailService, times(1)).sendEmail(
                    eq("user@test.com"),
                    eq("RikReay IAM Notification"),
                    eq("Body without subject")
            );
        });
    }

    @Test
    @DisplayName("Should use empty body when command has null body")
    void shouldUseEmptyBodyWhenNull() throws Exception {
        NotificationCommandDto command = NotificationCommandDto.builder()
                .channelType(NotificationCommandDto.ChannelType.EMAIL)
                .recipient("user@test.com")
                .subject("No Body")
                .body(null)
                .build();

        kafkaTemplate.send(KafkaTopics.NOTIFICATION_COMMANDS, command);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(emailService, times(1)).sendEmail(
                    eq("user@test.com"),
                    eq("No Body"),
                    eq("")
            );
        });
    }

    @Test
    @DisplayName("Should handle multiple notifications in sequence")
    void shouldHandleMultipleNotifications() throws Exception {
        NotificationCommandDto email = NotificationCommandDto.builder()
                .channelType(NotificationCommandDto.ChannelType.EMAIL)
                .recipient("user1@test.com")
                .subject("Email 1")
                .body("Email body")
                .build();

        NotificationCommandDto sms = NotificationCommandDto.builder()
                .channelType(NotificationCommandDto.ChannelType.SMS)
                .recipient("+85500000001")
                .subject("SMS 1")
                .body("SMS body")
                .build();

        NotificationCommandDto telegram = NotificationCommandDto.builder()
                .channelType(NotificationCommandDto.ChannelType.TELEGRAM)
                .recipient("chat1")
                .subject("TG 1")
                .body("Telegram body")
                .build();

        kafkaTemplate.send(KafkaTopics.NOTIFICATION_COMMANDS, email);
        kafkaTemplate.send(KafkaTopics.NOTIFICATION_COMMANDS, sms);
        kafkaTemplate.send(KafkaTopics.NOTIFICATION_COMMANDS, telegram);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            List<NotificationLog> logs = logRepository.findAll();
            assertThat(logs).hasSize(3);
            assertThat(logs).extracting(NotificationLog::getChannelType)
                    .containsExactlyInAnyOrder(ChannelType.EMAIL, ChannelType.SMS, ChannelType.TELEGRAM);
        });
    }
}
