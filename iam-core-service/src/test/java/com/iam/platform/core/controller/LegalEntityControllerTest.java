package com.iam.platform.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.common.enums.EntityType;
import com.iam.platform.common.enums.MemberClass;
import com.iam.platform.common.test.ApiResponseAssertions;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.core.entity.LegalEntity;
import com.iam.platform.core.entity.Representation;
import com.iam.platform.core.service.LegalEntityService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("LegalEntityController Tests")
class LegalEntityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LegalEntityService entityService;

    @MockitoBean
    private RepresentationService representationService;

    private static final UUID TEST_ENTITY_UUID = UUID.fromString(TestConstants.TEST_ENTITY_ID);

    private LegalEntity createTestEntity() {
        LegalEntity entity = new LegalEntity();
        entity.setId(TEST_ENTITY_UUID);
        entity.setRegistrationNumber("REG-001");
        entity.setTaxIdentificationNumber("TIN-001");
        entity.setNameKh("ក្រុមហ៊ុន សាកល្បង");
        entity.setNameEn("Test Company LLC");
        entity.setEntityType(EntityType.PRIVATE_LLC);
        entity.setMemberClass(MemberClass.COM);
        entity.setXroadMemberCode("COM/test-company");
        entity.setRealmName("test-company");
        entity.setProvince("Phnom Penh");
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    @Nested
    @DisplayName("POST /api/v1/entities")
    class CreateEntity {

        @Test
        @DisplayName("Should create entity with iam-admin role")
        void createWithIamAdmin() throws Exception {
            LegalEntity entity = createTestEntity();
            when(entityService.create(any(LegalEntity.class))).thenReturn(entity);

            mockMvc.perform(post("/api/v1/entities")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(entity)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Legal entity registered"))
                    .andExpect(jsonPath("$.data.registrationNumber").value("REG-001"));
        }

        @Test
        @DisplayName("Should return 403 for tenant-admin on create (only iam-admin allowed)")
        void createForbiddenForTenantAdmin() throws Exception {
            LegalEntity entity = createTestEntity();

            mockMvc.perform(post("/api/v1/entities")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(entity)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 for internal-user on create")
        void createForbiddenForInternalUser() throws Exception {
            LegalEntity entity = createTestEntity();

            mockMvc.perform(post("/api/v1/entities")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_INTERNAL, TestConstants.ROLE_INTERNAL_USER))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(entity)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void createUnauthenticated() throws Exception {
            LegalEntity entity = createTestEntity();

            mockMvc.perform(post("/api/v1/entities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(entity)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/entities")
    class ListEntities {

        @Test
        @DisplayName("Should list entities with iam-admin role")
        void listWithIamAdmin() throws Exception {
            LegalEntity entity = createTestEntity();
            Page<LegalEntity> page = new PageImpl<>(List.of(entity));
            when(entityService.findAll(any(Pageable.class))).thenReturn(page);

            ApiResponseAssertions.assertApiSuccess(
                    mockMvc.perform(get("/api/v1/entities")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
            );
        }

        @Test
        @DisplayName("Should list entities with internal-user role")
        void listWithInternalUser() throws Exception {
            Page<LegalEntity> page = new PageImpl<>(List.of());
            when(entityService.findAll(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/entities")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_INTERNAL, TestConstants.ROLE_INTERNAL_USER)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 403 for external-user role")
        void listForbiddenForExternalUser() throws Exception {
            mockMvc.perform(get("/api/v1/entities")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should support pagination parameters")
        void listWithPagination() throws Exception {
            Page<LegalEntity> page = new PageImpl<>(List.of());
            when(entityService.findAll(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/entities")
                            .param("page", "1")
                            .param("size", "5")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/entities/{id}")
    class GetEntityById {

        @Test
        @DisplayName("Should get entity by ID with iam-admin role")
        void getByIdWithIamAdmin() throws Exception {
            LegalEntity entity = createTestEntity();
            when(entityService.findById(TEST_ENTITY_UUID)).thenReturn(entity);

            mockMvc.perform(get("/api/v1/entities/{id}", TEST_ENTITY_UUID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nameEn").value("Test Company LLC"));
        }

        @Test
        @DisplayName("Should get entity by ID with tenant-admin role")
        void getByIdWithTenantAdmin() throws Exception {
            LegalEntity entity = createTestEntity();
            when(entityService.findById(TEST_ENTITY_UUID)).thenReturn(entity);

            mockMvc.perform(get("/api/v1/entities/{id}", TEST_ENTITY_UUID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 403 for internal-user on getById")
        void getByIdForbiddenForInternalUser() throws Exception {
            mockMvc.perform(get("/api/v1/entities/{id}", TEST_ENTITY_UUID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_INTERNAL, TestConstants.ROLE_INTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/entities/{id}")
    class UpdateEntity {

        @Test
        @DisplayName("Should update entity with iam-admin role")
        void updateWithIamAdmin() throws Exception {
            LegalEntity updated = createTestEntity();
            updated.setNameEn("Updated Company");
            when(entityService.update(eq(TEST_ENTITY_UUID), any(LegalEntity.class))).thenReturn(updated);

            mockMvc.perform(put("/api/v1/entities/{id}", TEST_ENTITY_UUID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updated)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Legal entity updated"));
        }

        @Test
        @DisplayName("Should return 403 for external-user on update")
        void updateForbiddenForExternalUser() throws Exception {
            LegalEntity entity = createTestEntity();

            mockMvc.perform(put("/api/v1/entities/{id}", TEST_ENTITY_UUID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(entity)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/entities/{id}/representatives")
    class GetRepresentatives {

        @Test
        @DisplayName("Should list representatives with tenant-admin role")
        void getRepresentativesWithTenantAdmin() throws Exception {
            Page<Representation> page = new PageImpl<>(List.of());
            when(representationService.findByLegalEntity(eq(TEST_ENTITY_UUID), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/entities/{id}/representatives", TEST_ENTITY_UUID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Should return 403 for internal-user on representatives")
        void getRepresentativesForbiddenForInternalUser() throws Exception {
            mockMvc.perform(get("/api/v1/entities/{id}/representatives", TEST_ENTITY_UUID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_INTERNAL, TestConstants.ROLE_INTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }
    }
}
