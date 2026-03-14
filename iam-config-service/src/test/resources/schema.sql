-- H2 test schema for iam-config-service

-- Create jsonb as domain type mapping to varchar for H2 compatibility
CREATE DOMAIN IF NOT EXISTS JSONB AS VARCHAR(10000);

CREATE SEQUENCE IF NOT EXISTS config_version_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS config_change_logs (
    id UUID NOT NULL,
    version BIGINT NOT NULL,
    application VARCHAR(255) NOT NULL,
    profile VARCHAR(100) NOT NULL,
    changes_json JSONB NOT NULL,
    author VARCHAR(255) NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS feature_flags (
    id UUID NOT NULL,
    flag_key VARCHAR(255) NOT NULL,
    flag_value VARCHAR(1000),
    description VARCHAR(500),
    enabled BOOLEAN NOT NULL,
    environment VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (flag_key, environment)
);
