package com.iam.platform.xroad;

import com.iam.platform.common.constants.XRoadHeaders;
import com.iam.platform.common.dto.XRoadContextDto;
import com.iam.platform.common.filter.XRoadRequestFilter;
import com.iam.platform.xroad.service.XRoadRoutingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for XRoadProxyController — proxy forwarding, header validation, ACL.
 * /xroad/** is permitAll — authenticated by X-Road Security Server via XRoadRequestFilter.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class XRoadProxyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private XRoadRoutingService routingService;

    private static final String XROAD_CLIENT = "KH/GOV/MOF/BUDGET-SYSTEM";
    private static final String XROAD_MSG_ID = "msg-" + System.currentTimeMillis();

    @Test
    @DisplayName("GET /xroad/{serviceCode} — valid X-Road headers should proxy request")
    void proxyRequest_validHeaders_shouldSucceed() throws Exception {
        when(routingService.routeRequest(eq("getTaxpayerInfo"), any(), any(XRoadContextDto.class), any()))
                .thenReturn("{\"success\":true,\"data\":{\"tin\":\"K001234567\"}}");

        mockMvc.perform(get("/xroad/getTaxpayerInfo")
                        .header(XRoadHeaders.CLIENT, XROAD_CLIENT)
                        .header(XRoadHeaders.ID, XROAD_MSG_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string(XRoadHeaders.ID, XROAD_MSG_ID));
    }

    @Test
    @DisplayName("GET /xroad/{serviceCode}/{path} — should forward path suffix")
    void proxyRequest_withPathSuffix_shouldForward() throws Exception {
        when(routingService.routeRequest(eq("taxpayer"), eq("K001234567/details"),
                any(XRoadContextDto.class), any()))
                .thenReturn("{\"success\":true}");

        mockMvc.perform(get("/xroad/taxpayer/K001234567/details")
                        .header(XRoadHeaders.CLIENT, XROAD_CLIENT)
                        .header(XRoadHeaders.ID, XROAD_MSG_ID))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /xroad/{serviceCode} — should forward request body")
    void proxyRequest_post_shouldForwardBody() throws Exception {
        String requestBody = "{\"tin\":\"K001234567\"}";
        when(routingService.routeRequest(eq("submitDeclaration"), any(),
                any(XRoadContextDto.class), eq(requestBody)))
                .thenReturn("{\"success\":true,\"data\":{\"declarationId\":\"D001\"}}");

        mockMvc.perform(post("/xroad/submitDeclaration")
                        .header(XRoadHeaders.CLIENT, XROAD_CLIENT)
                        .header(XRoadHeaders.ID, XROAD_MSG_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /xroad/{serviceCode} — missing X-Road-Client header should return 400")
    void proxyRequest_missingClientHeader_shouldReturn400() throws Exception {
        mockMvc.perform(get("/xroad/getTaxpayerInfo")
                        .header(XRoadHeaders.ID, XROAD_MSG_ID))
                .andExpect(status().isBadRequest());

        verify(routingService, never()).routeRequest(any(), any(), any(), any());
    }

    @Test
    @DisplayName("GET /xroad/{serviceCode} — missing X-Road-Id header should return 400")
    void proxyRequest_missingIdHeader_shouldReturn400() throws Exception {
        mockMvc.perform(get("/xroad/getTaxpayerInfo")
                        .header(XRoadHeaders.CLIENT, XROAD_CLIENT))
                .andExpect(status().isBadRequest());

        verify(routingService, never()).routeRequest(any(), any(), any(), any());
    }

    @Test
    @DisplayName("GET /xroad/{serviceCode} — invalid client header format should return 400")
    void proxyRequest_invalidClientFormat_shouldReturn400() throws Exception {
        mockMvc.perform(get("/xroad/getTaxpayerInfo")
                        .header(XRoadHeaders.CLIENT, "INVALID_FORMAT")
                        .header(XRoadHeaders.ID, XROAD_MSG_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /xroad/{serviceCode} — no JWT required (permitAll)")
    void proxyRequest_noJwt_shouldNotReturn401() throws Exception {
        // X-Road endpoints don't need JWT — they are authenticated by Security Server
        when(routingService.routeRequest(eq("someService"), any(), any(XRoadContextDto.class), any()))
                .thenReturn("{\"success\":true}");

        mockMvc.perform(get("/xroad/someService")
                        .header(XRoadHeaders.CLIENT, XROAD_CLIENT)
                        .header(XRoadHeaders.ID, XROAD_MSG_ID))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /xroad/{serviceCode} — should pass X-Road-UserId header in context")
    void proxyRequest_withUserId_shouldPassInContext() throws Exception {
        when(routingService.routeRequest(eq("verifyIdentity"), any(), any(XRoadContextDto.class), any()))
                .thenReturn("{\"success\":true}");

        mockMvc.perform(get("/xroad/verifyIdentity")
                        .header(XRoadHeaders.CLIENT, XROAD_CLIENT)
                        .header(XRoadHeaders.ID, XROAD_MSG_ID)
                        .header(XRoadHeaders.USER_ID, "EE12345678901"))
                .andExpect(status().isOk());
    }
}
