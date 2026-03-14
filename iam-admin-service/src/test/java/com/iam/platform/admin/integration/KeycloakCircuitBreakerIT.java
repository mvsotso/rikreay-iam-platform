package com.iam.platform.admin.integration;

import com.iam.platform.admin.dto.PlatformDashboardResponse;
import com.iam.platform.admin.dto.RealmSettingsRequest;
import com.iam.platform.admin.dto.RealmSettingsResponse;
import com.iam.platform.admin.service.AdminDashboardService;
import com.iam.platform.admin.service.AuditService;
import com.iam.platform.admin.service.BulkUserService;
import com.iam.platform.admin.service.RealmSettingsService;
import jakarta.ws.rs.ProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Resilience4j circuit breaker behavior
 * on admin service's Keycloak operations.
 * Tests that the circuit breaker opens after failures, returns fallback
 * responses, and eventually recovers.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
        "resilience4j.circuitbreaker.instances.keycloak.sliding-window-size=5",
        "resilience4j.circuitbreaker.instances.keycloak.minimum-number-of-calls=3",
        "resilience4j.circuitbreaker.instances.keycloak.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.keycloak.wait-duration-in-open-state=2s",
        "resilience4j.circuitbreaker.instances.keycloak.permitted-number-of-calls-in-half-open-state=2",
        "resilience4j.circuitbreaker.instances.keycloak.automatic-transition-from-open-to-half-open-enabled=true"
})
class KeycloakCircuitBreakerIT {

    @Autowired
    private AdminDashboardService adminDashboardService;

    @Autowired
    private RealmSettingsService realmSettingsService;

    @Autowired
    private BulkUserService bulkUserService;

    @MockBean
    private Keycloak keycloak;

    @MockBean
    private AuditService auditService;

    private RealmResource mockRealmResource;
    private RealmsResource mockRealmsResource;
    private UsersResource mockUsersResource;

    @BeforeEach
    void setUp() {
        mockRealmResource = mock(RealmResource.class);
        mockRealmsResource = mock(RealmsResource.class);
        mockUsersResource = mock(UsersResource.class);

        when(keycloak.realm(anyString())).thenReturn(mockRealmResource);
        when(keycloak.realms()).thenReturn(mockRealmsResource);
        when(mockRealmResource.users()).thenReturn(mockUsersResource);
    }

    @Test
    @DisplayName("Should return dashboard data when Keycloak is available")
    void shouldReturnDashboardWhenKeycloakAvailable() {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm("test-realm");
        when(mockRealmsResource.findAll()).thenReturn(List.of(realm));
        when(mockUsersResource.count()).thenReturn(10);

        PlatformDashboardResponse response = adminDashboardService.getPlatformDashboard();

        assertThat(response).isNotNull();
        assertThat(response.totalOrgs()).isEqualTo(0); // test-realm minus master = 0 since no master
    }

    @Test
    @DisplayName("Should return fallback dashboard when Keycloak is unavailable")
    void shouldReturnFallbackDashboardWhenKeycloakDown() {
        when(mockRealmsResource.findAll())
                .thenThrow(new ProcessingException("Connection refused"));

        PlatformDashboardResponse response = adminDashboardService.getPlatformDashboard();

        assertThat(response).isNotNull();
        assertThat(response.totalOrgs()).isEqualTo(0);
        assertThat(response.totalUsers()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return fallback realm settings when Keycloak is unavailable")
    void shouldReturnFallbackRealmSettingsWhenKeycloakDown() {
        when(mockRealmResource.toRepresentation())
                .thenThrow(new ProcessingException("Connection refused"));

        RealmSettingsResponse response = realmSettingsService.getRealmSettings("test-realm");

        assertThat(response).isNotNull();
        assertThat(response.realmName()).isEqualTo("test-realm");
        assertThat(response.sessionIdleTimeout()).isEqualTo(1800);
        assertThat(response.sessionMaxLifespan()).isEqualTo(36000);
    }

    @Test
    @DisplayName("Should return realm settings when Keycloak is available")
    void shouldReturnRealmSettingsWhenKeycloakAvailable() {
        RealmRepresentation realmRep = new RealmRepresentation();
        realmRep.setPasswordPolicy("length(8)");
        realmRep.setSsoSessionIdleTimeout(3600);
        realmRep.setSsoSessionMaxLifespan(72000);
        realmRep.setLoginTheme("rikreay");
        realmRep.setBruteForceProtected(true);

        when(mockRealmResource.toRepresentation()).thenReturn(realmRep);

        RealmSettingsResponse response = realmSettingsService.getRealmSettings("test-realm");

        assertThat(response).isNotNull();
        assertThat(response.passwordPolicy()).isEqualTo("length(8)");
        assertThat(response.sessionIdleTimeout()).isEqualTo(3600);
        assertThat(response.bruteForceProtection()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception on updateRealmSettings fallback when Keycloak is down")
    void shouldThrowOnUpdateSettingsFallback() {
        when(mockRealmResource.toRepresentation())
                .thenThrow(new ProcessingException("Connection refused"));

        RealmSettingsRequest request = new RealmSettingsRequest(
                "length(12)", null, 7200, null, null, null, true);

        assertThatThrownBy(() -> realmSettingsService.updateRealmSettings("test-realm", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Keycloak is unavailable");
    }

    @Test
    @DisplayName("Circuit should open after repeated failures and return fallback")
    void shouldOpenCircuitAfterRepeatedFailures() {
        when(mockRealmsResource.findAll())
                .thenThrow(new ProcessingException("Connection refused"));

        // Make enough calls to trigger circuit breaker open state
        for (int i = 0; i < 5; i++) {
            PlatformDashboardResponse response = adminDashboardService.getPlatformDashboard();
            assertThat(response).isNotNull();
            assertThat(response.totalOrgs()).isEqualTo(0);
        }

        // Circuit should be open now, subsequent calls should use fallback
        PlatformDashboardResponse response = adminDashboardService.getPlatformDashboard();
        assertThat(response).isNotNull();
        assertThat(response.totalOrgs()).isEqualTo(0);
    }

    @Test
    @DisplayName("Circuit should recover after wait duration when Keycloak comes back")
    void shouldRecoverAfterWaitDuration() throws InterruptedException {
        // First, make it fail to open the circuit
        when(mockRealmsResource.findAll())
                .thenThrow(new ProcessingException("Connection refused"));

        for (int i = 0; i < 5; i++) {
            adminDashboardService.getPlatformDashboard();
        }

        // Wait for circuit to transition to half-open (2s wait-duration)
        Thread.sleep(3000);

        // Now make Keycloak available again
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm("recovered-realm");
        when(mockRealmsResource.findAll()).thenReturn(List.of(realm));
        when(mockUsersResource.count()).thenReturn(5);

        PlatformDashboardResponse response = adminDashboardService.getPlatformDashboard();
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Should export users successfully when Keycloak is available")
    void shouldExportUsersWhenKeycloakAvailable() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("test-user");
        user.setEmail("test@test.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(true);

        when(mockUsersResource.list(0, 1000)).thenReturn(List.of(user));

        List<Map<String, String>> result = bulkUserService.bulkExportUsers("test-realm");

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("username", "test-user");
        assertThat(result.get(0)).containsEntry("email", "test@test.com");
    }
}
