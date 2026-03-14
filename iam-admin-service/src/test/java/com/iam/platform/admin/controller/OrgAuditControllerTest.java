package com.iam.platform.admin.controller;

import com.iam.platform.common.test.ApiResponseAssertions;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("OrgAuditController Tests")
class OrgAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Nested
    @DisplayName("GET /api/v1/platform-admin/org/audit")
    class GetAuditEvents {

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/org/audit")
                            .param("tenantId", TestConstants.TEST_TENANT_ID))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 200 for tenant-admin")
        void tenantAdmin_returnsAuditEvents() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/org/audit")
                            .param("tenantId", TestConstants.TEST_TENANT_ID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("Should accept optional filter parameters")
        void tenantAdmin_withFilters() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/org/audit")
                            .param("tenantId", TestConstants.TEST_TENANT_ID)
                            .param("type", "USER_LOGIN")
                            .param("dateFrom", "2024-01-01")
                            .param("dateTo", "2024-12-31")
                            .param("username", "testuser")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Should return 403 for iam-admin")
        void iamAdmin_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/org/audit")
                            .param("tenantId", TestConstants.TEST_TENANT_ID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 for sector-admin")
        void sectorAdmin_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/org/audit")
                            .param("tenantId", TestConstants.TEST_TENANT_ID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_SECTOR_ADMIN, TestConstants.ROLE_SECTOR_ADMIN)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 for auditor")
        void auditor_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/org/audit")
                            .param("tenantId", TestConstants.TEST_TENANT_ID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_AUDITOR, TestConstants.ROLE_AUDITOR)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/platform-admin/org/audit/export")
    class ExportAuditEvents {

        @Test
        @DisplayName("Should return 200 for tenant-admin")
        void tenantAdmin_exportSucceeds() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/org/audit/export")
                            .param("tenantId", TestConstants.TEST_TENANT_ID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Should return 403 for external-user")
        void externalUser_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/org/audit/export")
                            .param("tenantId", TestConstants.TEST_TENANT_ID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/platform-admin/org/audit/login-history")
    class LoginHistory {

        @Test
        @DisplayName("Should return 200 for tenant-admin")
        void tenantAdmin_returnsLoginHistory() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/org/audit/login-history")
                            .param("tenantId", TestConstants.TEST_TENANT_ID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Should accept optional filter parameters")
        void tenantAdmin_withFilters() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/org/audit/login-history")
                            .param("tenantId", TestConstants.TEST_TENANT_ID)
                            .param("username", "john")
                            .param("dateFrom", "2024-01-01")
                            .param("dateTo", "2024-06-30")
                            .param("status", "SUCCESS")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 403 for iam-admin")
        void iamAdmin_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/org/audit/login-history")
                            .param("tenantId", TestConstants.TEST_TENANT_ID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isForbidden());
        }
    }
}
