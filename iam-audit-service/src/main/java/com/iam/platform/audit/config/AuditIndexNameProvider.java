package com.iam.platform.audit.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Provides dynamic Elasticsearch index names with monthly rolling pattern.
 */
@Component("auditIndexNameProvider")
@RequiredArgsConstructor
public class AuditIndexNameProvider {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM");

    private final AuditProperties auditProperties;

    public String getIndexName() {
        return auditProperties.getIndexPrefix() + "-" + YearMonth.now().format(MONTH_FORMAT);
    }

    public String getIndexName(YearMonth yearMonth) {
        return auditProperties.getIndexPrefix() + "-" + yearMonth.format(MONTH_FORMAT);
    }

    public String getXroadIndexName() {
        return auditProperties.getXroadIndexPrefix() + "-" + YearMonth.now().format(MONTH_FORMAT);
    }

    public String getXroadIndexName(YearMonth yearMonth) {
        return auditProperties.getXroadIndexPrefix() + "-" + yearMonth.format(MONTH_FORMAT);
    }

    public String getIndexPattern() {
        return auditProperties.getIndexPrefix() + "-*";
    }

    public String getXroadIndexPattern() {
        return auditProperties.getXroadIndexPrefix() + "-*";
    }
}
