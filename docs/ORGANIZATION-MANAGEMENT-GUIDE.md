# Organization Management Feature Guide

**Date:** November 15, 2025
**Status:** In Development
**Version:** 1.0.0

---

## Overview

The OSCAL Tools application now supports **multi-tenant organization management**, allowing multiple organizations to use the system with isolated data and hierarchical access control.

### Key Features

- **Multi-Organization Support** - Users can belong to multiple organizations
- **Role-Based Access Control** - Three-tier role system (Super Admin, Org Admin, User)
- **NASCAR Organization Selector** - Visual grid of organization logos for easy selection
- **Organization Switching** - Switch between organizations without re-login
- **Access Request Workflow** - Users request access, admins approve/reject
- **User Management** - Org admins can lock, deactivate, and reset passwords

---

## Architecture

### Role Hierarchy

```
SUPER_ADMIN (Platform-level)
    └── Full system access
    └── Create/edit/deactivate organizations
    └── Assign organization administrators
    └── Access all organizations

ORG_ADMIN (Organization-level)
    └── Manage users in their organization
    └── Approve/reject access requests
    └── Lock, deactivate, reset passwords
    └── View organization data

USER (Organization-level)
    └── Access OSCAL tools within their organization
    └── Create and edit documents
    └── Request access to other organizations
```

### Database Schema

```sql
organizations
    ├── id (PK)
    ├── name (UNIQUE)
    ├── description
    ├── logo_url (for NASCAR page)
    ├── active
    └── created_at

users
    ├── id (PK)
    ├── username
    ├── password (BCrypt)
    ├── email
    ├── global_role (SUPER_ADMIN | USER)
    ├── must_change_password (boolean)
    └── ... (other fields)

organization_memberships
    ├── id (PK)
    ├── user_id (FK)
    ├── organization_id (FK)
    ├── role (ORG_ADMIN | USER)
    ├── status (ACTIVE | LOCKED | DEACTIVATED)
    └── joined_at

user_access_requests
    ├── id (PK)
    ├── user_id (FK, nullable)
    ├── organization_id (FK)
    ├── email, first_name, last_name, username
    ├── status (PENDING | APPROVED | REJECTED)
    ├── request_date
    └── reviewed_by (FK)
```

---

## Default Super Admin Account

**IMPORTANT: Change this password immediately after first login!**

### Credentials

```
Username: admin
Password: Admin@12345
Email: admin@oscal-tools.local
Role: SUPER_ADMIN
```

### First Login Steps

1. Navigate to `http://localhost:3000/login`
2. Enter username: `admin`
3. Enter password: `Admin@12345`
4. You will be **forced to change the password** before proceeding
5. Select "Default Organization" from the NASCAR page
6. Access the super admin dashboard at `/admin`

### Security Requirements

The system enforces strong password requirements:
- Minimum 10 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)

**Example strong password:** `SuperSecure@2025!`

---

## User Workflows

### Workflow 1: Super Admin Creates Organization

1. Login as super admin (`admin`)
2. Navigate to `/admin` (Super Admin Dashboard)
3. Click "Create Organization"
4. Enter organization details:
   - Name (required, unique)
   - Description
   - Upload logo (PNG/JPG/SVG, max 2MB, recommended 200x200px)
5. Click "Save"
6. Assign an organization administrator:
   - Select existing user OR create new user
   - Set role to "ORG_ADMIN"
7. Organization is now active and appears on NASCAR page

### Workflow 2: User Requests Access to Organization

1. Navigate to login page
2. Click "Request Access to Organization"
3. Fill out access request form:
   - Select organization from dropdown
   - Enter email, first name, last name
   - Choose username
   - Provide message explaining why access is needed
4. Submit request
5. Request goes to organization administrators for approval

### Workflow 3: Org Admin Approves Access Request

1. Login as org admin
2. Navigate to `/org-admin` (Organization Admin Dashboard)
3. View "Pending Access Requests" tab
4. Review request details
5. Click "Approve" or "Reject"
   - If approved: User account is created or access granted
   - If rejected: Request is marked rejected with optional notes

### Workflow 4: User Switches Organizations

1. While logged in, click user menu (top-right)
2. Click "Switch Organization"
3. Select organization from dropdown OR
4. Click organization logo from NASCAR selector
5. JWT is re-issued with new organization context
6. Dashboard refreshes with selected organization's data

### Workflow 5: Org Admin Manages Users

1. Login as org admin
2. Navigate to `/org-admin`
3. View "User Management" tab
4. Available actions for each user:
   - **Lock Account** - Temporarily prevent login
   - **Deactivate Account** - Permanently disable within organization
   - **Reset Password** - Generate temporary password, force change

---

## API Endpoints

### Organization Management (Super Admin Only)

```http
GET    /api/admin/organizations              # List all organizations
POST   /api/admin/organizations              # Create organization
PUT    /api/admin/organizations/{id}         # Update organization
DELETE /api/admin/organizations/{id}         # Deactivate organization
POST   /api/admin/organizations/{id}/logo    # Upload logo
DELETE /api/admin/organizations/{id}/logo    # Remove logo
POST   /api/admin/organizations/{id}/admins  # Assign org admin
```

### Organization Admin (Org Admin Only)

```http
GET    /api/org-admin/access-requests                  # List pending requests
POST   /api/org-admin/access-requests/{id}/approve     # Approve request
POST   /api/org-admin/access-requests/{id}/reject      # Reject request
GET    /api/org-admin/users                            # List org users
PUT    /api/org-admin/users/{id}/lock                  # Lock user account
PUT    /api/org-admin/users/{id}/deactivate            # Deactivate user
POST   /api/org-admin/users/{id}/reset-password        # Reset password
```

### Authentication & Organization Selection

```http
POST   /api/auth/login                       # Step 1: Authenticate
GET    /api/auth/my-organizations            # Step 2: Get user's orgs
POST   /api/auth/select-organization/{id}    # Step 3: Select org, get JWT
POST   /api/auth/switch-organization/{id}    # Switch org (re-issue JWT)
POST   /api/auth/change-password             # Force password change
```

### Public Endpoints (No Auth Required)

```http
GET    /api/organizations                    # List active organizations
POST   /api/auth/request-access              # Request org access
GET    /api/files/org-logos/{filename}       # Serve organization logos
```

---

## JWT Token Structure

### Before Organization Selection

```json
{
  "sub": "username",
  "userId": 123,
  "globalRole": "USER",
  "mustChangePassword": false,
  "exp": 1234567890
}
```

### After Organization Selection

```json
{
  "sub": "username",
  "userId": 123,
  "globalRole": "USER",
  "organizationId": 5,
  "organizationName": "Acme Corp",
  "orgRole": "ORG_ADMIN",
  "mustChangePassword": false,
  "exp": 1234567890
}
```

---

## NASCAR Page Design

The organization selector displays a grid of clickable organization cards:

```
┌─────────────────────────────────────────────────────────┐
│           Select Your Organization                      │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │  [LOGO]  │  │  [LOGO]  │  │  [LOGO]  │             │
│  │          │  │          │  │          │             │
│  │ Acme Corp│  │  TechCo  │  │ GovOrg   │             │
│  │ ORG_ADMIN│  │   USER   │  │   USER   │             │
│  └──────────┘  └──────────┘  └──────────┘             │
│                                                          │
│  ┌──────────┐  ┌──────────┐                            │
│  │  [LOGO]  │  │  [LOGO]  │                            │
│  │          │  │          │                            │
│  │ Defense  │  │  Cloud9  │                            │
│  │ ORG_ADMIN│  │   USER   │                            │
│  └──────────┘  └──────────┘                            │
│                                                          │
│  [ Request Access to Another Organization ]            │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

- Responsive grid (1-4 columns based on screen size)
- Hover effects and visual feedback
- Shows user's role badge on each card
- Only displays organizations where user has ACTIVE membership

---

## Logo Requirements

### File Specifications

- **Formats:** PNG, JPG, JPEG, SVG
- **Maximum Size:** 2MB
- **Recommended Dimensions:** 200x200 pixels (square)
- **Aspect Ratio:** 1:1 preferred (will be displayed in squares)

### Storage

- Logos stored in: `back-end/uploads/org-logos/`
- Served via: `GET /api/files/org-logos/{filename}`
- Filename format: `org-{id}-{timestamp}.{ext}`

---

## Security Considerations

### Password Security

- BCrypt hashing with 10 rounds
- Strong password complexity requirements enforced
- Force password change on first login for temp passwords
- Account lockout after 5 failed attempts (15 minutes)

### Authorization

- All endpoints protected with JWT authentication
- Role-based authorization via `@PreAuthorize` annotations
- Organization-scoped data isolation (users only see their org's data)
- Super admin bypass for cross-org operations

### Audit Trail

- All admin actions logged (create/edit org, approve requests, user management)
- Login attempts tracked with IP addresses
- Failed login attempts logged for security monitoring

---

## Migration Guide

### Migrating Existing Users

The Flyway migration `V1.7__add_multi_tenant_organization_system.sql` automatically:

1. Creates "Default Organization"
2. Adds all existing users to "Default Organization" as USER role
3. Creates super admin account
4. Links super admin to Default Organization as ORG_ADMIN

**No manual data migration required!**

---

## Troubleshooting

### Issue: Cannot Login with Admin Account

**Solution:**
1. Verify database migration ran successfully: `SELECT * FROM users WHERE username = 'admin';`
2. Password is case-sensitive: `Admin@12345` (capital A)
3. Clear browser cache and try again
4. Check backend logs for authentication errors

### Issue: Organization Logo Not Displaying

**Solution:**
1. Verify logo file exists in `back-end/uploads/org-logos/`
2. Check file permissions (readable by backend process)
3. Verify logo URL in database: `SELECT logo_url FROM organizations WHERE id = X;`
4. Test logo endpoint directly: `http://localhost:8080/api/files/org-logos/{filename}`

### Issue: User Cannot See Their Organization

**Solution:**
1. Verify organization membership exists: `SELECT * FROM organization_memberships WHERE user_id = X;`
2. Check membership status is ACTIVE: `UPDATE organization_memberships SET status = 'ACTIVE' WHERE id = X;`
3. Check organization is active: `UPDATE organizations SET active = true WHERE id = X;`

---

## Future Enhancements

- Email notifications for access request approvals/rejections
- Organization-specific branding (colors, themes)
- Multi-factor authentication (MFA) for super admin
- Organization-level settings and customization
- Bulk user import/export
- Organization usage metrics and analytics
- SSO/SAML integration for enterprise organizations

---

## Support

For issues or questions about the organization management feature:

1. Check this guide first
2. Review backend logs: `back-end/logs/spring.log`
3. Check database state with SQL queries
4. File an issue: https://github.com/RegScale/oscal-hub/issues

---

**Generated with Claude Code** - November 15, 2025
