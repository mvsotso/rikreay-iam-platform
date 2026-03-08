package com.iam.platform.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.monitoring.controller.IncidentController;
import com.iam.platform.monitoring.dto.IncidentRequest;
import com.iam.platform.monitoring.dto.IncidentResponse;
import com.iam.platform.monitoring.enums.IncidentStatus;
import com.iam.platform.monitoring.enums.Severity;
import com.iam.platform.monitoring.service.IncidentService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IncidentController.class)
@ActiveProfiles("test")
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IncidentService incidentService;

    @Test
    @WithMockUser(roles = "ops-admin")
    void createIncident_asOpsAdmin_shouldSucceed() throws Exception {
        IncidentRequest request = new IncidentRequest(
                "Gateway Down", Severity.CRITICAL, "Gateway not responding",
                "iam-gateway", "admin");

        IncidentResponse response = new IncidentResponse(
                UUID.randomUUID(), "Gateway Down", Severity.CRITICAL, IncidentStatus.OPEN,
                "Gateway not responding", "iam-gateway", "admin",
                null, Instant.now(), Instant.now());

        when(incidentService.createIncident(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/monitoring/incidents")
                        .with(jwt().jwt(j -> j.claim("preferred_username", "ops-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Gateway Down"));
    }

    @Test
    @WithMockUser(roles = "iam-admin")
    void getIncident_asIamAdmin_shouldSucceed() throws Exception {
        UUID id = UUID.randomUUID();
        IncidentResponse response = new IncidentResponse(
                id, "DB Slow", Severity.MEDIUM, IncidentStatus.INVESTIGATING,
                "Database queries slow", "iam-core-service", null,
                null, Instant.now(), Instant.now());

        when(incidentService.getIncident(eq(id))).thenReturn(response);

        mockMvc.perform(get("/api/v1/monitoring/incidents/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("DB Slow"));
    }

    @Test
    @WithMockUser(roles = "tenant-admin")
    void listIncidents_asTenantAdmin_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/incidents"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ops-admin")
    void listIncidents_asOpsAdmin_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/incidents"))
                .andExpect(status().isOk());
    }
}
