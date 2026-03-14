package com.iam.platform.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.notification.controller.TemplateController;
import com.iam.platform.notification.dto.TemplateRequest;
import com.iam.platform.notification.dto.TemplateResponse;
import com.iam.platform.notification.enums.ChannelType;
import com.iam.platform.notification.service.TemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.iam.platform.common.security.IamSecurityAutoConfiguration;
import com.iam.platform.notification.config.SecurityConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TemplateController.class)
@Import({SecurityConfig.class, IamSecurityAutoConfiguration.class})
@ActiveProfiles("test")
class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TemplateService templateService;

    private static final TemplateResponse SAMPLE_TEMPLATE = new TemplateResponse(
            UUID.randomUUID(), "welcome-email", "Welcome to RikReay",
            "<p>Welcome ${name}!</p>", ChannelType.EMAIL,
            List.of("name"), Instant.now(), Instant.now()
    );

    @Test
    @WithMockUser(roles = "iam-admin")
    void createTemplate_asIamAdmin_shouldSucceed() throws Exception {
        TemplateRequest request = new TemplateRequest(
                "welcome-email", "Welcome to RikReay",
                "<p>Welcome ${name}!</p>", ChannelType.EMAIL, List.of("name"));

        when(templateService.createTemplate(any())).thenReturn(SAMPLE_TEMPLATE);

        mockMvc.perform(post("/api/v1/notifications/templates")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("welcome-email"));
    }

    @Test
    @WithMockUser(roles = "iam-admin")
    void listTemplates_shouldSucceed() throws Exception {
        when(templateService.listTemplates(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(SAMPLE_TEMPLATE)));

        mockMvc.perform(get("/api/v1/notifications/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "ops-admin")
    void createTemplate_asOpsAdmin_shouldBeForbidden() throws Exception {
        TemplateRequest request = new TemplateRequest(
                "test", "Test", "<p>Test</p>", ChannelType.EMAIL, List.of());

        mockMvc.perform(post("/api/v1/notifications/templates")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "iam-admin")
    void deleteTemplate_asIamAdmin_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/v1/notifications/templates/" + UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isOk());
    }
}
