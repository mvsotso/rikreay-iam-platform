package com.iam.platform.core.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.core.entity.LegalEntity;
import com.iam.platform.core.entity.Representation;
import com.iam.platform.core.service.LegalEntityService;
import com.iam.platform.core.service.RepresentationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/entities")
@RequiredArgsConstructor
@Tag(name = "Legal Entities", description = "Legal entity (organization) management")
public class LegalEntityController {

    private final LegalEntityService entityService;
    private final RepresentationService representationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('iam-admin')")
    @Operation(summary = "Register a legal entity")
    public ApiResponse<LegalEntity> create(@RequestBody LegalEntity entity) {
        return ApiResponse.ok(entityService.create(entity), "Legal entity registered");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('internal-user', 'tenant-admin', 'iam-admin')")
    @Operation(summary = "List legal entities (paginated)")
    public ApiResponse<Page<LegalEntity>> list(Pageable pageable) {
        return ApiResponse.ok(entityService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('iam-admin', 'tenant-admin')")
    @Operation(summary = "Get legal entity by ID")
    public ApiResponse<LegalEntity> getById(@PathVariable UUID id) {
        return ApiResponse.ok(entityService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('iam-admin', 'tenant-admin')")
    @Operation(summary = "Update legal entity")
    public ApiResponse<LegalEntity> update(@PathVariable UUID id, @RequestBody LegalEntity entity) {
        return ApiResponse.ok(entityService.update(id, entity), "Legal entity updated");
    }

    @GetMapping("/{id}/representatives")
    @PreAuthorize("hasAnyRole('tenant-admin', 'iam-admin')")
    @Operation(summary = "List representatives of a legal entity")
    public ApiResponse<Page<Representation>> getRepresentatives(@PathVariable UUID id,
                                                                  Pageable pageable) {
        return ApiResponse.ok(representationService.findByLegalEntity(id, pageable));
    }
}
