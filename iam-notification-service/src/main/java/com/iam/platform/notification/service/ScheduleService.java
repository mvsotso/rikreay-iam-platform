package com.iam.platform.notification.service;

import com.iam.platform.notification.dto.ScheduledReportRequest;
import com.iam.platform.notification.dto.ScheduledReportResponse;
import com.iam.platform.notification.entity.ScheduledReport;
import com.iam.platform.notification.enums.ChannelType;
import com.iam.platform.notification.repository.NotificationTemplateRepository;
import com.iam.platform.notification.repository.ScheduledReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduledReportRepository scheduledReportRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationDispatcher dispatcher;

    @Transactional
    public ScheduledReportResponse createSchedule(ScheduledReportRequest request) {
        if (scheduledReportRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Scheduled report '" + request.name() + "' already exists");
        }

        if (!templateRepository.existsById(request.templateId())) {
            throw new IllegalArgumentException("Template not found: " + request.templateId());
        }

        ScheduledReport report = ScheduledReport.builder()
                .name(request.name())
                .cronExpression(request.cronExpression())
                .templateId(request.templateId())
                .recipientList(request.recipientList())
                .enabled(request.enabled())
                .build();

        report = scheduledReportRepository.save(report);
        log.info("Scheduled report created: {}", request.name());
        return toResponse(report);
    }

    @Transactional(readOnly = true)
    public Page<ScheduledReportResponse> listSchedules(Pageable pageable) {
        return scheduledReportRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ScheduledReportResponse getSchedule(UUID id) {
        ScheduledReport report = scheduledReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled report not found: " + id));
        return toResponse(report);
    }

    @Transactional
    public ScheduledReportResponse updateSchedule(UUID id, ScheduledReportRequest request) {
        ScheduledReport report = scheduledReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled report not found: " + id));

        report.setName(request.name());
        report.setCronExpression(request.cronExpression());
        report.setTemplateId(request.templateId());
        report.setRecipientList(request.recipientList());
        report.setEnabled(request.enabled());

        report = scheduledReportRepository.save(report);
        log.info("Scheduled report updated: {}", request.name());
        return toResponse(report);
    }

    @Transactional
    public void deleteSchedule(UUID id) {
        ScheduledReport report = scheduledReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled report not found: " + id));
        report.softDelete();
        scheduledReportRepository.save(report);
        log.info("Scheduled report deleted: {}", report.getName());
    }

    @Scheduled(cron = "0 0 * * * *") // Check every hour
    @Transactional
    public void executeScheduledReports() {
        var enabledReports = scheduledReportRepository.findByEnabledTrue();
        for (ScheduledReport report : enabledReports) {
            try {
                var template = templateRepository.findById(report.getTemplateId());
                if (template.isEmpty()) {
                    log.warn("Template not found for scheduled report: {}", report.getName());
                    continue;
                }

                for (String recipient : report.getRecipientList()) {
                    dispatcher.dispatch(
                            template.get().getChannelType(),
                            recipient,
                            template.get().getSubject(),
                            template.get().getBodyTemplate(),
                            report.getTemplateId()
                    );
                }

                report.setLastRunAt(Instant.now());
                scheduledReportRepository.save(report);
                log.info("Scheduled report executed: {}", report.getName());
            } catch (Exception e) {
                log.error("Failed to execute scheduled report: {}", report.getName(), e);
            }
        }
    }

    private ScheduledReportResponse toResponse(ScheduledReport report) {
        return new ScheduledReportResponse(
                report.getId(),
                report.getName(),
                report.getCronExpression(),
                report.getTemplateId(),
                report.getRecipientList(),
                report.isEnabled(),
                report.getLastRunAt(),
                report.getNextRunAt(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
