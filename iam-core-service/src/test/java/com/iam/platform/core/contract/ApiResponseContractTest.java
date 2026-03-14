package com.iam.platform.core.contract;

import com.iam.platform.common.test.JwtTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests verifying the ApiResponse envelope structure
 * is consistently applied across all response paths (success, 400, 403, 404, 500).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiResponseContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Success response has correct envelope: success=true, timestamp, requestId, data")
    void successResponse_hasCorrectEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/persons")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("401 response when no JWT provided")
    void unauthorizedResponse_noJwt() throws Exception {
        mockMvc.perform(get("/api/v1/persons"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("403 response when user lacks required role")
    void forbiddenResponse_wrongRole() throws Exception {
        mockMvc.perform(get("/api/v1/persons")
                        .with(JwtTestUtils.jwtWithRoles("citizen", "external-user")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("404 response has error envelope: success=false, errorCode, message")
    void notFoundResponse_hasErrorEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/persons/00000000-0000-0000-0000-000000000001")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.path").exists());
    }

    @Test
    @DisplayName("Success response message field is present")
    void successResponse_hasMessage() throws Exception {
        mockMvc.perform(get("/api/v1/persons")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Paginated response wraps Page in data field")
    void paginatedResponse_hasPageStructure() throws Exception {
        mockMvc.perform(get("/api/v1/persons?page=0&size=10")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.pageable").exists())
                .andExpect(jsonPath("$.data.totalElements").exists())
                .andExpect(jsonPath("$.data.totalPages").exists());
    }

    @Test
    @DisplayName("Error response does not contain data field")
    void errorResponse_noDataField() throws Exception {
        mockMvc.perform(get("/api/v1/persons/00000000-0000-0000-0000-000000000001")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("Health endpoint is not wrapped in ApiResponse")
    void healthEndpoint_notWrapped() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @DisplayName("Representations endpoint respects RBAC — tenant-admin allowed (returns error when no params)")
    void representationsContract_tenantAdminAllowed() throws Exception {
        // tenant-admin passes RBAC check (200, not 403), but controller returns
        // ApiResponse.error when neither entityId nor personId is provided
        mockMvc.perform(get("/api/v1/representations")
                        .with(JwtTestUtils.jwtWithRoles("org-admin", "tenant-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("MISSING_PARAMETER"));
    }

    @Test
    @DisplayName("Representations endpoint respects RBAC — internal-user denied")
    void representationsContract_internalUserDenied() throws Exception {
        mockMvc.perform(get("/api/v1/representations")
                        .with(JwtTestUtils.jwtWithRoles("staff", "internal-user")))
                .andExpect(status().isForbidden());
    }
}
