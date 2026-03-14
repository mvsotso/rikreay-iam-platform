package com.iam.platform.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.admin.dto.SectorAdminRequest;
import com.iam.platform.admin.entity.SectorAdminAssignment;
import com.iam.platform.admin.enums.AssignmentStatus;
import com.iam.platform.admin.repository.SectorAdminAssignmentRepository;
import com.iam.platform.common.enums.MemberClass;
import com.iam.platform.common.test.ApiResponseAssertions;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SectorAdminController Tests")
class SectorAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SectorAdminAssignmentRepository assignmentRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final UUID TEST_PERSON_UUID = UUID.fromString(TestConstants.TEST_PERSON_ID);
    private static final UUID TEST_ASSIGNMENT_ID = UUID.randomUUID();

    private SectorAdminAssignment createAssignment() {
        SectorAdminAssignment assignment = SectorAdminAssignment.builder()
                .naturalPersonId(TEST_PERSON_UUID)
                .memberClass(MemberClass.GOV)
                .assignedByUserId("admin-user")
                .validFrom(Instant.now())
                .validUntil(Instant.now().plusSeconds(86400 * 365))
                .status(AssignmentStatus.ACTIVE)
                .build();
        assignment.setId(TEST_ASSIGNMENT_ID);
        assignment.setCreatedAt(Instant.now());
        return assignment;
    }

    @Nested
    @DisplayName("POST /api/v1/platform-admin/sector-admins")
    class AssignSectorAdmin {

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void unauthenticated_returns401() throws Exception {
            SectorAdminRequest request = new SectorAdminRequest(
                    TEST_PERSON_UUID, MemberClass.GOV, Instant.now(), null
            );

            mockMvc.perform(post("/api/v1/platform-admin/sector-admins")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 201 for iam-admin")
        void iamAdmin_assignsSuccessfully() throws Exception {
            SectorAdminRequest request = new SectorAdminRequest(
                    TEST_PERSON_UUID, MemberClass.GOV, Instant.now(), null
            );

            SectorAdminAssignment saved = createAssignment();
            when(assignmentRepository.save(any(SectorAdminAssignment.class))).thenReturn(saved);

            mockMvc.perform(post("/api/v1/platform-admin/sector-admins")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Sector admin assigned"))
                    .andExpect(jsonPath("$.data.naturalPersonId").value(TEST_PERSON_UUID.toString()))
                    .andExpect(jsonPath("$.data.memberClass").value("GOV"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Should return 403 for tenant-admin")
        void tenantAdmin_forbidden() throws Exception {
            SectorAdminRequest request = new SectorAdminRequest(
                    TEST_PERSON_UUID, MemberClass.COM, Instant.now(), null
            );

            mockMvc.perform(post("/api/v1/platform-admin/sector-admins")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verify(assignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should return 403 for sector-admin (only iam-admin can assign)")
        void sectorAdmin_forbidden() throws Exception {
            SectorAdminRequest request = new SectorAdminRequest(
                    TEST_PERSON_UUID, MemberClass.NGO, Instant.now(), null
            );

            mockMvc.perform(post("/api/v1/platform-admin/sector-admins")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_SECTOR_ADMIN, TestConstants.ROLE_SECTOR_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/platform-admin/sector-admins")
    class ListAssignments {

        @Test
        @DisplayName("Should return 200 with paged assignments for iam-admin")
        void iamAdmin_listsAssignments() throws Exception {
            SectorAdminAssignment assignment = createAssignment();
            Page<SectorAdminAssignment> page = new PageImpl<>(List.of(assignment));
            when(assignmentRepository.findByStatus(eq(AssignmentStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/platform-admin/sector-admins")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());
        }

        @Test
        @DisplayName("Should return 403 for tenant-admin")
        void tenantAdmin_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/platform-admin/sector-admins")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_TENANT_ADMIN, TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/platform-admin/sector-admins/{id}")
    class RevokeAssignment {

        @Test
        @DisplayName("Should return 200 for iam-admin revoking assignment")
        void iamAdmin_revokesSuccessfully() throws Exception {
            SectorAdminAssignment assignment = createAssignment();
            when(assignmentRepository.findById(TEST_ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
            when(assignmentRepository.save(any(SectorAdminAssignment.class))).thenReturn(assignment);

            mockMvc.perform(delete("/api/v1/platform-admin/sector-admins/" + TEST_ASSIGNMENT_ID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Sector admin assignment revoked"));
        }

        @Test
        @DisplayName("Should return 403 for sector-admin")
        void sectorAdmin_forbidden() throws Exception {
            mockMvc.perform(delete("/api/v1/platform-admin/sector-admins/" + TEST_ASSIGNMENT_ID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_SECTOR_ADMIN, TestConstants.ROLE_SECTOR_ADMIN)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 for external-user")
        void externalUser_forbidden() throws Exception {
            mockMvc.perform(delete("/api/v1/platform-admin/sector-admins/" + TEST_ASSIGNMENT_ID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER)))
                    .andExpect(status().isForbidden());
        }
    }
}
