package com.iam.platform.core.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.common.dto.XRoadContextDto;
import com.iam.platform.common.exception.AccessDeniedException;
import com.iam.platform.common.filter.XRoadRequestFilter;
import com.iam.platform.core.entity.LegalEntity;
import com.iam.platform.core.entity.NaturalPerson;
import com.iam.platform.core.service.AuditService;
import com.iam.platform.core.service.LegalEntityService;
import com.iam.platform.core.service.NaturalPersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/xroad/v1")
@RequiredArgsConstructor
@Tag(name = "X-Road Services", description = "X-Road authenticated endpoints (no JWT)")
public class XRoadServiceController {

    private final LegalEntityService entityService;
    private final NaturalPersonService personService;
    private final AuditService auditService;

    @GetMapping("/taxpayer/{tin}")
    @Operation(summary = "Get taxpayer info by TIN (X-Road)")
    public ApiResponse<LegalEntity> getTaxpayer(@PathVariable String tin,
                                                 HttpServletRequest request) {
        XRoadContextDto ctx = XRoadRequestFilter.getContext();
        LegalEntity entity = entityService.findByTin(tin);
        auditService.logXRoadAccess(
                ctx != null ? ctx.getUserId() : null,
                "GET_TAXPAYER", "taxpayer/" + tin,
                request.getRemoteAddr(), true,
                ctx != null ? Map.of("clientId", ctx.getFullClientId()) : null);
        return ApiResponse.ok(entity);
    }

    @GetMapping("/taxpayer/{tin}/status")
    @Operation(summary = "Get taxpayer status (X-Road)")
    public ApiResponse<Map<String, String>> getTaxpayerStatus(@PathVariable String tin,
                                                                HttpServletRequest request) {
        XRoadContextDto ctx = XRoadRequestFilter.getContext();
        LegalEntity entity = entityService.findByTin(tin);
        auditService.logXRoadAccess(
                ctx != null ? ctx.getUserId() : null,
                "GET_TAXPAYER_STATUS", "taxpayer/" + tin + "/status",
                request.getRemoteAddr(), true, null);
        return ApiResponse.ok(Map.of(
                "tin", entity.getTaxIdentificationNumber(),
                "status", entity.getStatus(),
                "entityType", entity.getEntityType().name()
        ));
    }

    @GetMapping("/declaration/{declarationId}")
    @Operation(summary = "Get declaration by ID (X-Road, GOV only)")
    public ApiResponse<Map<String, String>> getDeclaration(@PathVariable String declarationId,
                                                            HttpServletRequest request) {
        XRoadContextDto ctx = XRoadRequestFilter.getContext();
        if (ctx != null && !ctx.isGovernmentRequest()) {
            throw new AccessDeniedException("Only GOV member class can access declarations");
        }
        auditService.logXRoadAccess(
                ctx != null ? ctx.getUserId() : null,
                "GET_DECLARATION", "declaration/" + declarationId,
                request.getRemoteAddr(), true, null);
        return ApiResponse.ok(Map.of(
                "declarationId", declarationId,
                "status", "PLACEHOLDER"
        ), "Declaration data (placeholder)");
    }

    @GetMapping("/person/{personalIdCode}/verify")
    @Operation(summary = "Verify natural person identity (X-Road)")
    public ApiResponse<Map<String, Object>> verifyPerson(@PathVariable String personalIdCode,
                                                          HttpServletRequest request) {
        XRoadContextDto ctx = XRoadRequestFilter.getContext();
        NaturalPerson person = personService.findByPersonalIdCode(personalIdCode);
        auditService.logXRoadAccess(
                ctx != null ? ctx.getUserId() : null,
                "VERIFY_PERSON", "person/" + personalIdCode,
                request.getRemoteAddr(), true, null);
        return ApiResponse.ok(Map.of(
                "personalIdCode", personalIdCode,
                "verified", person.getIdentityVerificationStatus() != null
                        ? person.getIdentityVerificationStatus().name() : "UNVERIFIED",
                "nameEn", (person.getFirstNameEn() != null ? person.getFirstNameEn() : "")
                        + " " + (person.getLastNameEn() != null ? person.getLastNameEn() : "")
        ));
    }

    @GetMapping("/entity/{registrationNumber}/verify")
    @Operation(summary = "Verify legal entity (X-Road)")
    public ApiResponse<Map<String, Object>> verifyEntity(@PathVariable String registrationNumber,
                                                          HttpServletRequest request) {
        XRoadContextDto ctx = XRoadRequestFilter.getContext();
        LegalEntity entity = entityService.findByRegistrationNumber(registrationNumber);
        auditService.logXRoadAccess(
                ctx != null ? ctx.getUserId() : null,
                "VERIFY_ENTITY", "entity/" + registrationNumber,
                request.getRemoteAddr(), true, null);
        return ApiResponse.ok(Map.of(
                "registrationNumber", registrationNumber,
                "status", entity.getStatus(),
                "entityType", entity.getEntityType().name(),
                "memberClass", entity.getMemberClass().name()
        ));
    }

    @GetMapping("/health")
    @Operation(summary = "X-Road health check")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of("status", "UP", "service", "iam-core-service"));
    }
}
