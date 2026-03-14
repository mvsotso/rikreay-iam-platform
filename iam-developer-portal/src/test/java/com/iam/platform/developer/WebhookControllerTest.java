package com.iam.platform.developer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.common.security.IamSecurityAutoConfiguration;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.developer.config.SecurityConfig;
import com.iam.platform.developer.controller.WebhookController;
import com.iam.platform.developer.dto.WebhookConfigRequest;
import com.iam.platform.developer.dto.WebhookConfigResponse;
import com.iam.platform.developer.service.WebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookController.class)
@Import({SecurityConfig.class, IamSecurityAutoConfiguration.class})
@ActiveProfiles("test")
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WebhookService webhookService;

    @Test
    void createWebhook_asDeveloper_shouldSucceed() throws Exception {
        UUID appId = UUID.randomUUID();
        WebhookConfigRequest request = new WebhookConfigRequest(
                appId, "USER_CREATED", "https://example.com/webhook", "mysecret");

        WebhookConfigResponse response = new WebhookConfigResponse(
                UUID.randomUUID(), appId, "USER_CREATED",
                "https://example.com/webhook", true, Instant.now());

        when(webhookService.createWebhook(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/webhooks")
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_DEVELOPER, TestConstants.ROLE_DEVELOPER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventType").value("USER_CREATED"));
    }

    @Test
    void createWebhook_asExternalUser_shouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks")
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDeliveryLogs_asDeveloper_shouldSucceed() throws Exception {
        UUID webhookId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/webhooks/" + webhookId + "/deliveries")
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_DEVELOPER, TestConstants.ROLE_DEVELOPER)))
                .andExpect(status().isOk());
    }
}
