-- Developer Portal Schema
-- V1: registered_apps, webhook_configs, webhook_delivery_logs, sandbox_realms

CREATE TABLE registered_apps (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    client_id VARCHAR(255) NOT NULL UNIQUE,
    client_secret_encrypted TEXT,
    redirect_uris_json JSONB DEFAULT '[]',
    owner_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE TABLE webhook_configs (
    id UUID PRIMARY KEY,
    app_id UUID NOT NULL REFERENCES registered_apps(id),
    event_type VARCHAR(50) NOT NULL,
    target_url VARCHAR(1024) NOT NULL,
    secret_hash VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Hard delete for log rotation
CREATE TABLE webhook_delivery_logs (
    id UUID PRIMARY KEY,
    webhook_id UUID NOT NULL REFERENCES webhook_configs(id),
    event_type VARCHAR(50) NOT NULL,
    http_status INTEGER,
    response_time BIGINT,
    error TEXT,
    sent_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Hard delete on expiry cleanup
CREATE TABLE sandbox_realms (
    id UUID PRIMARY KEY,
    owner_id VARCHAR(255) NOT NULL,
    realm_name VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'DELETED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_registered_apps_owner ON registered_apps(owner_id);
CREATE INDEX idx_registered_apps_status ON registered_apps(status);
CREATE INDEX idx_registered_apps_deleted ON registered_apps(deleted);
CREATE INDEX idx_webhook_configs_app_id ON webhook_configs(app_id);
CREATE INDEX idx_webhook_configs_event_type ON webhook_configs(event_type);
CREATE INDEX idx_webhook_configs_deleted ON webhook_configs(deleted);
CREATE INDEX idx_webhook_delivery_logs_webhook_id ON webhook_delivery_logs(webhook_id);
CREATE INDEX idx_webhook_delivery_logs_sent_at ON webhook_delivery_logs(sent_at);
CREATE INDEX idx_sandbox_realms_owner ON sandbox_realms(owner_id);
CREATE INDEX idx_sandbox_realms_status ON sandbox_realms(status);
CREATE INDEX idx_sandbox_realms_expires_at ON sandbox_realms(expires_at);
