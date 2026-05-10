-- ============================================================================
-- V1.12: Add Security Policy and MFA Support
-- ============================================================================
-- This migration adds:
-- 1. Security Policy table for global security settings (singleton)
-- 2. MFA fields to users table
-- 3. MFA Backup Codes table for account recovery
-- ============================================================================

-- ============================================================================
-- 1. Security Policy Table (Singleton - always ID=1)
-- ============================================================================
CREATE TABLE IF NOT EXISTS security_policy (
    id BIGSERIAL PRIMARY KEY,

    -- MFA Policy
    mfa_required BOOLEAN NOT NULL DEFAULT false,

    -- Password Policy
    password_min_length INTEGER NOT NULL DEFAULT 10,
    password_max_length INTEGER NOT NULL DEFAULT 128,
    password_rotation_days INTEGER NOT NULL DEFAULT 0,  -- 0 = never expires

    -- Audit Log Retention
    audit_log_retention_days INTEGER NOT NULL DEFAULT 90,

    -- Metadata
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100)
);

COMMENT ON TABLE security_policy IS 'Global security policy settings (singleton row with ID=1)';
COMMENT ON COLUMN security_policy.mfa_required IS 'When true, all users must configure MFA before accessing the system';
COMMENT ON COLUMN security_policy.password_min_length IS 'Minimum password length (8-128 characters)';
COMMENT ON COLUMN security_policy.password_max_length IS 'Maximum password length (8-128 characters)';
COMMENT ON COLUMN security_policy.password_rotation_days IS 'Days before password expires (0 = never expires)';
COMMENT ON COLUMN security_policy.audit_log_retention_days IS 'Days to retain audit logs before automatic deletion';

-- ============================================================================
-- 2. MFA Fields on Users Table
-- ============================================================================
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS mfa_secret VARCHAR(512),  -- Encrypted TOTP secret
    ADD COLUMN IF NOT EXISTS mfa_setup_completed BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN users.mfa_enabled IS 'Whether MFA is enabled for this user';
COMMENT ON COLUMN users.mfa_secret IS 'Encrypted TOTP secret (AES-256-GCM)';
COMMENT ON COLUMN users.mfa_setup_completed IS 'Whether user has completed MFA setup';

-- ============================================================================
-- 3. MFA Backup Codes Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS mfa_backup_codes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code_hash VARCHAR(64) NOT NULL,  -- SHA-256 hash of backup code
    used BOOLEAN NOT NULL DEFAULT false,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_backup_code_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for efficient lookup
CREATE INDEX IF NOT EXISTS idx_backup_codes_user_id ON mfa_backup_codes(user_id);
CREATE INDEX IF NOT EXISTS idx_backup_codes_hash ON mfa_backup_codes(code_hash);
CREATE INDEX IF NOT EXISTS idx_backup_codes_user_unused ON mfa_backup_codes(user_id, used) WHERE used = false;

COMMENT ON TABLE mfa_backup_codes IS 'One-time backup codes for MFA recovery (10 codes per user)';
COMMENT ON COLUMN mfa_backup_codes.code_hash IS 'SHA-256 hash of the backup code (never store plaintext)';
COMMENT ON COLUMN mfa_backup_codes.used IS 'Whether this backup code has been consumed';
COMMENT ON COLUMN mfa_backup_codes.used_at IS 'Timestamp when the backup code was used';

-- ============================================================================
-- 4. Seed Default Security Policy (Singleton Row)
-- ============================================================================
INSERT INTO security_policy (
    id,
    mfa_required,
    password_min_length,
    password_max_length,
    password_rotation_days,
    audit_log_retention_days,
    updated_at
)
VALUES (
    1,      -- Always ID=1 (singleton)
    false,  -- MFA not required by default
    10,     -- 10 character minimum (OWASP recommendation)
    128,    -- 128 character maximum
    0,      -- Passwords never expire by default
    90,     -- 90 day audit log retention
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 5. Add MFA-related Audit Event Types (if audit_events table exists)
-- ============================================================================
-- Note: Audit event types are defined in Java enum AuditEventType
-- The following events will be logged:
--   - MFA_SETUP_INITIATED: User started MFA setup
--   - MFA_SETUP_COMPLETED: User completed MFA setup
--   - MFA_VERIFICATION_SUCCESS: User successfully verified MFA code
--   - MFA_VERIFICATION_FAILURE: User failed MFA verification
--   - MFA_BACKUP_CODE_USED: User used a backup code
--   - MFA_DISABLED: MFA was disabled for a user
--   - SECURITY_POLICY_UPDATED: Security policy was modified
