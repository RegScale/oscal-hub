# MFA and Security Policy Implementation Guide

**Date:** February 2026
**Status:** Complete

## Overview

This document describes the Multi-Factor Authentication (MFA) and Security Policy features added to OSCAL Hub. These features allow Super Admins to:

1. Require MFA globally for all users
2. Configure password policies (minimum length, rotation requirements)
3. Set audit log retention periods
4. Manually trigger audit log cleanup

## Features

### Multi-Factor Authentication (MFA)

MFA uses Time-based One-Time Passwords (TOTP) compatible with:
- Google Authenticator
- Microsoft Authenticator
- Authy
- Any RFC 6238 compliant authenticator app

#### MFA Flow

1. **Global MFA Enforcement**: When a Super Admin enables "Require MFA for all users" in Security Policy, existing users who haven't set up MFA will be forced to complete MFA setup on their next login.

2. **MFA Setup Flow**:
   - User attempts to login
   - If MFA is required but not set up, user is redirected to `/mfa-setup`
   - QR code is displayed for scanning with authenticator app
   - User enters the 6-digit code to verify
   - 10 backup codes are generated and displayed
   - User is logged in with full access

3. **MFA Verification Flow**:
   - User with MFA enabled attempts to login
   - After password verification, user is redirected to `/mfa-verify`
   - User enters 6-digit code from authenticator app
   - If code is valid, user is logged in

4. **Backup Codes**:
   - 10 one-time backup codes generated during MFA setup
   - Each code can only be used once
   - Users can regenerate backup codes (requires TOTP verification)
   - Warning displayed when backup codes are running low (3 or fewer remaining)

### Security Policy Settings

| Setting | Description | Default |
|---------|-------------|---------|
| MFA Required | Enforce MFA for all users | false |
| Password Min Length | Minimum password length (8-128) | 10 |
| Password Max Length | Maximum password length (32-128) | 128 |
| Password Rotation Days | Days until password expires (0 = disabled) | 0 |
| Audit Log Retention Days | Days to keep audit logs (1-3650) | 90 |

## API Endpoints

### Security Policy (Super Admin only)

```
GET  /api/admin/security-policy         - Get current security policy
PUT  /api/admin/security-policy         - Update security policy
POST /api/admin/security-policy/cleanup-logs - Trigger manual audit log cleanup
```

### MFA Endpoints

```
POST   /api/auth/mfa/setup/initiate       - Start MFA setup (returns QR code)
POST   /api/auth/mfa/setup/complete       - Complete MFA setup with TOTP code
POST   /api/auth/mfa/verify               - Verify TOTP code during login
POST   /api/auth/mfa/verify-backup        - Verify backup code during login
GET    /api/auth/mfa/status               - Get MFA status for current user
GET    /api/auth/mfa/backup-codes/count   - Get remaining backup codes count
POST   /api/auth/mfa/backup-codes/regenerate - Regenerate backup codes
DELETE /api/auth/mfa/disable              - Disable MFA (self)
DELETE /api/auth/mfa/admin/users/{id}/mfa - Disable MFA for user (Super Admin)
```

## Database Schema

### security_policy Table

```sql
CREATE TABLE security_policy (
    id BIGSERIAL PRIMARY KEY,
    mfa_required BOOLEAN NOT NULL DEFAULT false,
    password_min_length INTEGER NOT NULL DEFAULT 10,
    password_max_length INTEGER NOT NULL DEFAULT 128,
    password_rotation_days INTEGER NOT NULL DEFAULT 0,
    audit_log_retention_days INTEGER NOT NULL DEFAULT 90,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100)
);
```

### Users Table (MFA fields)

```sql
ALTER TABLE users
    ADD COLUMN mfa_enabled BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN mfa_secret VARCHAR(512),
    ADD COLUMN mfa_setup_completed BOOLEAN NOT NULL DEFAULT false;
```

### mfa_backup_codes Table

```sql
CREATE TABLE mfa_backup_codes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash VARCHAR(64) NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Security Considerations

### TOTP Secret Encryption

TOTP secrets are encrypted at rest using AES-256-GCM encryption. The encryption key is configured via the `ENCRYPTION_KEY` environment variable.

```properties
encryption.key=${ENCRYPTION_KEY:your-32-char-encryption-key-here}
```

**Important**: In production, always set a strong, random 32-character encryption key.

### Backup Code Storage

Backup codes are stored as SHA-256 hashes, never in plaintext. When a user verifies a backup code:
1. The input is hashed
2. The hash is compared against stored hashes
3. If matched, the code is marked as used

### JWT Token Types

Three types of JWT tokens are used in the MFA flow:

| Token Type | Purpose | Expiration |
|------------|---------|------------|
| Full Token | Normal authenticated access | 24 hours |
| MFA Setup Token | During MFA setup flow | 10 minutes |
| MFA Partial Token | After password, before MFA verification | 5 minutes |

### Rate Limiting

MFA verification endpoints should be rate-limited to prevent brute-force attacks. Consider implementing:
- 5 failed MFA attempts per minute
- Account lockout after multiple failures
- IP-based rate limiting

## Frontend Pages

### Security Policy Admin Page

Located at `/admin/security-policy`

Features:
- MFA requirement toggle
- Password length sliders
- Password rotation days input
- Audit log retention days input
- Manual cleanup trigger button

### MFA Setup Page

Located at `/mfa-setup`

Features:
- QR code display
- Manual entry code (for accessibility)
- 6-digit code input
- Backup codes display after successful setup
- Copy and download backup codes

### MFA Verify Page

Located at `/mfa-verify`

Features:
- 6-digit code input with auto-advance
- Backup code fallback option
- Warning for low backup codes remaining

## Audit Events

New audit event types for MFA:

| Event Type | Description | Risk Level |
|------------|-------------|------------|
| MFA_SETUP_INITIATED | MFA setup started | LOW |
| MFA_SETUP_COMPLETED | MFA setup completed | MEDIUM |
| MFA_VERIFICATION_SUCCESS | TOTP verification successful | LOW |
| MFA_VERIFICATION_FAILURE | TOTP verification failed | MEDIUM |
| MFA_BACKUP_CODE_USED | Backup code used for login | MEDIUM |
| MFA_BACKUP_CODES_REGENERATED | Backup codes regenerated | MEDIUM |
| MFA_DISABLED | MFA disabled for account | HIGH |

## Testing

### Backend Tests

- `MfaServiceTest.java` - Tests for MFA service operations
- `SecurityPolicyServiceTest.java` - Tests for security policy management

### Manual Testing Checklist

1. **Enable MFA Requirement**
   - Log in as Super Admin
   - Go to Admin > Security Policy
   - Enable "Require MFA for all users"
   - Save changes

2. **Verify MFA Setup Flow**
   - Log out
   - Log in as a user without MFA
   - Should redirect to MFA setup page
   - Scan QR code with authenticator app
   - Enter verification code
   - Should see backup codes
   - Copy/download backup codes
   - Should be logged in

3. **Verify MFA Login Flow**
   - Log out
   - Log in with same user
   - Should redirect to MFA verify page
   - Enter code from authenticator
   - Should be logged in

4. **Test Backup Code**
   - Log out
   - Log in, at MFA verify page
   - Click "Use a backup code"
   - Enter a backup code
   - Should be logged in
   - Should see warning if backup codes are low

5. **Disable MFA**
   - Go to profile settings
   - Click "Disable MFA"
   - Enter current TOTP code
   - MFA should be disabled

## Configuration

### Environment Variables

```bash
# Encryption key for TOTP secrets (required in production)
ENCRYPTION_KEY=your-32-character-secure-key-here

# JWT secret (required)
JWT_SECRET=your-jwt-secret-here

# Optional: Audit log cleanup schedule (default: 2 AM daily)
# Format: cron expression
AUDIT_CLEANUP_SCHEDULE=0 0 2 * * ?
```

### application.properties

```properties
# Encryption key for MFA secrets
encryption.key=${ENCRYPTION_KEY:dev-encryption-key-32-chars!!}

# Audit log cleanup schedule (daily at 2 AM)
audit.cleanup.schedule=0 0 2 * * ?
```

## Dependencies

Added Maven dependency for TOTP support:

```xml
<dependency>
    <groupId>dev.samstevens.totp</groupId>
    <artifactId>totp</artifactId>
    <version>1.7.1</version>
</dependency>
```

## Troubleshooting

### Common Issues

1. **QR Code Not Scanning**
   - Ensure the authenticator app supports TOTP
   - Try using the manual entry code instead
   - Check that the device camera has permission

2. **Invalid TOTP Code**
   - Ensure device time is accurate (TOTP is time-sensitive)
   - Wait for the next code if current one is about to expire
   - Check that you're using the correct account in your authenticator

3. **Backup Code Not Working**
   - Ensure the code hasn't already been used
   - Enter the code exactly as shown (case-insensitive)
   - Check that MFA is still enabled for the account

4. **Encryption Key Issues**
   - Ensure the encryption key is exactly 32 characters for AES-256
   - The key can be provided as hex (64 chars) or base64 (44 chars) encoded
