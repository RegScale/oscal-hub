-- V1.7 — Per-authorization role-based ACL grants.
-- See docs/superpowers/specs/2026-05-06-conmon-and-documents-design.md.
-- Adds an authorization_grants table for explicit (user, role) entries
-- on a specific authorization, and a share_with_org_default_role column
-- on authorizations for the "share with whole org as VIEWER/CONTRIBUTOR/EDITOR"
-- convenience case (no fan-out — resolved at access-check time).

CREATE TABLE IF NOT EXISTS authorization_grants (
    id               BIGSERIAL PRIMARY KEY,
    authorization_id BIGINT NOT NULL REFERENCES authorizations(id) ON DELETE CASCADE,
    user_id          BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role             VARCHAR(32) NOT NULL,
    granted_by       BIGINT NOT NULL REFERENCES users(id),
    granted_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_authorization_grants_user UNIQUE (authorization_id, user_id),
    CONSTRAINT ck_authorization_grants_role CHECK (role IN ('OWNER', 'EDITOR', 'CONTRIBUTOR', 'VIEWER'))
);

CREATE INDEX IF NOT EXISTS idx_authorization_grants_user
    ON authorization_grants (user_id);

CREATE INDEX IF NOT EXISTS idx_authorization_grants_auth
    ON authorization_grants (authorization_id);

ALTER TABLE authorizations
    ADD COLUMN IF NOT EXISTS share_with_org_default_role VARCHAR(32) NULL;

ALTER TABLE authorizations
    DROP CONSTRAINT IF EXISTS ck_authorizations_share_role;
ALTER TABLE authorizations
    ADD CONSTRAINT ck_authorizations_share_role
        CHECK (share_with_org_default_role IS NULL OR share_with_org_default_role IN ('VIEWER', 'CONTRIBUTOR', 'EDITOR'));
