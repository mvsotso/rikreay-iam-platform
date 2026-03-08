package com.iam.platform.xroad.entity;

import com.iam.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = "serviceRegistration")
@Entity
@Table(name = "xroad_acl_entries")
public class XRoadAclEntry extends BaseEntity {

    @Column(name = "consumer_identifier", nullable = false, length = 500)
    private String consumerIdentifier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_registration_id", nullable = false)
    private XRoadServiceRegistration serviceRegistration;

    @Column(name = "allowed", nullable = false)
    private boolean allowed;

    @Column(name = "description")
    private String description;
}
