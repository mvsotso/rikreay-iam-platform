package com.iam.platform.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.admin.dto.BulkUserImportRequest;
import com.iam.platform.admin.service.BulkUserService;
import com.iam.platform.common.test.ApiResponseAssertions;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("BulkUserController Tests")
class BulkUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BulkUserService bulkUserService;

    @SuppressWarnings("unused")
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private BulkUserImportRequest createValidImportRequest() {
        return new BulkUserImportRequest(
                TestConstants.TEST_REALM_NAME,
                List.of(
                        new BulkUserImportRequest.UserEntry("user1", "user1@test.com", "User", "One", "temp123", List.of()),
                        new BulkUserImportRequest.UserEntry("user2", "user2@test.com", "User", "Two", "temp456", List.of())
                )
        );
    }

    @Nested
    @DisplayName("POST /api/v1/platform-admin/users/bulk-import")
    class BulkImport {

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/api/v1/platform-admin/users/bulk-import")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidImportRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 200 for iam-admin with valid request")
        void iamAdmin_importSucceeds() throws Exception {
            BulkUserImportRequest request = createValidImportRequest();
            when(bulkUserService.bulkImportUsers(any(), anyString()))
                    .thenReturn(Map.of("created", 2, "failed", 0, "errors", List.of()));

            mockMvc.perform(post("/api/v1/platform-admin/users/bulk-import")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Bulk import completed"))
                    .andExpect(jsonPath("$.data.created").value(2))
                    .andExpect(jsonPath("$.data.failed").value(0));
        }

        @Test
        @DisplayName("Should return 403 for tenant-admin")
        void tenantAdmin_forbidden() throws Exception {
            mockMvc.perform(post("/api/v1/platform-admin/users/bulk-import")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidImportRequest())))
                    .andExpect(status().isForbidden());

            verify(bulkUserService, never()).bulkImportUsers(any(), anyString());
        }

        @Test
        @DisplayName("Should return 403 for sector-admin")
        void sectorAdmin_forbidden() throws Exception {
            mockMvc.perform(post("/api/v1/platform-admin/users/bulk-import")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_SECTOR_ADMIN, TestConstants.ROLE_SECTOR_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidImportRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 400 when realmName is blank")
        void blankRealmName_validationError() throws Exception {
            BulkUserImportRequest request = new BulkUserImportRequest(
                    "",
                    List.of(new BulkUserImportRequest.UserEntry("u1", "u1@test.com", "F", "L", "p", List.of()))
            );

            mockMvc.perform(post("/api/v1/platform-admin/users/bulk-import")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when users list is empty")
        void emptyUsersList_validationError() throws Exception {
            BulkUserImportRequest request = new BulkUserImportRequest(
                    TestConstants.TEST_REALM_NAME,
                    List.of()
            );

            mockMvc.perform(post("/api/v1/platform-admin/users/bulk-import")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return partial success when some users fail")
        void partialFailure_returnsPartialResult() throws Exception {
            BulkUserImportRequest request = createValidImportRequest();
            when(bulkUserService.bulkImportUsers(any(), anyString()))
                    .thenReturn(Map.of("created", 1, "failed", 1, "errors", List.of("user2: already exists")));

            mockMvc.perform(post("/api/v1/platform-admin/users/bulk-import")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.created").value(1))
                    .andExpect(jsonPath("$.data.failed").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/platform-admin/users/bulk-export")
    class BulkExport {

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/users/bulk-export")
                            .param("realmName", TestConstants.TEST_REALM_NAME))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 200 for iam-admin")
        void iamAdmin_exportSucceeds() throws Exception {
            List<Map<String, String>> exportData = List.of(
                    Map.of("username", "user1", "email", "user1@test.com", "enabled", "true")
            );
            when(bulkUserService.bulkExportUsers(TestConstants.TEST_REALM_NAME)).thenReturn(exportData);

            mockMvc.perform(get("/api/v1/platform-admin/users/bulk-export")
                            .param("realmName", TestConstants.TEST_REALM_NAME)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].username").value("user1"));
        }

        @Test
        @DisplayName("Should return 403 for tenant-admin")
        void tenantAdmin_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/users/bulk-export")
                            .param("realmName", TestConstants.TEST_REALM_NAME)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/platform-admin/users/bulk-disable")
    class BulkDisable {

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/api/v1/platform-admin/users/bulk-disable")
                            .param("realmName", TestConstants.TEST_REALM_NAME)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[\"user1\", \"user2\"]"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 200 for iam-admin")
        void iamAdmin_disableSucceeds() throws Exception {
            when(bulkUserService.bulkDisableUsers(eq(TestConstants.TEST_REALM_NAME), anyList(), anyString()))
                    .thenReturn(Map.of("disabled", 2, "failed", 0));

            mockMvc.perform(post("/api/v1/platform-admin/users/bulk-disable")
                            .param("realmName", TestConstants.TEST_REALM_NAME)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[\"user1\", \"user2\"]"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Bulk disable completed"))
                    .andExpect(jsonPath("$.data.disabled").value(2));
        }

        @Test
        @DisplayName("Should return 403 for tenant-admin")
        void tenantAdmin_forbidden() throws Exception {
            mockMvc.perform(post("/api/v1/platform-admin/users/bulk-disable")
                            .param("realmName", TestConstants.TEST_REALM_NAME)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[\"user1\"]"))
                    .andExpect(status().isForbidden());

            verify(bulkUserService, never()).bulkDisableUsers(anyString(), anyList(), anyString());
        }
    }
}
