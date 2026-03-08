package com.iam.platform.admin.service;

import com.iam.platform.admin.dto.SectorDashboardResponse;
import com.iam.platform.common.enums.MemberClass;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectorDashboardService {

    public SectorDashboardResponse getSectorDashboard(MemberClass memberClass) {
        // In production, this would query iam-core-service for LegalEntity counts
        // filtered by memberClass, and aggregate UsageRecord data
        log.info("Getting sector dashboard for: {}", memberClass);

        return new SectorDashboardResponse(
                memberClass.name(),
                0, 0, 0, 0, 0, 0
        );
    }
}
