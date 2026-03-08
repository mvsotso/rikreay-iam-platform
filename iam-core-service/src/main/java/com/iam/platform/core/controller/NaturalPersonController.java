package com.iam.platform.core.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.core.entity.NaturalPerson;
import com.iam.platform.core.entity.Representation;
import com.iam.platform.core.service.IdentityVerificationService;
import com.iam.platform.core.service.NaturalPersonService;
import com.iam.platform.core.service.RepresentationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
@Tag(name = "Natural Persons", description = "Natural person identity management")
public class NaturalPersonController {

    private final NaturalPersonService personService;
    private final RepresentationService representationService;
    private final IdentityVerificationService verificationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('iam-admin', 'tenant-admin')")
    @Operation(summary = "Register a natural person")
    public ApiResponse<NaturalPerson> create(@RequestBody NaturalPerson person) {
        return ApiResponse.ok(personService.create(person), "Natural person registered");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('internal-user', 'tenant-admin', 'iam-admin')")
    @Operation(summary = "List natural persons (paginated)")
    public ApiResponse<Page<NaturalPerson>> list(Pageable pageable) {
        return ApiResponse.ok(personService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('iam-admin', 'tenant-admin')")
    @Operation(summary = "Get natural person by ID")
    public ApiResponse<NaturalPerson> getById(@PathVariable UUID id) {
        return ApiResponse.ok(personService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('iam-admin', 'tenant-admin')")
    @Operation(summary = "Update natural person")
    public ApiResponse<NaturalPerson> update(@PathVariable UUID id, @RequestBody NaturalPerson person) {
        return ApiResponse.ok(personService.update(id, person), "Natural person updated");
    }

    @GetMapping("/me")
    @Operation(summary = "Get own profile from JWT")
    public ApiResponse<NaturalPerson> getOwnProfile(@AuthenticationPrincipal Jwt jwt) {
        String keycloakUserId = jwt.getSubject();
        return ApiResponse.ok(personService.findByKeycloakUserId(keycloakUserId));
    }

    @PutMapping("/me")
    @Operation(summary = "Update own profile")
    public ApiResponse<NaturalPerson> updateOwnProfile(@AuthenticationPrincipal Jwt jwt,
                                                        @RequestBody NaturalPerson person) {
        NaturalPerson existing = personService.findByKeycloakUserId(jwt.getSubject());
        return ApiResponse.ok(personService.update(existing.getId(), person), "Profile updated");
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasRole('iam-admin')")
    @Operation(summary = "Trigger identity verification")
    public ApiResponse<NaturalPerson> verify(@PathVariable UUID id,
                                              @RequestBody VerifyRequest request) {
        return ApiResponse.ok(verificationService.verifyNaturalPerson(id, request.method()),
                "Identity verification initiated");
    }

    @GetMapping("/{id}/representations")
    @Operation(summary = "List entities this person represents")
    public ApiResponse<Page<Representation>> getRepresentations(@PathVariable UUID id,
                                                                  Pageable pageable) {
        return ApiResponse.ok(representationService.findByNaturalPerson(id, pageable));
    }

    public record VerifyRequest(String method) {}
}
