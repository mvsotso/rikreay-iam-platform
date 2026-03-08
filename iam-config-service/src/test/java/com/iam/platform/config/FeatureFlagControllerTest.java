package com.iam.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.config.controller.FeatureFlagController;
import com.iam.platform.config.dto.FeatureFlagRequest;
import com.iam.platform.config.dto.FeatureFlagResponse;
import com.iam.platform.config.service.FeatureFlagService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeatureFlagController.class)
@ActiveProfiles("test")
class FeatureFlagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeatureFlagService featureFlagService;

    private static final FeatureFlagResponse SAMPLE_FLAG = new FeatureFlagResponse(
            UUID.randomUUID(), "test-flag", "true", "Test flag",
            true, "dev", Instant.now(), Instant.now()
    );

    @Test
    @WithMockUser(roles = "config-admin")
    void createFlag_asConfigAdmin_shouldSucceed() throws Exception {
        FeatureFlagRequest request = new FeatureFlagRequest(
                "test-flag", "true", "Test flag", true, "dev");

        when(featureFlagService.createFlag(any())).thenReturn(SAMPLE_FLAG);

        mockMvc.perform(post("/api/v1/config/flags")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.flagKey").value("test-flag"));
    }

    @Test
    @WithMockUser(roles = "iam-admin")
    void listFlags_asIamAdmin_shouldSucceed() throws Exception {
        Page<FeatureFlagResponse> page = new PageImpl<>(List.of(SAMPLE_FLAG));
        when(featureFlagService.listFlags(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/config/flags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "config-admin")
    void getFlag_shouldReturnFlag() throws Exception {
        when(featureFlagService.getFlag("test-flag", "dev")).thenReturn(SAMPLE_FLAG);

        mockMvc.perform(get("/api/v1/config/flags/test-flag")
                        .param("environment", "dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.flagKey").value("test-flag"));
    }

    @Test
    @WithMockUser(roles = "config-admin")
    void toggleFlag_shouldToggle() throws Exception {
        FeatureFlagResponse toggled = new FeatureFlagResponse(
                SAMPLE_FLAG.id(), "test-flag", "true", "Test flag",
                false, "dev", SAMPLE_FLAG.createdAt(), Instant.now());

        when(featureFlagService.toggleFlag("test-flag", "dev")).thenReturn(toggled);

        mockMvc.perform(put("/api/v1/config/flags/test-flag/toggle")
                        .with(csrf())
                        .param("environment", "dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    @WithMockUser(roles = "external-user")
    void createFlag_asExternalUser_shouldBeForbidden() throws Exception {
        FeatureFlagRequest request = new FeatureFlagRequest(
                "test-flag", "true", "Test flag", true, "dev");

        mockMvc.perform(post("/api/v1/config/flags")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createFlag_unauthenticated_shouldBeUnauthorized() throws Exception {
        FeatureFlagRequest request = new FeatureFlagRequest(
                "test-flag", "true", "Test flag", true, "dev");

        mockMvc.perform(post("/api/v1/config/flags")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "config-admin")
    void deleteFlag_asConfigAdmin_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/v1/config/flags/test-flag")
                        .with(csrf())
                        .param("environment", "dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
