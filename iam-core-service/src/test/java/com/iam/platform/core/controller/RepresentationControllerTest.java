package com.iam.platform.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.common.enums.DelegationScope;
import com.iam.platform.common.enums.RepresentativeRole;
import com.iam.platform.common.enums.VerificationStatus;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.core.entity.Representation;
import com.iam.platform.core.service.IdentityVerificationService;
import com.iam.platform.core.service.RepresentationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
@DisplayName("RepresentationController Tests")
class RepresentationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RepresentationService representationService;

    @MockitoBean
    private IdentityVerificationService verificationService;

    private static final UUID TEST_REP_ID = UUID.randomUUID();
    private static final UUID TEST_ENTITY_UUID = UUID.fromString(TestConstants.TEST_ENTITY_ID);
    private static final UUID TEST_PERSON_UUID = UUID.fromString(TestConstants.TEST_PERSON_ID);

    private Representation createTestRepresentation() {
        Representation rep = new Representation();
        rep.setId(TEST_REP_ID);
        rep.setRepresentativeRole(RepresentativeRole.LEGAL_REPRESENTATIVE);
        rep.setDelegationScope(DelegationScope.FULL_AUTHORITY);
        rep.setTitle("CEO");
        rep.setValidFrom(LocalDate.of(2024, 1, 1));
        rep.setValidUntil(LocalDate.of(2025, 12, 31));
        rep.setVerificationStatus(VerificationStatus.UNVERIFIED);
        rep.setIsPrimary(true);
        rep.setStatus("ACTIVE");
        rep.setCreatedAt(Instant.now());
        rep.setUpdatedAt(Instant.now());
        return rep;
    }

    @Nested
    @DisplayName("POST /api/v1/representations")
    class CreateRepresentation {

        @Test
        @DisplayName("Should create representation with tenant-admin role")
        void createWithTenantAdmin() throws Exception {
            Representation rep = createTestRepresentation();
            when(representationService.create(any(Representation.class))).thenReturn(rep);

            mockMvc.perform(post("/api/v1/representations")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rep)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Representation created"));
        }

        @Test
        @DisplayName("Should create representation with iam-admin role")
        void createWithIamAdmin() throws Exception {
            Representation rep = createTestRepresentation();
            when(representationService.create(any(Representation.class))).thenReturn(rep);

            mockMvc.perform(post("/api/v1/representations")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rep)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should return 403 for internal-user on create")
        void createForbiddenForInternalUser() throws Exception {
            Representation rep = createTestRepresentation();

            mockMvc.perform(post("/api/v1/representations")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_INTERNAL, TestConstants.ROLE_INTERNAL_USER))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rep)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void createUnauthenticated() throws Exception {
            Representation rep = createTestRepresentation();

            mockMvc.perform(post("/api/v1/representations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rep)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/representations")
    class ListRepresentations {

        @Test
        @DisplayName("Should list representations by entityId with tenant-admin")
        void listByEntityId() throws Exception {
            Page<Representation> page = new PageImpl<>(List.of(createTestRepresentation()));
            when(representationService.findByLegalEntity(eq(TEST_ENTITY_UUID), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/representations")
                            .param("entityId", TEST_ENTITY_UUID.toString())
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Should list representations by personId with iam-admin")
        void listByPersonId() throws Exception {
            Page<Representation> page = new PageImpl<>(List.of());
            when(representationService.findByNaturalPerson(eq(TEST_PERSON_UUID), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/representations")
                            .param("personId", TEST_PERSON_UUID.toString())
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Should return error when neither entityId nor personId provided")
        void listWithoutParams() throws Exception {
            mockMvc.perform(get("/api/v1/representations")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("MISSING_PARAMETER"));
        }

        @Test
        @DisplayName("Should return 403 for external-user")
        void listForbiddenForExternalUser() throws Exception {
            mockMvc.perform(get("/api/v1/representations")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/representations/{id}")
    class UpdateRepresentation {

        @Test
        @DisplayName("Should update representation with tenant-admin role")
        void updateWithTenantAdmin() throws Exception {
            Representation rep = createTestRepresentation();
            when(representationService.update(eq(TEST_REP_ID), any(Representation.class))).thenReturn(rep);

            mockMvc.perform(put("/api/v1/representations/{id}", TEST_REP_ID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rep)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Representation updated"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/representations/{id}")
    class RevokeRepresentation {

        @Test
        @DisplayName("Should revoke representation with iam-admin role")
        void revokeWithIamAdmin() throws Exception {
            doNothing().when(representationService).revoke(TEST_REP_ID);

            mockMvc.perform(delete("/api/v1/representations/{id}", TEST_REP_ID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Representation revoked"));
        }

        @Test
        @DisplayName("Should return 403 for internal-user on revoke")
        void revokeForbiddenForInternalUser() throws Exception {
            mockMvc.perform(delete("/api/v1/representations/{id}", TEST_REP_ID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_INTERNAL, TestConstants.ROLE_INTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/representations/{id}/verify")
    class VerifyRepresentation {

        @Test
        @DisplayName("Should verify representation with iam-admin role")
        void verifyWithIamAdmin() throws Exception {
            Representation rep = createTestRepresentation();
            rep.setVerificationStatus(VerificationStatus.VERIFIED);
            when(verificationService.verifyRepresentation(TEST_REP_ID)).thenReturn(rep);

            mockMvc.perform(post("/api/v1/representations/{id}/verify", TEST_REP_ID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Representation verified"));
        }

        @Test
        @DisplayName("Should return 403 for tenant-admin on verify")
        void verifyForbiddenForTenantAdmin() throws Exception {
            mockMvc.perform(post("/api/v1/representations/{id}/verify", TEST_REP_ID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isForbidden());
        }
    }
}
