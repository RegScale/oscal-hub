# OSCAL Hub Onboarding UX Redesign — Design Spec

**Date:** 2026-05-01
**Author:** Travis Howerton
**Status:** Approved for implementation planning

## Problem

When a user registers a new account in OSCAL Hub today, the flow fails them in three ways:

1. **No email is ever sent.** The post-login pending page tells the user "you will be notified via email once your request has been reviewed," but the codebase has no email-sending implementation. Registration, access-request acknowledgment, and approval/rejection all happen silently.
2. **No personal/default organization.** Every user must be granted access to a shared organization before they can do anything useful. There is no self-serve path.
3. **Empty pending state.** A registered user with no organization membership lands on a static "Access Request Pending" message at `front-end/src/app/page.tsx:87` with nothing to do, no progress signal, and no alternate path forward.

The result: new users register, request access, get nothing, and bounce.

## Decisions

| Decision | Choice |
|---|---|
| Onboarding model | Self-serve hybrid: user can create+name their own organization at registration; can also request access to existing organizations. |
| Email verification on registration | None — instant login after registration. |
| Email provider | SendGrid, reusing the existing API key and sender identity from the user's `trust-center` repo. |
| Registration screen shape | Single combined page (account fields + organization name on one form), with a quiet "request access instead" link. |
| Invitations | In scope — org admins invite teammates by email. |
| Existing stuck users | Treat as fresh on next login. Their existing pending requests stay queued; the redesigned root page also offers them the create-your-own-organization path. |

## High-Level Flow

### Self-serve path (happy path)

1. User opens `/login`, switches to "Sign up" tab.
2. Single form captures **email, username, password, organization name**. A quiet "Looking to join an existing organization instead? Request access" link sits below.
3. On submit, the backend transactionally creates the `User`, the `Organization`, and an `OrganizationMembership` linking them with role `ORG_ADMIN`, status `ACTIVE`. SendGrid fires a welcome email.
4. User is logged in and dropped onto the org's dashboard.

### Request-access path

- The "Request access" link routes to `/request-access` (existing page) with email/username pre-filled from the registration form. The user account is created, the request is queued, and the requester receives an acknowledgment email. Org admins of the target org receive a notification email. The user lands on the redesigned pending-state root page — not stuck.

### Invited-teammate path

- Org admin enters a teammate's email at `/org-admin/invitations` → an `Invitation` row is created with a token → SendGrid sends an invite link `…/accept-invite?token=…`.
- Recipient clicks link, and the accept-invite route handles three cases:
  - **No account, logged out:** slimmed signup form (email pre-filled, no org-name field). On submit, account is created and auto-added to the inviting org as `USER`.
  - **Existing account, logged out:** redirected to login, returned to accept route, auto-added.
  - **Logged in:** one-click confirmation, auto-added.

### Existing stuck users (deploy-time)

- No data migration. Their pending requests stay in place. Their next login lands on the redesigned root page, which now also offers "Or create your own organization to get started right away."

## Backend Design

### New entity: `Invitation`

| Field | Type | Notes |
|---|---|---|
| `id` | Long | PK |
| `email` | String | invitee email |
| `organization_id` | Long | FK → organizations |
| `invited_by_user_id` | Long | FK → users |
| `token` | String (UUID) | unique, indexed |
| `role` | enum (USER, ORG_ADMIN) | role granted on accept |
| `status` | enum (PENDING, ACCEPTED, REVOKED, EXPIRED) | |
| `expires_at` | timestamp | default +7 days |
| `created_at` | timestamp | |
| `accepted_at` | timestamp nullable | |
| `accepted_by_user_id` | Long nullable | FK → users |

**Indexes:** unique(`token`), index(`email`), index(`organization_id`, `status`).

No structural changes to existing entities (`User`, `Organization`, `OrganizationMembership`, `UserAccessRequest`).

### New service: `EmailService`

Thin wrapper over the SendGrid Java SDK (`com.sendgrid:sendgrid-java`). One method per email type:

- `sendWelcome(user)`
- `sendAccessRequestAcknowledged(request)`
- `sendAccessRequestPendingForAdmins(request, admins)`
- `sendAccessRequestApproved(request, approver)`
- `sendAccessRequestRejected(request, rejector, reason)`
- `sendInvitation(invitation, inviter)`

**Templates** live in `back-end/src/main/resources/email-templates/` as `.html` + `.txt` pairs. A shared `_layout.html` base template provides the brand header/footer; each email is included via simple `String.replace` substitution. No FreeMarker / Thymeleaf — six templates do not justify a templating library.

**Rendering** is handled by a `TemplateRenderer` utility that performs `${placeholder}` substitution and HTML-escapes all user-supplied values (org names, requester messages, rejection reasons) before substitution.

**Configuration** (env-driven):

- `SENDGRID_API_KEY` — sourced from GCP Secret Manager (e.g., `oscal-hub-sendgrid-key`); value copied from the trust-center repo's deployment secret.
- `SENDGRID_FROM_EMAIL`, `SENDGRID_FROM_NAME` — match trust-center's sender identity.
- `APP_BASE_URL` — environment-specific, used to build login/accept-invite/admin deep links inside email bodies.
- `email.enabled` — kill switch. When `false`, all sends become no-ops with a single info log line. Defaults to `true` in deployed environments. For local dev, an unset `SENDGRID_API_KEY` automatically engages the no-op path so `dev.sh` works without the key.

**Failure handling.** A SendGrid send failure must NEVER fail the request that triggered it. Failures are logged and swallowed; an audit-log entry records every send attempt (success or failure) using the existing audit infrastructure.

### New controller: `InvitationController`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/org-admin/invitations` | ORG_ADMIN of target org | create invitation |
| GET | `/api/org-admin/invitations` | ORG_ADMIN | list invitations for current org |
| DELETE | `/api/org-admin/invitations/{id}` | ORG_ADMIN | revoke pending invitation |
| GET | `/api/invitations/{token}` | public | return invitation details for accept page |
| POST | `/api/invitations/{token}/accept` | optional auth | accept; creates user if needed, adds membership |

### Modified controller: `AuthController`

- `POST /api/auth/register` accepts an optional `organizationName` field. When present, the request transactionally creates the user, organization, and `ORG_ADMIN` membership in one DB transaction. When absent, behavior is unchanged.
- Org-name collision returns HTTP 409 with a field-level error so the frontend can highlight only the org-name field.

### Existing endpoints (email triggers added)

- `POST /api/auth/request-access` → fires `sendAccessRequestAcknowledged` to requester and `sendAccessRequestPendingForAdmins` to all `ORG_ADMIN`s of the target org.
- `POST /api/org-admin/access-requests/{id}/approve` → fires `sendAccessRequestApproved` to requester.
- `POST /api/org-admin/access-requests/{id}/reject` → fires `sendAccessRequestRejected` to requester (rejection reason included in body).

### Edge cases

- Org-name collision on registration → 409 with field-level error.
- Invitation to email already in another org → allowed; memberships are many-to-many.
- Invitation to email belonging to an existing user → token accept simply adds the membership, no new user created.
- Expired invitation token → 410 with friendly message ("ask your admin to resend").
- Re-invite to same email → revoke prior `PENDING` invitation, create new one.
- Invite to email of a user who is already an `ACTIVE` member of the inviting org → 409 with message "user is already in this organization."

## Email Templates

| # | Template | Trigger | Recipient | Key content |
|---|---|---|---|---|
| 1 | `welcome.html` | User registers (any path) | New user | Welcome message + link back to app + brief "what's next" |
| 2 | `access-request-acknowledged.html` | User submits request-access | Requester | "We received your request for {org}. You'll hear from an admin soon." |
| 3 | `access-request-pending-admin.html` | User submits request-access | All `ORG_ADMIN`s of target org | "{name} ({email}) requested access to {org}. {message}." + deep link to admin requests page |
| 4 | `access-request-approved.html` | Admin approves | Requester | "You're in. Access to {org} approved by {admin}." + login link |
| 5 | `access-request-rejected.html` | Admin rejects | Requester | "Your request for {org} was not approved. {reason}" + link to request access elsewhere |
| 6 | `invitation.html` | Admin invites teammate | Invitee email | "{admin} invited you to join {org} on OSCAL Hub" + accept link with token, expiry, instructions for both new and existing users |

Each template ships as HTML + plain-text fallback. All user-supplied values are HTML-escaped before substitution.

## Frontend Design

### Modified pages

- **`/login` (signup tab)** — `front-end/src/app/login/page.tsx`
  - New `organizationName` field below the existing email/username/password fields, with helper text: "You'll be the admin. You can invite teammates or rename later."
  - Quiet link below: "Looking to join an existing organization? Request access".
  - Submit calls extended `POST /api/auth/register`. A 409 on `organizationName` surfaces as an inline field error.

- **`/` (root post-login page)** — `front-end/src/app/page.tsx`
  - Replace the dead-end "Access Request Pending" block at lines ~87–122 with a richer view:
    - **Zero memberships, has pending request:** show pending request status (org name, when submitted) **plus** a "Or create your own organization now" CTA leading to a small modal that captures only the org name.
    - **Zero memberships, no pending requests:** show "Get started" with two equal-weight cards: "Create an organization" and "Request access to an existing one".
    - **Has at least one membership:** existing dashboard rendering, unchanged.

### New pages

- **`/accept-invite?token=...`** — handles all three invitee states. Fetches invite details via `GET /api/invitations/{token}`. On 404/410, renders an "expired or invalid invitation" state with a "request a new one" guide. On success, branches into the appropriate signup, login, or one-click acceptance UI. Routes into the org's dashboard after acceptance.

- **`/org-admin/invitations`** — Org admin management page:
  - Form: invite by email (email, optional role dropdown defaulting to USER, send button).
  - Table of pending invitations: email, role, sent timestamp, expires timestamp, "Resend" / "Revoke" actions.
  - Linked from the existing Org Admin nav alongside "Requests".

### Reused

- `components/organization-switcher.tsx` already handles users with multiple memberships — no changes.
- `/request-access` page stays. Small tweak: pre-fill email/username from `sessionStorage` when arriving from the registration form (the registration form writes `pendingRegistration` keys to sessionStorage when the user clicks the "Request access" link). sessionStorage keeps form data out of URLs and out of browser history.

### API client

Thin additions to `front-end/src/lib/api-client.ts` for the four invitation endpoints, following existing conventions.

## Migration & Rollout

### Database

1. `V1.NN__create_invitations_table.sql` — `invitations` table per schema above. Indexes as specified.

No data migration. Existing stuck users are handled by the redesigned root page on next login.

### Configuration / secrets

- Add `SENDGRID_API_KEY` to GCP Secret Manager (e.g., `oscal-hub-sendgrid-key`); value copied from the trust-center deployment secret.
- Add `SENDGRID_FROM_EMAIL`, `SENDGRID_FROM_NAME`, `APP_BASE_URL` to backend Cloud Run service definition.
- Document the same vars in `dev.sh` / `.env.example` for local dev. Local dev defaults to no-op `EmailService` if `SENDGRID_API_KEY` is unset.

### Deployment order

1. Deploy backend first. The new `register` endpoint accepts `organizationName` as **optional** — the old frontend keeps working.
2. Deploy frontend. It now sends `organizationName`.
3. Verify in staging end-to-end before promoting to prod.

### Rollback

- Frontend rollback: safe; backend still accepts the old register payload.
- Backend rollback: invitations table left in place (orphaned but harmless). No existing-entity changes means no data loss.
- SendGrid kill switch: `email.enabled=false` short-circuits all sends to no-ops in case of provider outage or template bug.

### Observability

- Every email send (success or failure) gets an audit-log entry via the existing audit infrastructure.
- Cloud Run logs gain structured `email_send` lines: `{template, recipient_hash, message_id, status}`. Recipient is hashed in structured logs to keep PII out of log search; full address remains in the audit table.

## Testing

### Backend (Maven, JUnit 5)

**Unit:**
- `EmailServiceTest` — mock SendGrid client; verify each `sendX` builds the expected request and that failures are swallowed-with-log.
- `TemplateRendererTest` — placeholder substitution and HTML escaping (a requester message of `<script>` must render escaped).
- `InvitationServiceTest` — happy create/accept paths, token uniqueness, expiry, re-invite revokes prior pending, accept-by-existing-user vs new-user.
- `AuthServiceTest` extension — register-with-org-name creates org + ORG_ADMIN membership atomically, register-without-org-name preserves old behavior, org-name collision throws.

**Integration (`@SpringBootTest`):**
- A test-profile fake `EmailService` captures sends in-memory. Walk the four real flows end-to-end:
  1. Register with org name → DB has user, org, ORG_ADMIN membership; one welcome email captured.
  2. Request access → DB has pending request; two emails captured (acknowledged + admin notification).
  3. Approve / reject request → membership added or rejection recorded; correct email captured.
  4. Invite → accept (new-user and existing-user variants) → membership added; emails captured.
- All transactions roll back per test.

### Frontend (Jest + React Testing Library)

- Registration form: validates required fields, surfaces server 409 on `organizationName`, sends correct payload.
- Root page (`/`): renders correct empty-state for each of the three branches (zero memberships + pending, zero memberships + no pending, has memberships).
- `/accept-invite`: handles logged-in / logged-out-with-account / logged-out-without-account branches; renders expired-token error state.

### E2E (Playwright)

Three flows, real backend, real DB, fake `EmailService`:

1. Sign up with org name → land on dashboard.
2. Sign up without org → request access → admin approves → access works.
3. Org admin invites teammate → teammate accepts via invite link (new user) → joins org.

### Manual / verification

- Staging smoke test in a real browser against real SendGrid sandbox: actually receive each email, click each link, confirm rendering on Gmail and Outlook.
- Confirm `email.enabled=false` kill switch suppresses all sends without breaking the flows.

## Out of Scope

- Email verification (deferred — Section "Decisions").
- SSO / SAML / OAuth providers.
- Organization deletion or transfer of `ORG_ADMIN` between users.
- Bulk invite (CSV upload).
- Per-user notification preferences.
- Password reset email (existing flow not modified by this work).
- Account-lockout and password-change notifications. The `account.security.email-on-account-lockout` and `account.security.email-on-password-change` config flags exist today with no implementation; wiring them through the new `EmailService` is a natural follow-up but is not part of this work.
