package com.iam.platform.admin.entity;

import com.iam.platform.admin.enums.AssignmentStatus;
import com.iam.platform.common.entity.BaseEntity;
import com.iam.platform.common.enums.MemberClass;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sector_admin_assignments")
public class SectorAdminAssignment extends BaseEntity {

    @Column(name = "natural_person_id", nullable = false)
    private UUID naturalPersonId;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_class", nullable = false, length = 10)
    private MemberClass memberClass;

    @Column(name = "assigned_by_user_id", nullable = false)
    private String assignedByUserId;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status;
}
