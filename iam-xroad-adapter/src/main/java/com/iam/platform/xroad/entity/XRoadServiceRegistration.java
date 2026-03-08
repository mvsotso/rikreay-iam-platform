package com.iam.platform.xroad.entity;

import com.iam.platform.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = "aclEntries")
@Entity
@Table(name = "xroad_service_registrations")
public class XRoadServiceRegistration extends BaseEntity {

    @Column(name = "service_code", nullable = false)
    private String serviceCode;

    @Column(name = "service_version", nullable = false, length = 20)
    private String serviceVersion;

    @Column(name = "target_service", nullable = false)
    private String targetService;

    @Column(name = "target_path", nullable = false, length = 500)
    private String targetPath;

    @Column(name = "description")
    private String description;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @OneToMany(mappedBy = "serviceRegistration", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<XRoadAclEntry> aclEntries = new ArrayList<>();
}
