# Production Error Fixes — July 2026

**Date**: 2026-07-25
**Status**: Fixed, tests passing, pending deploy
**Source**: GCP Cloud Logging review of `oscal-tools-prod` (90-day window; Cloud Logging retains ~30 days on the default bucket)

## Summary

A log sweep found four distinct recurring error classes. All four are fixed in this change set, with regression tests.

| # | Symptom in prod | Root cause | Fix |
|---|---|---|---|
| 1 | User registration failed 3× for `iorga`/`iorga2` on 2026-07-23 (Michaela Iorga), user gave up | Backend rejected password ("must contain at least one uppercase letter") but the frontend displayed only the literal string **"Bad Request"** — it read `error.error` instead of `error.message`. Form also claimed "at least 8 characters" while the backend requires 10, and had no client-side complexity validation | Frontend now shows the real server message, and all password forms share a live requirements checklist that mirrors backend rules exactly |
| 2 | 500s on `GET /api/org-admin/invitations` (2026-07-06/07) — org admins couldn't see pending invites | `LazyInitializationException`: `InvitationResponse.from()` reads the LAZY `organization`/`invitedBy` associations in the controller, after the service transaction closed (OSIV is off) | Fetch joins added to `InvitationRepository.findByOrganizationIdAndStatus` and `findByToken` |
| 3 | "Unexpected error occurred in scheduled task" **every night at 02:00 UTC** | `security_policy` singleton row (id=1) was never seeded, and `createDefaultPolicy()` set the id on an IDENTITY entity before `save()`, so JPA issued an UPDATE against a nonexistent row → `Row was already updated or deleted` — unrecoverable by retry | Migration `V1.14__seed_security_policy.sql` seeds the row idempotently; `createDefaultPolicy()` now uses a native `INSERT ... ON CONFLICT DO NOTHING` |
| 4 | 500s "Library file not found" on public catalog content (2026-07-22) | `LibraryStorageService` only supported Azure Blob or local disk. On GCP the Azure connection string is empty, so it silently wrote library files to **Cloud Run's ephemeral local disk** — files vanish when the instance is replaced | Service is now `storage.provider`-aware: on `gcs` it stores under `gs://<build-bucket>/library/`. Missing files now return **404** (`LibraryFileNotFoundException`) instead of a generic 500 |

Also observed but intentionally not changed:
- **OpenTelemetry exporter timeouts** (`InterruptedIOException: timeout` from `okhttp3` / OTLP): telemetry-export noise, does not affect requests. Consider raising the exporter timeout or reviewing the collector endpoint if it gets louder.
- **Bot probes** (404s on `/users.php`, `/.ssh/authorized_keys`, etc.): normal internet background noise.
- **401s on `/api/auth/refresh`**: expected token expiry behavior.

## Files changed

### Back-end
- `repository/InvitationRepository.java` — fetch joins for `organization`/`invitedBy`
- `repository/SecurityPolicyRepository.java` — native `insertDefaultPolicy()` upsert
- `service/SecurityPolicyService.java` — `createDefaultPolicy()` uses the native insert
- `service/LibraryStorageService.java` — GCS mode; throws `LibraryFileNotFoundException`
- `exception/LibraryFileNotFoundException.java` — new, maps to 404
- `resources/db/migration/V1.14__seed_security_policy.sql` — new, seeds singleton row
- `resources/application-gcp.properties` — `gcp.storage.library-folder` property

### Front-end
- `lib/password-policy.ts` — new; client-side mirror of `PasswordValidationService` rules (min 10 chars, upper/lower/digit/special, no sequential/repeated runs, no username in password)
- `components/password-requirements.tsx` — new; live checklist component
- `lib/api-client.ts` — register/login errors prefer `error.message` over `error.error`
- `app/login/page.tsx` — checklist + client-side gate on signup (was: only length ≥ 8)
- `app/accept-invite/page.tsx` — checklist + gate (previously no guidance at all)
- `app/change-password/page.tsx` — checklist + gate (was: stale "8 characters" copy)
- `app/profile/page.tsx` — checklist + gate (was: stale "8 characters" list)

### Tests added
- `InvitationRepositoryFetchTest` — asserts associations are initialized after `em.clear()`; fails if the fetch joins are removed. (Note: the pre-existing `InvitationControllerTest` is class-level `@Transactional`, which keeps a session open during MockMvc calls and therefore *cannot* catch lazy-loading bugs — that is why this one reached prod.)
- `SecurityPolicyBootstrapIntegrationTest` — non-transactional; empty table → `getPolicy()` creates row id=1; idempotency; the exact nightly-job call path
- `AuthControllerTest.testRegister_weakPassword_returnsReasonInMessage` — locks the 400 contract (`message` carries the human-readable reason) that the frontend depends on
- `front-end/src/lib/password-policy.test.ts` — full rule coverage, including the exact Michaela failure mode
- `login-page.test.tsx` — weak password blocks submit without an API call; checklist renders; server rejection message is displayed

## Deployment notes

1. Standard deploy (merge to `main`); Flyway runs V1.14 automatically. On prod the seed INSERT fires (row missing); on dev/staging it's a no-op if the row exists.
2. **Library content written before this fix on GCP is unrecoverable** (it lived on ephemeral disk). Affected library items will now return 404 instead of 500; their owners need to re-upload.
3. The nightly 02:00 UTC error should disappear after the first deployed night — verify with:
   ```
   gcloud logging read 'resource.type=cloud_run_revision AND resource.labels.service_name=oscal-tools-prod AND severity>=ERROR' --project=oscal-hub --freshness=1d
   ```
4. If the backend password policy ever changes, update `front-end/src/lib/password-policy.ts` (and its test) to match — the file header says so too.
