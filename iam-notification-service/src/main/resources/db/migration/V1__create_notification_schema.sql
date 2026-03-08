-- Notification Service Schema

CREATE TABLE notification_channels (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_type    VARCHAR(50) NOT NULL,
    channel_name    VARCHAR(255) NOT NULL,
    config_json     JSONB NOT NULL DEFAULT '{}',
    enabled         BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted         BOOLEAN NOT NULL DEFAULT false,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_channel_name UNIQUE (channel_name)
);

CREATE TABLE notification_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    subject         VARCHAR(500),
    body_template   TEXT NOT NULL,
    channel_type    VARCHAR(50) NOT NULL,
    variable_names  TEXT[],
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted         BOOLEAN NOT NULL DEFAULT false,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_template_name UNIQUE (name)
);

-- NotificationLog uses HARD delete for log rotation (no soft delete columns)
CREATE TABLE notification_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id     UUID REFERENCES notification_templates(id),
    channel_type    VARCHAR(50) NOT NULL,
    recipient       VARCHAR(500) NOT NULL,
    subject         VARCHAR(500),
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    sent_at         TIMESTAMP WITH TIME ZONE,
    error_message   TEXT,
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE scheduled_reports (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    cron_expression VARCHAR(100) NOT NULL,
    template_id     UUID REFERENCES notification_templates(id),
    recipient_list  TEXT[] NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT true,
    last_run_at     TIMESTAMP WITH TIME ZONE,
    next_run_at     TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted         BOOLEAN NOT NULL DEFAULT false,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_scheduled_report_name UNIQUE (name)
);

CREATE INDEX idx_notification_logs_status ON notification_logs (status);
CREATE INDEX idx_notification_logs_channel_type ON notification_logs (channel_type);
CREATE INDEX idx_notification_logs_created_at ON notification_logs (created_at);
CREATE INDEX idx_notification_logs_recipient ON notification_logs (recipient);
CREATE INDEX idx_notification_templates_channel_type ON notification_templates (channel_type) WHERE deleted = false;
CREATE INDEX idx_notification_channels_type ON notification_channels (channel_type) WHERE deleted = false;
CREATE INDEX idx_scheduled_reports_enabled ON scheduled_reports (enabled) WHERE deleted = false;
