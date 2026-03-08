package com.iam.platform.admin;

import com.iam.platform.admin.dto.PlatformDashboardResponse;
import com.iam.platform.admin.service.AdminDashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AdminServiceTest {

    @Autowired
    private AdminDashboardService dashboardService;

    @Test
    @DisplayName("Dashboard should return aggregated stats")
    void dashboardReturnsStats() {
        PlatformDashboardResponse response = dashboardService.getPlatformDashboard();

        assertThat(response).isNotNull();
        assertThat(response.totalOrgs()).isGreaterThanOrEqualTo(0);
        assertThat(response.orgsByMemberClass()).isNotNull();
    }
}
