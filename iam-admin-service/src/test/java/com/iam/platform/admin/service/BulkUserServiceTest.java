package com.iam.platform.admin.service;

import com.iam.platform.admin.dto.BulkUserImportRequest;
import com.iam.platform.common.dto.NotificationCommandDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BulkUserService Unit Tests")
class BulkUserServiceTest {

    @Mock
    private Keycloak keycloak;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private BulkUserService bulkUserService;

    private RealmResource mockRealm(String realmName) {
        RealmResource realmResource = mock(RealmResource.class);
        when(keycloak.realm(realmName)).thenReturn(realmResource);
        return realmResource;
    }

    private UsersResource mockUsers(RealmResource realmResource) {
        UsersResource usersResource = mock(UsersResource.class);
        when(realmResource.users()).thenReturn(usersResource);
        return usersResource;
    }

    @Nested
    @DisplayName("bulkImportUsers")
    class BulkImportUsers {

        @Test
        @DisplayName("Should create all users successfully")
        void allUsersCreated_successfully() {
            String realmName = "test-realm";
            BulkUserImportRequest request = new BulkUserImportRequest(
                    realmName,
                    List.of(
                            new BulkUserImportRequest.UserEntry("user1", "user1@test.com", "User", "One", "pass1", List.of()),
                            new BulkUserImportRequest.UserEntry("user2", "user2@test.com", "User", "Two", "pass2", List.of())
                    )
            );

            RealmResource realmResource = mockRealm(realmName);
            UsersResource usersResource = mockUsers(realmResource);

            Map<String, Object> result = bulkUserService.bulkImportUsers(request, "admin-user");

            assertThat(result.get("created")).isEqualTo(2);
            assertThat(result.get("failed")).isEqualTo(0);
            assertThat((List<?>) result.get("errors")).isEmpty();

            verify(usersResource, times(2)).create(any(UserRepresentation.class));
            verify(auditService).logAdminAction(eq("admin-user"), eq("BULK_IMPORT"),
                    eq("users/" + realmName), eq(true), any());
        }

        @Test
        @DisplayName("Should handle partial failures")
        void partialFailure_countsBoth() {
            String realmName = "test-realm";
            BulkUserImportRequest request = new BulkUserImportRequest(
                    realmName,
                    List.of(
                            new BulkUserImportRequest.UserEntry("user1", "user1@test.com", "User", "One", null, List.of()),
                            new BulkUserImportRequest.UserEntry("user2", "user2@test.com", "User", "Two", null, List.of())
                    )
            );

            RealmResource realmResource = mockRealm(realmName);
            UsersResource usersResource = mockUsers(realmResource);

            // First call succeeds, second throws
            when(usersResource.create(any(UserRepresentation.class)))
                    .thenReturn(null)
                    .thenThrow(new WebApplicationException("User already exists", 409));

            Map<String, Object> result = bulkUserService.bulkImportUsers(request, "admin-user");

            assertThat(result.get("created")).isEqualTo(1);
            assertThat(result.get("failed")).isEqualTo(1);
            assertThat((List<?>) result.get("errors")).hasSize(1);

            verify(auditService).logAdminAction(eq("admin-user"), eq("BULK_IMPORT"),
                    anyString(), eq(false), any());
        }

        @Test
        @DisplayName("Should handle all users failing")
        void allUsersFail() {
            String realmName = "test-realm";
            BulkUserImportRequest request = new BulkUserImportRequest(
                    realmName,
                    List.of(
                            new BulkUserImportRequest.UserEntry("user1", "user1@test.com", "U", "O", null, List.of())
                    )
            );

            RealmResource realmResource = mockRealm(realmName);
            UsersResource usersResource = mockUsers(realmResource);
            when(usersResource.create(any(UserRepresentation.class)))
                    .thenThrow(new RuntimeException("Keycloak error"));

            Map<String, Object> result = bulkUserService.bulkImportUsers(request, "admin-user");

            assertThat(result.get("created")).isEqualTo(0);
            assertThat(result.get("failed")).isEqualTo(1);

            // Should not send notification when no users created
            verify(auditService, never()).sendNotification(any(NotificationCommandDto.class));
        }

        @Test
        @DisplayName("Should set temporary password when provided")
        void temporaryPassword_isSet() {
            String realmName = "test-realm";
            BulkUserImportRequest request = new BulkUserImportRequest(
                    realmName,
                    List.of(
                            new BulkUserImportRequest.UserEntry("user1", "user1@test.com", "User", "One", "tempPass123", List.of())
                    )
            );

            RealmResource realmResource = mockRealm(realmName);
            UsersResource usersResource = mockUsers(realmResource);

            ArgumentCaptor<UserRepresentation> userCaptor = ArgumentCaptor.forClass(UserRepresentation.class);

            bulkUserService.bulkImportUsers(request, "admin-user");

            verify(usersResource).create(userCaptor.capture());
            UserRepresentation capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getCredentials()).isNotEmpty();
            assertThat(capturedUser.getCredentials().get(0).getValue()).isEqualTo("tempPass123");
            assertThat(capturedUser.getCredentials().get(0).isTemporary()).isTrue();
        }

        @Test
        @DisplayName("Should not set credentials when password is null")
        void nullPassword_noCredentials() {
            String realmName = "test-realm";
            BulkUserImportRequest request = new BulkUserImportRequest(
                    realmName,
                    List.of(
                            new BulkUserImportRequest.UserEntry("user1", "user1@test.com", "User", "One", null, List.of())
                    )
            );

            RealmResource realmResource = mockRealm(realmName);
            UsersResource usersResource = mockUsers(realmResource);

            ArgumentCaptor<UserRepresentation> userCaptor = ArgumentCaptor.forClass(UserRepresentation.class);

            bulkUserService.bulkImportUsers(request, "admin-user");

            verify(usersResource).create(userCaptor.capture());
            UserRepresentation capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getCredentials()).isNull();
        }

        @Test
        @DisplayName("Should send notification on successful import")
        void successfulImport_sendsNotification() {
            String realmName = "test-realm";
            BulkUserImportRequest request = new BulkUserImportRequest(
                    realmName,
                    List.of(
                            new BulkUserImportRequest.UserEntry("user1", "user1@test.com", "U", "O", null, List.of())
                    )
            );

            RealmResource realmResource = mockRealm(realmName);
            mockUsers(realmResource);

            bulkUserService.bulkImportUsers(request, "admin-user");

            ArgumentCaptor<NotificationCommandDto> notifCaptor = ArgumentCaptor.forClass(NotificationCommandDto.class);
            verify(auditService).sendNotification(notifCaptor.capture());

            NotificationCommandDto notification = notifCaptor.getValue();
            assertThat(notification.getRecipient()).isEqualTo("admin-user");
            assertThat(notification.getSubject()).contains("Bulk User Import");
            assertThat(notification.getChannelType()).isEqualTo(NotificationCommandDto.ChannelType.EMAIL);
        }

        @Test
        @DisplayName("Should log audit event with correct metadata")
        void auditEvent_containsCorrectMetadata() {
            String realmName = "test-realm";
            BulkUserImportRequest request = new BulkUserImportRequest(
                    realmName,
                    List.of(
                            new BulkUserImportRequest.UserEntry("user1", "u1@test.com", "U", "O", null, List.of()),
                            new BulkUserImportRequest.UserEntry("user2", "u2@test.com", "U", "T", null, List.of())
                    )
            );

            RealmResource realmResource = mockRealm(realmName);
            mockUsers(realmResource);

            bulkUserService.bulkImportUsers(request, "admin-user");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
            verify(auditService).logAdminAction(eq("admin-user"), eq("BULK_IMPORT"),
                    eq("users/test-realm"), eq(true), metadataCaptor.capture());

            Map<String, Object> metadata = metadataCaptor.getValue();
            assertThat(metadata.get("created")).isEqualTo(2);
            assertThat(metadata.get("failed")).isEqualTo(0);
            assertThat(metadata.get("total")).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("bulkExportUsers")
    class BulkExportUsers {

        @Test
        @DisplayName("Should export users from realm")
        void exportsUsers() {
            String realmName = "export-realm";
            UserRepresentation user1 = new UserRepresentation();
            user1.setUsername("user1");
            user1.setEmail("user1@test.com");
            user1.setFirstName("User");
            user1.setLastName("One");
            user1.setEnabled(true);

            RealmResource realmResource = mockRealm(realmName);
            UsersResource usersResource = mockUsers(realmResource);
            when(usersResource.list(0, 1000)).thenReturn(List.of(user1));

            List<Map<String, String>> result = bulkUserService.bulkExportUsers(realmName);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("username")).isEqualTo("user1");
            assertThat(result.get(0).get("email")).isEqualTo("user1@test.com");
            assertThat(result.get(0).get("enabled")).isEqualTo("true");
        }

        @Test
        @DisplayName("Should handle null user fields in export")
        void nullFields_returnEmptyStrings() {
            String realmName = "export-realm";
            UserRepresentation user = new UserRepresentation();
            user.setUsername("user1");
            // email, firstName, lastName are null

            RealmResource realmResource = mockRealm(realmName);
            UsersResource usersResource = mockUsers(realmResource);
            when(usersResource.list(0, 1000)).thenReturn(List.of(user));

            List<Map<String, String>> result = bulkUserService.bulkExportUsers(realmName);

            assertThat(result.get(0).get("email")).isEqualTo("");
            assertThat(result.get(0).get("firstName")).isEqualTo("");
            assertThat(result.get(0).get("lastName")).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("bulkDisableUsers")
    class BulkDisableUsers {

        @Test
        @DisplayName("Should disable users successfully")
        void disablesUsers() {
            String realmName = "test-realm";
            List<String> usernames = List.of("user1", "user2");

            RealmResource realmResource = mockRealm(realmName);
            UsersResource usersResource = mockUsers(realmResource);

            UserRepresentation user1 = new UserRepresentation();
            user1.setId("id1");
            user1.setEnabled(true);

            UserRepresentation user2 = new UserRepresentation();
            user2.setId("id2");
            user2.setEnabled(true);

            when(usersResource.searchByUsername("user1", true)).thenReturn(List.of(user1));
            when(usersResource.searchByUsername("user2", true)).thenReturn(List.of(user2));

            UserResource userResource1 = mock(UserResource.class);
            UserResource userResource2 = mock(UserResource.class);
            when(usersResource.get("id1")).thenReturn(userResource1);
            when(usersResource.get("id2")).thenReturn(userResource2);

            Map<String, Object> result = bulkUserService.bulkDisableUsers(realmName, usernames, "admin-user");

            assertThat(result.get("disabled")).isEqualTo(2);
            assertThat(result.get("failed")).isEqualTo(0);

            verify(auditService).logAdminAction(eq("admin-user"), eq("BULK_DISABLE"),
                    eq("users/test-realm"), eq(true), any());
        }

        @Test
        @DisplayName("Should handle user not found during disable")
        void userNotFound_skipped() {
            String realmName = "test-realm";
            List<String> usernames = List.of("nonexistent");

            RealmResource realmResource = mockRealm(realmName);
            UsersResource usersResource = mockUsers(realmResource);
            when(usersResource.searchByUsername("nonexistent", true)).thenReturn(List.of());

            Map<String, Object> result = bulkUserService.bulkDisableUsers(realmName, usernames, "admin-user");

            assertThat(result.get("disabled")).isEqualTo(0);
            assertThat(result.get("failed")).isEqualTo(0);
        }

        @Test
        @DisplayName("Should count failures when Keycloak throws exceptions")
        void keycloakError_countsFailed() {
            String realmName = "test-realm";
            List<String> usernames = List.of("user1");

            RealmResource realmResource = mockRealm(realmName);
            UsersResource usersResource = mockUsers(realmResource);
            when(usersResource.searchByUsername("user1", true))
                    .thenThrow(new RuntimeException("Keycloak unavailable"));

            Map<String, Object> result = bulkUserService.bulkDisableUsers(realmName, usernames, "admin-user");

            assertThat(result.get("disabled")).isEqualTo(0);
            assertThat(result.get("failed")).isEqualTo(1);
        }
    }
}
