package com.iam.platform.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        return mock(ReactiveRedisConnectionFactory.class);
    }

    @Bean
    @Primary
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate() {
        return mock(ReactiveStringRedisTemplate.class);
    }
}
