package com.iam.platform.monitoring.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "monitoring")
public class MonitoringProperties {

    private List<ServiceEntry> services = new ArrayList<>();
    private int healthCheckInterval = 30;
    private int redisCacheTtl = 30;

    @Data
    public static class ServiceEntry {
        private String name;
        private String url;
    }
}
