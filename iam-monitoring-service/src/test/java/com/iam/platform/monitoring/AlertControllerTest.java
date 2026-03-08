package com.iam.platform.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.monitoring.controller.AlertController;
import com.iam.platform.monitoring.dto.AlertRuleRequest;
import com.iam.platform.monitoring.dto.AlertRuleResponse;
import com.iam.platform.monitoring.enums.ChannelType;
import com.iam.platform.monitoring.service.AlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
@ActiveProfiles("test")
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AlertService alertService;

    @Test
    @WithMockUser(roles = "ops-admin")
    void createAlertRule_asOpsAdmin_shouldSucceed() throws Exception {
        AlertRuleRequest request = new AlertRuleRequest(
                "Gateway Down Alert", "SERVICE_DOWN", "1",
                ChannelType.EMAIL, "iam-gateway", true);

        AlertRuleResponse response = new AlertRuleResponse(
                UUID.randomUUID(), "Gateway Down Alert", "SERVICE_DOWN", "1",
                ChannelType.EMAIL, "iam-gateway", true, null, Instant.now());

        when(alertService.createAlertRule(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/monitoring/alerts")
                        .with(jwt().jwt(j -> j.claim("preferred_username", "ops-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Gateway Down Alert"));
    }

    @Test
    @WithMockUser(roles = "tenant-admin")
    void listAlerts_asTenantAdmin_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/alerts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "iam-admin")
    void listAlerts_asIamAdmin_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/alerts"))
                .andExpect(status().isOk());
    }
}
