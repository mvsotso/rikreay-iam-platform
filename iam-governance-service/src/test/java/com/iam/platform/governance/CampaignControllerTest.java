package com.iam.platform.governance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.governance.dto.CampaignRequest;
import com.iam.platform.governance.dto.CampaignResponse;
import com.iam.platform.governance.enums.CampaignStatus;
import com.iam.platform.governance.service.CampaignService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CampaignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CampaignService campaignService;

    @Test
    void createCampaign_asGovernanceAdmin_shouldSucceed() throws Exception {
        CampaignRequest request = new CampaignRequest("Q1 Review", "Quarterly access review",
                Instant.now(), Instant.now().plusSeconds(86400 * 30), null);

        CampaignResponse response = new CampaignResponse(UUID.randomUUID(), "Q1 Review",
                "Quarterly access review", CampaignStatus.DRAFT, Instant.now(),
                Instant.now().plusSeconds(86400 * 30), null, "gov-admin", 50, 50, Instant.now());

        when(campaignService.createCampaign(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/governance/campaigns")
                        .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Q1 Review"));
    }

    @Test
    void createCampaign_asTenantAdmin_shouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/governance/campaigns")
                        .with(JwtTestUtils.jwtWithRoles("tenant-user", TestConstants.ROLE_TENANT_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listCampaigns_asIamAdmin_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/governance/campaigns")
                        .with(JwtTestUtils.jwtWithRoles("admin", TestConstants.ROLE_IAM_ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void listCampaigns_asReportViewer_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/governance/campaigns")
                        .with(JwtTestUtils.jwtWithRoles("viewer", TestConstants.ROLE_REPORT_VIEWER)))
                .andExpect(status().isForbidden());
    }
}
