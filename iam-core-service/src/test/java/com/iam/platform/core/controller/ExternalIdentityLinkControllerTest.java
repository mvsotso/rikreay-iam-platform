package com.iam.platform.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.common.enums.ExternalSystem;
import com.iam.platform.common.enums.VerificationStatus;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.core.entity.ExternalIdentityLink;
import com.iam.platform.core.service.ExternalIdentityLinkService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ExternalIdentityLinkController Tests")
class ExternalIdentityLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExternalIdentityLinkService linkService;

    private static final UUID TEST_PERSON_UUID = UUID.fromString(TestConstants.TEST_PERSON_ID);
    private static final UUID TEST_LINK_ID = UUID.randomUUID();

    private ExternalIdentityLink createTestLink() {
        ExternalIdentityLink link = new ExternalIdentityLink();
        link.setId(TEST_LINK_ID);
        link.setOwnerType("NATURAL_PERSON");
        link.setOwnerId(TEST_PERSON_UUID);
        link.setExternalSystem(ExternalSystem.MOI_NATIONAL_ID);
        link.setExternalIdentifier("NID-123456");
        link.setVerificationStatus(VerificationStatus.UNVERIFIED);
        link.setCreatedAt(Instant.now());
        link.setUpdatedAt(Instant.now());
        return link;
    }

    @Nested
    @DisplayName("POST /api/v1/persons/{personId}/external-links")
    class CreateLink {

        @Test
        @DisplayName("Should create link with iam-admin role")
        void createWithIamAdmin() throws Exception {
            ExternalIdentityLink link = createTestLink();
            when(linkService.create(any(ExternalIdentityLink.class))).thenReturn(link);

            mockMvc.perform(post("/api/v1/persons/{personId}/external-links", TEST_PERSON_UUID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(link)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("External link created"));
        }

        @Test
        @DisplayName("Should create link with tenant-admin role")
        void createWithTenantAdmin() throws Exception {
            ExternalIdentityLink link = createTestLink();
            when(linkService.create(any(ExternalIdentityLink.class))).thenReturn(link);

            mockMvc.perform(post("/api/v1/persons/{personId}/external-links", TEST_PERSON_UUID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(link)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should return 403 for external-user on create")
        void createForbiddenForExternalUser() throws Exception {
            ExternalIdentityLink link = createTestLink();

            mockMvc.perform(post("/api/v1/persons/{personId}/external-links", TEST_PERSON_UUID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(link)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/persons/{personId}/external-links")
    class ListLinks {

        @Test
        @DisplayName("Should list links with internal-user role")
        void listWithInternalUser() throws Exception {
            when(linkService.findByOwner("NATURAL_PERSON", TEST_PERSON_UUID))
                    .thenReturn(List.of(createTestLink()));

            mockMvc.perform(get("/api/v1/persons/{personId}/external-links", TEST_PERSON_UUID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_INTERNAL, TestConstants.ROLE_INTERNAL_USER)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Should list links with iam-admin role")
        void listWithIamAdmin() throws Exception {
            when(linkService.findByOwner("NATURAL_PERSON", TEST_PERSON_UUID))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/v1/persons/{personId}/external-links", TEST_PERSON_UUID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 403 for external-user")
        void listForbiddenForExternalUser() throws Exception {
            mockMvc.perform(get("/api/v1/persons/{personId}/external-links", TEST_PERSON_UUID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/persons/{personId}/external-links/{linkId}")
    class UpdateLink {

        @Test
        @DisplayName("Should update link with iam-admin role")
        void updateWithIamAdmin() throws Exception {
            ExternalIdentityLink link = createTestLink();
            link.setVerificationStatus(VerificationStatus.VERIFIED);
            when(linkService.update(eq(TEST_LINK_ID), any(ExternalIdentityLink.class))).thenReturn(link);

            mockMvc.perform(put("/api/v1/persons/{personId}/external-links/{linkId}",
                            TEST_PERSON_UUID, TEST_LINK_ID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(link)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("External link updated"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/persons/{personId}/external-links/{linkId}")
    class DeleteLink {

        @Test
        @DisplayName("Should delete link with iam-admin role")
        void deleteWithIamAdmin() throws Exception {
            doNothing().when(linkService).softDelete(TEST_LINK_ID);

            mockMvc.perform(delete("/api/v1/persons/{personId}/external-links/{linkId}",
                            TEST_PERSON_UUID, TEST_LINK_ID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("External link deleted"));
        }

        @Test
        @DisplayName("Should return 403 for tenant-admin on delete (only iam-admin)")
        void deleteForbiddenForTenantAdmin() throws Exception {
            mockMvc.perform(delete("/api/v1/persons/{personId}/external-links/{linkId}",
                            TEST_PERSON_UUID, TEST_LINK_ID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isForbidden());
        }
    }
}
