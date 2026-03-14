package com.iam.platform.audit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("test")
public class TestConfig {

    @SuppressWarnings("unchecked")
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return mock(KafkaTemplate.class);
    }

    @Bean
    public ElasticsearchOperations elasticsearchOperations() {
        return mock(ElasticsearchOperations.class);
    }
}
