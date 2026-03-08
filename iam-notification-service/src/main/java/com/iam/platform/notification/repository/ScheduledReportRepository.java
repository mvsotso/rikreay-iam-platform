package com.iam.platform.notification.repository;

import com.iam.platform.notification.entity.ScheduledReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduledReportRepository extends JpaRepository<ScheduledReport, UUID> {

    List<ScheduledReport> findByEnabledTrue();

    Optional<ScheduledReport> findByName(String name);

    boolean existsByName(String name);
}
