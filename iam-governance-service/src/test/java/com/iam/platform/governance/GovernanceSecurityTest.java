package com.iam.platform.governance;

import com.iam.platform.common.test.JwtTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GovernanceSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Health endpoint should be public")
    void healthEndpointPermitAll() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Governance API should return 401 without auth")
    void governanceUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/governance/campaigns"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Campaigns should return 403 for external-user")
    void campaignsExternalUserForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/governance/campaigns")
                        .with(JwtTestUtils.jwtWithRoles("user", "external-user")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Campaigns should return 200 for governance-admin")
    void campaignsGovernanceAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/governance/campaigns")
                        .with(JwtTestUtils.jwtWithRoles("gov-admin", "governance-admin")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Campaigns should return 200 for iam-admin")
    void campaignsIamAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/governance/campaigns")
                        .with(JwtTestUtils.jwtWithRoles("admin", "iam-admin")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Campaigns should return 403 for tenant-admin (create)")
    void campaignCreateTenantAdminForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/governance/campaigns")
                        .with(JwtTestUtils.jwtWithRoles("user", "tenant-admin"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // Consent endpoints — authenticated users
    @Test
    @DisplayName("Give consent should succeed for any authenticated user")
    void consentAuthenticated() throws Exception {
        // Use valid UUID for dataSubjectId and valid ConsentMethod enum value (ELECTRONIC, WRITTEN, or VERBAL)
        mockMvc.perform(post("/api/v1/governance/consents")
                        .with(JwtTestUtils.jwtWithRoles("citizen", "external-user"))
                        .contentType("application/json")
                        .content("{\"dataSubjectId\":\"550e8400-e29b-41d4-a716-446655440000\",\"purpose\":\"MARKETING\",\"dataSubjectType\":\"NATURAL_PERSON\",\"legalBasis\":\"CONSENT\",\"consentMethod\":\"ELECTRONIC\",\"dataCategories\":[\"name\"]}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Get own consents should succeed for any authenticated user")
    void myConsentsAuthenticated() throws Exception {
        // The controller calls UUID.fromString(jwt.getSubject()), so the subject must be a valid UUID.
        // Use a unique UUID that has no existing consent records to avoid H2 JSON deserialization issues.
        var jwt = JwtTestUtils.createJwt("660e8400-e29b-41d4-a716-446655440099", "citizen", "external-user");
        mockMvc.perform(get("/api/v1/governance/consents/me")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .jwt().jwt(jwt).authorities(
                                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_external-user"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin view consents should return 403 for external-user")
    void adminConsentsExternalForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/governance/consents")
                        .with(JwtTestUtils.jwtWithRoles("user", "external-user")))
                .andExpect(status().isForbidden());
    }

    // Reports — report-viewer, governance-admin, iam-admin
    @Test
    @DisplayName("Reports should return 200 for report-viewer")
    void reportsReportViewer() throws Exception {
        mockMvc.perform(get("/api/v1/governance/reports/compliance")
                        .with(JwtTestUtils.jwtWithRoles("viewer", "report-viewer")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Reports should return 403 for tenant-admin")
    void reportsTenantAdminForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/governance/reports/compliance")
                        .with(JwtTestUtils.jwtWithRoles("user", "tenant-admin")))
                .andExpect(status().isForbidden());
    }

    // Policies — governance-admin, iam-admin
    @Test
    @DisplayName("Policies should return 200 for governance-admin")
    void policiesGovernanceAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/governance/policies")
                        .with(JwtTestUtils.jwtWithRoles("gov-admin", "governance-admin")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Policies should return 403 for ops-admin")
    void policiesOpsAdminForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/governance/policies")
                        .with(JwtTestUtils.jwtWithRoles("ops", "ops-admin")))
                .andExpect(status().isForbidden());
    }
}
