-- V1.5: Add Digital Signature Fields to Authorization Table
-- Date: 2025-10-25
-- Description: Adds CAC/PIV digital signature support to authorizations

-- Add digital signature fields to authorizations table (idempotent)
ALTER TABLE authorizations
    ADD COLUMN IF NOT EXISTS digital_signature_method VARCHAR(50),
    ADD COLUMN IF NOT EXISTS signer_certificate TEXT,
    ADD COLUMN IF NOT EXISTS signer_common_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS signer_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS signer_edipi VARCHAR(10),
    ADD COLUMN IF NOT EXISTS certificate_issuer VARCHAR(500),
    ADD COLUMN IF NOT EXISTS certificate_serial VARCHAR(100),
    ADD COLUMN IF NOT EXISTS certificate_not_before TIMESTAMP,
    ADD COLUMN IF NOT EXISTS certificate_not_after TIMESTAMP,
    ADD COLUMN IF NOT EXISTS signature_timestamp TIMESTAMP,
    ADD COLUMN IF NOT EXISTS document_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS certificate_verified BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS certificate_verification_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS certificate_verification_notes TEXT;

-- Add index on signature timestamp for performance (idempotent)
CREATE INDEX IF NOT EXISTS idx_authorizations_signature_timestamp ON authorizations(signature_timestamp);

-- Add index on certificate verification for audit queries (idempotent)
CREATE INDEX IF NOT EXISTS idx_authorizations_certificate_verified ON authorizations(certificate_verified);

-- Add index on signer EDIPI for searching by user (idempotent)
CREATE INDEX IF NOT EXISTS idx_authorizations_signer_edipi ON authorizations(signer_edipi);
