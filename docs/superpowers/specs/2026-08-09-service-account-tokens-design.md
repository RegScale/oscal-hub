# Service Account Tokens — Permissions & Revocation — Design

**Date:** 2026-08-09
**Status:** Approved

## Problem

OSCAL Hub already issues long-lived service account tokens
(`POST /api/auth/service-account-token`, generated from the Profile page via
`ServiceAccountTokenGenerator.tsx`). Two defects make them unfit for real
external integrations:

1. **Tokens carry no permissions.** `JwtUtil.generateServiceAccountToken()`
   sets only `tokenName` and `tokenType`. It omits `globalRole`, `orgRole`,
   `organizationId`, and `userId`. Because `JwtAuthenticationFilter` derives
   admin authorities from those claims, a token minted by a SUPER_ADMIN acts
   as a plain `ROLE_USER`. The UI meanwhile tells the user the token grants
   "full API access", which is the opposite of what happens.

2. **Tokens cannot be revoked.** `AuthService.generateServiceAccountToken()`
   only computes an expiry date — nothing is persisted. There is no token
   entity, no `jti`, no blocklist, and no revoke endpoint. The user guide at
   `front-end/src/app/guide/account/service-tokens/page.mdx` documents a
   Revoke button and token list that do not exist. Today the only way to kill
   a leaked token is rotating `JWT_SECRET`, which invalidates every session
   and every other token at once.

Combined, a leaked token is an unrevocable credential valid for up to 10
years.

## Decisions (confirmed with user)

- **Permission model: snapshot at issuance.** Role claims are copied into the
  token when it is minted. Rejected: live resolution from the DB per request
  (proposed as the safer default), and scoped/least-privilege tokens (no
  permission taxonomy exists today).
- **Accepted consequence:** a token retains its snapshotted privileges even
  after the issuing user is demoted. Manual revocation is the mitigation.
  This mirrors GitHub's classic PATs.
- **Auto-revoke triggers: account disabled or deleted only.** Rejected: revoke
  on role downgrade, revoke on organization removal. In practice this means
  archive alone — the codebase has no account-delete path.
- **Legacy tokens: rejected.** Service-account tokens without a `jti` claim
  are refused with 401 from the moment this deploys. Rejected: grandfathering,
  with or without a deadline.
- **Expiration ceiling: 3650 days, configurable.** Enforced server-side,
  matching what the UI and docs already promise. Rejected: 365, 90, no cap.
- **`last_used_at` is tracked**, with writes throttled to at most one per
  token per hour.

## Approach

Add a `service_account_tokens` table keyed by a `jti` claim embedded in each
token. Issuance writes a row; validation checks that row for revocation.
Permissions ride along as JWT claims.

The permission half requires **no filter changes**.
`JwtAuthenticationFilter:81-90` already reads `globalRole` and `orgRole` from
claims unconditionally and converts them to `SimpleGrantedAuthority`. It does
not care how the token was minted. Populating the claims at generation is
sufficient. All new filter work belongs to revocation.

## Data model

New entity `ServiceAccountToken` → table `service_account_tokens`:

| Column | Type | Notes |
|---|---|---|
| `id` | bigserial PK | |
| `user_id` | bigint FK → `users`, not null | `@ManyToOne(fetch = LAZY)` |
| `token_name` | varchar(255) not null | user-supplied label |
| `jti` | varchar(36) not null unique | UUID; matches the JWT `jti` claim |
| `global_role` | varchar(50) null | snapshotted grant |
| `org_role` | varchar(50) null | snapshotted grant |
| `organization_id` | bigint null | snapshotted grant |
| `expires_at` | timestamp not null | mirrors the JWT `exp` |
| `revoked_at` | timestamp null | null while active |
| `revoked_by` | varchar(255) null | username that performed the revoke |
| `created_at` | timestamp not null | set in `@PrePersist` |
| `last_used_at` | timestamp null | throttled to hourly writes |

Indexes: unique on `jti`, plain on `user_id`.

**No token value or hash is stored.** This deviates deliberately from the
`PasswordResetToken` pattern, which hashes because the raw token is the secret
being looked up. Here the JWT signature already proves authenticity; the `jti`
is only an identity to check against a revocation list. A hash column would
protect nothing.

Derived status, computed in the DTO rather than stored:

- `REVOKED` — `revoked_at` is not null
- `EXPIRED` — not revoked and `expires_at` is in the past
- `ACTIVE` — otherwise

### Migration

`back-end/src/main/resources/db/migration/V1.18__service_account_tokens.sql`
(V1.17 is the current highest). Idempotent per project policy: `CREATE TABLE
IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`.

## Issuance

`JwtUtil.generateServiceAccountToken()` gains claims:

| Claim | Source |
|---|---|
| `jti` | freshly generated UUID |
| `userId` | caller's session token |
| `globalRole` | caller's session token |
| `orgRole` | caller's session token |
| `organizationId` | caller's session token |
| `tokenName` | request |
| `tokenType` | constant `service-account` |

The role values are read from the caller's *current* session JWT via the
existing `jwtUtil.extractGlobalRole` / `extractOrganizationRole` /
`extractOrganizationId` accessors, the same way `AuthController:245` already
does for org switching.

`AuthService.generateServiceAccountToken()` stops being a date calculator. It
persists the `ServiceAccountToken` row and returns the entity, in the same
transaction as the mint. The controller returns `id` alongside the existing
response fields.

Expiration is validated by `@Max` on `ServiceAccountTokenRequest.expirationDays`,
bound to property `oscal.service-tokens.max-expiration-days` (default 3650).
The existing `@Min(1)` stays.

## Validation

One new block in `JwtAuthenticationFilter`, entered only when the token's
`tokenType` claim equals `service-account`:

1. Extract `jti`. If absent → reject with 401. This is the legacy rejection.
2. Look up the row by `jti`. If absent → 401.
3. If `revoked_at` is set → 401.
4. Otherwise continue. The existing claim-to-authority code grants the
   snapshotted roles with no modification.
5. Update `last_used_at` if it is null or older than one hour.

Cost is one indexed lookup per request, on top of the `loadUserByUsername`
read the filter already performs.

Expiry needs no explicit check here — `jjwt` rejects expired tokens during
signature validation, before this block runs. The `expires_at` column exists
for display and for the status derivation, not for enforcement.

## Revocation

**Manual.** `DELETE /api/auth/service-account-tokens/{id}` sets `revoked_at`
and `revoked_by`. Idempotent: revoking an already-revoked token is a no-op
returning 204.

**Automatic.** A `revokeAllForUser(userId)` call fires when an account is
disabled.

The only account-disable path in the codebase is
`OrganizationController.archiveUser()` (line 130), a SUPER_ADMIN endpoint that
sets `user.setEnabled(false)` inline in the controller. **There is no
account-delete path** — accounts are archived, never deleted — so the
"disabled or deleted" trigger reduces to archive alone.

`OrgAdminController.deactivateUser()` (line 321) deactivates an *organization
membership*, not the account, and is deliberately not a trigger: that is the
"removed from organization" option that was declined.

Because `archiveUser` mutates the user inline, the archive logic moves into a
small service method that both disables the account and revokes its tokens, so
the two cannot drift apart. `unarchiveUser` does **not** restore revoked
tokens — a reinstated user mints new ones.

## API

All three endpoints are scoped to the calling user. No admin cross-user view:
the admin need this would serve — killing a departing employee's tokens — is
already covered by the disable/delete auto-revoke.

| Method | Path | Behavior |
|---|---|---|
| `POST` | `/api/auth/service-account-token` | existing; now persists a row and returns `id` |
| `GET` | `/api/auth/service-account-tokens` | list caller's own tokens, metadata only |
| `DELETE` | `/api/auth/service-account-tokens/{id}` | revoke |

`ServiceAccountTokenSummary` response DTO: `id`, `tokenName`, `globalRole`,
`orgRole`, `organizationName`, `createdAt`, `expiresAt`, `lastUsedAt`,
`revokedAt`, `status`.

The token value is never returned by `GET` — only by the original `POST`, once.

## Error handling

- Expired, revoked, and missing-`jti` tokens each return 401 with a distinct
  message. The caller already holds the credential, so distinguishing these
  leaks nothing and is what makes a failing CI job debuggable.
- Revoking a token owned by another user returns **404, not 403**, so the
  endpoint cannot be used to probe which token IDs exist.
- Revoking a nonexistent ID returns 404.

## Audit

`AUTH_SERVICE_TOKEN_GENERATED` already exists in `AuditEventType` at MEDIUM.
Add `AUTH_SERVICE_TOKEN_REVOKED` at MEDIUM beside it.

## Frontend

`ServiceAccountTokenGenerator.tsx` keeps its generate form and gains a table
below it: Name, Permissions, Created, Last used, Expires, Status, Revoke.
Revoke opens a confirm dialog, then refreshes the list.

The success panel additionally displays the snapshotted role, so nobody hands
out a SUPER_ADMIN token without seeing that they did.

Two strings in that component become incorrect and must change:

- Line 85, "Service account tokens provide full API access." Misleading before
  this change (tokens had *less* access than the user's session); true after
  it. Reword to state the token carries the creator's permissions and to name
  the snapshotted role.
- Line 86, "Tokens are not stored and cannot be retrieved after generation."
  Becomes half false — metadata is now stored, the token *value* is not.
  Reword to that distinction.

New `apiClient` methods for the list and revoke calls, following existing
conventions in `front-end/src/lib/api-client.ts`.

## Documentation

- `front-end/src/app/guide/account/service-tokens/page.mdx` — its "Revoking a
  token" section describes a UI that does not currently exist and becomes
  accurate for the first time. Add permission-snapshot semantics and the
  legacy-token break.
- `front-end/src/app/guide/reference/api-automation/page.mdx` — same
  legacy-token note.

## Testing

| Area | Cases |
|---|---|
| `JwtUtil` | role claims present; `jti` unique across mints |
| Filter | revoked → 401; missing `jti` → 401; valid → authorities include snapshotted roles; `last_used_at` throttling |
| Controller | `GET` returns only the caller's tokens; cross-user `DELETE` → 404; double revoke → 204 |
| Archive | archiving a user revokes all their tokens; unarchiving does not restore them |
| Request validation | `expirationDays` above the configured max → 400 |
| Frontend | list renders; revoke calls the API and refreshes |

## Deployment

Rejecting legacy tokens is a **breaking change**. Every currently-integrated
client stops working the moment this deploys and must mint a replacement.

Before deploying, query the `AUTH_SERVICE_TOKEN_GENERATED` audit events in
prod to identify who is affected, and put the break in the release notes.

## Out of scope

- Scoped / least-privilege tokens. Requires a permission taxonomy the app does
  not have. The entity can gain a nullable `scopes` column later without a
  breaking change.
- Admin cross-user token browser.
- Auto-revoke on role downgrade or organization removal.

## Related bug found during design (confirmed, out of scope)

**Archiving a user does not lock them out.**

`JwtUtil.validateToken()` (line 214) is:

```java
return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
```

It never consults `userDetails.isEnabled()`, and `JwtAuthenticationFilter`
does not check it either before building the
`UsernamePasswordAuthenticationToken`. `CustomUserDetailsService` faithfully
passes `user.getEnabled()` into the `UserDetails` object, and nothing reads it
on the request path.

Consequence: a SUPER_ADMIN archives a user, the API reports "They can no
longer log in" — true, they cannot obtain a *new* token — but the session
token already in their browser keeps working until it expires, up to 24 hours.

This is a live authentication bug that predates this feature. The fix is a
one-line `isEnabled()` check in the filter, but it is a behavior change on the
main authentication path for every user, so it does not belong in this
feature's diff. Tracked and fixed separately.

Note that the auto-revoke in this design closes the equivalent hole for
service account tokens only — it does not fix session tokens.
