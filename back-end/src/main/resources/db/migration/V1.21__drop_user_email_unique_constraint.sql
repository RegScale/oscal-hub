-- V1.21: Drop the leftover unique constraint on users.email
-- Description: V1.11 removed @Column(unique=true) from the User entity, but
--              ddl-auto=update never drops existing constraints, so prod DBs
--              still carry the old auto-generated unique key. This causes
--              approve-access-request to fail when a user registered separately
--              after submitting their access request (same email, new username).
--              Drops every uk* constraint we know about; safe with IF EXISTS.

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
ALTER TABLE users DROP CONSTRAINT IF EXISTS uk6dotkott2kjsp8vw4d0m25fb7;

-- Defensive: if the constraint name varies across environments, list known
-- variants here. The Java entity no longer declares unique=true on email,
-- so re-adding it via ddl-auto won't happen.
