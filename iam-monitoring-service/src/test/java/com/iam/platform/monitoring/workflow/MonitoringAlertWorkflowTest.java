package com.iam.platform.monitoring.workflow;

import com.iam.platform.monitoring.dto.AlertRuleRequest;
import com.iam.platform.monitoring.dto.AlertRuleResponse;
import com.iam.platform.monitoring.dto.AggregatedHealthResponse;
import com.iam.platform.monitoring.dto.IncidentRequest;
import com.iam.platform.monitoring.dto.IncidentResponse;
import com.iam.platform.monitoring.dto.ServiceHealthDto;
import com.iam.platform.monitoring.entity.AlertRule;
import com.iam.platform.monitoring.entity.Incident;
import com.iam.platform.monitoring.enums.ChannelType;
import com.iam.platform.monitoring.enums.IncidentStatus;
import com.iam.platform.monitoring.enums.Severity;
import com.iam.platform.monitoring.repository.AlertRuleRepository;
import com.iam.platform.monitoring.repository.IncidentRepository;
import com.iam.platform.monitoring.service.AlertService;
import com.iam.platform.monitoring.service.AuditService;
import com.iam.platform.monitoring.service.HealthAggregationService;
import com.iam.platform.monitoring.service.IncidentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Workflow: Monitoring Alert Evaluation & Incident Creation")
class MonitoringAlertWorkflowTest {

    @Autowired
    private AlertService alertService;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @MockitoBean
    private HealthAggregationService healthAggregationService;

    @MockitoBean
    private AuditService auditService;

    @Test
    @DisplayName("E2E: Create alert rule → mock health DOWN → evaluate → incident auto-created → alert trigger published")
    void fullAlertToIncidentWorkflow() {
        // Step 1: Create alert rule for SERVICE_DOWN
        AlertRuleRequest ruleRequest = new AlertRuleRequest(
                "Core Service Down Alert",
                "SERVICE_DOWN",
                "1",
                ChannelType.EMAIL,
                "iam-core-service",
                true);

        AlertRuleResponse rule = alertService.createAlertRule(ruleRequest, "ops-admin");
        assertThat(rule).isNotNull();
        assertThat(rule.name()).isEqualTo("Core Service Down Alert");
        assertThat(rule.enabled()).isTrue();

        // Verify audit for rule creation
        verify(auditService).logMonitoringAction(
                eq("ops-admin"), eq("CREATE_ALERT_RULE"), anyString(), eq(true), any());

        // Step 2: Mock health check returning DOWN for iam-core-service
        AggregatedHealthResponse unhealthyResponse = new AggregatedHealthResponse(
                "DEGRADED",
                5,
                4,
                1,
                List.of(
                        new ServiceHealthDto("iam-gateway", "http://localhost:8081", "UP", 45, Instant.now(), null),
                        new ServiceHealthDto("iam-core-service", "http://localhost:8082", "DOWN", 0, Instant.now(), "Connection refused"),
                        new ServiceHealthDto("iam-tenant-service", "http://localhost:8083", "UP", 30, Instant.now(), null),
                        new ServiceHealthDto("iam-audit-service", "http://localhost:8084", "UP", 55, Instant.now(), null),
                        new ServiceHealthDto("iam-xroad-adapter", "http://localhost:8085", "UP", 40, Instant.now(), null)
                ),
                Instant.now()
        );
        when(healthAggregationService.getAggregatedHealth()).thenReturn(unhealthyResponse);

        // Step 3: Evaluate alerts (this would normally run on schedule)
        long incidentCountBefore = incidentRepository.count();
        alertService.evaluateAlerts();

        // Step 4: Verify alert triggered → AlertTriggerDto published
        verify(auditService).publishAlertTrigger(any());

        // Step 5: Verify incident was auto-created
        long incidentCountAfter = incidentRepository.count();
        assertThat(incidentCountAfter).isGreaterThan(incidentCountBefore);

        // Verify the auto-created incident details
        List<Incident> incidents = incidentRepository.findAll();
        Incident autoIncident = incidents.stream()
                .filter(i -> i.getTitle().contains("Core Service Down Alert"))
                .findFirst()
                .orElseThrow();

        assertThat(autoIncident.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(autoIncident.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(autoIncident.getServiceAffected()).isEqualTo("iam-core-service");
    }

    @Test
    @DisplayName("E2E: Create incident → update status to INVESTIGATING → resolve")
    void incidentLifecycleWorkflow() {
        // Step 1: Create incident manually
        IncidentRequest request = new IncidentRequest(
                "Database Connection Pool Exhausted",
                Severity.HIGH,
                "PostgreSQL connection pool at 100% for iam-audit-service",
                "iam-audit-service",
                "ops-admin");

        IncidentResponse created = incidentService.createIncident(request, "ops-admin");
        assertThat(created).isNotNull();
        assertThat(created.status()).isEqualTo(IncidentStatus.OPEN);
        assertThat(created.severity()).isEqualTo(Severity.HIGH);

        // Step 2: Update to INVESTIGATING
        IncidentResponse investigating = incidentService.updateIncidentStatus(
                created.id(), IncidentStatus.INVESTIGATING, "ops-admin");
        assertThat(investigating.status()).isEqualTo(IncidentStatus.INVESTIGATING);

        // Step 3: Resolve
        IncidentResponse resolved = incidentService.updateIncidentStatus(
                created.id(), IncidentStatus.RESOLVED, "ops-admin");
        assertThat(resolved.status()).isEqualTo(IncidentStatus.RESOLVED);

        // Verify audit logged for all status changes
        verify(auditService, atLeast(3)).logMonitoringAction(
                eq("ops-admin"), anyString(), anyString(), eq(true), any());
    }

    @Test
    @DisplayName("E2E: Alert rule with all services UP → no incident created")
    void alertNotTriggeredWhenHealthy() {
        AlertRuleRequest ruleRequest = new AlertRuleRequest(
                "Healthy Check Rule", "SERVICE_DOWN", "1",
                ChannelType.EMAIL, "iam-gateway", true);

        alertService.createAlertRule(ruleRequest, "ops-admin");

        // Mock all services healthy
        AggregatedHealthResponse healthyResponse = new AggregatedHealthResponse(
                "UP", 3, 3, 0,
                List.of(
                        new ServiceHealthDto("iam-gateway", "http://localhost:8081", "UP", 20, Instant.now(), null),
                        new ServiceHealthDto("iam-core-service", "http://localhost:8082", "UP", 25, Instant.now(), null),
                        new ServiceHealthDto("iam-tenant-service", "http://localhost:8083", "UP", 30, Instant.now(), null)
                ),
                Instant.now()
        );
        when(healthAggregationService.getAggregatedHealth()).thenReturn(healthyResponse);

        long incidentCountBefore = incidentRepository.count();
        alertService.evaluateAlerts();

        // No alert triggered, no incident created
        verify(auditService, never()).publishAlertTrigger(any());
        assertThat(incidentRepository.count()).isEqualTo(incidentCountBefore);
    }

    @Test
    @DisplayName("E2E: UNHEALTHY_COUNT condition triggers when threshold exceeded")
    void unhealthyCountThresholdTrigger() {
        AlertRuleRequest ruleRequest = new AlertRuleRequest(
                "Multiple Services Down", "UNHEALTHY_COUNT", "2",
                ChannelType.EMAIL, null, true);

        alertService.createAlertRule(ruleRequest, "ops-admin");

        // Mock 3 services down (exceeds threshold of 2)
        AggregatedHealthResponse degradedResponse = new AggregatedHealthResponse(
                "DEGRADED", 5, 2, 3,
                List.of(
                        new ServiceHealthDto("iam-gateway", "http://localhost:8081", "UP", 20, Instant.now(), null),
                        new ServiceHealthDto("iam-core-service", "http://localhost:8082", "DOWN", 0, Instant.now(), null),
                        new ServiceHealthDto("iam-tenant-service", "http://localhost:8083", "DOWN", 0, Instant.now(), null),
                        new ServiceHealthDto("iam-audit-service", "http://localhost:8084", "DOWN", 0, Instant.now(), null),
                        new ServiceHealthDto("iam-xroad-adapter", "http://localhost:8085", "UP", 40, Instant.now(), null)
                ),
                Instant.now()
        );
        when(healthAggregationService.getAggregatedHealth()).thenReturn(degradedResponse);

        alertService.evaluateAlerts();

        // Alert triggered
        verify(auditService).publishAlertTrigger(any());
    }
}
