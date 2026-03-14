package com.iam.platform.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.admin.dto.RealmSettingsRequest;
import com.iam.platform.admin.dto.RealmSettingsResponse;
import com.iam.platform.admin.service.RealmSettingsService;
import com.iam.platform.common.test.ApiResponseAssertions;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("OrgSettingsController Tests")
class OrgSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RealmSettingsService realmSettingsService;

    @SuppressWarnings("unused")
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final RealmSettingsResponse SAMPLE_SETTINGS = new RealmSettingsResponse(
            TestConstants.TEST_REALM_NAME, "length(8)", true, 1800, 36000,
            "keycloak", List.of("http://localhost:3000/*"), true
    );

    @Nested
    @DisplayName("GET /api/v1/platform-admin/org/settings")
    class GetSettings {

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/org/settings")
                            .param("realmName", TestConstants.TEST_REALM_NAME))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 200 with settings for tenant-admin")
        void tenantAdmin_returnsSettings() throws Exception {
            when(realmSettingsService.getRealmSettings(TestConstants.TEST_REALM_NAME))
                    .thenReturn(SAMPLE_SETTINGS);

            mockMvc.perform(get("/api/v1/platform-admin/org/settings")
                            .param("realmName", TestConstants.TEST_REALM_NAME)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.realmName").value(TestConstants.TEST_REALM_NAME))
                    .andExpect(jsonPath("$.data.passwordPolicy").value("length(8)"))
                    .andExpect(jsonPath("$.data.mfaRequired").value(true))
                    .andExpect(jsonPath("$.data.sessionIdleTimeout").value(1800))
                    .andExpect(jsonPath("$.data.bruteForceProtection").value(true));
        }

        @Test
        @DisplayName("Should return 403 for iam-admin (org endpoints are tenant-admin only)")
        void iamAdmin_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/org/settings")
                            .param("realmName", TestConstants.TEST_REALM_NAME)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 for external-user")
        void externalUser_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/org/settings")
                            .param("realmName", TestConstants.TEST_REALM_NAME)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/platform-admin/org/settings")
    class UpdateSettings {

        @Test
        @DisplayName("Should return 200 for tenant-admin updating settings")
        void tenantAdmin_updatesSettings() throws Exception {
            RealmSettingsRequest request = new RealmSettingsRequest(
                    "length(12)", true, 900, 18000, "custom-theme", List.of(), true
            );

            when(realmSettingsService.updateRealmSettings(eq(TestConstants.TEST_REALM_NAME), any()))
                    .thenReturn(SAMPLE_SETTINGS);

            mockMvc.perform(put("/api/v1/platform-admin/org/settings")
                            .param("realmName", TestConstants.TEST_REALM_NAME)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Settings updated"));
        }

        @Test
        @DisplayName("Should return 403 for sector-admin")
        void sectorAdmin_forbidden() throws Exception {
            RealmSettingsRequest request = new RealmSettingsRequest(
                    "length(8)", null, null, null, null, null, null
            );

            mockMvc.perform(put("/api/v1/platform-admin/org/settings")
                            .param("realmName", TestConstants.TEST_REALM_NAME)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_SECTOR_ADMIN, TestConstants.ROLE_SECTOR_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verify(realmSettingsService, never()).updateRealmSettings(any(), any());
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void unauthenticated_returns401() throws Exception {
            RealmSettingsRequest request = new RealmSettingsRequest(
                    "length(8)", null, null, null, null, null, null
            );

            mockMvc.perform(put("/api/v1/platform-admin/org/settings")
                            .param("realmName", TestConstants.TEST_REALM_NAME)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
