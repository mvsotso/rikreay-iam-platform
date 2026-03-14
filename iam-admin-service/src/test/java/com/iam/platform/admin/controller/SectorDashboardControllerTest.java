package com.iam.platform.admin.controller;

import com.iam.platform.admin.dto.SectorDashboardResponse;
import com.iam.platform.admin.service.SectorDashboardService;
import com.iam.platform.common.enums.MemberClass;
import com.iam.platform.common.test.ApiResponseAssertions;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SectorDashboardController Tests")
class SectorDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SectorDashboardService sectorDashboardService;

    @SuppressWarnings("unused")
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final SectorDashboardResponse SAMPLE_DASHBOARD = new SectorDashboardResponse(
            "GOV", 15, 500, 12, 3, 25000, 5000
    );

    @Nested
    @DisplayName("GET /api/v1/platform-admin/sector/dashboard")
    class GetDashboard {

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/sector/dashboard")
                            .param("memberClass", "GOV"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 200 for sector-admin")
        void sectorAdmin_returnsDashboard() throws Exception {
            when(sectorDashboardService.getSectorDashboard(MemberClass.GOV)).thenReturn(SAMPLE_DASHBOARD);

            mockMvc.perform(get("/api/v1/platform-admin/sector/dashboard")
                            .param("memberClass", "GOV")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_SECTOR_ADMIN, TestConstants.ROLE_SECTOR_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.memberClass").value("GOV"))
                    .andExpect(jsonPath("$.data.orgCount").value(15))
                    .andExpect(jsonPath("$.data.totalUsers").value(500))
                    .andExpect(jsonPath("$.data.activeOrgs").value(12))
                    .andExpect(jsonPath("$.data.suspendedOrgs").value(3));
        }

        @ParameterizedTest
        @EnumSource(MemberClass.class)
        @DisplayName("Should accept all member classes")
        void allMemberClasses_accepted(MemberClass memberClass) throws Exception {
            SectorDashboardResponse response = new SectorDashboardResponse(
                    memberClass.name(), 10, 100, 8, 2, 5000, 1000
            );
            when(sectorDashboardService.getSectorDashboard(memberClass)).thenReturn(response);

            mockMvc.perform(get("/api/v1/platform-admin/sector/dashboard")
                            .param("memberClass", memberClass.name())
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_SECTOR_ADMIN, TestConstants.ROLE_SECTOR_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.memberClass").value(memberClass.name()));
        }

        @Test
        @DisplayName("Should return 403 for tenant-admin")
        void tenantAdmin_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/sector/dashboard")
                            .param("memberClass", "GOV")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isForbidden());

            verify(sectorDashboardService, never()).getSectorDashboard(any());
        }

        @Test
        @DisplayName("Should return 403 for iam-admin")
        void iamAdmin_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/sector/dashboard")
                            .param("memberClass", "GOV")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/platform-admin/sector/organizations")
    class GetOrganizations {

        @Test
        @DisplayName("Should return 200 for sector-admin")
        void sectorAdmin_returnsOrgs() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/sector/organizations")
                            .param("memberClass", "COM")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_SECTOR_ADMIN, TestConstants.ROLE_SECTOR_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Should return 403 for external-user")
        void externalUser_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/sector/organizations")
                            .param("memberClass", "COM")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/platform-admin/sector/audit")
    class GetSectorAudit {

        @Test
        @DisplayName("Should return 200 for sector-admin")
        void sectorAdmin_returnsAudit() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/sector/audit")
                            .param("memberClass", "NGO")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_SECTOR_ADMIN, TestConstants.ROLE_SECTOR_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/platform-admin/sector/compliance")
    class GetSectorCompliance {

        @Test
        @DisplayName("Should return 200 for sector-admin")
        void sectorAdmin_returnsCompliance() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/sector/compliance")
                            .param("memberClass", "MUN")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_SECTOR_ADMIN, TestConstants.ROLE_SECTOR_ADMIN)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/platform-admin/sector/reports")
    class GetSectorReports {

        @Test
        @DisplayName("Should return 200 for sector-admin")
        void sectorAdmin_returnsReports() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/sector/reports")
                            .param("memberClass", "GOV")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_SECTOR_ADMIN, TestConstants.ROLE_SECTOR_ADMIN)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/platform-admin/sector/organizations/{id}/approve")
    class ApproveOrg {

        @Test
        @DisplayName("Should return 200 for sector-admin")
        void sectorAdmin_approvesOrg() throws Exception {
            UUID orgId = UUID.randomUUID();

            mockMvc.perform(post("/api/v1/platform-admin/sector/organizations/" + orgId + "/approve")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_SECTOR_ADMIN, TestConstants.ROLE_SECTOR_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Should return 403 for tenant-admin")
        void tenantAdmin_forbidden() throws Exception {
            UUID orgId = UUID.randomUUID();

            mockMvc.perform(post("/api/v1/platform-admin/sector/organizations/" + orgId + "/approve")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/platform-admin/sector/organizations/{id}/reject")
    class RejectOrg {

        @Test
        @DisplayName("Should return 200 for sector-admin")
        void sectorAdmin_rejectsOrg() throws Exception {
            UUID orgId = UUID.randomUUID();

            mockMvc.perform(post("/api/v1/platform-admin/sector/organizations/" + orgId + "/reject")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_SECTOR_ADMIN, TestConstants.ROLE_SECTOR_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
