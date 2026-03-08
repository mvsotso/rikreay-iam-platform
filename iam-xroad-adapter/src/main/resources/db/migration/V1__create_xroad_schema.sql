-- V1: Create X-Road schema for iam-xroad-adapter
-- Service registrations and ACL entries for X-Road routing

CREATE TABLE xroad_service_registrations (
    id              UUID PRIMARY KEY,
    service_code    VARCHAR(255) NOT NULL,
    service_version VARCHAR(20)  NOT NULL DEFAULT 'v1',
    target_service  VARCHAR(255) NOT NULL,
    target_path     VARCHAR(500) NOT NULL,
    description     TEXT,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE,

    CONSTRAINT uk_xroad_service_code_version UNIQUE (service_code, service_version)
);

CREATE TABLE xroad_acl_entries (
    id                      UUID PRIMARY KEY,
    consumer_identifier     VARCHAR(500) NOT NULL,
    service_registration_id UUID         NOT NULL,
    allowed                 BOOLEAN      NOT NULL DEFAULT TRUE,
    description             TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted                 BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_acl_service_registration
        FOREIGN KEY (service_registration_id) REFERENCES xroad_service_registrations (id),
    CONSTRAINT uk_acl_consumer_service UNIQUE (consumer_identifier, service_registration_id)
);

CREATE INDEX idx_xroad_services_code ON xroad_service_registrations (service_code) WHERE deleted = false;
CREATE INDEX idx_xroad_services_enabled ON xroad_service_registrations (enabled) WHERE deleted = false;
CREATE INDEX idx_xroad_services_deleted ON xroad_service_registrations (deleted);
CREATE INDEX idx_xroad_acl_consumer ON xroad_acl_entries (consumer_identifier) WHERE deleted = false;
CREATE INDEX idx_xroad_acl_service_reg ON xroad_acl_entries (service_registration_id) WHERE deleted = false;
CREATE INDEX idx_xroad_acl_deleted ON xroad_acl_entries (deleted);
