package com.iam.platform.notification.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.notification.dto.ScheduledReportRequest;
import com.iam.platform.notification.dto.ScheduledReportResponse;
import com.iam.platform.notification.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/schedules")
@RequiredArgsConstructor
@Tag(name = "Scheduled Reports", description = "Scheduled report management APIs")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    @Operation(summary = "Create a scheduled report")
    public ResponseEntity<ApiResponse<ScheduledReportResponse>> createSchedule(
            @Valid @RequestBody ScheduledReportRequest request) {
        ScheduledReportResponse response = scheduleService.createSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.ok(response, "Scheduled report created"));
    }

    @GetMapping
    @Operation(summary = "List scheduled reports")
    public ResponseEntity<ApiResponse<Page<ScheduledReportResponse>>> listSchedules(Pageable pageable) {
        Page<ScheduledReportResponse> schedules = scheduleService.listSchedules(pageable);
        return ResponseEntity.ok(ApiResponse.ok(schedules));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a scheduled report by ID")
    public ResponseEntity<ApiResponse<ScheduledReportResponse>> getSchedule(@PathVariable UUID id) {
        ScheduledReportResponse response = scheduleService.getSchedule(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a scheduled report")
    public ResponseEntity<ApiResponse<ScheduledReportResponse>> updateSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody ScheduledReportRequest request) {
        ScheduledReportResponse response = scheduleService.updateSchedule(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Scheduled report updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a scheduled report (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(@PathVariable UUID id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Scheduled report deleted"));
    }
}
