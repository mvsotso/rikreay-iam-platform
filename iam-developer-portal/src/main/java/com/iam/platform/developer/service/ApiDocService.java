package com.iam.platform.developer.service;

import com.iam.platform.developer.config.DeveloperProperties;
import com.iam.platform.developer.dto.ApiDocResponse;
import com.iam.platform.developer.dto.SdkInfo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiDocService {

    private final WebClient apiDocsWebClient;
    private final DeveloperProperties properties;

    @CircuitBreaker(name = "api-docs", fallbackMethod = "getAllDocsFallback")
    public List<ApiDocResponse> getAllApiDocs() {
        return properties.getServices().stream()
                .map(service -> fetchApiDoc(service.getName(), service.getUrl()))
                .toList();
    }

    @CircuitBreaker(name = "api-docs", fallbackMethod = "getServiceDocFallback")
    public ApiDocResponse getServiceApiDoc(String serviceName) {
        DeveloperProperties.ServiceInfo service = properties.getServices().stream()
                .filter(s -> s.getName().equals(serviceName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Service not found: " + serviceName));

        return fetchApiDoc(service.getName(), service.getUrl());
    }

    public List<String> getAvailableServices() {
        return properties.getServices().stream()
                .map(DeveloperProperties.ServiceInfo::getName)
                .toList();
    }

    public List<SdkInfo> getAvailableSdks() {
        return List.of(
                new SdkInfo("Java", "1.0.0", "/api/v1/sdks/java", "/api/v1/docs/sdk-guide/java"),
                new SdkInfo("Python", "1.0.0", "/api/v1/sdks/python", "/api/v1/docs/sdk-guide/python"),
                new SdkInfo("JavaScript", "1.0.0", "/api/v1/sdks/javascript", "/api/v1/docs/sdk-guide/javascript")
        );
    }

    @SuppressWarnings("unchecked")
    private ApiDocResponse fetchApiDoc(String serviceName, String serviceUrl) {
        try {
            Map<String, Object> spec = apiDocsWebClient.get()
                    .uri(serviceUrl + "/v3/api-docs")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return new ApiDocResponse(serviceName, serviceUrl, spec);
        } catch (Exception e) {
            log.warn("Failed to fetch API docs from {}: {}", serviceName, e.getMessage());
            return new ApiDocResponse(serviceName, serviceUrl,
                    Map.of("error", "Service unavailable", "service", serviceName));
        }
    }

    public List<ApiDocResponse> getAllDocsFallback(Throwable t) {
        log.error("Circuit breaker open for API docs aggregation: {}", t.getMessage());
        return List.of();
    }

    public ApiDocResponse getServiceDocFallback(String serviceName, Throwable t) {
        log.error("Circuit breaker open for service {}: {}", serviceName, t.getMessage());
        return new ApiDocResponse(serviceName, "", Map.of("error", "Service unavailable"));
    }
}
