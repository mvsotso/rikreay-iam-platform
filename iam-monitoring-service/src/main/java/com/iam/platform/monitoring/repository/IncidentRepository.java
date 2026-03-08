package com.iam.platform.monitoring.repository;

import com.iam.platform.monitoring.entity.Incident;
import com.iam.platform.monitoring.enums.IncidentStatus;
import com.iam.platform.monitoring.enums.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    Page<Incident> findByStatus(IncidentStatus status, Pageable pageable);

    Page<Incident> findBySeverity(Severity severity, Pageable pageable);

    Page<Incident> findByServiceAffected(String serviceAffected, Pageable pageable);

    long countByStatus(IncidentStatus status);

    long countBySeverityAndStatus(Severity severity, IncidentStatus status);
}
