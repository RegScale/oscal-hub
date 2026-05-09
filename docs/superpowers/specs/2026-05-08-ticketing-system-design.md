# Ticketing System — Design

**Date:** 2026-05-08
**Status:** Approved (brainstorming complete, awaiting implementation plan)
**Author:** Travis Howerton (with Claude Code)

## Goal

Add a basic in-app ticketing system so logged-in users can submit bug reports and feature requests, hold a threaded conversation with the super admin, and track status. The super admin gets a dedicated panel with search, filter, and analytics. Email notifications fire on every meaningful event.

## Scope and non-goals

**In scope (v1):**
- Two ticket types: bug report and feature request, sharing a single table.
- Threaded comments between reporter and super admin.
- File attachments on the original ticket and on every comment.
- Six-state status workflow with reopen-via-comment.
- Per-event email notifications via the existing SendGrid email service.
- Reporter list view of their own tickets; super admin list view of all tickets with search, filter, pagination.
- Five analytics views in the super admin panel.
- Daily auto-close job for tickets that have sat in `RESOLVED` for 7 days.

**Deliberately out of scope (v1):**
- Assignment to admins other than the super admin (single-admin model).
- Tags, labels, or categories beyond `type` and `priority`.
- SLA tracking, first-response-time, or time-to-resolution metrics.
- Daily/weekly digest emails (per-event only).
- Markdown rendering in comments (plain text).
- Org-admin tier of visibility (only reporter-sees-own + super-admin-sees-all).
- Public ticket URLs; anonymous reporting.
- Voting on feature requests.
- Ticket merging or linking ("duplicate of TKT-X").

Each non-goal is a clean addition later if real usage demands it.

## Architecture overview

A standard CRUD feature mirroring the existing Profiles pattern (`Profile.java` / `ProfileRepository.java` / `ProfileService.java` / `ProfileController.java`):

- **Backend:** Three new tables (`tickets`, `ticket_comments`, `ticket_attachments`), JPA entities, repositories, a `TicketService`, a `TicketController` (`/api/tickets`) and `AdminTicketController` (`/api/admin/tickets`), five new email templates added to `EmailService` / `SendGridEmailService`, a `@Scheduled` auto-close job, and Flyway migration `V1.10__ticketing.sql`.
- **Frontend:** Four new pages under `/tickets` and `/admin/tickets`, an extension to `UserAvatarMenu.tsx`, and analytics charts on the admin page. Mirrors the search/filter/pagination patterns already used in `/admin/users`.
- **Storage:** Attachments live in the existing GCS bucket under a `tickets/{ticketId}/` prefix.
- **Auth:** Reuses the existing JWT + `globalRole` system. Reporter-only and `SUPER_ADMIN`-only endpoints are guarded with `@PreAuthorize` (already enabled via `@EnableMethodSecurity`).

## Data model

### `tickets`

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGSERIAL` PK | Displayed as `TKT-{id}` in the UI. |
| `reporter_id` | `UUID` FK → `users` | Who opened it. |
| `type` | `VARCHAR(16) NOT NULL` | `BUG` or `FEATURE`. |
| `title` | `VARCHAR(200) NOT NULL` | |
| `description` | `TEXT NOT NULL` | |
| `priority` | `VARCHAR(16) NOT NULL DEFAULT 'MEDIUM'` | `LOW` / `MEDIUM` / `HIGH` / `CRITICAL`. |
| `status` | `VARCHAR(16) NOT NULL DEFAULT 'OPEN'` | `OPEN` / `IN_PROGRESS` / `RESOLVED` / `CLOSED` / `WONT_FIX` / `DUPLICATE`. |
| `metadata` | `JSONB NOT NULL DEFAULT '{}'::jsonb` | Type-specific fields; see shape below. |
| `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT now()` | |
| `updated_at` | `TIMESTAMPTZ NOT NULL DEFAULT now()` | Bumped by service layer on any change (status, comment, attachment). |
| `resolved_at` | `TIMESTAMPTZ NULL` | Set when status moves to `RESOLVED` / `CLOSED` / `WONT_FIX` / `DUPLICATE`; cleared on reopen. |

**Indexes:**
- `(reporter_id, status)` — for the user's "my tickets" list.
- `(status, created_at DESC)` — for the admin list and stale-tickets analytics.
- `(type, status)` — for the type-split analytics.
- `(updated_at DESC)` — for "recent activity" sorting on the admin list.

**`metadata` JSONB shape:**

For `type = BUG`:
```json
{
  "stepsToReproduce": "...",
  "expectedBehavior": "...",
  "actualBehavior": "...",
  "severity": "MINOR" | "MAJOR" | "CRITICAL",
  "browser": "Chrome 131 on macOS",
  "viewport": "1920x1080",
  "url": "/page/where/it/happened"
}
```

The frontend auto-captures `browser`, `viewport`, and `url` at submit time from `navigator.userAgent`, `window.innerWidth/innerHeight`, and `window.location.pathname`.

For `type = FEATURE`:
```json
{
  "useCase": "..."
}
```

### `ticket_comments`

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGSERIAL` PK | |
| `ticket_id` | `BIGINT NOT NULL` FK → `tickets` ON DELETE CASCADE | |
| `author_id` | `UUID NOT NULL` FK → `users` | |
| `body` | `TEXT NOT NULL` | Plain text; required even for system comments (filled with a generated string like `"Status changed from OPEN to IN_PROGRESS"`). |
| `is_status_change` | `BOOLEAN NOT NULL DEFAULT false` | System-generated comments showing status transitions. |
| `old_status` | `VARCHAR(16) NULL` | Populated when `is_status_change = true`. |
| `new_status` | `VARCHAR(16) NULL` | Populated when `is_status_change = true`. |
| `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT now()` | |

**Index:** `(ticket_id, created_at)`.

### `ticket_attachments`

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGSERIAL` PK | |
| `ticket_id` | `BIGINT NOT NULL` FK → `tickets` ON DELETE CASCADE | |
| `comment_id` | `BIGINT NULL` FK → `ticket_comments` ON DELETE CASCADE | NULL means the attachment belongs to the original ticket, not a comment. |
| `uploader_id` | `UUID NOT NULL` FK → `users` | |
| `filename` | `VARCHAR(255) NOT NULL` | Original filename as uploaded. |
| `content_type` | `VARCHAR(100) NOT NULL` | |
| `size_bytes` | `BIGINT NOT NULL` | |
| `storage_path` | `VARCHAR(512) NOT NULL` | GCS object path, e.g. `tickets/123/4567-screenshot.png`. |
| `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT now()` | |

**Limits enforced at the controller layer:**
- 10 MB per file.
- Max 5 files per ticket-creation or comment-creation request.
- Whitelisted extensions: `png`, `jpg`, `jpeg`, `gif`, `pdf`, `txt`, `log`, `json`, `xml`, `yaml`, `yml`.

## Status lifecycle

```
                           ┌──────────┐
                           │   OPEN   │◄─────┐
                           └─────┬────┘      │
                                 │           │ (user comments
                                 ▼           │  on RESOLVED)
                          ┌──────────────┐   │
                          │ IN_PROGRESS  │   │
                          └──────┬───────┘   │
                                 │           │
                                 ▼           │
                          ┌──────────────┐   │
                          │   RESOLVED   │───┘
                          └──────┬───────┘
                                 │ (admin closes, OR auto-close
                                 │  after 7 days in RESOLVED)
                                 ▼
                          ┌──────────────┐
                          │    CLOSED    │  (terminal)
                          └──────────────┘

   Admin can also send any non-terminal status to:
                          ┌──────────────┐
                          │  WONT_FIX    │  (terminal)
                          └──────────────┘
                          ┌──────────────┐
                          │  DUPLICATE   │  (terminal)
                          └──────────────┘
```

**Transition rules:**
- Only `SUPER_ADMIN` can change status via `PATCH /api/tickets/{id}/status`.
- A user comment on a ticket in state `RESOLVED` triggers a service-side reopen: status → `OPEN`, `resolved_at` cleared, an additional `is_status_change = true` comment inserted recording the transition.
- `CLOSED` / `WONT_FIX` / `DUPLICATE` are terminal — comments are still allowed but do not trigger a status change.
- Every status change inserts a system comment in the same transaction, so the thread reads as a coherent timeline.

**Auto-close job:** A Spring `@Scheduled` task runs daily (cron `0 0 3 * * *` — 03:00 server time) and moves any ticket with `status = RESOLVED AND resolved_at < now() - interval '7 days'` to `CLOSED`. It generates a system comment `"Auto-closed after 7 days in Resolved."` and does **not** fire a status-change email (avoids inbox noise; users have already been notified about the resolution).

## API endpoints

All under `/api/tickets` and `/api/admin/tickets`. JWT authentication required on every endpoint.

### Authorization rule (universal)

For any endpoint that operates on a specific ticket: the caller must be either the ticket's `reporter_id` or a `SUPER_ADMIN`. Enforced in the service layer (single check, called from each controller method).

### User endpoints

| Method | Path | Body / Params | Purpose |
|---|---|---|---|
| `POST` | `/api/tickets` | `multipart/form-data`: `type`, `title`, `description`, `priority`, `metadata` (JSON string), `files[]` | Create ticket. Returns the created ticket with `id`. |
| `GET` | `/api/tickets/mine` | Query: `page`, `size`, `status[]` (optional) | List current user's tickets, paginated, newest first. |
| `GET` | `/api/tickets/{id}` | — | Full ticket: header fields, metadata, all comments (chronological), all attachments. |
| `POST` | `/api/tickets/{id}/comments` | `multipart/form-data`: `body`, `files[]` | Add a comment. If status is `RESOLVED` and caller is the reporter, also reopens the ticket. |
| `GET` | `/api/tickets/attachments/{attachmentId}` | — | Streams the attachment from GCS. Authorization checks the parent ticket. |

### Admin endpoints

| Method | Path | Body / Params | Purpose |
|---|---|---|---|
| `PATCH` | `/api/tickets/{id}/status` | `{ "status": "...", "note": "optional" }` | Change status. Optional `note` becomes a regular admin comment in the same transaction (in addition to the system status-change comment). |
| `GET` | `/api/admin/tickets` | Query: `page`, `size`, `q` (free-text), `status[]`, `type`, `priority[]`, `from`, `to`, `sort` | Search + filter + paginate all tickets. |
| `GET` | `/api/admin/tickets/analytics` | — | Returns the five analytics payloads in a single response (see Analytics section). |

**Search behavior:** `q` matches against `title ILIKE '%q%' OR description ILIKE '%q%'`. Postgres `ILIKE` is fine for v1; switch to a `tsvector` full-text index later if it gets slow. Date filters apply to `created_at`. Default sort is `updated_at DESC`. Page size default 25, max 100.

## Frontend pages

### 1. `UserAvatarMenu.tsx` change

Insert two new items between "Manage Profile" and "Admin Dashboard":
- **Open Ticket** → `/tickets/new`
- **My Tickets** → `/tickets`

Both visible to all logged-in users.

### 2. `/tickets/new` — New Ticket form

- Type radio: Bug Report / Feature Request.
- Title (required, max 200).
- Description (required, textarea, placeholder hint changes by type).
- Priority dropdown: Low / Medium / High / Critical (default Medium).
- Type-specific fields (revealed based on radio selection):
  - **Bug:** Steps to Reproduce, Expected Behavior, Actual Behavior, Severity dropdown (Minor / Major / Critical). `browser` / `viewport` / `url` captured silently.
  - **Feature:** Use Case ("Why does this matter? What problem does it solve?").
- File upload zone (drag-drop + click-to-browse), enforces the 10 MB / 5 files / extension whitelist client-side, with server-side enforcement as the source of truth.
- On submit: redirects to `/tickets/{id}`.

### 3. `/tickets` — My Tickets list

- Card or table view of the user's tickets (newest first by `updated_at`).
- Each row: ticket ID badge, type icon, title, status badge, priority badge, last-updated relative time.
- Status filter chips at the top.
- Click row → `/tickets/{id}`.
- Empty state with a CTA button to `/tickets/new`.

### 4. `/tickets/{id}` — Ticket detail

- Header: title, `TKT-{id}`, type badge, priority badge, status badge, created/updated timestamps, reporter name (admin-only sees this; the user already knows it's theirs).
- Original description and metadata fields rendered as a read-only summary.
- Original attachments below the description.
- Comment thread, chronological. Regular comments show author + timestamp + body + attachments. System comments (status changes) render in a muted, distinct style: e.g., "Status changed from Open to In Progress · 2h ago".
- Reply composer at the bottom: textarea + attachment uploader. For the reporter on a `RESOLVED` ticket, the composer shows a hint: "Posting a reply will reopen this ticket."
- Admin-only controls: a status dropdown in the header that posts to `PATCH /api/tickets/{id}/status` with an optional note input.

### 5. `/admin/tickets` — Admin Tickets panel

Mirrors the `/admin/users` patterns:
- **Top of page:** Analytics row (see next section).
- **Toolbar:** Search input, filter dropdowns (Status multi-select, Type, Priority multi-select, date range), sort dropdown.
- **Table:** ID, Type, Title (clickable → `/tickets/{id}`), Status, Priority, Reporter, Created, Last Updated. 25 per page with prev/next pagination.
- Direct deep-linkable URL state for filters (`?status=OPEN&type=BUG&q=login`) so admins can bookmark specific views.

## Email design

Five new templates, all routed through the existing `EmailService` interface (extend the interface; implement in `SendGridEmailService`). All emails are plain transactional emails with a clear subject, a short body, and a link.

| Template method | To | Subject | Trigger |
|---|---|---|---|
| `sendTicketCreatedToAdmin(Ticket)` | thowerton@regscale.com | `[OSCAL Hub] New {type}: TKT-{id} — {title}` | User creates ticket. |
| `sendTicketCreatedToReporter(Ticket)` | reporter | `[OSCAL Hub] We received your {type} — TKT-{id}` | User creates ticket. |
| `sendCommentAdded(Ticket, TicketComment, recipient)` | the *other* party | `[OSCAL Hub] New comment on TKT-{id}` | Either party adds a regular (non-system) comment. |
| `sendStatusChanged(Ticket, oldStatus, newStatus, optionalAdminNote)` | reporter | `[OSCAL Hub] TKT-{id} is now {newStatus}` | Admin changes status (any transition). |
| `sendTicketReopened(Ticket, TicketComment)` | thowerton@regscale.com | `[OSCAL Hub] Reopened: TKT-{id} — {title}` | User comment on RESOLVED reopens. |

**Notification matrix recap:**

| Event | Notify Admin | Notify Reporter |
|---|---|---|
| User creates ticket | ✅ | ✅ (receipt) |
| Admin comments | — | ✅ |
| User comments (non-reopen) | ✅ | — |
| Admin changes status (any) | — | ✅ |
| User reopens via comment on RESOLVED | ✅ | — |

**Body template common elements:** ticket link `{app.base-url}/tickets/{id}`, ticket title, type, priority, current status. For comment emails, include the comment body inline. For status-change emails, include `oldStatus → newStatus` and the optional admin note.

**Auto-close events do NOT email** — see Status lifecycle.

**Failure handling:** email send failures are logged but do not roll back the originating database transaction (matches the pattern used by existing email methods like `sendAccessRequestApproved`). A failed email never blocks a ticket action.

## Analytics

The admin analytics payload is returned by `GET /api/admin/tickets/analytics` as a single JSON object with five keys. Frontend renders the panel above the tickets table.

1. **Status counts** — `{ "OPEN": n, "IN_PROGRESS": n, "RESOLVED": n, "CLOSED": n, "WONT_FIX": n, "DUPLICATE": n }`. Six stat cards across the top.
   - SQL: `SELECT status, COUNT(*) FROM tickets GROUP BY status`.
2. **Type split** — `{ "BUG": n, "FEATURE": n }`. Small pie chart or two cards.
   - SQL: `SELECT type, COUNT(*) FROM tickets GROUP BY type`.
3. **Opened per week** — array of `{ "week": "YYYY-MM-DD", "count": n }` for the last 12 weeks.
   - SQL: `SELECT date_trunc('week', created_at)::date AS week, COUNT(*) FROM tickets WHERE created_at > now() - interval '12 weeks' GROUP BY 1 ORDER BY 1`.
4. **Resolved per week** — same shape, on `resolved_at`. Rendered as a second line on the same chart.
   - SQL: `SELECT date_trunc('week', resolved_at)::date AS week, COUNT(*) FROM tickets WHERE resolved_at IS NOT NULL AND resolved_at > now() - interval '12 weeks' GROUP BY 1 ORDER BY 1`.
5. **Stale tickets** — top 20 oldest open or in-progress tickets older than 30 days, returned as a list of `{ id, type, title, priority, created_at, age_days }`.
   - SQL: `SELECT * FROM tickets WHERE status IN ('OPEN','IN_PROGRESS') AND created_at < now() - interval '30 days' ORDER BY created_at ASC LIMIT 20`.

## Implementation slices

This is a single-spec project, but the work breaks naturally into four landable slices that can each be reviewed and merged independently:

1. **Backend foundation** — Flyway `V1.10__ticketing.sql`, JPA entities, repositories, `TicketService`, `TicketController`, `AdminTicketController`, the five new email methods on `EmailService` / `SendGridEmailService`, attachment storage path under the existing GCS bucket. No UI yet. Verifiable end-to-end via Swagger UI.
2. **Frontend ticket flow** — `UserAvatarMenu` extension, `/tickets/new`, `/tickets`, `/tickets/{id}`. End-to-end user-facing flow.
3. **Admin tickets panel** — `/admin/tickets` with search, filter, pagination, and the status-change action.
4. **Analytics + auto-close job** — `/api/admin/tickets/analytics` endpoint, dashboard panel above the admin tickets table, `@Scheduled` auto-close task.

Each slice is independently testable and deployable.

## Key file paths to mirror

- Entity / repo / service / controller pattern: `back-end/src/main/java/gov/nist/oscal/tools/api/{entity,repository,service,controller}/Profile*.java`.
- Email service: `back-end/src/main/java/gov/nist/oscal/tools/api/email/EmailService.java` and `SendGridEmailService.java`.
- Email config / `app.base-url`: `back-end/src/main/java/gov/nist/oscal/tools/api/config/EmailConfig.java`.
- Security / `@PreAuthorize`: `back-end/src/main/java/gov/nist/oscal/tools/api/config/SecurityConfig.java`.
- Migration directory: `back-end/src/main/resources/db/migration/` (next version: `V1.10`).
- User avatar dropdown: `front-end/src/components/UserAvatarMenu.tsx`.
- Admin list pattern (search, filter, pagination, role gate): `front-end/src/app/admin/users/page.tsx`.

## Open decisions deferred to implementation

- Exact Spring `@Scheduled` cron expression and timezone — settle in implementation; default to `0 0 3 * * *` server-local.
- Charting library on the frontend — defer to whatever the existing admin pages already use; do not add a new dependency just for this feature.
- Whether the admin status dropdown should require a confirmation modal for terminal states (`CLOSED` / `WONT_FIX` / `DUPLICATE`) — recommend yes; finalize during UI implementation.
