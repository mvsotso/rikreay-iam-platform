package com.iam.platform.governance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.governance.controller.CampaignController;
import com.iam.platform.governance.dto.CampaignRequest;
import com.iam.platform.governance.dto.CampaignResponse;
import com.iam.platform.governance.enums.CampaignStatus;
import com.iam.platform.governance.service.CampaignService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CampaignController.class)
@ActiveProfiles("test")
class CampaignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CampaignService campaignService;

    @Test
    @WithMockUser(roles = "governance-admin")
    void createCampaign_asGovernanceAdmin_shouldSucceed() throws Exception {
        CampaignRequest request = new CampaignRequest("Q1 Review", "Quarterly access review",
                Instant.now(), Instant.now().plusSeconds(86400 * 30), Map.of());

        CampaignResponse response = new CampaignResponse(UUID.randomUUID(), "Q1 Review",
                "Quarterly access review", CampaignStatus.DRAFT, Instant.now(),
                Instant.now().plusSeconds(86400 * 30), Map.of(), "gov-admin", 50, 50, Instant.now());

        when(campaignService.createCampaign(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/governance/campaigns")
                        .with(jwt().jwt(j -> j.claim("preferred_username", "gov-admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Q1 Review"));
    }

    @Test
    @WithMockUser(roles = "tenant-admin")
    void createCampaign_asTenantAdmin_shouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/governance/campaigns")
                        .with(jwt().jwt(j -> j.claim("preferred_username", "tenant-admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "iam-admin")
    void listCampaigns_asIamAdmin_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/governance/campaigns"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "report-viewer")
    void listCampaigns_asReportViewer_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/governance/campaigns"))
                .andExpect(status().isForbidden());
    }
}
