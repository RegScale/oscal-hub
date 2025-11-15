-- V1.7: Add Multi-Tenant Organization System
-- Date: 2025-11-15
-- Description: Adds organization entities, user memberships, and access request management
--              Migrates existing users to the default organization
--              Seeds default super admin account (username: admin, password: Admin@12345)

-- ============================================================================
-- 1. Create Organizations Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    logo_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

COMMENT ON TABLE organizations IS 'Organizations in the multi-tenant system';
COMMENT ON COLUMN organizations.logo_url IS 'Path or URL to the organization logo (NASCAR page display)';

-- ============================================================================
-- 2. Add new columns to users table
-- ============================================================================
-- Add global_role column (platform-level role: SUPER_ADMIN or USER)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS global_role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Add must_change_password column (force password change on first login)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN users.global_role IS 'Platform-level role: SUPER_ADMIN (full system access) or USER (org-scoped access)';
COMMENT ON COLUMN users.must_change_password IS 'Forces user to change password on next login (true for temp passwords)';

-- ============================================================================
-- 3. Create Organization Memberships Table (Junction with role/status)
-- ============================================================================
CREATE TABLE IF NOT EXISTS organization_memberships (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_membership_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_org UNIQUE (user_id, organization_id)
);

CREATE INDEX IF NOT EXISTS idx_membership_user ON organization_memberships(user_id);
CREATE INDEX IF NOT EXISTS idx_membership_org ON organization_memberships(organization_id);
CREATE INDEX IF NOT EXISTS idx_membership_status ON organization_memberships(status);

COMMENT ON TABLE organization_memberships IS 'User memberships in organizations with role and status';
COMMENT ON COLUMN organization_memberships.role IS 'Organization role: ORG_ADMIN (manage users/requests) or USER (standard access)';
COMMENT ON COLUMN organization_memberships.status IS 'Membership status: ACTIVE, LOCKED, or DEACTIVATED';

-- ============================================================================
-- 4. Create User Access Requests Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_access_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    organization_id BIGINT NOT NULL,
    email VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    username VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    request_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by BIGINT,
    reviewed_date TIMESTAMP,
    notes TEXT,
    message TEXT,
    CONSTRAINT fk_request_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_request_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_request_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_request_org ON user_access_requests(organization_id);
CREATE INDEX IF NOT EXISTS idx_request_status ON user_access_requests(status);
CREATE INDEX IF NOT EXISTS idx_request_email ON user_access_requests(email);

COMMENT ON TABLE user_access_requests IS 'Requests for access to organizations (new or existing users)';
COMMENT ON COLUMN user_access_requests.user_id IS 'Null if requester does not have an account yet';
COMMENT ON COLUMN user_access_requests.status IS 'Request status: PENDING, APPROVED, or REJECTED';

-- ============================================================================
-- 5. Seed Default Organization
-- ============================================================================
INSERT INTO organizations (id, name, description, active, created_at)
VALUES (1, 'Default Organization', 'Default organization for existing users and super admin', true, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- ============================================================================
-- 6. Seed Super Admin Account
-- ============================================================================
-- Password: Admin@12345 (BCrypt hash for 10 rounds)
-- User MUST change this password on first login (must_change_password = true)
INSERT INTO users (id, username, password, email, global_role, must_change_password, enabled, created_at)
VALUES (
    1,
    'admin',
    '$2a$10$LQd5LH/PnC3yV5qC8K8aGuGqGz7pxw5FKjGJqZH/TfY6yZ8qV8F7G',  -- Admin@12345
    'admin@oscal-tools.local',
    'SUPER_ADMIN',
    true,  -- Force password change on first login
    true,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO NOTHING;

-- ============================================================================
-- 7. Link Super Admin to Default Organization (as ORG_ADMIN)
-- ============================================================================
INSERT INTO organization_memberships (user_id, organization_id, role, status, joined_at)
VALUES (1, 1, 'ORG_ADMIN', 'ACTIVE', CURRENT_TIMESTAMP)
ON CONFLICT (user_id, organization_id) DO NOTHING;

-- ============================================================================
-- 8. Migrate Existing Users to Default Organization
-- ============================================================================
-- Add all existing users (except super admin) to the default organization as standard users
INSERT INTO organization_memberships (user_id, organization_id, role, status, joined_at)
SELECT
    u.id,
    1,  -- Default organization ID
    'USER',
    'ACTIVE',
    CURRENT_TIMESTAMP
FROM users u
WHERE u.id > 1  -- Skip super admin (already added)
  AND u.global_role = 'USER'  -- Only migrate regular users
  AND NOT EXISTS (
      SELECT 1 FROM organization_memberships om
      WHERE om.user_id = u.id AND om.organization_id = 1
  );

-- ============================================================================
-- 9. Reset sequence for auto-increment (if needed)
-- ============================================================================
SELECT setval('organizations_id_seq', (SELECT COALESCE(MAX(id), 1) FROM organizations), true);
SELECT setval('organization_memberships_id_seq', (SELECT COALESCE(MAX(id), 1) FROM organization_memberships), true);
SELECT setval('user_access_requests_id_seq', (SELECT COALESCE(MAX(id), 1) FROM user_access_requests), true);
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users), true);
