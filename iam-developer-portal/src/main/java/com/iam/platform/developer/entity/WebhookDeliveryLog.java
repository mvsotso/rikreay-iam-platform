package com.iam.platform.developer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_delivery_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "webhook_id", nullable = false)
    private UUID webhookId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "response_time")
    private Long responseTime;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(name = "sent_at", nullable = false)
    @Builder.Default
    private Instant sentAt = Instant.now();
}
