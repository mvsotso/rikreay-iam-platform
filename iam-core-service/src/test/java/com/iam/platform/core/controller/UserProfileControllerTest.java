package com.iam.platform.core.controller;

import com.iam.platform.common.test.ApiResponseAssertions;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("UserProfileController Tests")
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /api/v1/users/me")
    class GetCurrentUser {

        @Test
        @DisplayName("Should return user profile from JWT claims")
        void getCurrentUser() throws Exception {
            Jwt jwt = JwtTestUtils.createJwtWithClientRoles(
                    "user-id-test", "testuser",
                    List.of("iam-admin"),
                    "iam-core-service", List.of("read", "write"));

            // Add extra claims for profile extraction
            Jwt enrichedJwt = Jwt.withTokenValue(jwt.getTokenValue())
                    .headers(h -> h.putAll(jwt.getHeaders()))
                    .claims(c -> {
                        c.putAll(jwt.getClaims());
                        c.put("email", "testuser@example.com");
                        c.put("given_name", "Test");
                        c.put("family_name", "User");
                    })
                    .build();

            mockMvc.perform(get("/api/v1/users/me")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(enrichedJwt)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.email").value("testuser@example.com"))
                    .andExpect(jsonPath("$.data.firstName").value("Test"))
                    .andExpect(jsonPath("$.data.lastName").value("User"))
                    .andExpect(jsonPath("$.data.realmRoles").isArray())
                    .andExpect(jsonPath("$.data.clientRoles").isMap());
        }

        @Test
        @DisplayName("Should return profile with empty roles when JWT has no roles")
        void getCurrentUserNoRoles() throws Exception {
            Jwt jwt = JwtTestUtils.createJwtNoRoles("user-id-noroles");

            mockMvc.perform(get("/api/v1/users/me")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.realmRoles").isEmpty())
                    .andExpect(jsonPath("$.data.clientRoles").isEmpty());
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void getCurrentUserUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users")
    class ListUsers {

        @Test
        @DisplayName("Should return placeholder with internal-user role")
        void listUsersWithInternalUser() throws Exception {
            mockMvc.perform(get("/api/v1/users")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_INTERNAL, TestConstants.ROLE_INTERNAL_USER)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isString());
        }

        @Test
        @DisplayName("Should return placeholder with tenant-admin role")
        void listUsersWithTenantAdmin() throws Exception {
            mockMvc.perform(get("/api/v1/users")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return placeholder with iam-admin role")
        void listUsersWithIamAdmin() throws Exception {
            mockMvc.perform(get("/api/v1/users")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 403 for external-user role")
        void listUsersForbiddenForExternalUser() throws Exception {
            mockMvc.perform(get("/api/v1/users")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void listUsersUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
