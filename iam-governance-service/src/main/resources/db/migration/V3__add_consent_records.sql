-- Consent records for LPDP compliance
CREATE TABLE consent_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    data_subject_type VARCHAR(30) NOT NULL CHECK (data_subject_type IN ('NATURAL_PERSON', 'LEGAL_ENTITY_CONTACT')),
    data_subject_id UUID NOT NULL,
    purpose VARCHAR(500) NOT NULL,
    legal_basis VARCHAR(30) NOT NULL CHECK (legal_basis IN ('CONSENT', 'CONTRACT', 'LEGAL_OBLIGATION', 'VITAL_INTEREST', 'PUBLIC_INTEREST', 'LEGITIMATE_INTEREST')),
    consent_given BOOLEAN NOT NULL DEFAULT false,
    consent_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    consent_method VARCHAR(20) NOT NULL DEFAULT 'ELECTRONIC' CHECK (consent_method IN ('ELECTRONIC', 'WRITTEN', 'VERBAL')),
    withdrawn_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    ip_address VARCHAR(45),
    data_categories JSONB DEFAULT '[]',
    third_party_sharing BOOLEAN NOT NULL DEFAULT false,
    cross_border_transfer BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_consent_subject ON consent_records(data_subject_id);
CREATE INDEX idx_consent_purpose ON consent_records(purpose);
CREATE INDEX idx_consent_given ON consent_records(consent_given);
