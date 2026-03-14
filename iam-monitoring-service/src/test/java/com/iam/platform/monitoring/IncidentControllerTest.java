package com.iam.platform.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.monitoring.dto.IncidentRequest;
import com.iam.platform.monitoring.dto.IncidentResponse;
import com.iam.platform.monitoring.enums.IncidentStatus;
import com.iam.platform.monitoring.enums.Severity;
import com.iam.platform.monitoring.service.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IncidentService incidentService;

    @Test
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
                        .with(JwtTestUtils.jwtWithRoles("ops-user", TestConstants.ROLE_OPS_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Gateway Down"));
    }

    @Test
    void getIncident_asIamAdmin_shouldSucceed() throws Exception {
        UUID id = UUID.randomUUID();
        IncidentResponse response = new IncidentResponse(
                id, "DB Slow", Severity.MEDIUM, IncidentStatus.INVESTIGATING,
                "Database queries slow", "iam-core-service", null,
                null, Instant.now(), Instant.now());

        when(incidentService.getIncident(eq(id))).thenReturn(response);

        mockMvc.perform(get("/api/v1/monitoring/incidents/" + id)
                        .with(JwtTestUtils.jwtWithRoles("admin", TestConstants.ROLE_IAM_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("DB Slow"));
    }

    @Test
    void listIncidents_asTenantAdmin_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/incidents")
                        .with(JwtTestUtils.jwtWithRoles("tenant-user", TestConstants.ROLE_TENANT_ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listIncidents_asOpsAdmin_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/incidents")
                        .with(JwtTestUtils.jwtWithRoles("ops-user", TestConstants.ROLE_OPS_ADMIN)))
                .andExpect(status().isOk());
    }
}
