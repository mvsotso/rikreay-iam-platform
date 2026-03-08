package com.iam.platform.core.entity;

import com.iam.platform.common.entity.BaseEntity;
import com.iam.platform.common.enums.DelegationScope;
import com.iam.platform.common.enums.RepresentativeRole;
import com.iam.platform.common.enums.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "representations")
@SQLRestriction("deleted = false")
public class Representation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "natural_person_id", nullable = false)
    private NaturalPerson naturalPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "representative_role", nullable = false)
    private RepresentativeRole representativeRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "delegation_scope", nullable = false)
    private DelegationScope delegationScope;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specific_permissions", columnDefinition = "jsonb")
    private Map<String, Object> specificPermissions;

    private String title;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "authorized_by_person_id")
    private UUID authorizedByPersonId;

    @Column(name = "authorization_document")
    private String authorizationDocument;

    @Column(name = "authorization_document_type")
    private String authorizationDocumentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status")
    private VerificationStatus verificationStatus;

    @Column(name = "is_primary")
    private Boolean isPrimary;

    private String status;
}
