-- V1.9: Promote howieavp to Organization Admin
-- Date: 2025-11-16
-- Description: Updates howieavp user to be an ORG_ADMIN in the Default Organization

-- ============================================================================
-- Update howieavp membership to ORG_ADMIN role
-- ============================================================================
UPDATE organization_memberships
SET
    role = 'ORG_ADMIN',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = (SELECT id FROM users WHERE username = 'howieavp')
  AND organization_id = 1  -- Default Organization
  AND EXISTS (SELECT 1 FROM users WHERE username = 'howieavp');

-- Log the change
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE username = 'howieavp') THEN
        RAISE NOTICE 'Successfully promoted howieavp to ORG_ADMIN in Default Organization';
    ELSE
        RAISE NOTICE 'User howieavp does not exist - no action taken';
    END IF;
END $$;
