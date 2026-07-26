-- Race-condition backstops for the registration/onboarding flows.
--
-- 1) Organization names: the application pre-check is case-INsensitive
--    (existsByNameIgnoreCase) but the column's unique constraint is
--    case-sensitive, so "Acme" and "acme" could both be created under
--    concurrency. Enforce case-insensitive uniqueness at the DB.
--
--    Wrapped in a DO block: if a prod database already contains names that
--    differ only by case, index creation would fail and block boot. In that
--    case we log a WARNING instead — resolve the duplicate names manually,
--    then re-run:  CREATE UNIQUE INDEX uq_organizations_name_lower
--                  ON organizations (LOWER(name));
DO $$
BEGIN
    CREATE UNIQUE INDEX IF NOT EXISTS uq_organizations_name_lower
        ON organizations (LOWER(name));
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'V1.15: could not create uq_organizations_name_lower — organizations table likely contains names differing only by case; fix manually. Error: %', SQLERRM;
END $$;

-- 2) Access requests: the application rejects a second PENDING request for the
--    same email+org, but the check-then-insert is racy and unconstrained, so
--    double-submits create duplicates. First auto-reject any existing duplicate
--    PENDING rows (keeping the earliest), then add a partial unique index so
--    the race can never recur.
UPDATE user_access_requests
SET status = 'REJECTED',
    notes = COALESCE(notes || ' ', '') || '[auto-rejected: duplicate pending request, migration V1.15]'
WHERE status = 'PENDING'
  AND email IS NOT NULL
  AND id NOT IN (
      SELECT MIN(id)
      FROM user_access_requests
      WHERE status = 'PENDING' AND email IS NOT NULL
      GROUP BY organization_id, LOWER(email)
  );

CREATE UNIQUE INDEX IF NOT EXISTS uq_access_requests_pending_email_org
    ON user_access_requests (organization_id, LOWER(email))
    WHERE status = 'PENDING';
