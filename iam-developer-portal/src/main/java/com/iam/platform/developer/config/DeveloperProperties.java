package com.iam.platform.developer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "developer")
public class DeveloperProperties {

    private List<ServiceInfo> services = List.of();
    private SandboxConfig sandbox = new SandboxConfig();
    private WebhookConfig webhook = new WebhookConfig();

    @Data
    public static class ServiceInfo {
        private String name;
        private String url;
    }

    @Data
    public static class SandboxConfig {
        private int maxPerUser = 3;
        private int expiryDays = 7;
    }

    @Data
    public static class WebhookConfig {
        private int maxRetries = 3;
        private String retryDelays = "1000,5000,25000";
    }
}
