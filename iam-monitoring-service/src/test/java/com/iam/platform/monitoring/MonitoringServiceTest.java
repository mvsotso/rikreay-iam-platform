package com.iam.platform.monitoring;

import com.iam.platform.monitoring.dto.AlertRuleRequest;
import com.iam.platform.monitoring.dto.AlertRuleResponse;
import com.iam.platform.monitoring.dto.IncidentRequest;
import com.iam.platform.monitoring.dto.IncidentResponse;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class MonitoringServiceTest {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private AlertService alertService;

    @MockitoBean
    private IncidentRepository incidentRepository;

    @MockitoBean
    private AlertRuleRepository alertRuleRepository;

    @MockitoBean
    private HealthAggregationService healthAggregationService;

    @MockitoBean
    private AuditService auditService;

    @Test
    @DisplayName("Create incident should persist and return response")
    void createIncident() {
        IncidentRequest request = new IncidentRequest(
                "Service Down", Severity.CRITICAL,
                "iam-core-service is not responding", "iam-core-service", "ops-admin");

        Incident saved = Incident.builder()
                .title("Service Down")
                .severity(Severity.CRITICAL)
                .status(IncidentStatus.OPEN)
                .description("iam-core-service is not responding")
                .serviceAffected("iam-core-service")
                .assignedTo("ops-admin")
                .build();

        when(incidentRepository.save(any())).thenReturn(saved);

        IncidentResponse response = incidentService.createIncident(request, "ops-admin");

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Service Down");
        assertThat(response.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(response.status()).isEqualTo(IncidentStatus.OPEN);
    }

    @Test
    @DisplayName("Create alert rule should persist and return response")
    void createAlertRule() {
        AlertRuleRequest request = new AlertRuleRequest(
                "High CPU", "SERVICE_DOWN", "1",
                ChannelType.EMAIL, "iam-core-service", true);

        AlertRule saved = AlertRule.builder()
                .name("High CPU")
                .condition("SERVICE_DOWN")
                .threshold("1")
                .channelType(ChannelType.EMAIL)
                .serviceTarget("iam-core-service")
                .enabled(true)
                .build();

        when(alertRuleRepository.save(any())).thenReturn(saved);

        AlertRuleResponse response = alertService.createAlertRule(request, "ops-admin");

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("High CPU");
        assertThat(response.enabled()).isTrue();
    }

    @Test
    @DisplayName("Resolve incident should update status")
    void resolveIncident() {
        UUID id = UUID.randomUUID();
        Incident incident = Incident.builder()
                .title("Test Incident")
                .severity(Severity.MEDIUM)
                .status(IncidentStatus.OPEN)
                .serviceAffected("test")
                .build();

        when(incidentRepository.findById(id)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IncidentResponse response = incidentService.updateIncidentStatus(id, IncidentStatus.RESOLVED, "ops-admin");

        assertThat(response.status()).isEqualTo(IncidentStatus.RESOLVED);
    }
}
