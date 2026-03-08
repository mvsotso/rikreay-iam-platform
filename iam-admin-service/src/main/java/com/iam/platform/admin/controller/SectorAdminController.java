package com.iam.platform.admin.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.admin.dto.SectorAdminRequest;
import com.iam.platform.admin.dto.SectorAdminResponse;
import com.iam.platform.admin.entity.SectorAdminAssignment;
import com.iam.platform.admin.enums.AssignmentStatus;
import com.iam.platform.admin.repository.SectorAdminAssignmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/platform-admin/sector-admins")
@RequiredArgsConstructor
@Tag(name = "Sector Admin Assignments", description = "Manage sector admin assignments (Tier 0)")
public class SectorAdminController {

    private final SectorAdminAssignmentRepository assignmentRepository;

    @PostMapping
    @Operation(summary = "Assign a sector admin")
    public ResponseEntity<ApiResponse<SectorAdminResponse>> assignSectorAdmin(
            @Valid @RequestBody SectorAdminRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        SectorAdminAssignment assignment = SectorAdminAssignment.builder()
                .naturalPersonId(request.naturalPersonId())
                .memberClass(request.memberClass())
                .assignedByUserId(username)
                .validFrom(request.validFrom())
                .validUntil(request.validUntil())
                .status(AssignmentStatus.ACTIVE)
                .build();

        assignment = assignmentRepository.save(assignment);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.ok(toResponse(assignment), "Sector admin assigned"));
    }

    @GetMapping
    @Operation(summary = "List sector admin assignments")
    public ResponseEntity<ApiResponse<Page<SectorAdminResponse>>> listAssignments(Pageable pageable) {
        Page<SectorAdminResponse> assignments = assignmentRepository
                .findByStatus(AssignmentStatus.ACTIVE, pageable)
                .map(this::toResponse);
        return ResponseEntity.ok(ApiResponse.ok(assignments));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Revoke a sector admin assignment")
    public ResponseEntity<ApiResponse<Void>> revokeAssignment(@PathVariable UUID id) {
        SectorAdminAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + id));
        assignment.setStatus(AssignmentStatus.REVOKED);
        assignmentRepository.save(assignment);
        return ResponseEntity.ok(ApiResponse.ok(null, "Sector admin assignment revoked"));
    }

    private SectorAdminResponse toResponse(SectorAdminAssignment a) {
        return new SectorAdminResponse(
                a.getId(), a.getNaturalPersonId(), a.getMemberClass(),
                a.getAssignedByUserId(), a.getValidFrom(), a.getValidUntil(),
                a.getStatus().name(), a.getCreatedAt()
        );
    }
}
