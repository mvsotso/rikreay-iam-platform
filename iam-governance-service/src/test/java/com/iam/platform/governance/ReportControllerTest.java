package com.iam.platform.governance;

import com.iam.platform.governance.controller.ReportController;
import com.iam.platform.governance.dto.ComplianceReportDto;
import com.iam.platform.governance.service.ComplianceReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@ActiveProfiles("test")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComplianceReportService reportService;

    @Test
    @WithMockUser(roles = "report-viewer")
    void getComplianceReport_asReportViewer_shouldSucceed() throws Exception {
        ComplianceReportDto report = new ComplianceReportDto("COMPLIANCE_OVERVIEW",
                Instant.now(), 100, 80, 5, 10, 2, 3, 15, 50, 45, Map.of());

        when(reportService.generateComplianceReport()).thenReturn(report);

        mockMvc.perform(get("/api/v1/governance/reports/compliance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportType").value("COMPLIANCE_OVERVIEW"));
    }

    @Test
    @WithMockUser(roles = "governance-admin")
    void getRiskReport_asGovernanceAdmin_shouldSucceed() throws Exception {
        ComplianceReportDto report = new ComplianceReportDto("RISK_ASSESSMENT",
                Instant.now(), 0, 80, 5, 0, 0, 0, 0, 0, 0,
                Map.of("highRisk", 5, "mediumRisk", 20, "lowRisk", 55));

        when(reportService.generateRiskReport()).thenReturn(report);

        mockMvc.perform(get("/api/v1/governance/reports/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportType").value("RISK_ASSESSMENT"));
    }

    @Test
    @WithMockUser(roles = "tenant-admin")
    void getReports_asTenantAdmin_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/governance/reports/compliance"))
                .andExpect(status().isForbidden());
    }
}
