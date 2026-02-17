-- V1.14: Add Electronic Signature Image Column
-- Date: 2026-02-17
-- Description: Adds electronic signature image support for non-CAC/PIV signatures

-- Add electronic_signature_image column to authorizations table
-- This stores a base64-encoded PNG image of hand-drawn signatures
ALTER TABLE authorizations
    ADD COLUMN IF NOT EXISTS electronic_signature_image TEXT;
