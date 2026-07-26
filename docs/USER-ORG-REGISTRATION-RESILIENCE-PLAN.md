# User & Organization Registration — Resilience Plan

**Date**: 2026-07-25
**Status**: All five phases implemented (see per-phase records below)

## Phase 1 implementation (2026-07-25)

All "stop the crashes" items are done, tested (73 tests green across the six affected classes), and the migration was validated against a scratch PostgreSQL 16:

- `UserRepository.findAllByEmailIgnoreCase` added; `findByEmailIgnoreCase` deprecated. All three duplicate-email crash sites fixed: `InvitationService.acceptInvitation` (≥2 matches → clear 400), `InvitationService.createInvitation` (already-member check iterates all matching accounts), `UserAccessRequestService.approveRequest` (≥2 matches with no username match → clear admin-facing error).
- Invitation accept is idempotent (retry with same token+username returns the accepted user instead of 410) and pre-checks username availability (clear "already taken" message; DB constraint still backstops the race).
- DEACTIVATED memberships are reactivated on invitation accept and on access-request approval (explicit admin intent); LOCKED memberships produce a clear error without consuming the invitation/request.
- `AuthService.register` uses `saveAndFlush` and translates the constraint violation from a lost username race into the same 409 `UsernameAlreadyExistsException` as the pre-check; `createOrganizationForUser` flushes for the same reason.
- Migration `V1.15__registration_race_constraints.sql`: case-insensitive unique index on `organizations(LOWER(name))` (DO-block wrapped: logs a WARNING instead of blocking boot if legacy case-duplicates exist), auto-rejects duplicate PENDING access requests (keeps earliest), then adds a partial unique index on `(organization_id, LOWER(email)) WHERE status='PENDING'`.

Tests added: 6 in `InvitationServiceTest`, new `UserAccessRequestApprovalResilienceTest` (3), 1 race test in `AuthServiceTest`.

## Phase 2 implementation (2026-07-25)

**Forgot-password flow** (finding #1):
- `POST /api/auth/forgot-password` — public, always 200 with the same message (no user enumeration), even if the service throws internally. Shares the registration rate-limit bucket per IP. Issues one token per matching *enabled* account (username first, then all accounts sharing the email — capped at 5), each emailed with its username.
- Tokens: 32 bytes from `SecureRandom`, base64url in the link, **SHA-256 hash at rest** (`password_reset_tokens`, migration `V1.16`), 60-minute TTL, single use; a successful reset invalidates all sibling tokens and **clears any failed-login lockout**.
- `POST /api/auth/reset-password` — validates against the full password policy; a rejected password does not consume the token.
- Frontend: `/forgot-password` and `/reset-password` pages (with the live requirements checklist), "Forgot your password?" link on the login page, `apiClient.forgotPassword/resetPassword`.

**Invitation email no longer a single point of failure** (finding #3 + #10):
- `invitations.email_sent` persisted (migration V1.16); create/resend record send success or failure.
- `InvitationResponse` now carries `emailSent` and, on org-admin endpoints only, `acceptUrl` — the admin UI shows an "email failed" badge and **Copy link** / **Resend** buttons on every pending invite, plus an inline warning when a just-created invitation's email failed.
- `POST /api/org-admin/invitations/{id}/resend` — org-admin gated; regenerates the token (invalidating old links), restarts the 7-day expiry, revives EXPIRED invitations to PENDING; rejects ACCEPTED/REVOKED with clear messages.
- `EmailConfig` logs an unmissable multi-line ERROR banner at startup when `NoOpEmailService` is active outside dev/test profiles.

Tests: new `PasswordResetServiceTest` (8: happy path, duplicate emails → one email per account, unknown/disabled identifiers silent, expiry, single-use, weak-password doesn't consume token, lockout cleared, sibling invalidation), 5 resend/email-status tests in `InvitationServiceTest`, 3 endpoint-contract tests in `AuthControllerTest`, frontend `reset-password.test.tsx` (4). Migration V1.16 validated against scratch PostgreSQL 16.

## Phase 3 implementation (2026-07-26)

**Single password-policy source** (finding #7):
- `PasswordValidationService` now reads min/max length from the admin-editable `SecurityPolicyService` (DB, cached) with the env config as fail-safe fallback — the Security Policy screen's length settings are finally enforced. Character-class toggles remain env-config.
- New public `GET /api/auth/password-policy` serves a machine-readable descriptor; the frontend's `usePasswordPolicy()` hook hydrates it (module-cached, static defaults as offline fallback) and feeds both the shared checklist component and every page's submit gate — all six password surfaces now render exactly what the server enforces.
- Duplicate `@Size` minimums removed from `RegisterRequest`/`ResetPasswordRequest` (max 128 kept as a DoS bound).

**Username normalization** (finding #8):
- Registration and invite-accept trim username/email and reject case-only variants of existing usernames (`existsByUsernameIgnoreCase`); access requests trim identity fields at submission.
- Login (`CustomUserDetailsService`) falls back to a **unique** case-insensitive match when the exact form isn't found; ambiguous legacy duplicates still require the exact form.
- Migration `V1.17` adds a unique index on `users(LOWER(username))`, DO-block wrapped (warns instead of blocking boot if legacy case-duplicates exist). Validated on scratch PostgreSQL: warns with duplicates present, creates cleanly after cleanup.

**MFA enforced at registration** (finding #9):
- `register()` applies the same global-MFA branch as login: when required, it returns `mfaSetupRequired` + an MFA setup token instead of a session token. `AuthContext.register` routes to `/mfa-setup` and the api-client no longer stores a token when MFA-gated.

**Invite links can no longer take over existing accounts** (finding #4):
- `acceptInvitation` takes the authenticated caller. Signed-in accepts bind the **authenticated** account (even when the invite was addressed to a different email — visible/attributable to org admins). Anonymous accepts for an email that already has an account are refused with a "sign in first" message and do **not** consume the invitation. The new-user path is unchanged. The api-client now sends the session token on accept, and the idempotent-retry check covers authenticated retries.

Tests: `PasswordValidationServicePolicyTest` (3), `CustomUserDetailsServiceTest` (3), MFA + trim + policy-endpoint tests in `AuthServiceTest`/`AuthControllerTest`, takeover-refusal + signed-in-binding tests in `InvitationServiceTest`.

## Phase 4 implementation (2026-07-26)

**Client-IP extraction fixed everywhere** (finding #6, part 1):
- New `ClientIpResolver` (util) is the single source of truth, used by `AuthService`, `RateLimitFilter`, and `AuditLogService`. It takes the **rightmost trusted** `X-Forwarded-For` entry (`security.trusted-proxy-hops`, default 1 = Cloud Run; 2 = external LB + Cloud Run; 0 = header ignored). The previous implementations took the FIRST entry — client-supplied and spoofable — letting anyone rotate fake IPs to bypass per-IP rate limits and IP lockouts and pollute the audit trail. `X-Real-IP` is no longer honored (spoofable, not set by GCP).
- Audit "from unknown" fixed: `AuditLogService` now builds events **synchronously on the request thread** (where IP/user-agent/URL are available and the request object is live) and only persists them async (`persistEventAsync`). Building on the async executor was why entries showed "from unknown" — and reading a possibly-recycled request object off-thread was a latent bug of its own.

**Account lockout is DB-backed** (finding #6, part 2):
- `login()` now READS `users.account_locked_until` (previously written but never read), so a lockout holds on instances that never saw the failures and survives restarts. The lock decision comes from the shared DB `failed_login_attempts` counter vs `account.security.lockout-max-attempts`, with a sliding window: an expired lock or a last failure older than `lockout-window-seconds` restarts the count (the DB counter has no TTL — without this, stale failures would accumulate forever). The in-memory `LoginAttemptService` remains as a per-instance fast path and for IP tracking. Also fixed: the login success path now looks up the user by the *canonical* username from the authenticated principal (case-insensitive login from Phase 3 made the typed form unreliable).

**Onboarding emails are post-commit, async, retried** (finding #7 of P1 list / "async email"):
- Welcome, access-request (ack + admin notify), and password-reset-link emails are published as events and sent by `TransactionalEmailListener` — `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`, entities reloaded in the listener's own transaction, one retry after 2s, failures logged loudly but never propagated. SendGrid latency no longer extends registration transactions, and a reset link can no longer race ahead of its token row. Invitation emails stay synchronous by design (the admin UI needs the immediate `emailSent` result for the copy-link/resend fallback).

**Temp passwords use `SecureRandom`** (was `Math.random()`, whose state is recoverable from observed outputs).

Tests: `ClientIpResolverTest` (7, incl. spoofed-first-entry and multi-hop cases), `TransactionalEmailListenerTest` (5, incl. retry and swallow-after-retries), DB-lockout tests in `AuthServiceTest` (fresh-instance lock hold, 5th-failure locks, stale-window reset); `RateLimitFilterTest`/`AuditLogServiceTest` updated to the trusted-proxy semantics; `PasswordResetServiceTest`/`AccessRequestEmailTriggerTest` reworked to `@RecordApplicationEvents` (AFTER_COMMIT listeners never fire in rolled-back test transactions).

## Phase 5 implementation (2026-07-26)

**Log-based alerting** (`terraform/gcp/monitoring-registration.tf`, applied by the deploy pipeline's `terraform apply`):
- Three log-based metrics on `oscal-tools-prod`: `oscal_registration_failures` (the audit "User registration failed" line), `oscal_scheduled_task_failures` (the phrase that repeated silently every night for weeks), `oscal_email_send_failures` (listener terminal failures + invitation send failures).
- Three alert policies with runbook documentation: registration failures >3/hour, any scheduled-task failure, any email delivery failure in 15 min. They route through the existing email channel in `alerts.tf`, fed by the `ALERT_EMAIL` GitHub variable in the deploy pipeline (already set). `alert_email` is now also set in the local (gitignored) `terraform.tfvars` so manual `terraform apply` runs don't silently disable the channel.

**Funnel telemetry**: new events `auth.register_succeeded`, `invitation.created` (with `email_sent`), `invitation.accepted` (with `existing_account`), `access_request.submitted`, `access_request.approved` — emitted best-effort at the controller layer alongside the existing `auth.login_*` events, so stage-to-stage drop-off is measurable.

**Production canary** (`.github/workflows/prod-canary.yml`, every 6h + manual dispatch): health ping; password-policy endpoint served; **registration 400-contract probe** (policy-violating password → must get 400 with a human-readable `.message` — creates no data, and its breakage is exactly what made the July 2026 failures invisible to users); invitation-lookup 404 contract. A failing run produces the standard GitHub Actions failure notification. Probes were dry-run against live prod during development.

**Contract hole found by the dry-run and fixed**: bean-validation failures (`@NotBlank`/`@Email`/`@Size` on request DTOs) bypassed `GlobalErrorAdvice` and returned Spring's default body with NO `message` field — the unreadable-error failure mode, still live in prod for blank/oversized fields. `GlobalErrorAdvice` now handles `MethodArgumentNotValidException` and returns the first field message.

**Post-deploy verification checklist**:
1. Merge → pipeline applies Terraform (metrics + policies + email channel) and deploys the app.
2. Confirm the canary passes: Actions → "Production Canary - Registration Funnel" → Run workflow.
3. Confirm alert delivery: Cloud Console → Monitoring → Alerting shows the three new policies with the email channel attached.
4. `gcloud logging metrics list --project=oscal-hub` shows the three `oscal_*` metrics.
**Scope**: Every path by which a person or organization enters OSCAL Hub, end to end.

## The four entry paths

1. **Self-serve signup** — login page → `POST /api/auth/register` (optionally creates an org + ORG_ADMIN membership in the same transaction) → `/select-organization`
2. **Invitation** — org admin creates invite → SendGrid email with token link → `/accept-invite` → `POST /api/invitations/{token}/accept` (creates user if new, adds membership, returns JWT)
3. **Request access** — `/request-access` → `POST /api/auth/request-access` → admin approves → membership created; if the requester never registered, an account is created with a **temp password sent by email**
4. **Admin-managed** — org/super admins reset passwords, manage memberships

## Findings, ranked by severity

### P0 — dead ends and crashes a user cannot recover from

**1. There is no self-serve password reset.**
Only admin-initiated resets exist (`/users/{id}/reset-password` on the org-admin/organization controllers). Consequences:
- Forgot password → must find an org admin. The *first* admin of an org has nobody.
- Typo'd email at signup → welcome email silently bounces, and there is no recovery contact anyway.
- Approval-created accounts (path 3) receive a temp password **by email only**. `createUserFromRequest` swallows email failures with a warn — if SendGrid hiccups, the account exists with a password nobody knows and `mustChangePassword=true`. Bricked on arrival.

*Fix*: a standard forgot-password flow — `POST /api/auth/forgot-password` (always 200, no user enumeration), single-use hashed token with 30–60 min expiry, `/reset-password?token=` page, audit events. This one feature removes the single largest class of "user is stuck forever" outcomes.

**2. Duplicate emails crash invitation accept and access-request approval.**
Emails are *intentionally non-unique* (documented in `AuthService.register`), but `InvitationService.acceptInvitation` and `UserAccessRequestService.approveRequest` both call `userRepository.findByEmailIgnoreCase(...)` which returns `Optional` — with two accounts sharing an email this throws `IncorrectResultSizeDataAccessException`: a 500 on accept, and a raw-message 400 on approve. Once any email is duplicated, every future invite/approval for that email fails.

*Fix*: `findAllByEmailIgnoreCase` returning a list; define deterministic behavior (e.g., for invitations: if ≥1 account matches, require the person to sign in and accept while authenticated rather than guessing which account).

**3. Email failure is silent and there is no fallback link.**
- `createInvitation` catches email exceptions and still returns 200 — the admin believes the invite went out.
- `InvitationResponse` does **not** include the token, so an admin has no way to copy the accept link out of the UI if the email was lost.
- `EmailConfig` silently swaps in `NoOpEmailService` when `email.enabled=false` **or the API key is blank** — a misconfigured deploy blackholes every invitation, welcome, temp-password, and approval email with no operator-visible signal.
- All sends are synchronous inside `@Transactional` service methods — SendGrid latency extends DB transactions and registration latency; an outage adds its full timeout to every signup.

*Fix*: (a) return/display `emailSent: true|false` from invitation create and show a warning banner with a **"Copy invite link"** action (admins are authenticated and authorized — exposing the token to the inviting admin is safe); (b) move sends to after-commit async (`@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`) with one retry; (c) persist per-message send status (SENT/FAILED) via the existing `EmailAuditLogger` and surface failures on an admin screen; (d) log a loud, unmistakable startup banner when `NoOpEmailService` is active outside dev.

**4. Invitation accept hands out a JWT for *existing* accounts without authentication, and breaks silently for deactivated members.**
`POST /api/invitations/{token}/accept` matches an existing user by the invitation email and returns a **full JWT for that account** — possession of the link equals account takeover of any existing user an admin invites (email forwarding, shared inboxes, compromised mail). Separately: if the matched user's membership in that org is DEACTIVATED, accept marks the invitation ACCEPTED but leaves the membership DEACTIVATED — the invite is consumed and the user still cannot enter the org.

*Fix*: if the email matches an existing account, require sign-in first (the accept-invite page already offers a sign-in link; make the backend enforce it: accept-with-credentials or accept-while-authenticated only). Reactivate a DEACTIVATED membership on accept — or reject with a clear message telling the admin to reactivate the member instead.

### P1 — races, consistency, multi-instance reality

**5. Check-then-insert races.**
- Username: `existsByUsername` then save. The DB unique constraint backstops it, but the race surfaces as a generic 409 "The requested operation conflicts with existing data" instead of "Username already exists". Map `DataIntegrityViolationException` on the register path to the same 409 shape as the pre-check.
- Org name: the pre-check is `existsByNameIgnoreCase`, but the DB unique constraint is **case-sensitive** — "Acme" and "acme" can both be created under concurrency. Add a functional unique index `LOWER(name)` by migration.
- Access requests: duplicate-pending check then insert, no DB constraint; double-submit creates duplicates (code already tolerates reading them, but admins see doubles). Add a partial unique index on `(organization_id, LOWER(email)) WHERE status='PENDING'`.
- Double-click on invitation accept: two threads pass the PENDING check; the loser dies on the username unique constraint as a 500. Make accept idempotent (already-ACCEPTED + same user → return success).

**6. Security state is per-instance in-memory, but prod scales to 3 instances.**
`LoginAttemptService` and `RateLimitService` use Caffeine/Bucket4j local caches. With `app_max_instances=3`: effective limits are up to 3× looser, lockouts don't propagate, and every deploy resets them (fail-open). Additionally, audit logs show `from unknown` for client IPs — if the `X-Forwarded-For` extraction fails in some paths, **per-IP rate buckets can collapse onto one shared "unknown" key, letting one abuser (or a busy day) 429 everyone**.

*Fix*: first verify/repair client-IP extraction behind the Cloud Run proxy (honor the rightmost trusted `X-Forwarded-For` hop). Then either accept documented per-instance limits, or move lockout state to the DB (the `users` table already has `failed_login_attempts` / `account_locked_until` — make login *read* them instead of only writing).

**7. Password policy has three sources of truth, one of which is dead.**
- `AccountSecurityConfig` (env/properties, min 10) — what `PasswordValidationService` actually enforces.
- `SecurityPolicy` DB row (admin-editable min/max via the Security Policy screen) — **read by no enforcement path**; an admin setting min length 12 changes nothing.
- `RegisterRequest` bean validation `@Size(min = 8)` — weaker than both.
- (Now also the frontend mirror `password-policy.ts`, kept in sync by convention.)

*Fix*: make `PasswordValidationService` read length bounds from `SecurityPolicyService` (cached), keep character rules in config, align `@Size(min = 10)`, and add a `GET /api/auth/password-policy` public endpoint so the frontend renders requirements from the server instead of a hand-maintained mirror.

**8. Username normalization is absent.**
No trimming or case rules: `"Iorga "` and `"iorga"` are distinct accounts, and login is exact-match — a user who registers `Iorga` and types `iorga` gets "invalid credentials" with no hint. *Fix*: trim username/email on registration; enforce case-insensitive username uniqueness (functional index on `LOWER(username)` after checking for existing collisions); look up case-insensitively at login.

### P2 — policy and UX consistency

**9. MFA policy is skipped for the first session.** `register()` returns a full JWT without consulting `isMfaRequired()`; `selectOrganization` doesn't check it either. A globally-required-MFA tenant still gets un-MFA'd first sessions. *Fix*: apply the same MFA-setup branch at registration that login uses.

**10. Invitations expire in a fixed 7 days with no resend.** The only recovery is revoke + re-create (and the email-failure gap in #3 makes this worse). *Fix*: `POST /api/org-admin/invitations/{id}/resend` (regenerate token, reset expiry, re-send, audit) + a resend button next to pending invites; consider making expiry days configurable.

**11. Minor items.**
- `generateTemporaryPassword` uses `Math.random()` → switch to `SecureRandom`.
- Login failure message reveals whether an account exists ("N attempts remaining") — username enumeration; standardize on a generic message (keep the lockout notice).
- The expired-invitation path sets status EXPIRED then throws inside `@Transactional`, so the status write rolls back — harmless but re-does work every attempt; move the mark-expired write to a `REQUIRES_NEW` helper or just drop the write.
- `OrgAdminController` catch-blocks return raw `e.getMessage()` in 400s — can leak internals (the register path already fixed this pattern; apply the same treatment).
- No CAPTCHA on registration; per-IP rate limiting is the only bot control. Fine for now; revisit if the bot probes in the logs turn into signup spam.

### Observability gap (how Michaela's failure went unnoticed)

Her three failed registrations were in the logs for two days; nobody saw them until she reported it. There is no alerting on the registration funnel.

*Fix*:
- Log-based metrics + alert policies in `oscal-hub` for: `User registration failed`, invitation accept failures, access-request approval failures, and any 5xx on `/api/auth/*` or `/api/invitations/*`. Route to email.
- Funnel telemetry: the register/login endpoints already emit telemetry events — add events for invitation created→accepted and request→approved so drop-off is measurable.
- A weekly synthetic canary: register a throwaway user against staging (or a `/api/health`-style self-check) so regressions surface before humans do.

## Suggested execution order

| Phase | Items | Effort | Payoff |
|---|---|---|---|
| **1. Stop the crashes** | #2 duplicate-email lookups, #5 idempotent accept + race constraint indexes, #4 deactivated-membership handling | ~1 day | No more 500s in the two admin-driven onboarding paths |
| **2. Recovery paths** | #1 forgot-password flow, #3 copy-invite-link + email-sent status + NoOp banner, #10 resend invitation | ~2–3 days | Nobody gets permanently stuck; email stops being a single point of failure |
| **3. Consistency** | #7 single password-policy source + server-served rules, #8 username normalization, #9 MFA at registration, #4 existing-account accept requires sign-in | ~2 days | Policy behaves as admins configure it; no first-session MFA hole |
| **4. Scale & abuse** | #6 client-IP extraction fix + DB-backed lockout decision, async email, SecureRandom | ~1–2 days | Correct behavior at >1 instance; registrations not hostage to SendGrid latency |
| **5. Observability** | Log-based alerts, funnel telemetry, canary | ~1 day | Next Michaela-class failure pages you the same day |

Phases 1–2 are the "super smooth and reliable" core: they eliminate every discovered way a legitimate new user can hit a wall they can't get past.

## What already works well (keep)

- Registration + org creation are atomic in one transaction (org-name conflict rolls back the user cleanly, and the frontend renders it as a field error).
- Username/org-name have real DB unique constraints backstopping the pre-checks.
- Welcome-email failure correctly doesn't fail registration.
- JWT secret comes from Secret Manager in prod (stable across deploys — the CLAUDE.md note about tokens dying on restart applies to local dev only).
- Password complexity enforcement itself is solid, and (as of this branch) mirrored client-side with live feedback on all four password surfaces.
