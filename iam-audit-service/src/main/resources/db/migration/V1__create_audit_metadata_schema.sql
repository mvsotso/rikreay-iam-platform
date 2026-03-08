-- V1: Create audit metadata schema for iam-audit-service
-- Primary storage is Elasticsearch; PostgreSQL serves as fallback when ES is unavailable

CREATE TABLE audit_events (
    id          UUID PRIMARY KEY,
    event_type  VARCHAR(50)  NOT NULL,
    timestamp   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    username    VARCHAR(255),
    action      VARCHAR(255) NOT NULL,
    resource    VARCHAR(500),
    source_ip   VARCHAR(45),
    success     BOOLEAN      NOT NULL DEFAULT TRUE,
    metadata    JSONB        DEFAULT '{}',
    tenant_id   VARCHAR(255),
    member_class VARCHAR(10),
    indexed_to_es BOOLEAN    NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_audit_events_type ON audit_events (event_type);
CREATE INDEX idx_audit_events_username ON audit_events (username);
CREATE INDEX idx_audit_events_timestamp ON audit_events (timestamp);
CREATE INDEX idx_audit_events_tenant_id ON audit_events (tenant_id) WHERE tenant_id IS NOT NULL;
CREATE INDEX idx_audit_events_member_class ON audit_events (member_class) WHERE member_class IS NOT NULL;
CREATE INDEX idx_audit_events_action ON audit_events (action);
CREATE INDEX idx_audit_events_not_indexed ON audit_events (indexed_to_es) WHERE indexed_to_es = false;
CREATE INDEX idx_audit_events_deleted ON audit_events (deleted);
