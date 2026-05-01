-- V1.20: Synchronize audit_events_event_type_check with the AuditEventType enum
-- Description: Several event types accumulated in the Java enum without being
--              added to the DB check constraint (API_REQUEST, API_REQUEST_ERROR,
--              CONFIG_SECURITY_POLICY_CHANGE, MFA_*, SECURITY_MALWARE_DETECTED).
--              Async audit writes for these were silently rolling back. This
--              migration drops and recreates the constraint with the full set.

ALTER TABLE audit_events DROP CONSTRAINT IF EXISTS audit_events_event_type_check;

ALTER TABLE audit_events ADD CONSTRAINT audit_events_event_type_check
CHECK (event_type IN (
    -- API request audit
    'API_REQUEST',
    'API_REQUEST_ERROR',
    -- Authentication
    'AUTH_LOGIN_FAILURE',
    'AUTH_LOGIN_SUCCESS',
    'AUTH_LOGOUT',
    'AUTH_ORG_SELECTION',
    'AUTH_REGISTER_FAILURE',
    'AUTH_REGISTER_SUCCESS',
    'AUTH_SERVICE_TOKEN_GENERATED',
    'AUTH_TOKEN_REFRESH',
    -- Authorization
    'AUTHZ_ACCESS_DENIED',
    'AUTHZ_ACCESS_GRANTED',
    'AUTHZ_PERMISSION_CHANGED',
    -- Configuration
    'CONFIG_LOGO_UPLOAD',
    'CONFIG_PASSWORD_CHANGE',
    'CONFIG_PROFILE_UPDATE',
    'CONFIG_SECURITY_POLICY_CHANGE',
    'CONFIG_SYSTEM_CHANGE',
    -- Data access
    'DATA_CONVERSION',
    'DATA_FILE_ACCESS',
    'DATA_FILE_DELETE',
    'DATA_FILE_MODIFY',
    'DATA_FILE_UPLOAD',
    'DATA_PROFILE_RESOLVE',
    'DATA_VALIDATION',
    -- Onboarding (added in V1.18)
    'EMAIL_SEND_FAILURE',
    'EMAIL_SEND_SUCCESS',
    'INVITATION_ACCEPTED',
    'INVITATION_CREATED',
    'INVITATION_EXPIRED',
    'INVITATION_REVOKED',
    'ORG_CREATED',
    -- MFA
    'MFA_BACKUP_CODE_USED',
    'MFA_BACKUP_CODES_REGENERATED',
    'MFA_DISABLED',
    'MFA_SETUP_COMPLETED',
    'MFA_SETUP_INITIATED',
    'MFA_VERIFICATION_FAILURE',
    'MFA_VERIFICATION_SUCCESS',
    -- Security
    'SECURITY_ACCOUNT_LOCKED',
    'SECURITY_ACCOUNT_UNLOCKED',
    'SECURITY_INVALID_FILE_UPLOAD',
    'SECURITY_IP_BLOCKED',
    'SECURITY_MALWARE_DETECTED',
    'SECURITY_PASSWORD_RESET_COMPLETE',
    'SECURITY_PASSWORD_RESET_REQUEST',
    'SECURITY_RATE_LIMIT_EXCEEDED',
    'SECURITY_SUSPICIOUS_ACTIVITY',
    -- System
    'SYSTEM_DATABASE_ERROR',
    'SYSTEM_ERROR',
    'SYSTEM_EXTERNAL_API_ERROR',
    'SYSTEM_SHUTDOWN',
    'SYSTEM_STARTUP'
));
