-- Seed the singleton security_policy row (id = 1).
--
-- V1.0 created the table but never inserted the row. The application's
-- runtime fallback (SecurityPolicyService.createDefaultPolicy) could not
-- create it either: it set the id on an IDENTITY entity, so JPA issued an
-- UPDATE against a nonexistent row and failed every time. That made the
-- nightly audit-log cleanup job error out at 02:00 UTC on every run.
--
-- Idempotent: safe to re-run, no-op where the row already exists.
INSERT INTO security_policy (
    id,
    mfa_required,
    password_min_length,
    password_max_length,
    password_rotation_days,
    audit_log_retention_days,
    updated_at,
    updated_by
)
VALUES (1, false, 10, 128, 0, 90, now(), 'system')
ON CONFLICT (id) DO NOTHING;

-- Keep the identity sequence ahead of the explicitly-inserted id.
SELECT setval(
    pg_get_serial_sequence('security_policy', 'id'),
    GREATEST((SELECT COALESCE(MAX(id), 1) FROM security_policy), 1)
);
