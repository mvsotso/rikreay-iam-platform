package com.iam.platform.core.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.core.entity.Representation;
import com.iam.platform.core.service.IdentityVerificationService;
import com.iam.platform.core.service.RepresentationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/representations")
@RequiredArgsConstructor
@Tag(name = "Representations", description = "Person-to-entity delegation management")
public class RepresentationController {

    private final RepresentationService representationService;
    private final IdentityVerificationService verificationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('tenant-admin', 'iam-admin')")
    @Operation(summary = "Create a representation/delegation")
    public ApiResponse<Representation> create(@RequestBody Representation representation) {
        return ApiResponse.ok(representationService.create(representation), "Representation created");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('tenant-admin', 'iam-admin')")
    @Operation(summary = "List representations for an entity")
    public ApiResponse<Page<Representation>> list(
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) UUID personId,
            Pageable pageable) {
        if (entityId != null) {
            return ApiResponse.ok(representationService.findByLegalEntity(entityId, pageable));
        } else if (personId != null) {
            return ApiResponse.ok(representationService.findByNaturalPerson(personId, pageable));
        }
        return ApiResponse.error("Either entityId or personId is required", "MISSING_PARAMETER");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('tenant-admin', 'iam-admin')")
    @Operation(summary = "Update a representation")
    public ApiResponse<Representation> update(@PathVariable UUID id,
                                                @RequestBody Representation representation) {
        return ApiResponse.ok(representationService.update(id, representation), "Representation updated");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('tenant-admin', 'iam-admin')")
    @Operation(summary = "Revoke a representation (soft delete)")
    public ApiResponse<Void> revoke(@PathVariable UUID id) {
        representationService.revoke(id);
        return ApiResponse.ok(null, "Representation revoked");
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasRole('iam-admin')")
    @Operation(summary = "Verify authorization document")
    public ApiResponse<Representation> verify(@PathVariable UUID id) {
        return ApiResponse.ok(verificationService.verifyRepresentation(id),
                "Representation verified");
    }
}
