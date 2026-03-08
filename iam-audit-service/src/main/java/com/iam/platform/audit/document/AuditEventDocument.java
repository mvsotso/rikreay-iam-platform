package com.iam.platform.audit.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.Map;

/**
 * Elasticsearch document for audit events.
 * Uses dynamic index name pattern: iam-audit-yyyy.MM
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "#{@auditIndexNameProvider.getIndexName()}", createIndex = false)
public class AuditEventDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String eventType;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant timestamp;

    @Field(type = FieldType.Keyword)
    private String username;

    @Field(type = FieldType.Keyword)
    private String action;

    @Field(type = FieldType.Text)
    private String resource;

    @Field(type = FieldType.Ip)
    private String sourceIp;

    @Field(type = FieldType.Boolean)
    private boolean success;

    @Field(type = FieldType.Object)
    private Map<String, Object> metadata;

    @Field(type = FieldType.Keyword)
    private String tenantId;

    @Field(type = FieldType.Keyword)
    private String memberClass;
}
