-- V1.16: Add missing columns to audit_events table
-- Date: 2026-02-18
-- Description: Adds request tracking and integrity columns that were missing from the schema

-- Add request_url column for tracking API endpoints
ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS request_url VARCHAR(2000);

-- Add http_method column for tracking HTTP methods (GET, POST, etc.)
ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS http_method VARCHAR(10);

-- Add integrity_hash column for tamper detection (SHA-256 hash)
ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS integrity_hash VARCHAR(64);

-- Add previous_hash column for blockchain-style audit chain
ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS previous_hash VARCHAR(64);

-- Create index on http_method for filtering by request type
CREATE INDEX IF NOT EXISTS idx_audit_event_http_method ON audit_events(http_method);

-- Create index on integrity_hash for verification queries
CREATE INDEX IF NOT EXISTS idx_audit_event_integrity_hash ON audit_events(integrity_hash);
