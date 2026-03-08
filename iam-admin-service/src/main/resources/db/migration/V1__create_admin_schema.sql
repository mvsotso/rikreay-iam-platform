-- Admin Service Schema

CREATE TABLE platform_settings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key     VARCHAR(255) NOT NULL UNIQUE,
    setting_value   TEXT,
    category        VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted         BOOLEAN NOT NULL DEFAULT false,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE TABLE usage_records (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(255) NOT NULL,
    legal_entity_id UUID,
    record_date     DATE NOT NULL,
    metric_type     VARCHAR(50) NOT NULL,
    count           BIGINT NOT NULL DEFAULT 0,
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted         BOOLEAN NOT NULL DEFAULT false,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_usage_tenant_date_metric UNIQUE (tenant_id, record_date, metric_type)
);

CREATE TABLE sector_admin_assignments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    natural_person_id   UUID NOT NULL,
    member_class        VARCHAR(10) NOT NULL,
    assigned_by_user_id VARCHAR(255) NOT NULL,
    valid_from          TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until         TIMESTAMP WITH TIME ZONE,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted             BOOLEAN NOT NULL DEFAULT false,
    deleted_at          TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_member_class CHECK (member_class IN ('GOV', 'COM', 'NGO', 'MUN')),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'REVOKED'))
);

CREATE TABLE org_notification_configs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(255) NOT NULL,
    event_types     TEXT[] NOT NULL DEFAULT '{}',
    channels        TEXT[] NOT NULL DEFAULT '{}',
    recipients      TEXT[] NOT NULL DEFAULT '{}',
    enabled         BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted         BOOLEAN NOT NULL DEFAULT false,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_org_notification_tenant UNIQUE (tenant_id)
);

CREATE INDEX idx_platform_settings_category ON platform_settings (category) WHERE deleted = false;
CREATE INDEX idx_platform_settings_key ON platform_settings (setting_key);

CREATE INDEX idx_usage_records_tenant ON usage_records (tenant_id);
CREATE INDEX idx_usage_records_date ON usage_records (record_date);
CREATE INDEX idx_usage_records_metric ON usage_records (metric_type);
CREATE INDEX idx_usage_records_tenant_date ON usage_records (tenant_id, record_date);

CREATE INDEX idx_sector_admin_member_class ON sector_admin_assignments (member_class) WHERE deleted = false;
CREATE INDEX idx_sector_admin_person ON sector_admin_assignments (natural_person_id);
CREATE INDEX idx_sector_admin_status ON sector_admin_assignments (status) WHERE deleted = false;

CREATE INDEX idx_org_notification_tenant ON org_notification_configs (tenant_id) WHERE deleted = false;
