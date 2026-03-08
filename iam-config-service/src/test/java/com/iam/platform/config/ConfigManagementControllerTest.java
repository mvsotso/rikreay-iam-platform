package com.iam.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.config.controller.ConfigManagementController;
import com.iam.platform.config.dto.ConfigChangeLogResponse;
import com.iam.platform.config.dto.ConfigUpdateRequest;
import com.iam.platform.config.service.ConfigVersionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConfigManagementController.class)
@ActiveProfiles("test")
class ConfigManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ConfigVersionService configVersionService;

    private static final ConfigChangeLogResponse SAMPLE_LOG = new ConfigChangeLogResponse(
            UUID.randomUUID(), 1L, "iam-core-service", "dev",
            Map.of("key", "value"), "admin", "UPDATE", Instant.now()
    );

    @Test
    @WithMockUser(roles = "config-admin")
    void updateConfig_asConfigAdmin_shouldSucceed() throws Exception {
        ConfigUpdateRequest request = new ConfigUpdateRequest(Map.of("key", "value"));

        when(configVersionService.recordChange(eq("iam-core-service"), eq("dev"), any(), eq("UPDATE")))
                .thenReturn(SAMPLE_LOG);

        mockMvc.perform(put("/api/v1/config/iam-core-service/dev")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.version").value(1));
    }

    @Test
    @WithMockUser(roles = "iam-admin")
    void getHistory_asIamAdmin_shouldSucceed() throws Exception {
        Page<ConfigChangeLogResponse> page = new PageImpl<>(List.of(SAMPLE_LOG));
        when(configVersionService.getHistory(any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/config/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "config-admin")
    void rollback_asConfigAdmin_shouldSucceed() throws Exception {
        ConfigChangeLogResponse rollbackLog = new ConfigChangeLogResponse(
                UUID.randomUUID(), 2L, "iam-core-service", "dev",
                Map.of("key", "value"), "admin", "ROLLBACK", Instant.now());

        when(configVersionService.rollback(1L)).thenReturn(rollbackLog);

        mockMvc.perform(post("/api/v1/config/rollback/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.changeType").value("ROLLBACK"));
    }

    @Test
    @WithMockUser(roles = "external-user")
    void updateConfig_asExternalUser_shouldBeForbidden() throws Exception {
        ConfigUpdateRequest request = new ConfigUpdateRequest(Map.of("key", "value"));

        mockMvc.perform(put("/api/v1/config/iam-core-service/dev")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "config-admin")
    void getLatestConfig_shouldReturn() throws Exception {
        when(configVersionService.getLatestVersion("iam-core-service", "dev")).thenReturn(SAMPLE_LOG);

        mockMvc.perform(get("/api/v1/config/iam-core-service/dev/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.application").value("iam-core-service"));
    }
}
