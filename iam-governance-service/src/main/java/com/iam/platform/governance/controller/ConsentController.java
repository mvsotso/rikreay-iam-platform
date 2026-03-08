package com.iam.platform.governance.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.governance.dto.ConsentRequest;
import com.iam.platform.governance.dto.ConsentResponse;
import com.iam.platform.governance.service.ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/governance/consents")
@RequiredArgsConstructor
@Tag(name = "Consent Management", description = "LPDP-compliant consent tracking")
public class ConsentController {

    private final ConsentService consentService;

    @PostMapping
    @Operation(summary = "Give consent (data subject)")
    public ResponseEntity<ApiResponse<ConsentResponse>> giveConsent(
            @Valid @RequestBody ConsentRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        return ResponseEntity.ok(ApiResponse.ok(
                consentService.giveConsent(request, ipAddress), "Consent recorded"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get own active consents")
    public ResponseEntity<ApiResponse<List<ConsentResponse>>> getMyConsents(
            @AuthenticationPrincipal Jwt jwt) {
        String subjectId = jwt.getSubject();
        return ResponseEntity.ok(ApiResponse.ok(
                consentService.getActiveConsents(UUID.fromString(subjectId))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Withdraw consent")
    public ResponseEntity<ApiResponse<Void>> withdrawConsent(@PathVariable UUID id) {
        consentService.withdrawConsent(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Consent withdrawn"));
    }

    @GetMapping
    @Operation(summary = "List all consents (admin)")
    public ResponseEntity<ApiResponse<Page<ConsentResponse>>> listAllConsents(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(consentService.listAllConsents(pageable)));
    }

    @GetMapping("/subject/{dataSubjectId}")
    @Operation(summary = "Get consents for a data subject")
    public ResponseEntity<ApiResponse<Page<ConsentResponse>>> getSubjectConsents(
            @PathVariable UUID dataSubjectId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(consentService.getConsentsForSubject(dataSubjectId, pageable)));
    }

    @GetMapping("/check")
    @Operation(summary = "Check if consent exists for purpose")
    public ResponseEntity<ApiResponse<Boolean>> checkConsent(
            @RequestParam UUID dataSubjectId,
            @RequestParam String purpose) {
        return ResponseEntity.ok(ApiResponse.ok(consentService.checkConsent(dataSubjectId, purpose)));
    }

    @GetMapping("/export/{dataSubjectId}")
    @Operation(summary = "Export all consents for data subject access request")
    public ResponseEntity<ApiResponse<List<ConsentResponse>>> exportConsents(
            @PathVariable UUID dataSubjectId) {
        return ResponseEntity.ok(ApiResponse.ok(consentService.exportConsents(dataSubjectId)));
    }
}
