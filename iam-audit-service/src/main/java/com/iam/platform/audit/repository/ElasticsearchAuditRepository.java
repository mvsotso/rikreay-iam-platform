package com.iam.platform.audit.repository;

import com.iam.platform.audit.config.AuditIndexNameProvider;
import com.iam.platform.audit.document.AuditEventDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.SortOrder;


/**
 * Elasticsearch repository for indexing and querying audit events.
 * Uses monthly rolling indexes: iam-audit-yyyy.MM
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ElasticsearchAuditRepository {

    private final ElasticsearchOperations elasticsearchOperations;
    private final AuditIndexNameProvider indexNameProvider;

    /**
     * Indexes an audit event document to the current month's index.
     */
    public AuditEventDocument indexAuditEvent(AuditEventDocument document) {
        IndexCoordinates index = IndexCoordinates.of(indexNameProvider.getIndexName());
        return elasticsearchOperations.save(document, index);
    }

    /**
     * Indexes an X-Road event document to the current month's X-Road index.
     */
    public AuditEventDocument indexXroadEvent(AuditEventDocument document) {
        IndexCoordinates index = IndexCoordinates.of(indexNameProvider.getXroadIndexName());
        return elasticsearchOperations.save(document, index);
    }

    /**
     * Searches audit events with filters across all monthly indexes.
     */
    public PageImpl<AuditEventDocument> searchAuditEvents(
            String eventType, String username, String action,
            String tenantId, String memberClass,
            Instant from, Instant to, Pageable pageable) {

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        if (eventType != null) {
            boolBuilder.filter(QueryBuilders.term(t -> t.field("eventType").value(eventType)));
        }
        if (username != null) {
            boolBuilder.filter(QueryBuilders.term(t -> t.field("username").value(username)));
        }
        if (action != null) {
            boolBuilder.filter(QueryBuilders.term(t -> t.field("action").value(action)));
        }
        if (tenantId != null) {
            boolBuilder.filter(QueryBuilders.term(t -> t.field("tenantId").value(tenantId)));
        }
        if (memberClass != null) {
            boolBuilder.filter(QueryBuilders.term(t -> t.field("memberClass").value(memberClass)));
        }
        if (from != null || to != null) {
            boolBuilder.filter(QueryBuilders.range(r -> r.date(d -> {
                d.field("timestamp");
                if (from != null) d.gte(from.toString());
                if (to != null) d.lte(to.toString());
                return d;
            })));
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(new Query.Builder().bool(boolBuilder.build()).build())
                .withSort(s -> s.field(f -> f.field("timestamp").order(SortOrder.Desc)))
                .withPageable(pageable)
                .build();

        IndexCoordinates index = IndexCoordinates.of(indexNameProvider.getIndexPattern());
        SearchHits<AuditEventDocument> searchHits =
                elasticsearchOperations.search(query, AuditEventDocument.class, index);

        List<AuditEventDocument> documents = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        return new PageImpl<>(documents, pageable, searchHits.getTotalHits());
    }

    /**
     * Searches X-Road events across all monthly indexes.
     */
    public PageImpl<AuditEventDocument> searchXroadEvents(
            String tenantId, String memberClass,
            Instant from, Instant to, Pageable pageable) {

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        if (tenantId != null) {
            boolBuilder.filter(QueryBuilders.term(t -> t.field("tenantId").value(tenantId)));
        }
        if (memberClass != null) {
            boolBuilder.filter(QueryBuilders.term(t -> t.field("memberClass").value(memberClass)));
        }
        if (from != null || to != null) {
            boolBuilder.filter(QueryBuilders.range(r -> r.date(d -> {
                d.field("timestamp");
                if (from != null) d.gte(from.toString());
                if (to != null) d.lte(to.toString());
                return d;
            })));
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(new Query.Builder().bool(boolBuilder.build()).build())
                .withSort(s -> s.field(f -> f.field("timestamp").order(SortOrder.Desc)))
                .withPageable(pageable)
                .build();

        IndexCoordinates index = IndexCoordinates.of(indexNameProvider.getXroadIndexPattern());
        SearchHits<AuditEventDocument> searchHits =
                elasticsearchOperations.search(query, AuditEventDocument.class, index);

        List<AuditEventDocument> documents = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        return new PageImpl<>(documents, pageable, searchHits.getTotalHits());
    }

    /**
     * Searches login history events (AUTH_EVENT type).
     */
    public PageImpl<AuditEventDocument> searchLoginHistory(
            String username, String tenantId,
            Instant from, Instant to, Boolean successOnly,
            Pageable pageable) {

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        boolBuilder.filter(QueryBuilders.term(t -> t.field("eventType").value("AUTH_EVENT")));

        if (username != null) {
            boolBuilder.filter(QueryBuilders.term(t -> t.field("username").value(username)));
        }
        if (tenantId != null) {
            boolBuilder.filter(QueryBuilders.term(t -> t.field("tenantId").value(tenantId)));
        }
        if (successOnly != null) {
            boolBuilder.filter(QueryBuilders.term(t -> t.field("success").value(successOnly)));
        }
        if (from != null || to != null) {
            boolBuilder.filter(QueryBuilders.range(r -> r.date(d -> {
                d.field("timestamp");
                if (from != null) d.gte(from.toString());
                if (to != null) d.lte(to.toString());
                return d;
            })));
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(new Query.Builder().bool(boolBuilder.build()).build())
                .withSort(s -> s.field(f -> f.field("timestamp").order(SortOrder.Desc)))
                .withPageable(pageable)
                .build();

        IndexCoordinates index = IndexCoordinates.of(indexNameProvider.getIndexPattern());
        SearchHits<AuditEventDocument> searchHits =
                elasticsearchOperations.search(query, AuditEventDocument.class, index);

        List<AuditEventDocument> documents = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        return new PageImpl<>(documents, pageable, searchHits.getTotalHits());
    }
}
