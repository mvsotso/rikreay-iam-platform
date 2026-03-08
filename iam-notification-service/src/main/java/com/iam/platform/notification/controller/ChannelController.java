package com.iam.platform.notification.controller;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.notification.dto.ChannelRequest;
import com.iam.platform.notification.dto.ChannelResponse;
import com.iam.platform.notification.entity.NotificationChannel;
import com.iam.platform.notification.repository.NotificationChannelRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/channels")
@RequiredArgsConstructor
@Tag(name = "Notification Channels", description = "Channel configuration management")
public class ChannelController {

    private final NotificationChannelRepository channelRepository;

    @PostMapping
    @Operation(summary = "Create a notification channel")
    public ResponseEntity<ApiResponse<ChannelResponse>> createChannel(
            @Valid @RequestBody ChannelRequest request) {
        if (channelRepository.existsByChannelName(request.channelName())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Channel '" + request.channelName() + "' already exists"));
        }

        NotificationChannel channel = NotificationChannel.builder()
                .channelType(request.channelType())
                .channelName(request.channelName())
                .configJson(request.configJson() != null ? request.configJson() : Map.of())
                .enabled(request.enabled())
                .build();

        channel = channelRepository.save(channel);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.ok(toResponse(channel), "Channel created"));
    }

    @GetMapping
    @Operation(summary = "List all notification channels")
    public ResponseEntity<ApiResponse<List<ChannelResponse>>> listChannels() {
        List<ChannelResponse> channels = channelRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(channels));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a notification channel by ID")
    public ResponseEntity<ApiResponse<ChannelResponse>> getChannel(@PathVariable UUID id) {
        NotificationChannel channel = channelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + id));
        return ResponseEntity.ok(ApiResponse.ok(toResponse(channel)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a notification channel")
    public ResponseEntity<ApiResponse<ChannelResponse>> updateChannel(
            @PathVariable UUID id,
            @Valid @RequestBody ChannelRequest request) {
        NotificationChannel channel = channelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + id));

        channel.setChannelType(request.channelType());
        channel.setChannelName(request.channelName());
        channel.setConfigJson(request.configJson() != null ? request.configJson() : Map.of());
        channel.setEnabled(request.enabled());

        channel = channelRepository.save(channel);
        return ResponseEntity.ok(ApiResponse.ok(toResponse(channel), "Channel updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification channel (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteChannel(@PathVariable UUID id) {
        NotificationChannel channel = channelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + id));
        channel.softDelete();
        channelRepository.save(channel);
        return ResponseEntity.ok(ApiResponse.ok(null, "Channel deleted"));
    }

    private ChannelResponse toResponse(NotificationChannel channel) {
        return new ChannelResponse(
                channel.getId(),
                channel.getChannelType(),
                channel.getChannelName(),
                channel.getConfigJson(),
                channel.isEnabled(),
                channel.getCreatedAt(),
                channel.getUpdatedAt()
        );
    }
}
