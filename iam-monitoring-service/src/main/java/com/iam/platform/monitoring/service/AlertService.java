package com.iam.platform.monitoring.service;

import com.iam.platform.common.dto.AlertTriggerDto;
import com.iam.platform.common.dto.AlertTriggerDto.AlertSeverity;
import com.iam.platform.monitoring.dto.AlertRuleRequest;
import com.iam.platform.monitoring.dto.AlertRuleResponse;
import com.iam.platform.monitoring.dto.AggregatedHealthResponse;
import com.iam.platform.monitoring.dto.ServiceHealthDto;
import com.iam.platform.monitoring.entity.AlertRule;
import com.iam.platform.monitoring.enums.Severity;
import com.iam.platform.monitoring.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRuleRepository alertRuleRepository;
    private final HealthAggregationService healthAggregationService;
    private final IncidentService incidentService;
    private final AuditService auditService;

    @Transactional
    public AlertRuleResponse createAlertRule(AlertRuleRequest request, String username) {
        AlertRule rule = AlertRule.builder()
                .name(request.name())
                .condition(request.condition())
                .threshold(request.threshold())
                .channelType(request.channelType())
                .serviceTarget(request.serviceTarget())
                .enabled(request.enabled())
                .build();

        AlertRule saved = alertRuleRepository.save(rule);
        log.info("Alert rule created: id={}, name={}", saved.getId(), saved.getName());

        auditService.logMonitoringAction(username, "CREATE_ALERT_RULE", "alerts/" + saved.getId(),
                true, Map.of("name", saved.getName(), "condition", saved.getCondition()));

        return toResponse(saved);
    }

    public AlertRuleResponse getAlertRule(UUID id) {
        return alertRuleRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Alert rule not found: " + id));
    }

    public Page<AlertRuleResponse> listAlertRules(Pageable pageable) {
        return alertRuleRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public AlertRuleResponse updateAlertRule(UUID id, AlertRuleRequest request, String username) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert rule not found: " + id));

        rule.setName(request.name());
        rule.setCondition(request.condition());
        rule.setThreshold(request.threshold());
        rule.setChannelType(request.channelType());
        rule.setServiceTarget(request.serviceTarget());
        rule.setEnabled(request.enabled());

        AlertRule saved = alertRuleRepository.save(rule);
        log.info("Alert rule updated: id={}", id);

        auditService.logMonitoringAction(username, "UPDATE_ALERT_RULE", "alerts/" + id,
                true, Map.of("name", saved.getName()));

        return toResponse(saved);
    }

    @Transactional
    public void deleteAlertRule(UUID id, String username) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert rule not found: " + id));

        rule.softDelete();
        alertRuleRepository.save(rule);
        log.info("Alert rule deleted: id={}", id);

        auditService.logMonitoringAction(username, "DELETE_ALERT_RULE", "alerts/" + id,
                true, Map.of("name", rule.getName()));
    }

    @Scheduled(fixedDelayString = "${monitoring.health-check-interval:30}000")
    @Transactional
    public void evaluateAlerts() {
        List<AlertRule> enabledRules = alertRuleRepository.findByEnabled(true);
        if (enabledRules.isEmpty()) {
            return;
        }

        AggregatedHealthResponse health = healthAggregationService.getAggregatedHealth();

        for (AlertRule rule : enabledRules) {
            try {
                evaluateRule(rule, health);
            } catch (Exception e) {
                log.error("Failed to evaluate alert rule: id={}, name={}", rule.getId(), rule.getName(), e);
            }
        }
    }

    private void evaluateRule(AlertRule rule, AggregatedHealthResponse health) {
        boolean triggered = false;

        if ("SERVICE_DOWN".equals(rule.getCondition())) {
            if (rule.getServiceTarget() != null) {
                triggered = health.services().stream()
                        .filter(s -> s.serviceName().equals(rule.getServiceTarget()))
                        .anyMatch(s -> "DOWN".equals(s.status()));
            } else {
                triggered = health.unhealthyServices() > 0;
            }
        } else if ("UNHEALTHY_COUNT".equals(rule.getCondition())) {
            int threshold = Integer.parseInt(rule.getThreshold());
            triggered = health.unhealthyServices() >= threshold;
        } else if ("RESPONSE_TIME".equals(rule.getCondition())) {
            long threshold = Long.parseLong(rule.getThreshold());
            triggered = health.services().stream()
                    .anyMatch(s -> s.responseTimeMs() > threshold);
        }

        if (triggered) {
            triggerAlert(rule, health);
        }
    }

    private void triggerAlert(AlertRule rule, AggregatedHealthResponse health) {
        rule.setLastTriggeredAt(Instant.now());
        alertRuleRepository.save(rule);

        String affectedServices = health.services().stream()
                .filter(s -> "DOWN".equals(s.status()))
                .map(ServiceHealthDto::serviceName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("unknown");

        AlertTriggerDto trigger = AlertTriggerDto.builder()
                .alertRuleId(rule.getId())
                .alertName(rule.getName())
                .severity(mapSeverity(rule.getCondition()))
                .condition(rule.getCondition())
                .currentValue(String.valueOf(health.unhealthyServices()))
                .threshold(rule.getThreshold())
                .serviceAffected(affectedServices)
                .timestamp(Instant.now())
                .metadata(Map.of(
                        "totalServices", health.totalServices(),
                        "healthyServices", health.healthyServices(),
                        "unhealthyServices", health.unhealthyServices()))
                .build();

        auditService.publishAlertTrigger(trigger);

        incidentService.createIncidentFromAlert(
                "Alert: " + rule.getName(),
                Severity.valueOf(mapSeverity(rule.getCondition()).name()),
                rule.getServiceTarget() != null ? rule.getServiceTarget() : affectedServices,
                "Alert triggered: " + rule.getCondition() + " (threshold: " + rule.getThreshold() + ")"
        );

        log.warn("Alert triggered: rule={}, affected={}", rule.getName(), affectedServices);
    }

    private AlertSeverity mapSeverity(String condition) {
        return switch (condition) {
            case "SERVICE_DOWN" -> AlertSeverity.CRITICAL;
            case "UNHEALTHY_COUNT" -> AlertSeverity.HIGH;
            case "RESPONSE_TIME" -> AlertSeverity.MEDIUM;
            default -> AlertSeverity.LOW;
        };
    }

    private AlertRuleResponse toResponse(AlertRule rule) {
        return new AlertRuleResponse(
                rule.getId(),
                rule.getName(),
                rule.getCondition(),
                rule.getThreshold(),
                rule.getChannelType(),
                rule.getServiceTarget(),
                rule.isEnabled(),
                rule.getLastTriggeredAt(),
                rule.getCreatedAt()
        );
    }
}
