-- V1.5: Add Digital Signature Fields to Authorization Table
-- Date: 2025-10-25
-- Description: Adds CAC/PIV digital signature support to authorizations

-- Add digital signature fields to authorizations table
ALTER TABLE authorizations
    ADD COLUMN digital_signature_method VARCHAR(50),
    ADD COLUMN signer_certificate TEXT,
    ADD COLUMN signer_common_name VARCHAR(255),
    ADD COLUMN signer_email VARCHAR(255),
    ADD COLUMN signer_edipi VARCHAR(10),
    ADD COLUMN certificate_issuer VARCHAR(500),
    ADD COLUMN certificate_serial VARCHAR(100),
    ADD COLUMN certificate_not_before TIMESTAMP,
    ADD COLUMN certificate_not_after TIMESTAMP,
    ADD COLUMN signature_timestamp TIMESTAMP,
    ADD COLUMN document_hash VARCHAR(64),
    ADD COLUMN certificate_verified BOOLEAN DEFAULT FALSE,
    ADD COLUMN certificate_verification_date TIMESTAMP,
    ADD COLUMN certificate_verification_notes TEXT;

-- Add index on signature timestamp for performance
CREATE INDEX idx_authorizations_signature_timestamp ON authorizations(signature_timestamp);

-- Add index on certificate verification for audit queries
CREATE INDEX idx_authorizations_certificate_verified ON authorizations(certificate_verified);

-- Add index on signer EDIPI for searching by user
CREATE INDEX idx_authorizations_signer_edipi ON authorizations(signer_edipi);
