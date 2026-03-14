package com.iam.platform.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.common.enums.VerificationStatus;
import com.iam.platform.common.exception.ResourceNotFoundException;
import com.iam.platform.common.test.ApiResponseAssertions;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.core.entity.NaturalPerson;
import com.iam.platform.core.entity.Representation;
import com.iam.platform.core.service.IdentityVerificationService;
import com.iam.platform.core.service.NaturalPersonService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("NaturalPersonController Tests")
class NaturalPersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NaturalPersonService personService;

    @MockitoBean
    private RepresentationService representationService;

    @MockitoBean
    private IdentityVerificationService verificationService;

    private static final UUID TEST_PERSON_UUID = UUID.fromString(TestConstants.TEST_PERSON_ID);

    private NaturalPerson createTestPerson() {
        NaturalPerson person = new NaturalPerson();
        person.setId(TEST_PERSON_UUID);
        person.setPersonalIdCode("KH-123456789");
        person.setNationalIdNumber("012345678");
        person.setFirstNameKh("សុខ");
        person.setLastNameKh("ដារា");
        person.setFirstNameEn("Sok");
        person.setLastNameEn("Dara");
        person.setDateOfBirth(LocalDate.of(1990, 1, 15));
        person.setGender("MALE");
        person.setNationality("KH");
        person.setKeycloakUserId("user-id-test");
        person.setStatus("ACTIVE");
        person.setIdentityVerificationStatus(VerificationStatus.UNVERIFIED);
        person.setCreatedAt(Instant.now());
        person.setUpdatedAt(Instant.now());
        return person;
    }

    @Nested
    @DisplayName("POST /api/v1/persons")
    class CreatePerson {

        @Test
        @DisplayName("Should create person with iam-admin role")
        void createWithIamAdmin() throws Exception {
            NaturalPerson person = createTestPerson();
            when(personService.create(any(NaturalPerson.class))).thenReturn(person);

            mockMvc.perform(post("/api/v1/persons")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(person)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Natural person registered"))
                    .andExpect(jsonPath("$.data.personalIdCode").value("KH-123456789"));
        }

        @Test
        @DisplayName("Should create person with tenant-admin role")
        void createWithTenantAdmin() throws Exception {
            NaturalPerson person = createTestPerson();
            when(personService.create(any(NaturalPerson.class))).thenReturn(person);

            mockMvc.perform(post("/api/v1/persons")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(person)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should return 403 for internal-user role on create")
        void createForbiddenForInternalUser() throws Exception {
            NaturalPerson person = createTestPerson();

            mockMvc.perform(post("/api/v1/persons")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_INTERNAL, TestConstants.ROLE_INTERNAL_USER))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(person)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void createUnauthenticated() throws Exception {
            NaturalPerson person = createTestPerson();

            mockMvc.perform(post("/api/v1/persons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(person)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/persons")
    class ListPersons {

        @Test
        @DisplayName("Should list persons with iam-admin role")
        void listWithIamAdmin() throws Exception {
            NaturalPerson person = createTestPerson();
            Page<NaturalPerson> page = new PageImpl<>(List.of(person));
            when(personService.findAll(any(Pageable.class))).thenReturn(page);

            ApiResponseAssertions.assertApiSuccess(
                    mockMvc.perform(get("/api/v1/persons")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
            );
        }

        @Test
        @DisplayName("Should list persons with internal-user role")
        void listWithInternalUser() throws Exception {
            Page<NaturalPerson> page = new PageImpl<>(List.of());
            when(personService.findAll(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/persons")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_INTERNAL, TestConstants.ROLE_INTERNAL_USER)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should list persons with tenant-admin role")
        void listWithTenantAdmin() throws Exception {
            Page<NaturalPerson> page = new PageImpl<>(List.of());
            when(personService.findAll(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/persons")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 403 for external-user role")
        void listForbiddenForExternalUser() throws Exception {
            mockMvc.perform(get("/api/v1/persons")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should support pagination parameters")
        void listWithPagination() throws Exception {
            Page<NaturalPerson> page = new PageImpl<>(List.of());
            when(personService.findAll(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/persons")
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "createdAt,desc")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/persons/{id}")
    class GetPersonById {

        @Test
        @DisplayName("Should get person by ID with iam-admin role")
        void getByIdWithIamAdmin() throws Exception {
            NaturalPerson person = createTestPerson();
            when(personService.findById(TEST_PERSON_UUID)).thenReturn(person);

            mockMvc.perform(get("/api/v1/persons/{id}", TEST_PERSON_UUID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.firstNameEn").value("Sok"));
        }

        @Test
        @DisplayName("Should return 403 for internal-user on getById")
        void getByIdForbiddenForInternalUser() throws Exception {
            // getById requires iam-admin or tenant-admin (PreAuthorize)
            mockMvc.perform(get("/api/v1/persons/{id}", TEST_PERSON_UUID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_INTERNAL, TestConstants.ROLE_INTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/persons/{id}")
    class UpdatePerson {

        @Test
        @DisplayName("Should update person with iam-admin role")
        void updateWithIamAdmin() throws Exception {
            NaturalPerson updated = createTestPerson();
            updated.setFirstNameEn("UpdatedName");
            when(personService.update(eq(TEST_PERSON_UUID), any(NaturalPerson.class))).thenReturn(updated);

            mockMvc.perform(put("/api/v1/persons/{id}", TEST_PERSON_UUID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updated)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Natural person updated"));
        }

        @Test
        @DisplayName("Should return 403 for external-user on update")
        void updateForbiddenForExternalUser() throws Exception {
            NaturalPerson person = createTestPerson();

            mockMvc.perform(put("/api/v1/persons/{id}", TEST_PERSON_UUID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(person)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/persons/me")
    class GetOwnProfile {

        @Test
        @DisplayName("Should get own profile for any authenticated user")
        void getOwnProfile() throws Exception {
            NaturalPerson person = createTestPerson();
            when(personService.findByKeycloakUserId("user-id-citizen-user")).thenReturn(person);

            mockMvc.perform(get("/api/v1/persons/me")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.firstNameEn").value("Sok"));
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void getOwnProfileUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/persons/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/persons/me")
    class UpdateOwnProfile {

        @Test
        @DisplayName("Should update own profile for any authenticated user")
        void updateOwnProfile() throws Exception {
            NaturalPerson existing = createTestPerson();
            NaturalPerson updated = createTestPerson();
            updated.setFirstNameEn("NewName");
            when(personService.findByKeycloakUserId("user-id-citizen-user")).thenReturn(existing);
            when(personService.update(eq(TEST_PERSON_UUID), any(NaturalPerson.class))).thenReturn(updated);

            mockMvc.perform(put("/api/v1/persons/me")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updated)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Profile updated"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/persons/{id}/verify")
    class VerifyPerson {

        @Test
        @DisplayName("Should verify person with iam-admin role")
        void verifyWithIamAdmin() throws Exception {
            NaturalPerson verified = createTestPerson();
            verified.setIdentityVerificationStatus(VerificationStatus.VERIFIED);
            when(verificationService.verifyNaturalPerson(eq(TEST_PERSON_UUID), eq("DOCUMENT")))
                    .thenReturn(verified);

            mockMvc.perform(post("/api/v1/persons/{id}/verify", TEST_PERSON_UUID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"method\":\"DOCUMENT\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Identity verification initiated"));
        }

        @Test
        @DisplayName("Should return 403 for tenant-admin on verify")
        void verifyForbiddenForTenantAdmin() throws Exception {
            mockMvc.perform(post("/api/v1/persons/{id}/verify", TEST_PERSON_UUID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"method\":\"DOCUMENT\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/persons/{id}/representations")
    class GetPersonRepresentations {

        @Test
        @DisplayName("Should list representations for a person")
        void getRepresentations() throws Exception {
            Page<Representation> page = new PageImpl<>(List.of());
            when(representationService.findByNaturalPerson(eq(TEST_PERSON_UUID), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/persons/{id}/representations", TEST_PERSON_UUID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
