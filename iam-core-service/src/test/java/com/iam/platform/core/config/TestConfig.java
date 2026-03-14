package com.iam.platform.core.config;

import com.iam.platform.common.dto.AuditEventDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("test")
public class TestConfig {

    @SuppressWarnings("unchecked")
    @Bean
    public KafkaTemplate<String, AuditEventDto> kafkaTemplate() {
        return mock(KafkaTemplate.class);
    }
}
