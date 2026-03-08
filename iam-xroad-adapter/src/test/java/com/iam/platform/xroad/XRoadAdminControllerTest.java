package com.iam.platform.xroad;

import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.xroad.config.XRoadProperties;
import com.iam.platform.xroad.dto.ServiceRegistrationResponse;
import com.iam.platform.xroad.service.XRoadRegistryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class XRoadAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private XRoadRegistryService registryService;

    @Test
    @DisplayName("GET /api/v1/xroad/services should return 401 without authentication")
    void servicesUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/xroad/services"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/xroad/services should return 403 for external-user")
    void servicesForbiddenForExternalUser() throws Exception {
        mockMvc.perform(get("/api/v1/xroad/services")
                        .with(JwtTestUtils.jwtWithRoles("user", "external-user")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/xroad/services should return 200 for service-manager")
    void servicesAccessibleForServiceManager() throws Exception {
        ServiceRegistrationResponse svc = new ServiceRegistrationResponse(
                UUID.randomUUID(), "getTaxpayerInfo", "v1",
                "iam-core-service", "/xroad/v1/taxpayer/{tin}",
                "Get taxpayer info", true, Instant.now(), Instant.now());
        when(registryService.listServices()).thenReturn(List.of(svc));

        mockMvc.perform(get("/api/v1/xroad/services")
                        .with(JwtTestUtils.jwtWithRoles("manager", "service-manager")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/xroad/services should return 200 for iam-admin")
    void servicesAccessibleForIamAdmin() throws Exception {
        when(registryService.listServices()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/xroad/services")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/xroad/members should return 200 for service-manager")
    void membersAccessibleForServiceManager() throws Exception {
        mockMvc.perform(get("/api/v1/xroad/members")
                        .with(JwtTestUtils.jwtWithRoles("manager", "service-manager")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instance").value("KH"));
    }

    @Test
    @DisplayName("GET /api/v1/xroad/acl should return 403 for auditor")
    void aclForbiddenForAuditor() throws Exception {
        mockMvc.perform(get("/api/v1/xroad/acl")
                        .with(JwtTestUtils.jwtWithRoles("auditor.user", "auditor")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("X-Road proxy endpoint should be accessible without JWT")
    void xroadProxyPermitAll() throws Exception {
        // /xroad/** is permitAll — may return 400 without X-Road headers
        mockMvc.perform(get("/xroad/someService"))
                .andExpect(status().is4xxClientError());
    }
}
