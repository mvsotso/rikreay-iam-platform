package com.iam.platform.admin.repository;

import com.iam.platform.admin.entity.SectorAdminAssignment;
import com.iam.platform.admin.enums.AssignmentStatus;
import com.iam.platform.common.enums.MemberClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SectorAdminAssignmentRepository extends JpaRepository<SectorAdminAssignment, UUID> {

    Page<SectorAdminAssignment> findByStatus(AssignmentStatus status, Pageable pageable);

    List<SectorAdminAssignment> findByNaturalPersonIdAndStatus(UUID naturalPersonId, AssignmentStatus status);

    Optional<SectorAdminAssignment> findByNaturalPersonIdAndMemberClassAndStatus(
            UUID naturalPersonId, MemberClass memberClass, AssignmentStatus status);

    List<SectorAdminAssignment> findByMemberClassAndStatus(MemberClass memberClass, AssignmentStatus status);
}
