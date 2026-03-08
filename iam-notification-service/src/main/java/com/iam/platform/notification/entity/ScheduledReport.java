package com.iam.platform.notification.entity;

import com.iam.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "scheduled_reports")
public class ScheduledReport extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "cron_expression", nullable = false, length = 100)
    private String cronExpression;

    @Column(name = "template_id")
    private UUID templateId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "recipient_list", columnDefinition = "TEXT[]", nullable = false)
    private List<String> recipientList;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "next_run_at")
    private Instant nextRunAt;
}
