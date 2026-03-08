package com.iam.platform.xroad.service;

import com.iam.platform.common.constants.XRoadHeaders;
import com.iam.platform.common.dto.XRoadContextDto;
import com.iam.platform.common.exception.XRoadServiceException;
import com.iam.platform.xroad.config.XRoadProperties;
import com.iam.platform.xroad.entity.XRoadServiceRegistration;
import com.iam.platform.xroad.repository.XRoadAclEntryRepository;
import com.iam.platform.xroad.repository.XRoadServiceRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * Routes incoming X-Road requests to internal services.
 * Validates ACL using Redis-cached entries (5 min TTL).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XRoadRoutingService {

    private final XRoadServiceRegistrationRepository serviceRepository;
    private final XRoadAclEntryRepository aclRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WebClient xroadWebClient;
    private final XRoadProperties xroadProperties;
    private final AuditService auditService;

    private static final String ACL_CACHE_PREFIX = "xroad:acl:";

    /**
     * Routes an X-Road request: resolve service → validate ACL → proxy to target.
     */
    public String routeRequest(String serviceCode, String pathSuffix,
                                XRoadContextDto context, String requestBody) {
        log.debug("Routing X-Road request: service={}, consumer={}", serviceCode, context.getFullClientId());

        // Resolve service registration
        XRoadServiceRegistration service = serviceRepository
                .findByServiceCode(serviceCode)
                .orElseThrow(() -> new XRoadServiceException(
                        "X-Road service not found: " + serviceCode));

        if (!service.isEnabled()) {
            throw new XRoadServiceException("X-Road service is disabled: " + serviceCode);
        }

        // Validate ACL
        if (!isAccessAllowed(context.getFullClientId(), serviceCode)) {
            auditService.logXRoadExchange(context.getFullClientId(), "ACL_DENIED",
                    serviceCode, false,
                    Map.of("serviceCode", serviceCode, "messageId", context.getMessageId()));
            throw new XRoadServiceException(
                    "Access denied for consumer '" + context.getFullClientId()
                            + "' to service '" + serviceCode + "'");
        }

        // Build target path
        String targetPath = service.getTargetPath();
        if (pathSuffix != null && !pathSuffix.isEmpty()) {
            targetPath = targetPath + "/" + pathSuffix;
        }

        // Proxy to target service
        try {
            String response = xroadWebClient.get()
                    .uri(targetPath)
                    .header(XRoadHeaders.CLIENT, context.getFullClientId())
                    .header(XRoadHeaders.ID, context.getMessageId())
                    .header(XRoadHeaders.USER_ID, context.getUserId())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));

            auditService.logXRoadExchange(context.getFullClientId(), "XROAD_REQUEST",
                    serviceCode, true,
                    Map.of("serviceCode", serviceCode, "messageId", context.getMessageId(),
                            "targetPath", targetPath));

            return response;

        } catch (XRoadServiceException e) {
            throw e;
        } catch (Exception e) {
            auditService.logXRoadExchange(context.getFullClientId(), "XROAD_REQUEST_FAILED",
                    serviceCode, false,
                    Map.of("serviceCode", serviceCode, "error", e.getMessage()));
            throw new XRoadServiceException("Failed to proxy request to " + serviceCode, e);
        }
    }

    /**
     * Checks ACL with Redis caching (TTL from config).
     */
    public boolean isAccessAllowed(String consumerIdentifier, String serviceCode) {
        String cacheKey = ACL_CACHE_PREFIX + consumerIdentifier + ":" + serviceCode;

        // Check Redis cache first
        try {
            Boolean cached = (Boolean) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("ACL cache hit: consumer={}, service={}, allowed={}",
                        consumerIdentifier, serviceCode, cached);
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis ACL cache read failed, checking DB: {}", e.getMessage());
        }

        // Check database
        boolean allowed = !aclRepository.findAllowedEntries(consumerIdentifier, serviceCode).isEmpty();

        // Cache result
        try {
            redisTemplate.opsForValue().set(cacheKey, allowed,
                    Duration.ofMinutes(xroadProperties.getAcl().getCacheTtlMinutes()));
        } catch (Exception e) {
            log.warn("Redis ACL cache write failed: {}", e.getMessage());
        }

        log.debug("ACL DB check: consumer={}, service={}, allowed={}",
                consumerIdentifier, serviceCode, allowed);
        return allowed;
    }
}
