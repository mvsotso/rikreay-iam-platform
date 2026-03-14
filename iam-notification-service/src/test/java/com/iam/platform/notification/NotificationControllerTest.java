package com.iam.platform.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.notification.controller.NotificationController;
import com.iam.platform.notification.dto.SendNotificationRequest;
import com.iam.platform.notification.entity.NotificationLog;
import com.iam.platform.notification.enums.ChannelType;
import com.iam.platform.notification.enums.NotificationStatus;
import com.iam.platform.notification.repository.NotificationLogRepository;
import com.iam.platform.notification.service.NotificationDispatcher;
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
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, IamSecurityAutoConfiguration.class})
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationLogRepository logRepository;

    @MockitoBean
    private NotificationDispatcher dispatcher;

    @MockitoBean
    private TemplateService templateService;

    @Test
    @WithMockUser(roles = "iam-admin")
    void listLogs_asIamAdmin_shouldSucceed() throws Exception {
        NotificationLog log = NotificationLog.builder()
                .id(UUID.randomUUID())
                .channelType(ChannelType.EMAIL)
                .recipient("test@example.com")
                .subject("Test")
                .status(NotificationStatus.SENT)
                .createdAt(Instant.now())
                .build();

        when(logRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "ops-admin")
    void sendNotification_asOpsAdmin_shouldSucceed() throws Exception {
        SendNotificationRequest request = new SendNotificationRequest(
                ChannelType.EMAIL, "test@example.com", "Test Subject",
                "Test body", null, null);

        mockMvc.perform(post("/api/v1/notifications/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "iam-admin")
    void getStats_asIamAdmin_shouldSucceed() throws Exception {
        when(logRepository.countByStatus(NotificationStatus.PENDING)).thenReturn(5L);
        when(logRepository.countByStatus(NotificationStatus.SENT)).thenReturn(100L);
        when(logRepository.countByStatus(NotificationStatus.FAILED)).thenReturn(2L);

        mockMvc.perform(get("/api/v1/notifications/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sent").value(100));
    }

    @Test
    @WithMockUser(roles = "external-user")
    void listLogs_asExternalUser_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listLogs_unauthenticated_shouldBeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }
}
