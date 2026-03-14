package com.iam.platform.developer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.common.security.IamSecurityAutoConfiguration;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.developer.config.SecurityConfig;
import com.iam.platform.developer.controller.AppController;
import com.iam.platform.developer.dto.AppRegistrationRequest;
import com.iam.platform.developer.dto.AppResponse;
import com.iam.platform.developer.enums.AppStatus;
import com.iam.platform.developer.service.AppRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppController.class)
@Import({SecurityConfig.class, IamSecurityAutoConfiguration.class})
@ActiveProfiles("test")
class AppControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppRegistrationService appService;

    @Test
    void registerApp_asDeveloper_shouldSucceed() throws Exception {
        AppRegistrationRequest request = new AppRegistrationRequest("My App", "Test app", List.of("http://localhost:3000/callback"));

        AppResponse response = new AppResponse(UUID.randomUUID(), "My App", "Test app",
                "dev-abc12345", List.of("http://localhost:3000/callback"), "dev-user",
                AppStatus.ACTIVE, Instant.now());

        when(appService.registerApp(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/apps")
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_DEVELOPER, TestConstants.ROLE_DEVELOPER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("My App"));
    }

    @Test
    void registerApp_asExternalUser_shouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/apps")
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listApps_asIamAdmin_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/apps")
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void getApp_asDeveloper_shouldSucceed() throws Exception {
        UUID appId = UUID.randomUUID();
        AppResponse response = new AppResponse(appId, "My App", "Test app",
                "dev-abc12345", List.of(), "dev-user", AppStatus.ACTIVE, Instant.now());

        when(appService.getApp(appId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/apps/" + appId)
                        .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_DEVELOPER, TestConstants.ROLE_DEVELOPER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientId").value("dev-abc12345"));
    }
}
