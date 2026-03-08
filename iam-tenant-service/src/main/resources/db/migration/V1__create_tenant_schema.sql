-- V1: Create tenant schema for iam-tenant-service
-- Each tenant maps to a Keycloak realm (realm-per-tenant model)

CREATE TABLE tenants (
    id              UUID PRIMARY KEY,
    tenant_name     VARCHAR(255) NOT NULL,
    realm_name      VARCHAR(255) NOT NULL,
    description     TEXT,
    member_class    VARCHAR(10)  NOT NULL,
    entity_type     VARCHAR(50),
    registration_number VARCHAR(100),
    tin             VARCHAR(50),
    member_code     VARCHAR(100),
    xroad_subsystem VARCHAR(255),
    status          VARCHAR(30)  NOT NULL DEFAULT 'PROVISIONING',
    admin_email     VARCHAR(255) NOT NULL,
    admin_username  VARCHAR(255) NOT NULL,
    settings        JSONB DEFAULT '{}',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE,

    CONSTRAINT uk_tenants_tenant_name UNIQUE (tenant_name),
    CONSTRAINT uk_tenants_realm_name UNIQUE (realm_name),
    CONSTRAINT chk_tenants_member_class CHECK (member_class IN ('GOV', 'COM', 'NGO', 'MUN')),
    CONSTRAINT chk_tenants_status CHECK (status IN ('PROVISIONING', 'ACTIVE', 'SUSPENDED', 'DECOMMISSIONED'))
);

CREATE INDEX idx_tenants_member_class ON tenants (member_class) WHERE deleted = false;
CREATE INDEX idx_tenants_status ON tenants (status) WHERE deleted = false;
CREATE INDEX idx_tenants_deleted ON tenants (deleted);
