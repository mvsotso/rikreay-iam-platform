package com.iam.platform.admin.service;

import com.iam.platform.admin.dto.UsageResponse;
import com.iam.platform.admin.entity.UsageRecord;
import com.iam.platform.admin.enums.MetricType;
import com.iam.platform.admin.repository.UsageRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageTrackingService {

    private final UsageRecordRepository usageRecordRepository;

    @Transactional(readOnly = true)
    public UsageResponse getUsageForTenant(String tenantId) {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate today = LocalDate.now();

        List<UsageRecord> records = usageRecordRepository.findByTenantIdAndRecordDateBetween(
                tenantId, monthStart, today);

        Map<String, Long> apiCallsByDay = new LinkedHashMap<>();
        Map<String, Long> loginsByDay = new LinkedHashMap<>();
        Map<String, Long> xroadByDay = new LinkedHashMap<>();
        long activeUsers = 0;

        for (UsageRecord record : records) {
            String day = record.getRecordDate().toString();
            switch (record.getMetricType()) {
                case API_CALLS -> apiCallsByDay.put(day, record.getCount());
                case LOGINS -> loginsByDay.put(day, record.getCount());
                case XROAD_TRANSACTIONS -> xroadByDay.put(day, record.getCount());
                case ACTIVE_USERS -> activeUsers = Math.max(activeUsers, record.getCount());
                default -> {}
            }
        }

        return new UsageResponse(apiCallsByDay, loginsByDay, xroadByDay, activeUsers);
    }

    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @Transactional
    public void aggregateDailyUsage() {
        log.info("Running daily usage aggregation...");
        // In production, this would query Elasticsearch audit events and Prometheus metrics,
        // aggregate by tenant per day, and store UsageRecord rows
        log.info("Daily usage aggregation complete");
    }
}
