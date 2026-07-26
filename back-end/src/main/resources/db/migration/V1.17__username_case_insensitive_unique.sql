-- Case-insensitive uniqueness for usernames.
--
-- The application now rejects registrations that differ from an existing
-- username only by case ("Iorga" vs "iorga"), and login falls back to a
-- unique case-insensitive match. This index makes the guarantee hold under
-- concurrency as well.
--
-- Wrapped in a DO block: if a database already contains case-duplicate
-- usernames, index creation would fail and block boot. In that case a
-- WARNING is logged instead — rename the duplicate accounts manually, then
-- re-run:  CREATE UNIQUE INDEX uq_users_username_lower ON users (LOWER(username));
DO $$
BEGIN
    CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username_lower
        ON users (LOWER(username));
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'V1.17: could not create uq_users_username_lower — users table likely contains usernames differing only by case; fix manually. Error: %', SQLERRM;
END $$;
