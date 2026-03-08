package com.iam.platform.developer.workflow;

import com.iam.platform.developer.dto.AppRegistrationRequest;
import com.iam.platform.developer.dto.AppResponse;
import com.iam.platform.developer.dto.WebhookConfigRequest;
import com.iam.platform.developer.dto.WebhookConfigResponse;
import com.iam.platform.developer.entity.RegisteredApp;
import com.iam.platform.developer.enums.AppStatus;
import com.iam.platform.developer.repository.RegisteredAppRepository;
import com.iam.platform.developer.repository.WebhookConfigRepository;
import com.iam.platform.developer.service.AppRegistrationService;
import com.iam.platform.developer.service.AuditService;
import com.iam.platform.developer.service.WebhookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Workflow: Developer App Registration & Webhooks")
class DeveloperAppWorkflowTest {

    @Autowired
    private AppRegistrationService appService;

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private RegisteredAppRepository appRepository;

    @Autowired
    private WebhookConfigRepository webhookRepository;

    @MockitoBean
    private Keycloak keycloakAdmin;

    @MockitoBean
    private AuditService auditService;

    @Test
    @DisplayName("E2E: Register app → verify Keycloak client creation → configure webhook → verify audit trail")
    void fullAppRegistrationWorkflow() {
        // Arrange — mock Keycloak client creation
        RealmResource realmResource = mock(RealmResource.class);
        ClientsResource clientsResource = mock(ClientsResource.class);
        Response createResponse = mock(Response.class);

        when(keycloakAdmin.realm(anyString())).thenReturn(realmResource);
        when(realmResource.clients()).thenReturn(clientsResource);
        when(clientsResource.create(any())).thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(201);
        when(createResponse.getLocation()).thenReturn(URI.create("http://localhost:8080/admin/realms/iam-platform/clients/test-client-id"));

        // Step 1: Register app
        AppRegistrationRequest request = new AppRegistrationRequest(
                "Tax Filing Portal",
                "GDT tax filing integration for citizens",
                List.of("https://tax.gdt.gov.kh/callback", "https://tax.gdt.gov.kh/redirect"));

        AppResponse app = appService.registerApp(request, "dev.user");

        // Assert — app created with correct details
        assertThat(app).isNotNull();
        assertThat(app.name()).isEqualTo("Tax Filing Portal");
        assertThat(app.status()).isEqualTo(AppStatus.ACTIVE);
        assertThat(app.ownerId()).isEqualTo("dev.user");

        // Verify Keycloak client creation attempted
        verify(clientsResource).create(any());

        // Verify audit event
        verify(auditService).logDeveloperAction(
                eq("dev.user"), eq("APP_REGISTERED"), anyString(), eq(true), any());

        // Step 2: Configure webhook for the app
        WebhookConfigRequest webhookRequest = new WebhookConfigRequest(
                app.id(),
                "USER_CREATED",
                "https://tax.gdt.gov.kh/webhooks/user-created",
                "webhook-secret-123");

        WebhookConfigResponse webhook = webhookService.createWebhook(webhookRequest, "dev.user");

        // Assert — webhook configured
        assertThat(webhook).isNotNull();
        assertThat(webhook.eventType()).isEqualTo("USER_CREATED");
        assertThat(webhook.targetUrl()).isEqualTo("https://tax.gdt.gov.kh/webhooks/user-created");

        // Verify webhook audit
        verify(auditService).logDeveloperAction(
                eq("dev.user"), eq("WEBHOOK_CREATED"), anyString(), eq(true), any());
    }

    @Test
    @DisplayName("E2E: Register app → suspend app → verify status change")
    void appRegistrationAndSuspension() {
        RealmResource realmResource = mock(RealmResource.class);
        ClientsResource clientsResource = mock(ClientsResource.class);
        Response createResponse = mock(Response.class);

        when(keycloakAdmin.realm(anyString())).thenReturn(realmResource);
        when(realmResource.clients()).thenReturn(clientsResource);
        when(clientsResource.create(any())).thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(201);
        when(createResponse.getLocation()).thenReturn(URI.create("http://localhost:8080/admin/realms/iam-platform/clients/test-id-2"));

        AppRegistrationRequest request = new AppRegistrationRequest(
                "Mobile Banking App", "Banking integration", List.of("https://aba.com.kh/callback"));

        AppResponse app = appService.registerApp(request, "dev.user");
        assertThat(app.status()).isEqualTo(AppStatus.ACTIVE);

        // Suspend
        appService.suspendApp(app.id(), "iam-admin");

        // Verify suspended in DB
        RegisteredApp suspendedApp = appRepository.findById(app.id()).orElseThrow();
        assertThat(suspendedApp.getStatus()).isEqualTo(AppStatus.SUSPENDED);
    }

    @Test
    @DisplayName("E2E: Register app → create multiple webhooks → list by app")
    void multipleWebhooksPerApp() {
        RealmResource realmResource = mock(RealmResource.class);
        ClientsResource clientsResource = mock(ClientsResource.class);
        Response createResponse = mock(Response.class);

        when(keycloakAdmin.realm(anyString())).thenReturn(realmResource);
        when(realmResource.clients()).thenReturn(clientsResource);
        when(clientsResource.create(any())).thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(201);
        when(createResponse.getLocation()).thenReturn(URI.create("http://localhost:8080/admin/realms/iam-platform/clients/test-id-3"));

        AppResponse app = appService.registerApp(
                new AppRegistrationRequest("Multi-Hook App", "Test", List.of("https://example.com/cb")),
                "dev.user");

        // Create 3 webhooks
        webhookService.createWebhook(new WebhookConfigRequest(
                app.id(), "USER_CREATED", "https://example.com/hook1", "s1"), "dev.user");
        webhookService.createWebhook(new WebhookConfigRequest(
                app.id(), "USER_UPDATED", "https://example.com/hook2", "s2"), "dev.user");
        webhookService.createWebhook(new WebhookConfigRequest(
                app.id(), "ROLE_CHANGED", "https://example.com/hook3", "s3"), "dev.user");

        // Verify all 3 webhooks exist
        List<WebhookConfigResponse> webhooks = webhookService.getWebhooksByApp(app.id());
        assertThat(webhooks).hasSize(3);
        assertThat(webhooks).extracting(WebhookConfigResponse::eventType)
                .containsExactlyInAnyOrder("USER_CREATED", "USER_UPDATED", "ROLE_CHANGED");
    }
}
