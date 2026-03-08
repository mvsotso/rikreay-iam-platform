package com.iam.platform.audit;

import com.iam.platform.audit.dto.AuditEventResponse;
import com.iam.platform.audit.dto.AuditStatsResponse;
import com.iam.platform.audit.service.AuditQueryService;
import com.iam.platform.common.test.JwtTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditQueryService auditQueryService;

    @Test
    @DisplayName("GET /api/v1/audit/events should return 401 without authentication")
    void eventsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/audit/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/audit/events should return 403 for external-user")
    void eventsForbiddenForExternalUser() throws Exception {
        mockMvc.perform(get("/api/v1/audit/events")
                        .with(JwtTestUtils.jwtWithRoles("user", "external-user")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/audit/events should return 200 for auditor")
    void eventsAccessibleForAuditor() throws Exception {
        Page<AuditEventResponse> page = new PageImpl<>(List.of());
        when(auditQueryService.searchEvents(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/audit/events")
                        .with(JwtTestUtils.jwtWithRoles("auditor.user", "auditor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/audit/events should return 200 for iam-admin")
    void eventsAccessibleForIamAdmin() throws Exception {
        Page<AuditEventResponse> page = new PageImpl<>(List.of());
        when(auditQueryService.searchEvents(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/audit/events")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/audit/events should return 200 for report-viewer")
    void eventsAccessibleForReportViewer() throws Exception {
        Page<AuditEventResponse> page = new PageImpl<>(List.of());
        when(auditQueryService.searchEvents(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/audit/events")
                        .with(JwtTestUtils.jwtWithRoles("viewer", "report-viewer")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/audit/xroad should return 200 for service-manager")
    void xroadEventsAccessibleForServiceManager() throws Exception {
        Page<AuditEventResponse> page = new PageImpl<>(List.of());
        when(auditQueryService.searchXroadEvents(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/audit/xroad")
                        .with(JwtTestUtils.jwtWithRoles("manager", "service-manager")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/audit/stats should return 200 for auditor")
    void statsAccessibleForAuditor() throws Exception {
        AuditStatsResponse stats = new AuditStatsResponse(100, 90, 10, Map.of(), Map.of());
        when(auditQueryService.getStats(any(), any(), any(), any())).thenReturn(stats);

        mockMvc.perform(get("/api/v1/audit/stats")
                        .with(JwtTestUtils.jwtWithRoles("auditor.user", "auditor")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/audit/login-history should return 403 for report-viewer")
    void loginHistoryForbiddenForReportViewer() throws Exception {
        mockMvc.perform(get("/api/v1/audit/login-history")
                        .with(JwtTestUtils.jwtWithRoles("viewer", "report-viewer")))
                .andExpect(status().isForbidden());
    }
}
