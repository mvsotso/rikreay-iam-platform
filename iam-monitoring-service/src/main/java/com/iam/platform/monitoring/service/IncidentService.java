package com.iam.platform.monitoring.service;

import com.iam.platform.monitoring.dto.IncidentRequest;
import com.iam.platform.monitoring.dto.IncidentResponse;
import com.iam.platform.monitoring.entity.Incident;
import com.iam.platform.monitoring.enums.IncidentStatus;
import com.iam.platform.monitoring.enums.Severity;
import com.iam.platform.monitoring.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final AuditService auditService;

    @Transactional
    public IncidentResponse createIncident(IncidentRequest request, String username) {
        Incident incident = Incident.builder()
                .title(request.title())
                .severity(request.severity())
                .status(IncidentStatus.OPEN)
                .description(request.description())
                .serviceAffected(request.serviceAffected())
                .assignedTo(request.assignedTo())
                .build();

        Incident saved = incidentRepository.save(incident);
        log.info("Incident created: id={}, title={}, severity={}", saved.getId(), saved.getTitle(), saved.getSeverity());

        auditService.logMonitoringAction(username, "CREATE_INCIDENT", "incidents/" + saved.getId(),
                true, Map.of("title", saved.getTitle(), "severity", saved.getSeverity().name()));

        return toResponse(saved);
    }

    public IncidentResponse getIncident(UUID id) {
        return incidentRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Incident not found: " + id));
    }

    public Page<IncidentResponse> listIncidents(IncidentStatus status, Severity severity,
                                                  String serviceAffected, Pageable pageable) {
        if (status != null) {
            return incidentRepository.findByStatus(status, pageable).map(this::toResponse);
        }
        if (severity != null) {
            return incidentRepository.findBySeverity(severity, pageable).map(this::toResponse);
        }
        if (serviceAffected != null) {
            return incidentRepository.findByServiceAffected(serviceAffected, pageable).map(this::toResponse);
        }
        return incidentRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public IncidentResponse updateIncidentStatus(UUID id, IncidentStatus newStatus, String username) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found: " + id));

        IncidentStatus oldStatus = incident.getStatus();
        incident.setStatus(newStatus);

        if (newStatus == IncidentStatus.RESOLVED || newStatus == IncidentStatus.CLOSED) {
            incident.setResolvedAt(Instant.now());
        }

        Incident saved = incidentRepository.save(incident);
        log.info("Incident status updated: id={}, {} -> {}", id, oldStatus, newStatus);

        auditService.logMonitoringAction(username, "UPDATE_INCIDENT_STATUS", "incidents/" + id,
                true, Map.of("oldStatus", oldStatus.name(), "newStatus", newStatus.name()));

        return toResponse(saved);
    }

    @Transactional
    public void deleteIncident(UUID id, String username) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found: " + id));

        incident.softDelete();
        incidentRepository.save(incident);
        log.info("Incident deleted: id={}", id);

        auditService.logMonitoringAction(username, "DELETE_INCIDENT", "incidents/" + id,
                true, Map.of("title", incident.getTitle()));
    }

    public Incident createIncidentFromAlert(String title, Severity severity, String serviceAffected, String description) {
        Incident incident = Incident.builder()
                .title(title)
                .severity(severity)
                .status(IncidentStatus.OPEN)
                .description(description)
                .serviceAffected(serviceAffected)
                .build();

        Incident saved = incidentRepository.save(incident);
        log.info("Auto-created incident from alert: id={}, title={}", saved.getId(), saved.getTitle());

        auditService.logMonitoringAction("system", "AUTO_CREATE_INCIDENT", "incidents/" + saved.getId(),
                true, Map.of("title", saved.getTitle(), "severity", severity.name(), "source", "alert"));

        return saved;
    }

    private IncidentResponse toResponse(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getDescription(),
                incident.getServiceAffected(),
                incident.getAssignedTo(),
                incident.getResolvedAt(),
                incident.getCreatedAt(),
                incident.getUpdatedAt()
        );
    }
}
