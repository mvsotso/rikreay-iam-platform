package com.iam.platform.audit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.audit")
public class AuditProperties {

    private boolean elasticsearchEnabled = true;
    private String indexPrefix = "iam-audit";
    private String xroadIndexPrefix = "iam-xroad";
}
