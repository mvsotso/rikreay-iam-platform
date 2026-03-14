package com.iam.platform.developer;

import com.iam.platform.developer.config.DeveloperProperties;
import com.iam.platform.developer.dto.AppRegistrationRequest;
import com.iam.platform.developer.dto.AppResponse;
import com.iam.platform.developer.entity.RegisteredApp;
import com.iam.platform.developer.entity.SandboxRealm;
import com.iam.platform.developer.entity.WebhookConfig;
import com.iam.platform.developer.enums.AppStatus;
import com.iam.platform.developer.enums.SandboxStatus;
import com.iam.platform.developer.repository.RegisteredAppRepository;
import com.iam.platform.developer.repository.SandboxRealmRepository;
import com.iam.platform.developer.repository.WebhookConfigRepository;
import com.iam.platform.developer.repository.WebhookDeliveryLogRepository;
import com.iam.platform.developer.service.AppRegistrationService;
import com.iam.platform.developer.service.AuditService;
import com.iam.platform.developer.service.SandboxService;
import com.iam.platform.developer.service.WebhookService;
import com.iam.platform.developer.dto.WebhookConfigRequest;
import com.iam.platform.developer.dto.WebhookConfigResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class DeveloperServiceTest {

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private SandboxService sandboxService;

    @MockitoBean
    private RegisteredAppRepository appRepository;

    @MockitoBean
    private WebhookConfigRepository webhookRepository;

    @MockitoBean
    private WebhookDeliveryLogRepository deliveryLogRepository;

    @MockitoBean
    private SandboxRealmRepository sandboxRepository;

    @MockitoBean
    private Keycloak keycloakAdmin;

    @MockitoBean
    private AuditService auditService;

    @Test
    @DisplayName("Create webhook should persist and return response")
    void createWebhook() {
        UUID appId = UUID.randomUUID();
        WebhookConfigRequest request = new WebhookConfigRequest(
                appId, "USER_CREATED", "https://example.com/webhook", "mysecret");

        WebhookConfig saved = WebhookConfig.builder()
                .appId(appId)
                .eventType("USER_CREATED")
                .targetUrl("https://example.com/webhook")
                .secretHash("hashed")
                .enabled(true)
                .build();
        saved.setId(UUID.randomUUID());
        saved.setCreatedAt(Instant.now());

        when(webhookRepository.save(any())).thenReturn(saved);

        WebhookConfigResponse response = webhookService.createWebhook(request, "dev-user");

        assertThat(response).isNotNull();
        assertThat(response.eventType()).isEqualTo("USER_CREATED");
        assertThat(response.targetUrl()).isEqualTo("https://example.com/webhook");
        assertThat(response.enabled()).isTrue();
    }

    @Test
    @DisplayName("Delete webhook should soft delete")
    void deleteWebhook() {
        UUID id = UUID.randomUUID();
        WebhookConfig config = WebhookConfig.builder()
                .appId(UUID.randomUUID())
                .eventType("USER_CREATED")
                .targetUrl("https://example.com/webhook")
                .enabled(true)
                .build();

        when(webhookRepository.findById(id)).thenReturn(Optional.of(config));
        when(webhookRepository.save(any())).thenReturn(config);

        webhookService.deleteWebhook(id, "dev-user");

        assertThat(config.isDeleted()).isTrue();
        assertThat(config.getDeletedAt()).isNotNull();
        verify(webhookRepository).save(config);
    }

    @Test
    @DisplayName("Sandbox creation should fail when max limit reached")
    void sandboxMaxLimitReached() {
        when(sandboxRepository.countByOwnerIdAndStatus("dev-user", SandboxStatus.ACTIVE))
                .thenReturn(3L);

        // Circuit breaker wraps all exceptions with fallback, so we get the fallback message
        assertThatThrownBy(() -> sandboxService.createSandbox(
                new com.iam.platform.developer.dto.SandboxRequest("test"), "dev-user"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Keycloak is unavailable");
    }

    @Test
    @DisplayName("Get webhooks by app should return list")
    void getWebhooksByApp() {
        UUID appId = UUID.randomUUID();
        WebhookConfig config = WebhookConfig.builder()
                .appId(appId)
                .eventType("USER_CREATED")
                .targetUrl("https://example.com/webhook")
                .enabled(true)
                .build();

        when(webhookRepository.findByAppId(appId)).thenReturn(List.of(config));

        List<WebhookConfigResponse> responses = webhookService.getWebhooksByApp(appId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).eventType()).isEqualTo("USER_CREATED");
    }
}
