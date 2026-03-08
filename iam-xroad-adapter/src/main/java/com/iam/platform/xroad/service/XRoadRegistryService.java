package com.iam.platform.xroad.service;

import com.iam.platform.common.exception.ResourceNotFoundException;
import com.iam.platform.common.exception.XRoadServiceException;
import com.iam.platform.xroad.dto.AclEntryRequest;
import com.iam.platform.xroad.dto.AclEntryResponse;
import com.iam.platform.xroad.dto.ServiceRegistrationRequest;
import com.iam.platform.xroad.dto.ServiceRegistrationResponse;
import com.iam.platform.xroad.entity.XRoadAclEntry;
import com.iam.platform.xroad.entity.XRoadServiceRegistration;
import com.iam.platform.xroad.repository.XRoadAclEntryRepository;
import com.iam.platform.xroad.repository.XRoadServiceRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD operations for X-Road service registrations and ACL entries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XRoadRegistryService {

    private final XRoadServiceRegistrationRepository serviceRepository;
    private final XRoadAclEntryRepository aclRepository;
    private final AuditService auditService;

    // --- Service Registrations ---

    @Transactional(readOnly = true)
    public List<ServiceRegistrationResponse> listServices() {
        return serviceRepository.findAll().stream()
                .map(this::toServiceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceRegistrationResponse getService(UUID id) {
        return toServiceResponse(findServiceById(id));
    }

    @Transactional
    public ServiceRegistrationResponse createService(ServiceRegistrationRequest request) {
        if (serviceRepository.existsByServiceCodeAndServiceVersion(
                request.serviceCode(), request.serviceVersion())) {
            throw new XRoadServiceException("Service '" + request.serviceCode()
                    + "' version '" + request.serviceVersion() + "' already exists");
        }

        XRoadServiceRegistration service = XRoadServiceRegistration.builder()
                .serviceCode(request.serviceCode())
                .serviceVersion(request.serviceVersion())
                .targetService(request.targetService())
                .targetPath(request.targetPath())
                .description(request.description())
                .enabled(request.enabled())
                .build();
        service = serviceRepository.save(service);

        auditService.logAdminAction(getCurrentUsername(), "XROAD_SERVICE_CREATED",
                "xroad-service:" + request.serviceCode(), true);

        return toServiceResponse(service);
    }

    @Transactional
    public void deleteService(UUID id) {
        XRoadServiceRegistration service = findServiceById(id);
        service.softDelete();
        serviceRepository.save(service);

        auditService.logAdminAction(getCurrentUsername(), "XROAD_SERVICE_DELETED",
                "xroad-service:" + service.getServiceCode(), true);
    }

    // --- ACL Entries ---

    @Transactional(readOnly = true)
    public List<AclEntryResponse> listAclEntries() {
        return aclRepository.findAll().stream()
                .map(this::toAclResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AclEntryResponse> getAclForService(UUID serviceRegistrationId) {
        return aclRepository.findByServiceRegistrationId(serviceRegistrationId).stream()
                .map(this::toAclResponse)
                .toList();
    }

    @Transactional
    public AclEntryResponse createAclEntry(AclEntryRequest request) {
        XRoadServiceRegistration service = findServiceById(request.serviceRegistrationId());

        if (aclRepository.existsByConsumerIdentifierAndServiceRegistrationId(
                request.consumerIdentifier(), request.serviceRegistrationId())) {
            throw new XRoadServiceException("ACL entry for consumer '"
                    + request.consumerIdentifier() + "' on service '"
                    + service.getServiceCode() + "' already exists");
        }

        XRoadAclEntry entry = XRoadAclEntry.builder()
                .consumerIdentifier(request.consumerIdentifier())
                .serviceRegistration(service)
                .allowed(true)
                .description(request.description())
                .build();
        entry = aclRepository.save(entry);

        auditService.logAdminAction(getCurrentUsername(), "XROAD_ACL_CREATED",
                "xroad-acl:" + request.consumerIdentifier() + "→" + service.getServiceCode(), true);

        return toAclResponse(entry);
    }

    @Transactional
    public void deleteAclEntry(UUID id) {
        XRoadAclEntry entry = aclRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ACL entry not found: " + id));
        entry.softDelete();
        aclRepository.save(entry);

        auditService.logAdminAction(getCurrentUsername(), "XROAD_ACL_DELETED",
                "xroad-acl:" + entry.getConsumerIdentifier(), true);
    }

    // --- Helpers ---

    private XRoadServiceRegistration findServiceById(UUID id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "X-Road service registration not found: " + id));
    }

    private ServiceRegistrationResponse toServiceResponse(XRoadServiceRegistration service) {
        return new ServiceRegistrationResponse(
                service.getId(),
                service.getServiceCode(),
                service.getServiceVersion(),
                service.getTargetService(),
                service.getTargetPath(),
                service.getDescription(),
                service.isEnabled(),
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }

    private AclEntryResponse toAclResponse(XRoadAclEntry entry) {
        return new AclEntryResponse(
                entry.getId(),
                entry.getConsumerIdentifier(),
                entry.getServiceRegistration().getId(),
                entry.getServiceRegistration().getServiceCode(),
                entry.isAllowed(),
                entry.getDescription(),
                entry.getCreatedAt()
        );
    }

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }
}
