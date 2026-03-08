-- Config Service Schema
-- Feature flags and configuration change tracking

CREATE TABLE feature_flags (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_key        VARCHAR(255) NOT NULL,
    flag_value      VARCHAR(1000),
    description     VARCHAR(500),
    enabled         BOOLEAN NOT NULL DEFAULT false,
    environment     VARCHAR(50) NOT NULL DEFAULT 'all',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted         BOOLEAN NOT NULL DEFAULT false,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_feature_flag_key_env UNIQUE (flag_key, environment)
);

CREATE TABLE config_change_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version         BIGINT NOT NULL,
    application     VARCHAR(255) NOT NULL,
    profile         VARCHAR(100) NOT NULL,
    changes_json    JSONB NOT NULL DEFAULT '{}',
    author          VARCHAR(255) NOT NULL,
    change_type     VARCHAR(50) NOT NULL DEFAULT 'UPDATE',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted         BOOLEAN NOT NULL DEFAULT false,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_feature_flags_key ON feature_flags (flag_key);
CREATE INDEX idx_feature_flags_environment ON feature_flags (environment);
CREATE INDEX idx_feature_flags_enabled ON feature_flags (enabled) WHERE deleted = false;

CREATE INDEX idx_config_change_logs_app_profile ON config_change_logs (application, profile);
CREATE INDEX idx_config_change_logs_version ON config_change_logs (version);
CREATE INDEX idx_config_change_logs_author ON config_change_logs (author);
CREATE INDEX idx_config_change_logs_created_at ON config_change_logs (created_at);

-- Sequence for config change version numbers
CREATE SEQUENCE config_version_seq START WITH 1 INCREMENT BY 1;
