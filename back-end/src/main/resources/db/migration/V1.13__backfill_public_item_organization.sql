-- V1.13: Backfill organization_id on PUBLIC library_items.
--
-- The original publish flow in LibraryService.setVisibility() was setting
-- library_items.organization_id = null for every visibility transition that
-- wasn't ORGANIZATION. That nuked the publishing-org attribution we now need
-- for the Top Organizations leaderboard on /catalog and the "Published by"
-- label on the public cards.
--
-- This migration sets organization_id on any PUBLIC item that's currently
-- null, by looking up the item creator's first ACTIVE organization
-- membership. Items whose creators have no active membership stay null and
-- get the generic "Community" label in the UI.
--
-- Idempotent: re-running has no effect because of the IS NULL guard.

UPDATE library_items li
SET organization_id = (
    SELECT om.organization_id
    FROM organization_memberships om
    WHERE om.user_id = li.created_by
      AND om.status = 'ACTIVE'
      AND om.organization_id IS NOT NULL
    ORDER BY om.joined_at ASC NULLS LAST, om.id ASC
    LIMIT 1
)
WHERE li.visibility = 'PUBLIC'
  AND li.organization_id IS NULL;
