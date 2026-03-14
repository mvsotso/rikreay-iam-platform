package com.iam.platform.admin.controller;

import com.iam.platform.common.test.ApiResponseAssertions;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AdminUserController Tests")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Keycloak keycloak;

    @SuppressWarnings("unused")
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private UserRepresentation createUser(String id, String username, String email, String first, String last) {
        UserRepresentation user = new UserRepresentation();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(first);
        user.setLastName(last);
        user.setEnabled(true);
        return user;
    }

    private void mockKeycloakUsersList(String realm, List<UserRepresentation> users) {
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        when(keycloak.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list(anyInt(), anyInt())).thenReturn(users);
        when(usersResource.search(anyString(), anyInt(), anyInt())).thenReturn(users);
    }

    @Nested
    @DisplayName("GET /api/v1/platform-admin/users")
    class ListUsers {

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/users"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 200 with user list for iam-admin")
        void iamAdmin_returnsUsers() throws Exception {
            List<UserRepresentation> users = List.of(
                    createUser("u1", "john", "john@test.com", "John", "Doe"),
                    createUser("u2", "jane", "jane@test.com", "Jane", "Smith")
            );
            mockKeycloakUsersList("iam-platform", users);

            mockMvc.perform(get("/api/v1/platform-admin/users")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].username").value("john"))
                    .andExpect(jsonPath("$.data[0].email").value("john@test.com"))
                    .andExpect(jsonPath("$.data[1].username").value("jane"));
        }

        @Test
        @DisplayName("Should return 200 with user list for tenant-admin")
        void tenantAdmin_returnsUsers() throws Exception {
            List<UserRepresentation> users = List.of(
                    createUser("u1", "orguser", "orguser@test.com", "Org", "User")
            );
            mockKeycloakUsersList("iam-platform", users);

            mockMvc.perform(get("/api/v1/platform-admin/users")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }

        @Test
        @DisplayName("Should return 403 for external-user")
        void externalUser_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/users")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 for sector-admin")
        void sectorAdmin_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/users")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_SECTOR_ADMIN, TestConstants.ROLE_SECTOR_ADMIN)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should use custom realmName parameter")
        void customRealm_usesProvidedRealm() throws Exception {
            String customRealm = "custom-org-realm";
            List<UserRepresentation> users = List.of(
                    createUser("u1", "user1", "user1@test.com", "User", "One")
            );
            mockKeycloakUsersList(customRealm, users);

            mockMvc.perform(get("/api/v1/platform-admin/users")
                            .param("realmName", customRealm)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));

            verify(keycloak).realm(customRealm);
        }

        @Test
        @DisplayName("Should use default pagination parameters")
        void defaultPagination() throws Exception {
            mockKeycloakUsersList("iam-platform", List.of());

            mockMvc.perform(get("/api/v1/platform-admin/users")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk());

            RealmResource realmResource = keycloak.realm("iam-platform");
            verify(realmResource.users()).list(0, 50);
        }

        @Test
        @DisplayName("Should use search parameter when provided")
        void searchParam_triggersSearch() throws Exception {
            List<UserRepresentation> users = List.of(
                    createUser("u1", "john", "john@test.com", "John", "Doe")
            );
            mockKeycloakUsersList("iam-platform", users);

            mockMvc.perform(get("/api/v1/platform-admin/users")
                            .param("search", "john")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));

            RealmResource realmResource = keycloak.realm("iam-platform");
            verify(realmResource.users()).search(eq("john"), anyInt(), anyInt());
        }

        @Test
        @DisplayName("Should handle empty user list gracefully")
        void emptyUserList_returnsEmptyArray() throws Exception {
            mockKeycloakUsersList("iam-platform", List.of());

            mockMvc.perform(get("/api/v1/platform-admin/users")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("Should handle null user fields gracefully")
        void nullFields_returnEmptyStrings() throws Exception {
            UserRepresentation user = new UserRepresentation();
            user.setUsername("testuser");
            user.setEnabled(true);
            // id, email, firstName, lastName are null
            mockKeycloakUsersList("iam-platform", List.of(user));

            mockMvc.perform(get("/api/v1/platform-admin/users")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].username").value("testuser"))
                    .andExpect(jsonPath("$.data[0].email").value(""))
                    .andExpect(jsonPath("$.data[0].firstName").value(""))
                    .andExpect(jsonPath("$.data[0].lastName").value(""));
        }
    }
}
