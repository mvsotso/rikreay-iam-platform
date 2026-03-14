package com.iam.platform.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.notification.dto.ScheduledReportRequest;
import com.iam.platform.notification.dto.ScheduledReportResponse;
import com.iam.platform.notification.service.ScheduleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for ScheduleController — scheduled report CRUD.
 * Requires iam-admin or ops-admin role.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ScheduleService scheduleService;

    private static final String SCHEDULES_URL = "/api/v1/notifications/schedules";

    private ScheduledReportResponse sampleResponse() {
        return new ScheduledReportResponse(
                UUID.randomUUID(), "Daily Auth Report", "0 9 * * *",
                UUID.randomUUID(), List.of("admin@rikreay.gov.kh"),
                true, null, null, Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("POST /schedules — iam-admin should create scheduled report")
    void createSchedule_asIamAdmin_shouldSucceed() throws Exception {
        UUID templateId = UUID.randomUUID();
        ScheduledReportRequest request = new ScheduledReportRequest(
                "Daily Report", "0 9 * * *", templateId,
                List.of("admin@rikreay.gov.kh"), true);

        when(scheduleService.createSchedule(any(ScheduledReportRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(post(SCHEDULES_URL)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Daily Auth Report"));
    }

    @Test
    @DisplayName("POST /schedules — ops-admin should also create scheduled report")
    void createSchedule_asOpsAdmin_shouldSucceed() throws Exception {
        UUID templateId = UUID.randomUUID();
        ScheduledReportRequest request = new ScheduledReportRequest(
                "Weekly Report", "0 9 * * 1", templateId,
                List.of("ops@rikreay.gov.kh"), true);

        when(scheduleService.createSchedule(any(ScheduledReportRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(post(SCHEDULES_URL)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_OPS_ADMIN, TestConstants.ROLE_OPS_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /schedules — iam-admin should list scheduled reports")
    void listSchedules_asIamAdmin_shouldSucceed() throws Exception {
        Page<ScheduledReportResponse> page = new PageImpl<>(List.of(sampleResponse()));
        when(scheduleService.listSchedules(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(SCHEDULES_URL)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("GET /schedules/{id} — should return specific schedule")
    void getSchedule_asIamAdmin_shouldSucceed() throws Exception {
        UUID id = UUID.randomUUID();
        ScheduledReportResponse response = sampleResponse();
        when(scheduleService.getSchedule(id)).thenReturn(response);

        mockMvc.perform(get(SCHEDULES_URL + "/" + id)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Daily Auth Report"));
    }

    @Test
    @DisplayName("PUT /schedules/{id} — iam-admin should update schedule")
    void updateSchedule_asIamAdmin_shouldSucceed() throws Exception {
        UUID id = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        ScheduledReportRequest request = new ScheduledReportRequest(
                "Updated Report", "0 18 * * *", templateId,
                List.of("admin@rikreay.gov.kh"), false);

        ScheduledReportResponse updatedResponse = new ScheduledReportResponse(
                id, "Updated Report", "0 18 * * *", templateId,
                List.of("admin@rikreay.gov.kh"), false, null, null,
                Instant.now(), Instant.now());

        when(scheduleService.updateSchedule(eq(id), any(ScheduledReportRequest.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put(SCHEDULES_URL + "/" + id)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Report"));
    }

    @Test
    @DisplayName("DELETE /schedules/{id} — iam-admin should soft-delete schedule")
    void deleteSchedule_asIamAdmin_shouldSucceed() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(scheduleService).deleteSchedule(id);

        mockMvc.perform(delete(SCHEDULES_URL + "/" + id)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(scheduleService).deleteSchedule(id);
    }

    @Test
    @DisplayName("POST /schedules — developer role should be forbidden")
    void createSchedule_asDeveloper_shouldBeForbidden() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "test", "cronExpression", "0 9 * * *",
                "templateId", UUID.randomUUID(), "recipientList", List.of("a@b.c"), "enabled", true
        ));

        mockMvc.perform(post(SCHEDULES_URL)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_DEVELOPER, TestConstants.ROLE_DEVELOPER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /schedules — unauthenticated should return 401")
    void listSchedules_noAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get(SCHEDULES_URL))
                .andExpect(status().isUnauthorized());
    }
}
