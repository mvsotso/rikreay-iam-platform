package com.iam.platform.notification.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.notification.dto.NotificationLogResponse;
import com.iam.platform.notification.dto.SendNotificationRequest;
import com.iam.platform.notification.entity.NotificationLog;
import com.iam.platform.notification.enums.ChannelType;
import com.iam.platform.notification.enums.NotificationStatus;
import com.iam.platform.notification.repository.NotificationLogRepository;
import com.iam.platform.notification.service.NotificationDispatcher;
import com.iam.platform.notification.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification log and ad-hoc send APIs")
public class NotificationController {

    private final NotificationLogRepository logRepository;
    private final NotificationDispatcher dispatcher;
    private final TemplateService templateService;

    @GetMapping
    @Operation(summary = "List notification logs")
    public ResponseEntity<ApiResponse<Page<NotificationLogResponse>>> listLogs(
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) ChannelType channelType,
            Pageable pageable) {

        Page<NotificationLog> logs;
        if (status != null) {
            logs = logRepository.findByStatus(status, pageable);
        } else if (channelType != null) {
            logs = logRepository.findByChannelType(channelType, pageable);
        } else {
            logs = logRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        Page<NotificationLogResponse> response = logs.map(this::toResponse);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get notification statistics")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStats() {
        Map<String, Long> stats = Map.of(
                "pending", logRepository.countByStatus(NotificationStatus.PENDING),
                "sent", logRepository.countByStatus(NotificationStatus.SENT),
                "failed", logRepository.countByStatus(NotificationStatus.FAILED)
        );
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @PostMapping("/send")
    @Operation(summary = "Send an ad-hoc notification")
    public ResponseEntity<ApiResponse<String>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {

        String body;
        if (request.templateName() != null && !request.templateName().isBlank()) {
            body = templateService.renderTemplate(request.templateName(),
                    request.variables() != null ? request.variables() : Map.of());
        } else {
            body = request.body();
        }

        dispatcher.dispatch(request.channelType(), request.recipient(),
                request.subject(), body, null);

        return ResponseEntity.ok(ApiResponse.ok("Notification dispatched", "Notification sent successfully"));
    }

    private NotificationLogResponse toResponse(NotificationLog log) {
        return new NotificationLogResponse(
                log.getId(),
                log.getTemplateId(),
                log.getChannelType(),
                log.getRecipient(),
                log.getSubject(),
                log.getStatus(),
                log.getSentAt(),
                log.getErrorMessage(),
                log.getCreatedAt()
        );
    }
}
