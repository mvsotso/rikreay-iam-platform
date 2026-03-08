package com.iam.platform.notification.config;

import com.iam.platform.common.constants.KafkaTopics;
import com.iam.platform.common.dto.NotificationCommandDto;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, NotificationCommandDto> notificationConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        JsonDeserializer<NotificationCommandDto> deserializer = new JsonDeserializer<>(NotificationCommandDto.class);
        deserializer.setRemoveTypeHeaders(true);
        deserializer.addTrustedPackages("com.iam.platform.common.dto");
        deserializer.setUseTypeMapperForKey(false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationCommandDto> notificationKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, NotificationCommandDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(notificationConsumerFactory());
        factory.setConcurrency(3);
        return factory;
    }

    @Bean
    public NewTopic notificationCommandsTopic() {
        return TopicBuilder.name(KafkaTopics.NOTIFICATION_COMMANDS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic alertTriggersTopic() {
        return TopicBuilder.name(KafkaTopics.ALERT_TRIGGERS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name(KafkaTopics.AUDIT_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
