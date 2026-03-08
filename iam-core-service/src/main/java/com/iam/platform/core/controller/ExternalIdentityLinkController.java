package com.iam.platform.core.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.core.entity.ExternalIdentityLink;
import com.iam.platform.core.service.ExternalIdentityLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/persons/{personId}/external-links")
@RequiredArgsConstructor
@Tag(name = "External Identity Links", description = "Links to external identity systems")
public class ExternalIdentityLinkController {

    private final ExternalIdentityLinkService linkService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('iam-admin', 'tenant-admin')")
    @Operation(summary = "Create an external identity link")
    public ApiResponse<ExternalIdentityLink> create(@PathVariable UUID personId,
                                                     @RequestBody ExternalIdentityLink link) {
        link.setOwnerType("NATURAL_PERSON");
        link.setOwnerId(personId);
        return ApiResponse.ok(linkService.create(link), "External link created");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('internal-user', 'tenant-admin', 'iam-admin')")
    @Operation(summary = "List external identity links for a person")
    public ApiResponse<List<ExternalIdentityLink>> list(
            @PathVariable UUID personId,
            @RequestParam(defaultValue = "NATURAL_PERSON") String ownerType) {
        return ApiResponse.ok(linkService.findByOwner(ownerType, personId));
    }

    @PutMapping("/{linkId}")
    @PreAuthorize("hasAnyRole('iam-admin', 'tenant-admin')")
    @Operation(summary = "Update an external identity link")
    public ApiResponse<ExternalIdentityLink> update(@PathVariable UUID personId,
                                                     @PathVariable UUID linkId,
                                                     @RequestBody ExternalIdentityLink link) {
        return ApiResponse.ok(linkService.update(linkId, link), "External link updated");
    }

    @DeleteMapping("/{linkId}")
    @PreAuthorize("hasRole('iam-admin')")
    @Operation(summary = "Delete an external identity link (soft delete)")
    public ApiResponse<Void> delete(@PathVariable UUID personId, @PathVariable UUID linkId) {
        linkService.softDelete(linkId);
        return ApiResponse.ok(null, "External link deleted");
    }
}
