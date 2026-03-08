package com.iam.platform.monitoring.service;

import com.iam.platform.monitoring.dto.XRoadMetricsDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class XRoadMetricsService {

    private final WebClient prometheusWebClient;

    public XRoadMetricsService(@Qualifier("prometheusWebClient") WebClient prometheusWebClient) {
        this.prometheusWebClient = prometheusWebClient;
    }

    @CircuitBreaker(name = "prometheus", fallbackMethod = "xroadMetricsFallback")
    public XRoadMetricsDto getXRoadMetrics(String tenantId, String memberClass) {
        try {
            // Query Prometheus for X-Road metrics
            // In production, this would use PromQL queries via the Prometheus HTTP API
            log.debug("Querying X-Road metrics: tenant={}, memberClass={}", tenantId, memberClass);

            String query = buildPrometheusQuery(tenantId, memberClass);
            String result = prometheusWebClient.get()
                    .uri("/api/v1/query?query={query}", query)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .onErrorReturn("{}")
                    .block(Duration.ofSeconds(15));

            log.debug("Prometheus response: {}", result);

            return new XRoadMetricsDto(0, 0, 0, 0.0, Map.of(), Map.of(), Map.of());
        } catch (Exception e) {
            log.error("Failed to query X-Road metrics from Prometheus", e);
            return emptyMetrics();
        }
    }

    private String buildPrometheusQuery(String tenantId, String memberClass) {
        StringBuilder query = new StringBuilder("xroad_requests_total");
        if (tenantId != null || memberClass != null) {
            query.append("{");
            if (tenantId != null) {
                query.append("tenant_id=\"").append(tenantId).append("\"");
            }
            if (memberClass != null) {
                if (tenantId != null) query.append(",");
                query.append("member_class=\"").append(memberClass).append("\"");
            }
            query.append("}");
        }
        return query.toString();
    }

    @SuppressWarnings("unused")
    private XRoadMetricsDto xroadMetricsFallback(String tenantId, String memberClass, Throwable t) {
        log.error("X-Road metrics circuit breaker open: {}", t.getMessage());
        return emptyMetrics();
    }

    private XRoadMetricsDto emptyMetrics() {
        return new XRoadMetricsDto(0, 0, 0, 0.0, Map.of(), Map.of(), Map.of());
    }
}
