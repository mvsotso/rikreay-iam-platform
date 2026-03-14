package com.iam.platform.admin;

import com.iam.platform.admin.config.SecurityConfig;
import com.iam.platform.admin.controller.PlatformDashboardController;
import com.iam.platform.admin.dto.PlatformDashboardResponse;
import com.iam.platform.admin.dto.UsageResponse;
import com.iam.platform.admin.service.AdminDashboardService;
import com.iam.platform.admin.service.UsageTrackingService;
import com.iam.platform.common.security.IamSecurityAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlatformDashboardController.class)
@Import({SecurityConfig.class, IamSecurityAutoConfiguration.class})
@ActiveProfiles("test")
class PlatformDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardService dashboardService;

    @MockitoBean
    private UsageTrackingService usageTrackingService;

    @Test
    @WithMockUser(roles = "iam-admin")
    void getDashboard_asIamAdmin_shouldSucceed() throws Exception {
        PlatformDashboardResponse response = new PlatformDashboardResponse(
                10, 500, Map.of("GOV", 4L), Map.of("GOV", 200L), 50, 10000);

        when(dashboardService.getPlatformDashboard()).thenReturn(response);

        mockMvc.perform(get("/api/v1/platform-admin/platform/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrgs").value(10));
    }

    @Test
    @WithMockUser(roles = "tenant-admin")
    void getDashboard_asTenantAdmin_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/platform/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "iam-admin")
    void getUsage_asIamAdmin_shouldSucceed() throws Exception {
        UsageResponse usage = new UsageResponse(Map.of(), Map.of(), Map.of(), 0);
        when(usageTrackingService.getUsageForTenant("platform")).thenReturn(usage);

        mockMvc.perform(get("/api/v1/platform-admin/platform/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
