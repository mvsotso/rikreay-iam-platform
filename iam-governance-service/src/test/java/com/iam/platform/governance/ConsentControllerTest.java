package com.iam.platform.governance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.common.test.ApiResponseAssertions;
import com.iam.platform.common.test.JwtTestUtils;
import com.iam.platform.common.test.TestConstants;
import com.iam.platform.governance.dto.ConsentRequest;
import com.iam.platform.governance.dto.ConsentResponse;
import com.iam.platform.governance.enums.ConsentMethod;
import com.iam.platform.governance.enums.DataSubjectType;
import com.iam.platform.governance.enums.LegalBasis;
import com.iam.platform.governance.service.ConsentService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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
class ConsentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ConsentService consentService;

    private static final UUID TEST_CONSENT_ID = UUID.randomUUID();
    private static final UUID TEST_SUBJECT_ID = UUID.fromString(TestConstants.TEST_PERSON_ID);

    private ConsentRequest validConsentRequest() {
        return new ConsentRequest(
                DataSubjectType.NATURAL_PERSON,
                TEST_SUBJECT_ID,
                "MARKETING",
                LegalBasis.CONSENT,
                ConsentMethod.ELECTRONIC,
                Instant.now().plusSeconds(86400 * 365),
                List.of("name", "email"),
                false,
                false
        );
    }

    private ConsentResponse sampleConsentResponse() {
        return new ConsentResponse(
                TEST_CONSENT_ID,
                DataSubjectType.NATURAL_PERSON,
                TEST_SUBJECT_ID,
                "MARKETING",
                LegalBasis.CONSENT,
                true,
                Instant.now(),
                ConsentMethod.ELECTRONIC,
                null,
                Instant.now().plusSeconds(86400 * 365),
                List.of("name", "email"),
                false,
                false,
                Instant.now()
        );
    }

    @Nested
    @DisplayName("POST /api/v1/governance/consents — Give consent")
    class GiveConsent {

        @Test
        @DisplayName("Should succeed for any authenticated user (external-user)")
        void giveConsent_authenticatedUser_shouldSucceed() throws Exception {
            when(consentService.giveConsent(any(ConsentRequest.class), anyString()))
                    .thenReturn(sampleConsentResponse());

            mockMvc.perform(post("/api/v1/governance/consents")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validConsentRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.purpose").value("MARKETING"))
                    .andExpect(jsonPath("$.data.consentGiven").value(true));
        }

        @Test
        @DisplayName("Should succeed for internal-user")
        void giveConsent_internalUser_shouldSucceed() throws Exception {
            when(consentService.giveConsent(any(ConsentRequest.class), anyString()))
                    .thenReturn(sampleConsentResponse());

            mockMvc.perform(post("/api/v1/governance/consents")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_INTERNAL, TestConstants.ROLE_INTERNAL_USER))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validConsentRequest())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void giveConsent_unauthenticated_shouldReturn401() throws Exception {
            mockMvc.perform(post("/api/v1/governance/consents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validConsentRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should capture LPDP legal basis correctly")
        void giveConsent_lpdpLegalBasis_shouldReturnCorrectBasis() throws Exception {
            ConsentRequest request = new ConsentRequest(
                    DataSubjectType.NATURAL_PERSON,
                    TEST_SUBJECT_ID,
                    "DATA_PROCESSING",
                    LegalBasis.LEGAL_OBLIGATION,
                    ConsentMethod.WRITTEN,
                    null,
                    List.of("financial_data"),
                    false,
                    false
            );

            ConsentResponse response = new ConsentResponse(
                    UUID.randomUUID(), DataSubjectType.NATURAL_PERSON, TEST_SUBJECT_ID,
                    "DATA_PROCESSING", LegalBasis.LEGAL_OBLIGATION, true,
                    Instant.now(), ConsentMethod.WRITTEN, null, null,
                    List.of("financial_data"), false, false, Instant.now()
            );

            when(consentService.giveConsent(any(ConsentRequest.class), anyString()))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/governance/consents")
                            .with(JwtTestUtils.jwtWithRoles("user", TestConstants.ROLE_EXTERNAL_USER))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.legalBasis").value("LEGAL_OBLIGATION"));
        }

        @Test
        @DisplayName("Should handle cross-border transfer flag for LPDP compliance")
        void giveConsent_crossBorderTransfer_shouldPersistFlag() throws Exception {
            ConsentRequest request = new ConsentRequest(
                    DataSubjectType.NATURAL_PERSON, TEST_SUBJECT_ID,
                    "INTERNATIONAL_TRANSFER", LegalBasis.CONSENT,
                    ConsentMethod.ELECTRONIC, null, List.of("personal_data"),
                    true, true
            );

            ConsentResponse response = new ConsentResponse(
                    UUID.randomUUID(), DataSubjectType.NATURAL_PERSON, TEST_SUBJECT_ID,
                    "INTERNATIONAL_TRANSFER", LegalBasis.CONSENT, true,
                    Instant.now(), ConsentMethod.ELECTRONIC, null, null,
                    List.of("personal_data"), true, true, Instant.now()
            );

            when(consentService.giveConsent(any(), anyString())).thenReturn(response);

            mockMvc.perform(post("/api/v1/governance/consents")
                            .with(JwtTestUtils.jwtWithRoles("user", TestConstants.ROLE_EXTERNAL_USER))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.crossBorderTransfer").value(true))
                    .andExpect(jsonPath("$.data.thirdPartySharing").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/governance/consents/me — My consents")
    class MyConsents {

        @Test
        @DisplayName("Should return own consents for any authenticated user")
        void getMyConsents_authenticated_shouldSucceed() throws Exception {
            when(consentService.getActiveConsents(any(UUID.class)))
                    .thenReturn(List.of(sampleConsentResponse()));

            // The controller calls UUID.fromString(jwt.getSubject()), so the subject must be a valid UUID.
            // Use a JWT with a UUID subject instead of the default "user-id-<username>" format.
            var jwt = JwtTestUtils.createJwt(TestConstants.TEST_PERSON_ID, TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER);
            mockMvc.perform(get("/api/v1/governance/consents/me")
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                    .jwt().jwt(jwt).authorities(
                                            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + TestConstants.ROLE_EXTERNAL_USER))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void getMyConsents_unauthenticated_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/v1/governance/consents/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/governance/consents/{id} — Withdraw consent")
    class WithdrawConsent {

        @Test
        @DisplayName("Should allow any authenticated user to withdraw consent")
        void withdrawConsent_authenticated_shouldSucceed() throws Exception {
            doNothing().when(consentService).withdrawConsent(any(UUID.class));

            mockMvc.perform(delete("/api/v1/governance/consents/" + TEST_CONSENT_ID)
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_CITIZEN, TestConstants.ROLE_EXTERNAL_USER)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Consent withdrawn"));

            verify(consentService).withdrawConsent(TEST_CONSENT_ID);
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void withdrawConsent_unauthenticated_shouldReturn401() throws Exception {
            mockMvc.perform(delete("/api/v1/governance/consents/" + TEST_CONSENT_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/governance/consents — Admin list all consents")
    class AdminListConsents {

        @Test
        @DisplayName("Should succeed for governance-admin")
        void listConsents_governanceAdmin_shouldSucceed() throws Exception {
            Page<ConsentResponse> page = new PageImpl<>(List.of(sampleConsentResponse()));
            when(consentService.listAllConsents(any(Pageable.class))).thenReturn(page);

            ApiResponseAssertions.assertApiSuccess(
                    mockMvc.perform(get("/api/v1/governance/consents")
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN))));
        }

        @Test
        @DisplayName("Should succeed for iam-admin")
        void listConsents_iamAdmin_shouldSucceed() throws Exception {
            Page<ConsentResponse> page = new PageImpl<>(List.of(sampleConsentResponse()));
            when(consentService.listAllConsents(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/governance/consents")
                            .with(JwtTestUtils.jwtWithRoles(TestConstants.USER_ADMIN, TestConstants.ROLE_IAM_ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 403 for external-user")
        void listConsents_externalUser_shouldBeForbidden() throws Exception {
            ApiResponseAssertions.assertForbidden(
                    mockMvc.perform(get("/api/v1/governance/consents")
                            .with(JwtTestUtils.jwtWithRoles("user", TestConstants.ROLE_EXTERNAL_USER))));
        }

        @Test
        @DisplayName("Should return 403 for tenant-admin")
        void listConsents_tenantAdmin_shouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/governance/consents")
                            .with(JwtTestUtils.jwtWithRoles("user", TestConstants.ROLE_TENANT_ADMIN)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/governance/consents/check — Check consent")
    class CheckConsent {

        @Test
        @DisplayName("Should succeed for governance-admin")
        void checkConsent_governanceAdmin_shouldSucceed() throws Exception {
            when(consentService.checkConsent(any(UUID.class), anyString())).thenReturn(true);

            mockMvc.perform(get("/api/v1/governance/consents/check")
                            .param("dataSubjectId", TEST_SUBJECT_ID.toString())
                            .param("purpose", "MARKETING")
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(true));
        }

        @Test
        @DisplayName("Should return false when consent does not exist")
        void checkConsent_noConsent_shouldReturnFalse() throws Exception {
            when(consentService.checkConsent(any(UUID.class), anyString())).thenReturn(false);

            mockMvc.perform(get("/api/v1/governance/consents/check")
                            .param("dataSubjectId", TEST_SUBJECT_ID.toString())
                            .param("purpose", "ANALYTICS")
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/governance/consents/export/{dataSubjectId} — Export consents")
    class ExportConsents {

        @Test
        @DisplayName("Should succeed for governance-admin (data subject access request)")
        void exportConsents_governanceAdmin_shouldSucceed() throws Exception {
            when(consentService.exportConsents(any(UUID.class)))
                    .thenReturn(List.of(sampleConsentResponse()));

            mockMvc.perform(get("/api/v1/governance/consents/export/" + TEST_SUBJECT_ID)
                            .with(JwtTestUtils.jwtWithRoles("gov-admin", TestConstants.ROLE_GOVERNANCE_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].purpose").value("MARKETING"));
        }

        @Test
        @DisplayName("Should return 403 for ops-admin")
        void exportConsents_opsAdmin_shouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/governance/consents/export/" + TEST_SUBJECT_ID)
                            .with(JwtTestUtils.jwtWithRoles("ops", TestConstants.ROLE_OPS_ADMIN)))
                    .andExpect(status().isForbidden());
        }
    }
}
