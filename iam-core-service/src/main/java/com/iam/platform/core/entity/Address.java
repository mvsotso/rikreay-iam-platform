package com.iam.platform.core.entity;

import com.iam.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "addresses")
@SQLRestriction("deleted = false")
public class Address extends BaseEntity {

    @Column(name = "owner_type", nullable = false)
    private String ownerType;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "address_type")
    private String addressType;

    @Column(name = "street_address")
    private String streetAddress;

    private String sangkat;

    private String khan;

    private String province;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(columnDefinition = "VARCHAR(10) DEFAULT 'KH'")
    private String country;

    @Column(name = "is_primary")
    private Boolean isPrimary;
}
