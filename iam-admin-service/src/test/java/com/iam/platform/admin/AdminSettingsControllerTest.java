package com.iam.platform.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.admin.controller.AdminSettingsController;
import com.iam.platform.admin.dto.PlatformSettingsRequest;
import com.iam.platform.admin.dto.PlatformSettingsResponse;
import com.iam.platform.admin.service.AdminSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminSettingsController.class)
@ActiveProfiles("test")
class AdminSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminSettingsService settingsService;

    private static final PlatformSettingsResponse SAMPLE_SETTING = new PlatformSettingsResponse(
            UUID.randomUUID(), "platform.name", "RikReay", "general",
            "Platform name", "admin", Instant.now()
    );

    @Test
    @WithMockUser(roles = "iam-admin")
    void getSettings_asIamAdmin_shouldSucceed() throws Exception {
        when(settingsService.getAllSettings()).thenReturn(List.of(SAMPLE_SETTING));

        mockMvc.perform(get("/api/v1/platform-admin/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "config-admin")
    void getSettings_asConfigAdmin_shouldSucceed() throws Exception {
        when(settingsService.getAllSettings()).thenReturn(List.of(SAMPLE_SETTING));

        mockMvc.perform(get("/api/v1/platform-admin/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "iam-admin")
    void updateSetting_asIamAdmin_shouldSucceed() throws Exception {
        PlatformSettingsRequest request = new PlatformSettingsRequest(
                "platform.name", "RikReay v2", "general", "Updated name");

        when(settingsService.saveSetting(any())).thenReturn(SAMPLE_SETTING);

        mockMvc.perform(put("/api/v1/platform-admin/settings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "external-user")
    void getSettings_asExternalUser_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/settings"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSettings_unauthenticated_shouldBeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/platform-admin/settings"))
                .andExpect(status().isUnauthorized());
    }
}
