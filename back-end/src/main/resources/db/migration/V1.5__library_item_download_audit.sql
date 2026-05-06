-- V1.5__library_item_download_audit.sql
-- Adds LIBRARY_ITEM_DOWNLOAD to the audit_events.event_type CHECK constraint.
-- Without this, attempts to insert download audit events fail the CHECK and
-- throw a JDBC error mid-download.

ALTER TABLE audit_events
    DROP CONSTRAINT IF EXISTS audit_events_event_type_check;

ALTER TABLE audit_events
    ADD CONSTRAINT audit_events_event_type_check
    CHECK (event_type IN (
        -- Authentication
        'AUTH_REGISTER_SUCCESS',
        'AUTH_REGISTER_FAILURE',
        'AUTH_LOGIN_SUCCESS',
        'AUTH_LOGIN_FAILURE',
        'AUTH_LOGOUT',
        'AUTH_TOKEN_REFRESH',
        'AUTH_SERVICE_TOKEN_GENERATED',
        'AUTH_ORG_SELECTION',
        -- Authorization
        'AUTHZ_ACCESS_DENIED',
        'AUTHZ_ACCESS_GRANTED',
        'AUTHZ_PERMISSION_CHANGED',
        -- Data Access
        'DATA_FILE_UPLOAD',
        'DATA_FILE_ACCESS',
        'DATA_FILE_DELETE',
        'DATA_FILE_MODIFY',
        'DATA_VALIDATION',
        'DATA_CONVERSION',
        'DATA_PROFILE_RESOLVE',
        -- Configuration
        'CONFIG_PROFILE_UPDATE',
        'CONFIG_PASSWORD_CHANGE',
        'CONFIG_LOGO_UPLOAD',
        'CONFIG_SYSTEM_CHANGE',
        'CONFIG_SECURITY_POLICY_CHANGE',
        -- Security
        'SECURITY_ACCOUNT_LOCKED',
        'SECURITY_ACCOUNT_UNLOCKED',
        'SECURITY_IP_BLOCKED',
        'SECURITY_PASSWORD_RESET_REQUEST',
        'SECURITY_PASSWORD_RESET_COMPLETE',
        'SECURITY_SUSPICIOUS_ACTIVITY',
        'SECURITY_RATE_LIMIT_EXCEEDED',
        'SECURITY_INVALID_FILE_UPLOAD',
        'SECURITY_MALWARE_DETECTED',
        -- MFA
        'MFA_SETUP_INITIATED',
        'MFA_SETUP_COMPLETED',
        'MFA_VERIFICATION_SUCCESS',
        'MFA_VERIFICATION_FAILURE',
        'MFA_BACKUP_CODE_USED',
        'MFA_BACKUP_CODES_REGENERATED',
        'MFA_DISABLED',
        -- System
        'SYSTEM_STARTUP',
        'SYSTEM_SHUTDOWN',
        'SYSTEM_ERROR',
        'SYSTEM_DATABASE_ERROR',
        'SYSTEM_EXTERNAL_API_ERROR',
        -- API
        'API_REQUEST',
        'API_REQUEST_ERROR',
        -- Email
        'EMAIL_SEND_SUCCESS',
        'EMAIL_SEND_FAILURE',
        -- Invitation
        'INVITATION_CREATED',
        'INVITATION_ACCEPTED',
        'INVITATION_REVOKED',
        'INVITATION_EXPIRED',
        -- Organization
        'ORG_CREATED',
        -- Library
        'LIBRARY_ITEM_PUBLISHED',
        'LIBRARY_ITEM_UNPUBLISHED',
        'LIBRARY_ITEM_FORCE_UNPUBLISHED',
        'LIBRARY_ITEM_VISIBILITY_CHANGED',
        'LIBRARY_ITEM_DOWNLOAD'
    ));
