-- =============================================================================
-- V1: Core Service Initial Schema — Cambodia Identity Model
-- =============================================================================

-- Natural Persons (រូបវន្តបុគ្គល)
CREATE TABLE natural_persons (
    id              UUID PRIMARY KEY,
    personal_id_code    VARCHAR(50) UNIQUE,
    national_id_number  VARCHAR(50),
    cam_digi_key_id     VARCHAR(100),
    first_name_kh       VARCHAR(255),
    last_name_kh        VARCHAR(255),
    first_name_en       VARCHAR(255),
    last_name_en        VARCHAR(255),
    date_of_birth       DATE,
    gender              VARCHAR(20),
    nationality         VARCHAR(50) DEFAULT 'KH',
    identity_verification_status  VARCHAR(30) DEFAULT 'UNVERIFIED',
    identity_verification_method  VARCHAR(50),
    identity_verified_at          TIMESTAMPTZ,
    keycloak_user_id    VARCHAR(100),
    status              VARCHAR(20) DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ
);

CREATE INDEX idx_natural_persons_keycloak ON natural_persons(keycloak_user_id);
CREATE INDEX idx_natural_persons_status ON natural_persons(status) WHERE deleted = false;

-- Legal Entities (នីតិបុគ្គល)
CREATE TABLE legal_entities (
    id                      UUID PRIMARY KEY,
    registration_number     VARCHAR(100) UNIQUE,
    tax_identification_number VARCHAR(50) UNIQUE,
    name_kh                 VARCHAR(500),
    name_en                 VARCHAR(500),
    entity_type             VARCHAR(50) NOT NULL,
    member_class            VARCHAR(10) NOT NULL,
    xroad_member_code       VARCHAR(100),
    xroad_subsystem         VARCHAR(100),
    sector_code             VARCHAR(50),
    incorporation_date      DATE,
    registered_address      TEXT,
    province                VARCHAR(100),
    realm_name              VARCHAR(100) UNIQUE,
    parent_entity_id        UUID REFERENCES legal_entities(id),
    status                  VARCHAR(20) DEFAULT 'ACTIVE',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted                 BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMPTZ
);

CREATE INDEX idx_legal_entities_member_class ON legal_entities(member_class) WHERE deleted = false;
CREATE INDEX idx_legal_entities_entity_type ON legal_entities(entity_type) WHERE deleted = false;
CREATE INDEX idx_legal_entities_status ON legal_entities(status) WHERE deleted = false;

-- Representations (អ្នកតំណាង)
CREATE TABLE representations (
    id                      UUID PRIMARY KEY,
    natural_person_id       UUID NOT NULL REFERENCES natural_persons(id),
    legal_entity_id         UUID NOT NULL REFERENCES legal_entities(id),
    representative_role     VARCHAR(50) NOT NULL,
    delegation_scope        VARCHAR(30) NOT NULL,
    specific_permissions    JSONB,
    title                   VARCHAR(255),
    valid_from              DATE NOT NULL,
    valid_until             DATE,
    authorized_by_person_id UUID REFERENCES natural_persons(id),
    authorization_document  VARCHAR(500),
    authorization_document_type VARCHAR(50),
    verification_status     VARCHAR(30) DEFAULT 'UNVERIFIED',
    is_primary              BOOLEAN DEFAULT FALSE,
    status                  VARCHAR(20) DEFAULT 'ACTIVE',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted                 BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMPTZ
);

CREATE INDEX idx_representations_person ON representations(natural_person_id) WHERE deleted = false;
CREATE INDEX idx_representations_entity ON representations(legal_entity_id) WHERE deleted = false;
CREATE INDEX idx_representations_person_entity ON representations(natural_person_id, legal_entity_id) WHERE deleted = false;

-- External Identity Links
CREATE TABLE external_identity_links (
    id                  UUID PRIMARY KEY,
    owner_type          VARCHAR(30) NOT NULL,
    owner_id            UUID NOT NULL,
    external_system     VARCHAR(50) NOT NULL,
    external_identifier VARCHAR(255) NOT NULL,
    verification_status VARCHAR(30) DEFAULT 'UNVERIFIED',
    verified_at         TIMESTAMPTZ,
    verification_method VARCHAR(50),
    metadata            JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ
);

CREATE INDEX idx_ext_links_owner ON external_identity_links(owner_type, owner_id) WHERE deleted = false;
CREATE UNIQUE INDEX idx_ext_links_system_id ON external_identity_links(external_system, external_identifier) WHERE deleted = false;

-- Addresses
CREATE TABLE addresses (
    id              UUID PRIMARY KEY,
    owner_type      VARCHAR(30) NOT NULL,
    owner_id        UUID NOT NULL,
    address_type    VARCHAR(30),
    street_address  TEXT,
    sangkat         VARCHAR(100),
    khan            VARCHAR(100),
    province        VARCHAR(100),
    postal_code     VARCHAR(20),
    country         VARCHAR(10) DEFAULT 'KH',
    is_primary      BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_addresses_owner ON addresses(owner_type, owner_id) WHERE deleted = false;

-- Contact Channels
CREATE TABLE contact_channels (
    id                      UUID PRIMARY KEY,
    owner_type              VARCHAR(30) NOT NULL,
    owner_id                UUID NOT NULL,
    channel_type            VARCHAR(20) NOT NULL,
    value                   VARCHAR(255) NOT NULL,
    is_primary              BOOLEAN DEFAULT FALSE,
    is_verified             BOOLEAN DEFAULT FALSE,
    verified_at             TIMESTAMPTZ,
    notification_enabled    BOOLEAN DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted                 BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMPTZ
);

CREATE INDEX idx_contact_channels_owner ON contact_channels(owner_type, owner_id) WHERE deleted = false;

-- Identity Documents
CREATE TABLE identity_documents (
    id                  UUID PRIMARY KEY,
    owner_type          VARCHAR(30) NOT NULL,
    owner_id            UUID NOT NULL,
    document_type       VARCHAR(50) NOT NULL,
    document_number     VARCHAR(100),
    issued_by           VARCHAR(255),
    issued_date         DATE,
    expiry_date         DATE,
    file_storage_path   VARCHAR(500),
    file_hash           VARCHAR(128),
    verification_status VARCHAR(30) DEFAULT 'UNVERIFIED',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ
);

CREATE INDEX idx_identity_docs_owner ON identity_documents(owner_type, owner_id) WHERE deleted = false;
