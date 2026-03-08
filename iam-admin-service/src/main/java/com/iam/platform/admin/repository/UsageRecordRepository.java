package com.iam.platform.admin.repository;

import com.iam.platform.admin.entity.UsageRecord;
import com.iam.platform.admin.enums.MetricType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, UUID> {

    List<UsageRecord> findByTenantIdAndRecordDateBetween(
            String tenantId, LocalDate from, LocalDate to);

    List<UsageRecord> findByTenantIdAndMetricTypeAndRecordDateBetween(
            String tenantId, MetricType metricType, LocalDate from, LocalDate to);

    @Query("SELECT u FROM UsageRecord u WHERE u.recordDate BETWEEN :from AND :to ORDER BY u.recordDate DESC")
    Page<UsageRecord> findByDateRange(@Param("from") LocalDate from,
                                       @Param("to") LocalDate to, Pageable pageable);

    @Query("SELECT SUM(u.count) FROM UsageRecord u WHERE u.tenantId = :tenantId " +
           "AND u.metricType = :metricType AND u.recordDate BETWEEN :from AND :to")
    Long sumByTenantAndMetric(@Param("tenantId") String tenantId,
                               @Param("metricType") MetricType metricType,
                               @Param("from") LocalDate from,
                               @Param("to") LocalDate to);
}
