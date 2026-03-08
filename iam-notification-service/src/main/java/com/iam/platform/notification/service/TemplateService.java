package com.iam.platform.notification.service;

import com.iam.platform.notification.dto.TemplateRequest;
import com.iam.platform.notification.dto.TemplateResponse;
import com.iam.platform.notification.entity.NotificationTemplate;
import com.iam.platform.notification.enums.ChannelType;
import com.iam.platform.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final NotificationTemplateRepository templateRepository;

    @Transactional
    public TemplateResponse createTemplate(TemplateRequest request) {
        if (templateRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Template '" + request.name() + "' already exists");
        }

        NotificationTemplate template = NotificationTemplate.builder()
                .name(request.name())
                .subject(request.subject())
                .bodyTemplate(request.bodyTemplate())
                .channelType(request.channelType())
                .variableNames(request.variableNames())
                .build();

        template = templateRepository.save(template);
        log.info("Template created: {}", request.name());
        return toResponse(template);
    }

    @Transactional(readOnly = true)
    public Page<TemplateResponse> listTemplates(ChannelType channelType, Pageable pageable) {
        Page<NotificationTemplate> templates;
        if (channelType != null) {
            templates = templateRepository.findByChannelType(channelType, pageable);
        } else {
            templates = templateRepository.findAll(pageable);
        }
        return templates.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TemplateResponse getTemplate(UUID id) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
        return toResponse(template);
    }

    @Transactional(readOnly = true)
    public TemplateResponse getTemplateByName(String name) {
        NotificationTemplate template = templateRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + name));
        return toResponse(template);
    }

    @Transactional
    public TemplateResponse updateTemplate(UUID id, TemplateRequest request) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));

        template.setName(request.name());
        template.setSubject(request.subject());
        template.setBodyTemplate(request.bodyTemplate());
        template.setChannelType(request.channelType());
        template.setVariableNames(request.variableNames());

        template = templateRepository.save(template);
        log.info("Template updated: {}", request.name());
        return toResponse(template);
    }

    @Transactional
    public void deleteTemplate(UUID id) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
        template.softDelete();
        templateRepository.save(template);
        log.info("Template deleted: {}", template.getName());
    }

    public String renderTemplate(String templateName, Map<String, String> variables) {
        NotificationTemplate template = templateRepository.findByName(templateName)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateName));

        String rendered = template.getBodyTemplate();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return rendered;
    }

    private TemplateResponse toResponse(NotificationTemplate template) {
        return new TemplateResponse(
                template.getId(),
                template.getName(),
                template.getSubject(),
                template.getBodyTemplate(),
                template.getChannelType(),
                template.getVariableNames(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }
}
