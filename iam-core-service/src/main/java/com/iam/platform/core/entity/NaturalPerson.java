package com.iam.platform.core.entity;

import com.iam.platform.common.entity.BaseEntity;
import com.iam.platform.common.enums.VerificationStatus;
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
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.time.LocalDate;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "natural_persons")
@SQLRestriction("deleted = false")
public class NaturalPerson extends BaseEntity {

    @Column(name = "personal_id_code", unique = true)
    private String personalIdCode;

    @Column(name = "national_id_number")
    private String nationalIdNumber;

    @Column(name = "cam_digi_key_id")
    private String camDigiKeyId;

    @Column(name = "first_name_kh")
    private String firstNameKh;

    @Column(name = "last_name_kh")
    private String lastNameKh;

    @Column(name = "first_name_en")
    private String firstNameEn;

    @Column(name = "last_name_en")
    private String lastNameEn;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String gender;

    private String nationality;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_verification_status")
    private VerificationStatus identityVerificationStatus;

    @Column(name = "identity_verification_method")
    private String identityVerificationMethod;

    @Column(name = "identity_verified_at")
    private Instant identityVerifiedAt;

    @Column(name = "keycloak_user_id")
    private String keycloakUserId;

    private String status;
}
