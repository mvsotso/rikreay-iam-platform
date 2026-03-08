package com.iam.platform.developer.config;

import com.iam.platform.common.constants.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name(KafkaTopics.AUDIT_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic platformEventsTopic() {
        return TopicBuilder.name(KafkaTopics.PLATFORM_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
