package com.iam.platform.monitoring.service;

import com.iam.platform.monitoring.config.MonitoringProperties;
import com.iam.platform.monitoring.dto.AggregatedHealthResponse;
import com.iam.platform.monitoring.dto.ServiceHealthDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthAggregationService {

    private final WebClient healthCheckWebClient;
    private final MonitoringProperties monitoringProperties;

    @CircuitBreaker(name = "health-check", fallbackMethod = "healthCheckFallback")
    @Cacheable(value = "health-status", key = "'aggregated'")
    public AggregatedHealthResponse getAggregatedHealth() {
        List<ServiceHealthDto> healthResults = Flux.fromIterable(monitoringProperties.getServices())
                .flatMap(service -> checkServiceHealth(service.getName(), service.getUrl()))
                .collectList()
                .block(Duration.ofSeconds(15));

        if (healthResults == null) {
            healthResults = List.of();
        }

        int healthy = (int) healthResults.stream()
                .filter(h -> "UP".equals(h.status()))
                .count();
        int unhealthy = healthResults.size() - healthy;
        String overall = unhealthy == 0 ? "UP" : (healthy == 0 ? "DOWN" : "DEGRADED");

        return new AggregatedHealthResponse(
                overall,
                healthResults.size(),
                healthy,
                unhealthy,
                healthResults,
                Instant.now()
        );
    }

    public List<ServiceHealthDto> getServiceDetails() {
        AggregatedHealthResponse health = getAggregatedHealth();
        return health.services();
    }

    private Mono<ServiceHealthDto> checkServiceHealth(String serviceName, String baseUrl) {
        long startTime = System.currentTimeMillis();
        return healthCheckWebClient.get()
                .uri(baseUrl + "/actuator/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .map(body -> new ServiceHealthDto(
                        serviceName, baseUrl, "UP",
                        System.currentTimeMillis() - startTime,
                        Instant.now(), "Healthy"))
                .onErrorResume(e -> {
                    log.warn("Health check failed for {}: {}", serviceName, e.getMessage());
                    return Mono.just(new ServiceHealthDto(
                            serviceName, baseUrl, "DOWN",
                            System.currentTimeMillis() - startTime,
                            Instant.now(), e.getMessage()));
                });
    }

    @SuppressWarnings("unused")
    private AggregatedHealthResponse healthCheckFallback(Throwable t) {
        log.error("Health check circuit breaker open: {}", t.getMessage());
        return new AggregatedHealthResponse(
                "UNKNOWN", 0, 0, 0, List.of(), Instant.now());
    }
}
