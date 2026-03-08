package com.iam.platform.core.entity;

import com.iam.platform.common.entity.BaseEntity;
import com.iam.platform.common.enums.EntityType;
import com.iam.platform.common.enums.MemberClass;
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
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "legal_entities")
@SQLRestriction("deleted = false")
public class LegalEntity extends BaseEntity {

    @Column(name = "registration_number", unique = true)
    private String registrationNumber;

    @Column(name = "tax_identification_number", unique = true)
    private String taxIdentificationNumber;

    @Column(name = "name_kh")
    private String nameKh;

    @Column(name = "name_en")
    private String nameEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private EntityType entityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_class", nullable = false)
    private MemberClass memberClass;

    @Column(name = "xroad_member_code")
    private String xroadMemberCode;

    @Column(name = "xroad_subsystem")
    private String xroadSubsystem;

    @Column(name = "sector_code")
    private String sectorCode;

    @Column(name = "incorporation_date")
    private LocalDate incorporationDate;

    @Column(name = "registered_address")
    private String registeredAddress;

    private String province;

    @Column(name = "realm_name", unique = true)
    private String realmName;

    @Column(name = "parent_entity_id", insertable = false, updatable = false)
    private UUID parentEntityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_entity_id")
    private LegalEntity parentEntity;

    private String status;
}
