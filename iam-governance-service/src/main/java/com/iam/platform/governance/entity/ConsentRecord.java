package com.iam.platform.governance.entity;

import com.iam.platform.common.entity.BaseEntity;
import com.iam.platform.governance.enums.ConsentMethod;
import com.iam.platform.governance.enums.DataSubjectType;
import com.iam.platform.governance.enums.LegalBasis;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "consent_records")
public class ConsentRecord extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "data_subject_type", nullable = false, length = 30)
    private DataSubjectType dataSubjectType;

    @Column(name = "data_subject_id", nullable = false)
    private UUID dataSubjectId;

    @Column(nullable = false, length = 500)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "legal_basis", nullable = false, length = 30)
    private LegalBasis legalBasis;

    @Column(name = "consent_given", nullable = false)
    private boolean consentGiven;

    @Column(name = "consent_timestamp", nullable = false)
    private Instant consentTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_method", nullable = false, length = 20)
    private ConsentMethod consentMethod;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_categories", columnDefinition = "jsonb")
    private List<String> dataCategories;

    @Column(name = "third_party_sharing", nullable = false)
    private boolean thirdPartySharing;

    @Column(name = "cross_border_transfer", nullable = false)
    private boolean crossBorderTransfer;
}
