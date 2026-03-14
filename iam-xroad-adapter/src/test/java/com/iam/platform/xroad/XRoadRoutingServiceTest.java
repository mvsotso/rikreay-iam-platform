package com.iam.platform.xroad;

import com.iam.platform.common.dto.XRoadContextDto;
import com.iam.platform.common.exception.XRoadServiceException;
import com.iam.platform.xroad.config.XRoadProperties;
import com.iam.platform.xroad.entity.XRoadAclEntry;
import com.iam.platform.xroad.entity.XRoadServiceRegistration;
import com.iam.platform.xroad.repository.XRoadAclEntryRepository;
import com.iam.platform.xroad.repository.XRoadServiceRegistrationRepository;
import com.iam.platform.xroad.service.AuditService;
import com.iam.platform.xroad.service.XRoadRoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for XRoadRoutingService — routing logic, ACL validation, caching.
 */
@ExtendWith(MockitoExtension.class)
class XRoadRoutingServiceTest {

    @Mock
    private XRoadServiceRegistrationRepository serviceRepository;

    @Mock
    private XRoadAclEntryRepository aclRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private WebClient xroadWebClient;

    @Mock
    private XRoadProperties xroadProperties;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private XRoadRoutingService routingService;

    private XRoadContextDto context;
    private XRoadServiceRegistration service;

    @BeforeEach
    void setUp() {
        context = XRoadContextDto.builder()
                .clientInstance("KH")
                .clientMemberClass("GOV")
                .clientMemberCode("MOF")
                .clientSubsystem("BUDGET-SYSTEM")
                .fullClientId("KH/GOV/MOF/BUDGET-SYSTEM")
                .messageId("msg-12345")
                .userId("user123")
                .requestTimestamp(System.currentTimeMillis())
                .build();

        service = XRoadServiceRegistration.builder()
                .serviceCode("getTaxpayerInfo")
                .serviceVersion("v1")
                .targetService("iam-core-service")
                .targetPath("/api/v1/persons/tin")
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("routeRequest — service not found should throw XRoadServiceException")
    void routeRequest_serviceNotFound_shouldThrow() {
        when(serviceRepository.findByServiceCode("unknown"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                routingService.routeRequest("unknown", "", context, null))
                .isInstanceOf(XRoadServiceException.class)
                .hasMessageContaining("X-Road service not found: unknown");
    }

    @Test
    @DisplayName("routeRequest — disabled service should throw XRoadServiceException")
    void routeRequest_disabledService_shouldThrow() {
        service.setEnabled(false);
        when(serviceRepository.findByServiceCode("getTaxpayerInfo"))
                .thenReturn(Optional.of(service));

        assertThatThrownBy(() ->
                routingService.routeRequest("getTaxpayerInfo", "", context, null))
                .isInstanceOf(XRoadServiceException.class)
                .hasMessageContaining("X-Road service is disabled");
    }

    @Test
    @DisplayName("routeRequest — ACL denied should throw and log audit event")
    void routeRequest_aclDenied_shouldThrowAndAudit() {
        when(serviceRepository.findByServiceCode("getTaxpayerInfo"))
                .thenReturn(Optional.of(service));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null); // cache miss
        when(aclRepository.findAllowedEntries("KH/GOV/MOF/BUDGET-SYSTEM", "getTaxpayerInfo"))
                .thenReturn(List.of()); // denied

        XRoadProperties.Acl aclProps = new XRoadProperties.Acl();
        aclProps.setCacheTtlMinutes(5);
        when(xroadProperties.getAcl()).thenReturn(aclProps);

        assertThatThrownBy(() ->
                routingService.routeRequest("getTaxpayerInfo", "", context, null))
                .isInstanceOf(XRoadServiceException.class)
                .hasMessageContaining("Access denied");

        verify(auditService).logXRoadExchange(
                eq("KH/GOV/MOF/BUDGET-SYSTEM"), eq("ACL_DENIED"),
                eq("getTaxpayerInfo"), eq(false), any());
    }

    @Test
    @DisplayName("isAccessAllowed — should check Redis cache first")
    void isAccessAllowed_cacheHit_shouldReturnCachedValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("xroad:acl:KH/GOV/MOF/BUDGET-SYSTEM:getTaxpayerInfo"))
                .thenReturn(Boolean.TRUE);

        boolean result = routingService.isAccessAllowed(
                "KH/GOV/MOF/BUDGET-SYSTEM", "getTaxpayerInfo");

        assertThat(result).isTrue();
        verify(aclRepository, never()).findAllowedEntries(any(), any());
    }

    @Test
    @DisplayName("isAccessAllowed — cache miss should check DB and cache result")
    void isAccessAllowed_cacheMiss_shouldCheckDbAndCache() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null); // cache miss

        XRoadAclEntry aclEntry = XRoadAclEntry.builder()
                .consumerIdentifier("KH/GOV/MOF/BUDGET-SYSTEM")
                .allowed(true)
                .build();
        when(aclRepository.findAllowedEntries("KH/GOV/MOF/BUDGET-SYSTEM", "getTaxpayerInfo"))
                .thenReturn(List.of(aclEntry));

        XRoadProperties.Acl aclProps = new XRoadProperties.Acl();
        aclProps.setCacheTtlMinutes(5);
        when(xroadProperties.getAcl()).thenReturn(aclProps);

        boolean result = routingService.isAccessAllowed(
                "KH/GOV/MOF/BUDGET-SYSTEM", "getTaxpayerInfo");

        assertThat(result).isTrue();
        verify(aclRepository).findAllowedEntries("KH/GOV/MOF/BUDGET-SYSTEM", "getTaxpayerInfo");
        verify(valueOperations).set(anyString(), eq(true), any(java.time.Duration.class));
    }

    @Test
    @DisplayName("isAccessAllowed — Redis failure should fallback to DB check")
    void isAccessAllowed_redisFails_shouldFallbackToDb() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis down"));

        when(aclRepository.findAllowedEntries("KH/COM/ABA/BANKING", "verifyIdentity"))
                .thenReturn(List.of());

        XRoadProperties.Acl aclProps = new XRoadProperties.Acl();
        aclProps.setCacheTtlMinutes(5);
        when(xroadProperties.getAcl()).thenReturn(aclProps);

        boolean result = routingService.isAccessAllowed("KH/COM/ABA/BANKING", "verifyIdentity");

        assertThat(result).isFalse();
        verify(aclRepository).findAllowedEntries("KH/COM/ABA/BANKING", "verifyIdentity");
    }

    @Test
    @DisplayName("isAccessAllowed — denied consumer should return false")
    void isAccessAllowed_denied_shouldReturnFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        when(aclRepository.findAllowedEntries("KH/NGO/UNDP/AID-SYSTEM", "restrictedService"))
                .thenReturn(List.of()); // no ACL entries = denied

        XRoadProperties.Acl aclProps = new XRoadProperties.Acl();
        aclProps.setCacheTtlMinutes(5);
        when(xroadProperties.getAcl()).thenReturn(aclProps);

        boolean result = routingService.isAccessAllowed(
                "KH/NGO/UNDP/AID-SYSTEM", "restrictedService");

        assertThat(result).isFalse();
    }
}
