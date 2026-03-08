package com.iam.platform.xroad.repository;

import com.iam.platform.xroad.entity.XRoadAclEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface XRoadAclEntryRepository extends JpaRepository<XRoadAclEntry, UUID> {

    List<XRoadAclEntry> findByConsumerIdentifier(String consumerIdentifier);

    List<XRoadAclEntry> findByServiceRegistrationId(UUID serviceRegistrationId);

    @Query("SELECT a FROM XRoadAclEntry a WHERE a.consumerIdentifier = :consumer " +
            "AND a.serviceRegistration.serviceCode = :serviceCode AND a.allowed = true")
    List<XRoadAclEntry> findAllowedEntries(
            @Param("consumer") String consumerIdentifier,
            @Param("serviceCode") String serviceCode);

    boolean existsByConsumerIdentifierAndServiceRegistrationId(
            String consumerIdentifier, UUID serviceRegistrationId);
}
