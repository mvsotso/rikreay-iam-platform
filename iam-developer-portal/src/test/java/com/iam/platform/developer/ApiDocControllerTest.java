package com.iam.platform.developer;

import com.iam.platform.common.test.ApiResponseAssertions;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.developer.dto.ApiDocResponse;
import com.iam.platform.developer.dto.SdkInfo;
import com.iam.platform.developer.service.ApiDocService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for ApiDocController — all endpoints are public (permitAll).
 * Verifies unauthenticated access, response structure, and service delegation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiDocControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiDocService apiDocService;

    @Test
    @DisplayName("GET /api/v1/docs — should return all API docs without authentication")
    void getAllDocs_noAuth_shouldSucceed() throws Exception {
        List<ApiDocResponse> docs = List.of(
                new ApiDocResponse("iam-core-service", "http://localhost:8082",
                        Map.of("openapi", "3.0.0")),
                new ApiDocResponse("iam-tenant-service", "http://localhost:8083",
                        Map.of("openapi", "3.0.0"))
        );
        when(apiDocService.getAllApiDocs()).thenReturn(docs);

        ApiResponseAssertions.assertApiSuccess(
                mockMvc.perform(get("/api/v1/docs"))
        ).andExpect(jsonPath("$.data").isArray())
         .andExpect(jsonPath("$.data.length()").value(2))
         .andExpect(jsonPath("$.data[0].serviceName").value("iam-core-service"));
    }

    @Test
    @DisplayName("GET /api/v1/docs/services — should list available services without auth")
    void listServices_noAuth_shouldSucceed() throws Exception {
        when(apiDocService.getAvailableServices()).thenReturn(
                List.of("iam-core-service", "iam-tenant-service", "iam-audit-service"));

        ApiResponseAssertions.assertApiSuccess(
                mockMvc.perform(get("/api/v1/docs/services"))
        ).andExpect(jsonPath("$.data").isArray())
         .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    @DisplayName("GET /api/v1/docs/{serviceName} — should return single service doc without auth")
    void getServiceDoc_noAuth_shouldSucceed() throws Exception {
        ApiDocResponse doc = new ApiDocResponse("iam-core-service", "http://localhost:8082",
                Map.of("openapi", "3.0.0", "info", Map.of("title", "Core Service")));
        when(apiDocService.getServiceApiDoc("iam-core-service")).thenReturn(doc);

        ApiResponseAssertions.assertApiSuccess(
                mockMvc.perform(get("/api/v1/docs/iam-core-service"))
        ).andExpect(jsonPath("$.data.serviceName").value("iam-core-service"))
         .andExpect(jsonPath("$.data.serviceUrl").value("http://localhost:8082"));
    }

    @Test
    @DisplayName("GET /api/v1/sdks — should return SDK info without authentication")
    void getSdks_noAuth_shouldSucceed() throws Exception {
        List<SdkInfo> sdks = List.of(
                new SdkInfo("Java", "1.0.0", "/api/v1/sdks/java", "/api/v1/docs/sdk-guide/java"),
                new SdkInfo("Python", "1.0.0", "/api/v1/sdks/python", "/api/v1/docs/sdk-guide/python")
        );
        when(apiDocService.getAvailableSdks()).thenReturn(sdks);

        ApiResponseAssertions.assertApiSuccess(
                mockMvc.perform(get("/api/v1/sdks"))
        ).andExpect(jsonPath("$.data").isArray())
         .andExpect(jsonPath("$.data.length()").value(2))
         .andExpect(jsonPath("$.data[0].language").value("Java"));
    }

    @Test
    @DisplayName("GET /api/v1/docs — authenticated user should also have access")
    void getAllDocs_withAuth_shouldSucceed() throws Exception {
        when(apiDocService.getAllApiDocs()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/docs")
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_DEVELOPER,
                                TestConstants.ROLE_DEVELOPER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
