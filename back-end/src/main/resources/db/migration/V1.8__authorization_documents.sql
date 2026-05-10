-- V1.8 — Documents tab on authorizations.
-- See docs/superpowers/specs/2026-05-06-conmon-and-documents-design.md.
-- Adds authorization_documents to store metadata about supporting artifacts
-- (vuln scans, pen tests, audit reports, SSP, SAR, etc.) uploaded by users
-- with grant-level access. The actual file bytes live in cloud/local storage
-- via FileStorageService; this table holds the metadata pointer + structured
-- fields used by the package-completeness checklist.

CREATE TABLE IF NOT EXISTS authorization_documents (
    id                 BIGSERIAL PRIMARY KEY,
    authorization_id   BIGINT NOT NULL REFERENCES authorizations(id) ON DELETE CASCADE,
    uploaded_by        BIGINT NOT NULL REFERENCES users(id),
    uploaded_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    original_filename  VARCHAR(512) NOT NULL,
    file_size          BIGINT NOT NULL,
    content_type       VARCHAR(128) NOT NULL,
    storage_path       VARCHAR(1024) NOT NULL,
    document_type      VARCHAR(64) NOT NULL,
    description        TEXT,
    tags               VARCHAR(512),
    version            VARCHAR(64),
    effective_date     DATE,
    expires_at         DATE,
    CONSTRAINT ck_authorization_documents_type CHECK (document_type IN (
        'VULNERABILITY_SCAN',
        'PENETRATION_TEST',
        'ASSET_INVENTORY',
        'SSP',
        'SAR',
        'CONFIGURATION_BASELINE',
        'CONTINGENCY_PLAN',
        'INCIDENT_RESPONSE_PLAN',
        'AUDIT_REPORT',
        'AUTHORIZATION_LETTER',
        'CHANGE_NOTICE_TICKET',
        'RISK_ASSESSMENT',
        'BUSINESS_CONTINUITY_PLAN',
        'DISASTER_RECOVERY_PLAN',
        'BUSINESS_IMPACT_ASSESSMENT',
        'OTHER'
    ))
);

CREATE INDEX IF NOT EXISTS idx_authorization_documents_auth
    ON authorization_documents (authorization_id, uploaded_at DESC);

CREATE INDEX IF NOT EXISTS idx_authorization_documents_type
    ON authorization_documents (authorization_id, document_type);
