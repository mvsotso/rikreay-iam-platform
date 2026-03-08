package com.iam.platform.xroad.repository;

import com.iam.platform.xroad.entity.XRoadServiceRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface XRoadServiceRegistrationRepository extends JpaRepository<XRoadServiceRegistration, UUID> {

    Optional<XRoadServiceRegistration> findByServiceCodeAndServiceVersion(String serviceCode, String serviceVersion);

    Optional<XRoadServiceRegistration> findByServiceCode(String serviceCode);

    List<XRoadServiceRegistration> findByEnabledTrue();

    boolean existsByServiceCodeAndServiceVersion(String serviceCode, String serviceVersion);
}
