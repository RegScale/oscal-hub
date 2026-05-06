-- V1.6 — Add organization_id FK to authorizations and authorization_templates.
-- Backfills from the creator's lowest-id ACTIVE organization_membership.
-- Fails noisily if any row cannot be backfilled (creator has no active membership).
-- See docs/superpowers/specs/2026-05-06-conmon-and-documents-design.md.

-- 1. Add nullable column on both tables.
ALTER TABLE authorizations
    ADD COLUMN IF NOT EXISTS organization_id BIGINT;

ALTER TABLE authorization_templates
    ADD COLUMN IF NOT EXISTS organization_id BIGINT;

-- 2. Backfill: pick the creator's lowest-id ACTIVE membership.
UPDATE authorizations a
SET organization_id = (
    SELECT m.organization_id
    FROM organization_memberships m
    WHERE m.user_id = a.authorized_by
      AND m.status = 'ACTIVE'
    ORDER BY m.id ASC
    LIMIT 1
)
WHERE a.organization_id IS NULL;

UPDATE authorization_templates t
SET organization_id = (
    SELECT m.organization_id
    FROM organization_memberships m
    WHERE m.user_id = t.created_by
      AND m.status = 'ACTIVE'
    ORDER BY m.id ASC
    LIMIT 1
)
WHERE t.organization_id IS NULL;

-- 3. Fail-noisy guard: surface unbackfillable rows before NOT NULL.
DO $$
DECLARE
    auth_count INT;
    tpl_count INT;
    auth_ids TEXT;
    tpl_ids TEXT;
BEGIN
    SELECT COUNT(*), STRING_AGG(id::TEXT, ',')
      INTO auth_count, auth_ids
      FROM authorizations
      WHERE organization_id IS NULL;

    SELECT COUNT(*), STRING_AGG(id::TEXT, ',')
      INTO tpl_count, tpl_ids
      FROM authorization_templates
      WHERE organization_id IS NULL;

    IF auth_count > 0 OR tpl_count > 0 THEN
        RAISE EXCEPTION
          'V1.6 backfill incomplete. Authorizations missing org: % (ids: %, creator column: authorized_by). Templates missing org: % (ids: %, creator column: created_by). A SUPER_ADMIN must assign an active OrganizationMembership to each listed creator (or delete the orphan row), then re-run the migration.',
          auth_count, COALESCE(auth_ids, ''), tpl_count, COALESCE(tpl_ids, '');
    END IF;
END $$;

-- 4. Enforce NOT NULL and FK. No ON DELETE clause: deleting an Organization
--    with referencing authorizations should fail noisily, not silently cascade.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'authorizations'
          AND column_name = 'organization_id'
          AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE authorizations
            ALTER COLUMN organization_id SET NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'authorization_templates'
          AND column_name = 'organization_id'
          AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE authorization_templates
            ALTER COLUMN organization_id SET NOT NULL;
    END IF;
END $$;

ALTER TABLE authorizations
    DROP CONSTRAINT IF EXISTS fk_authorizations_organization;
ALTER TABLE authorizations
    ADD CONSTRAINT fk_authorizations_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE authorization_templates
    DROP CONSTRAINT IF EXISTS fk_authorization_templates_organization;
ALTER TABLE authorization_templates
    ADD CONSTRAINT fk_authorization_templates_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id);

-- 5. Indexes for the most common query: list-by-org.
CREATE INDEX IF NOT EXISTS idx_authorizations_org
    ON authorizations (organization_id);

CREATE INDEX IF NOT EXISTS idx_authorization_templates_org
    ON authorization_templates (organization_id);
