package com.iam.platform.core.controller;

import com.iam.platform.common.enums.EntityType;
import com.iam.platform.common.enums.MemberClass;
import com.iam.platform.common.enums.VerificationStatus;
import com.iam.platform.common.exception.ResourceNotFoundException;
import com.iam.platform.core.entity.LegalEntity;
import com.iam.platform.core.entity.NaturalPerson;
import com.iam.platform.core.service.AuditService;
import com.iam.platform.core.service.LegalEntityService;
import com.iam.platform.core.service.NaturalPersonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("XRoadServiceController Tests")
class XRoadServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LegalEntityService entityService;

    @MockitoBean
    private NaturalPersonService personService;

    @MockitoBean
    private AuditService auditService;

    private LegalEntity createTestEntity() {
        LegalEntity entity = new LegalEntity();
        entity.setTaxIdentificationNumber("TIN-001");
        entity.setRegistrationNumber("REG-001");
        entity.setNameEn("Test Company");
        entity.setEntityType(EntityType.PRIVATE_LLC);
        entity.setMemberClass(MemberClass.COM);
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    private NaturalPerson createTestPerson() {
        NaturalPerson person = new NaturalPerson();
        person.setPersonalIdCode("KH-123456");
        person.setFirstNameEn("Sok");
        person.setLastNameEn("Dara");
        person.setIdentityVerificationStatus(VerificationStatus.VERIFIED);
        person.setCreatedAt(Instant.now());
        person.setUpdatedAt(Instant.now());
        return person;
    }

    // Standard X-Road headers required by XRoadRequestFilter
    private static final String XROAD_CLIENT = "KH/COM/12345/test-subsystem";
    private static final String XROAD_GOV_CLIENT = "KH/GOV/12345/test-subsystem";
    private static final String XROAD_MESSAGE_ID = "test-message-id-001";

    @Nested
    @DisplayName("X-Road endpoints are permitAll (no JWT required)")
    class XRoadPermitAll {

        @Test
        @DisplayName("GET /xroad/v1/health should be accessible without JWT")
        void healthEndpoint() throws Exception {
            mockMvc.perform(get("/xroad/v1/health")
                            .header("X-Road-Client", XROAD_CLIENT)
                            .header("X-Road-Id", XROAD_MESSAGE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("UP"))
                    .andExpect(jsonPath("$.data.service").value("iam-core-service"));
        }

        @Test
        @DisplayName("GET /xroad/v1/taxpayer/{tin} should be accessible without JWT")
        void taxpayerEndpoint() throws Exception {
            LegalEntity entity = createTestEntity();
            when(entityService.findByTin("TIN-001")).thenReturn(entity);
            doNothing().when(auditService).logXRoadAccess(any(), any(), any(), any(), anyBoolean(), any());

            mockMvc.perform(get("/xroad/v1/taxpayer/{tin}", "TIN-001")
                            .header("X-Road-Client", XROAD_CLIENT)
                            .header("X-Road-Id", XROAD_MESSAGE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.taxIdentificationNumber").value("TIN-001"));
        }

        @Test
        @DisplayName("GET /xroad/v1/taxpayer/{tin}/status should return status map")
        void taxpayerStatusEndpoint() throws Exception {
            LegalEntity entity = createTestEntity();
            when(entityService.findByTin("TIN-001")).thenReturn(entity);
            doNothing().when(auditService).logXRoadAccess(any(), any(), any(), any(), anyBoolean(), any());

            mockMvc.perform(get("/xroad/v1/taxpayer/{tin}/status", "TIN-001")
                            .header("X-Road-Client", XROAD_CLIENT)
                            .header("X-Road-Id", XROAD_MESSAGE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.tin").value("TIN-001"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.entityType").value("PRIVATE_LLC"));
        }

        @Test
        @DisplayName("GET /xroad/v1/person/{personalIdCode}/verify should verify person")
        void verifyPersonEndpoint() throws Exception {
            NaturalPerson person = createTestPerson();
            when(personService.findByPersonalIdCode("KH-123456")).thenReturn(person);
            doNothing().when(auditService).logXRoadAccess(any(), any(), any(), any(), anyBoolean(), any());

            mockMvc.perform(get("/xroad/v1/person/{personalIdCode}/verify", "KH-123456")
                            .header("X-Road-Client", XROAD_CLIENT)
                            .header("X-Road-Id", XROAD_MESSAGE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.personalIdCode").value("KH-123456"))
                    .andExpect(jsonPath("$.data.verified").value("VERIFIED"));
        }

        @Test
        @DisplayName("GET /xroad/v1/entity/{registrationNumber}/verify should verify entity")
        void verifyEntityEndpoint() throws Exception {
            LegalEntity entity = createTestEntity();
            when(entityService.findByRegistrationNumber("REG-001")).thenReturn(entity);
            doNothing().when(auditService).logXRoadAccess(any(), any(), any(), any(), anyBoolean(), any());

            mockMvc.perform(get("/xroad/v1/entity/{registrationNumber}/verify", "REG-001")
                            .header("X-Road-Client", XROAD_CLIENT)
                            .header("X-Road-Id", XROAD_MESSAGE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.registrationNumber").value("REG-001"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.memberClass").value("COM"));
        }

        @Test
        @DisplayName("GET /xroad/v1/declaration/{declarationId} should return placeholder")
        void declarationEndpoint() throws Exception {
            doNothing().when(auditService).logXRoadAccess(any(), any(), any(), any(), anyBoolean(), any());

            mockMvc.perform(get("/xroad/v1/declaration/{declarationId}", "DECL-001")
                            .header("X-Road-Client", XROAD_GOV_CLIENT)
                            .header("X-Road-Id", XROAD_MESSAGE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.declarationId").value("DECL-001"))
                    .andExpect(jsonPath("$.data.status").value("PLACEHOLDER"));
        }
    }

    @Nested
    @DisplayName("X-Road error scenarios")
    class XRoadErrors {

        @Test
        @DisplayName("Should return error when taxpayer TIN not found")
        void taxpayerNotFound() throws Exception {
            when(entityService.findByTin("INVALID"))
                    .thenThrow(new ResourceNotFoundException("LegalEntity", "INVALID"));

            mockMvc.perform(get("/xroad/v1/taxpayer/{tin}", "INVALID")
                            .header("X-Road-Client", XROAD_CLIENT)
                            .header("X-Road-Id", XROAD_MESSAGE_ID))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Should return error when person not found by personalIdCode")
        void personNotFound() throws Exception {
            when(personService.findByPersonalIdCode("INVALID"))
                    .thenThrow(new ResourceNotFoundException("NaturalPerson", "INVALID"));

            mockMvc.perform(get("/xroad/v1/person/{personalIdCode}/verify", "INVALID")
                            .header("X-Road-Client", XROAD_CLIENT)
                            .header("X-Road-Id", XROAD_MESSAGE_ID))
                    .andExpect(status().is4xxClientError());
        }
    }
}
