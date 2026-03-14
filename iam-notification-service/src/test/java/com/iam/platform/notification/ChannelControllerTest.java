package com.iam.platform.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.notification.entity.NotificationChannel;
import com.iam.platform.notification.enums.ChannelType;
import com.iam.platform.notification.repository.NotificationChannelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for ChannelController — CRUD operations on notification channels.
 * Requires iam-admin role for write operations; GET accessible via iam-admin or ops-admin
 * due to SecurityConfig wildcard GET rule ordering.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChannelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationChannelRepository channelRepository;

    private static final String CHANNELS_URL = "/api/v1/notifications/channels";

    private NotificationChannel sampleChannel(UUID id, String name, ChannelType type, boolean enabled) {
        NotificationChannel channel = NotificationChannel.builder()
                .channelType(type)
                .channelName(name)
                .configJson(Map.of("host", "smtp.test.com"))
                .enabled(enabled)
                .build();
        // Simulate persisted entity with ID and timestamps
        channel.setId(id);
        channel.setCreatedAt(Instant.now());
        channel.setUpdatedAt(Instant.now());
        return channel;
    }

    @Test
    @DisplayName("POST /channels — iam-admin should create email channel")
    void createChannel_asIamAdmin_shouldSucceed() throws Exception {
        UUID id = UUID.randomUUID();
        when(channelRepository.existsByChannelName("primary-email")).thenReturn(false);
        when(channelRepository.save(any(NotificationChannel.class)))
                .thenReturn(sampleChannel(id, "primary-email", ChannelType.EMAIL, true));

        String body = objectMapper.writeValueAsString(Map.of(
                "channelType", "EMAIL",
                "channelName", "primary-email",
                "configJson", Map.of("host", "smtp.test.com", "port", 587),
                "enabled", true
        ));

        mockMvc.perform(post(CHANNELS_URL)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.channelName").value("primary-email"))
                .andExpect(jsonPath("$.data.channelType").value("EMAIL"))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    @DisplayName("POST /channels — duplicate name should return 400")
    void createChannel_duplicateName_shouldReturn400() throws Exception {
        when(channelRepository.existsByChannelName("dup-channel")).thenReturn(true);

        String body = objectMapper.writeValueAsString(Map.of(
                "channelType", "SMS",
                "channelName", "dup-channel",
                "enabled", true
        ));

        mockMvc.perform(post(CHANNELS_URL)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /channels — iam-admin should list all channels")
    void listChannels_asIamAdmin_shouldReturnAll() throws Exception {
        when(channelRepository.findAll()).thenReturn(List.of(
                sampleChannel(UUID.randomUUID(), "email-ch", ChannelType.EMAIL, true),
                sampleChannel(UUID.randomUUID(), "sms-ch", ChannelType.SMS, false)
        ));

        mockMvc.perform(get(CHANNELS_URL)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("GET /channels/{id} — iam-admin should get channel by ID")
    void getChannel_asIamAdmin_shouldSucceed() throws Exception {
        UUID id = UUID.randomUUID();
        when(channelRepository.findById(id))
                .thenReturn(Optional.of(sampleChannel(id, "tg-ch", ChannelType.TELEGRAM, true)));

        mockMvc.perform(get(CHANNELS_URL + "/" + id)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.channelName").value("tg-ch"))
                .andExpect(jsonPath("$.data.channelType").value("TELEGRAM"));
    }

    @Test
    @DisplayName("PUT /channels/{id} — iam-admin should update channel")
    void updateChannel_asIamAdmin_shouldSucceed() throws Exception {
        UUID id = UUID.randomUUID();
        NotificationChannel existing = sampleChannel(id, "update-me", ChannelType.EMAIL, false);
        when(channelRepository.findById(id)).thenReturn(Optional.of(existing));

        NotificationChannel updated = sampleChannel(id, "updated-name", ChannelType.EMAIL, true);
        when(channelRepository.save(any(NotificationChannel.class))).thenReturn(updated);

        String body = objectMapper.writeValueAsString(Map.of(
                "channelType", "EMAIL",
                "channelName", "updated-name",
                "enabled", true
        ));

        mockMvc.perform(put(CHANNELS_URL + "/" + id)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.channelName").value("updated-name"))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    @DisplayName("DELETE /channels/{id} — iam-admin should soft-delete channel")
    void deleteChannel_asIamAdmin_shouldSucceed() throws Exception {
        UUID id = UUID.randomUUID();
        NotificationChannel channel = sampleChannel(id, "delete-me", ChannelType.SMS, true);
        when(channelRepository.findById(id)).thenReturn(Optional.of(channel));
        when(channelRepository.save(any(NotificationChannel.class))).thenReturn(channel);

        mockMvc.perform(delete(CHANNELS_URL + "/" + id)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(channelRepository).save(any(NotificationChannel.class));
    }

    @Test
    @DisplayName("POST /channels — ops-admin should be forbidden (iam-admin only for writes)")
    void createChannel_asOpsAdmin_shouldBeForbidden() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "channelType", "EMAIL",
                "channelName", "test",
                "enabled", true
        ));

        mockMvc.perform(post(CHANNELS_URL)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_OPS_ADMIN, TestConstants.ROLE_OPS_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /channels — unauthenticated should get 401")
    void listChannels_noAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get(CHANNELS_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /channels — ops-admin should have read access via GET wildcard rule")
    void listChannels_asOpsAdmin_shouldSucceed() throws Exception {
        when(channelRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get(CHANNELS_URL)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_OPS_ADMIN, TestConstants.ROLE_OPS_ADMIN)))
                .andExpect(status().isOk());
    }
}
