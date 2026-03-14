package com.iam.platform.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.RbacTestSupport;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.core.entity.LegalEntity;
import com.iam.platform.core.entity.NaturalPerson;
import com.iam.platform.core.entity.Representation;
import com.iam.platform.core.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive RBAC matrix test for iam-core-service.
 * Tests ALL 13 roles against each endpoint group to verify access control.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Core Service RBAC Matrix Tests")
class CoreRbacMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NaturalPersonService personService;

    @MockitoBean
    private LegalEntityService entityService;

    @MockitoBean
    private RepresentationService representationService;

    @MockitoBean
    private IdentityVerificationService verificationService;

    @MockitoBean
    private ExternalIdentityLinkService externalIdentityLinkService;

    @MockitoBean
    private AuditService auditService;

    private static final UUID TEST_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @BeforeEach
    void setupMocks() {
        // Setup mocks so that authorized requests succeed (don't throw exceptions)
        NaturalPerson person = new NaturalPerson();
        person.setId(TEST_ID);
        person.setKeycloakUserId("kc-user-1");
        person.setStatus("ACTIVE");

        LegalEntity entity = new LegalEntity();
        entity.setId(TEST_ID);
        entity.setStatus("ACTIVE");

        Representation rep = new Representation();
        rep.setId(TEST_ID);

        when(personService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(personService.findById(any(UUID.class))).thenReturn(person);
        when(personService.create(any(NaturalPerson.class))).thenReturn(person);
        when(personService.update(any(UUID.class), any(NaturalPerson.class))).thenReturn(person);
        when(personService.findByKeycloakUserId(any())).thenReturn(person);

        when(entityService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(entityService.findById(any(UUID.class))).thenReturn(entity);
        when(entityService.create(any(LegalEntity.class))).thenReturn(entity);
        when(entityService.update(any(UUID.class), any(LegalEntity.class))).thenReturn(entity);

        when(representationService.findByLegalEntity(any(UUID.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(representationService.findByNaturalPerson(any(UUID.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(representationService.create(any(Representation.class))).thenReturn(rep);
        when(representationService.update(any(UUID.class), any(Representation.class))).thenReturn(rep);
        doNothing().when(representationService).revoke(any(UUID.class));

        when(verificationService.verifyNaturalPerson(any(UUID.class), any())).thenReturn(person);
        when(verificationService.verifyRepresentation(any(UUID.class))).thenReturn(rep);

        when(externalIdentityLinkService.findByOwner(any(), any())).thenReturn(List.of());
    }

    // ===========================
    // GET /api/v1/persons
    // Allowed: internal-user, tenant-admin, iam-admin
    // ===========================
    static Stream<Arguments> personsListMatrix() {
        return RbacTestSupport.fullMatrix("GET", "/api/v1/persons",
                TestConstants.ROLE_INTERNAL_USER,
                TestConstants.ROLE_TENANT_ADMIN,
                TestConstants.ROLE_IAM_ADMIN);
    }

    // ===========================
    // POST /api/v1/persons
    // Allowed: tenant-admin, iam-admin (PreAuthorize on controller)
    // But SecurityConfig allows internal-user, tenant-admin, iam-admin at URL level
    // PreAuthorize on the method further restricts to tenant-admin, iam-admin
    // ===========================
    static Stream<Arguments> personsCreateMatrix() {
        return RbacTestSupport.fullMatrix("POST", "/api/v1/persons",
                TestConstants.ROLE_TENANT_ADMIN,
                TestConstants.ROLE_IAM_ADMIN);
    }

    // ===========================
    // GET /api/v1/entities
    // Allowed: internal-user, tenant-admin, iam-admin
    // ===========================
    static Stream<Arguments> entitiesListMatrix() {
        return RbacTestSupport.fullMatrix("GET", "/api/v1/entities",
                TestConstants.ROLE_INTERNAL_USER,
                TestConstants.ROLE_TENANT_ADMIN,
                TestConstants.ROLE_IAM_ADMIN);
    }

    // ===========================
    // POST /api/v1/entities
    // Allowed: iam-admin (PreAuthorize on controller)
    // SecurityConfig allows internal-user, tenant-admin, iam-admin at URL level
    // PreAuthorize further restricts to iam-admin only
    // ===========================
    static Stream<Arguments> entitiesCreateMatrix() {
        return RbacTestSupport.fullMatrix("POST", "/api/v1/entities",
                TestConstants.ROLE_IAM_ADMIN);
    }

    // ===========================
    // GET /api/v1/representations
    // Allowed: tenant-admin, iam-admin
    // ===========================
    static Stream<Arguments> representationsListMatrix() {
        return RbacTestSupport.fullMatrix("GET", "/api/v1/representations",
                TestConstants.ROLE_TENANT_ADMIN,
                TestConstants.ROLE_IAM_ADMIN);
    }

    // ===========================
    // POST /api/v1/representations
    // Allowed: tenant-admin, iam-admin
    // ===========================
    static Stream<Arguments> representationsCreateMatrix() {
        return RbacTestSupport.fullMatrix("POST", "/api/v1/representations",
                TestConstants.ROLE_TENANT_ADMIN,
                TestConstants.ROLE_IAM_ADMIN);
    }

    // ===========================
    // DELETE /api/v1/representations/{id}
    // Allowed: tenant-admin, iam-admin
    // ===========================
    static Stream<Arguments> representationsDeleteMatrix() {
        return RbacTestSupport.fullMatrix("DELETE", "/api/v1/representations/" + TEST_ID,
                TestConstants.ROLE_TENANT_ADMIN,
                TestConstants.ROLE_IAM_ADMIN);
    }

    // ===========================
    // GET /api/v1/users
    // Allowed: internal-user, tenant-admin, iam-admin
    // ===========================
    static Stream<Arguments> usersListMatrix() {
        return RbacTestSupport.fullMatrix("GET", "/api/v1/users",
                TestConstants.ROLE_INTERNAL_USER,
                TestConstants.ROLE_TENANT_ADMIN,
                TestConstants.ROLE_IAM_ADMIN);
    }

    // ===========================
    // Combined full matrix
    // ===========================
    @SuppressWarnings("unchecked")
    static Stream<Arguments> fullRbacMatrix() {
        return RbacTestSupport.combineMatrices(
                personsListMatrix(),
                personsCreateMatrix(),
                entitiesListMatrix(),
                entitiesCreateMatrix(),
                representationsListMatrix(),
                representationsCreateMatrix(),
                representationsDeleteMatrix(),
                usersListMatrix()
        );
    }

    @ParameterizedTest(name = "{0} {1} with role [{2}] should {3}")
    @MethodSource("fullRbacMatrix")
    @DisplayName("RBAC Matrix: role-based access control verification")
    void verifyRbacMatrix(String method, String endpoint, String role, boolean shouldSucceed) throws Exception {
        MockHttpServletRequestBuilder request = buildRequest(method, endpoint);
        request.with(JwtTestUtils.jwtWithRoles("rbac-test-user", role));

        ResultActions result = mockMvc.perform(request);

        if (shouldSucceed) {
            result.andExpect(status().is2xxSuccessful());
        } else {
            result.andExpect(status().isForbidden());
        }
    }

    private MockHttpServletRequestBuilder buildRequest(String method, String endpoint) throws Exception {
        return switch (method) {
            case "GET" -> get(endpoint);
            case "POST" -> {
                String body = buildRequestBody(endpoint);
                yield post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body);
            }
            case "PUT" -> {
                String body = buildRequestBody(endpoint);
                yield put(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body);
            }
            case "DELETE" -> delete(endpoint);
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        };
    }

    private String buildRequestBody(String endpoint) throws Exception {
        if (endpoint.contains("/persons")) {
            NaturalPerson person = new NaturalPerson();
            person.setFirstNameEn("Test");
            person.setLastNameEn("User");
            return objectMapper.writeValueAsString(person);
        } else if (endpoint.contains("/entities")) {
            LegalEntity entity = new LegalEntity();
            entity.setNameEn("Test Entity");
            return objectMapper.writeValueAsString(entity);
        } else if (endpoint.contains("/representations")) {
            Representation rep = new Representation();
            return objectMapper.writeValueAsString(rep);
        }
        return "{}";
    }
}
