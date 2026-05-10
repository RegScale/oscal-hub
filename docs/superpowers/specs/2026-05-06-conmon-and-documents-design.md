# Continuous Monitoring & Documents — Authorizations Expansion

**Status:** Draft — pending user review
**Date:** 2026-05-06
**Owner:** Travis Howerton

## Overview

Expand the Authorizations feature with two new capabilities, plus two foundational fixes that this work requires.

**New features:**

1. **Continuous Monitoring (ConMon).** Users upload POAM artifacts (OSCAL or FedRAMP `.xlsx`) on a recurring basis. Each upload is a dated *snapshot*. The latest snapshot is the "current state"; reconciliation diffs latest vs. previous; analytics chart trends across all snapshots over time.
2. **Documents.** Users upload arbitrary supporting artifacts (vuln scans, pen tests, asset inventories, audit reports, etc.) with structured `documentType` metadata so the package's completeness can be evaluated.

**Foundational fixes (required for the above to be usable in a multi-user environment):**

3. **Multi-tenant isolation on `Authorization`.** Today authorizations are scoped only by creator (`authorizedBy`). They MUST be scoped by `organization_id` so they don't leak across tenants.
4. **Per-authorization role-based ACLs.** Within an organization, an authorization is private to its creator by default. Owners (and org admins) grant other org members access at one of four roles: `OWNER`, `EDITOR`, `CONTRIBUTOR`, `VIEWER`.

## Goals

- Make ConMon a first-class artifact stream on the authorization, not a side note.
- Provide reconciliation between consecutive snapshots (new findings, newly closed, reopened, status/severity/due-date changes).
- Provide time-series analytics across the full snapshot history.
- Give users a single place to attach all non-OSCAL supporting documentation, with enough structure to support a future "package completeness" check.
- Enforce organization isolation so authorizations cannot leak across tenants.
- Allow owners to share authorizations with specific org members at specific privilege levels.

## Non-Goals (Out of Scope)

- Email notifications for new high-severity POAMs or expiring documents (sets up cleanly for a follow-on via the existing `EmailService`).
- Automated/scheduled snapshot ingestion (e.g., webhook from Tenable, scheduled SFTP pull). Manual upload only.
- Per-document role-based ACLs (documents inherit the parent authorization's ACLs).
- CSV-format POAM ingestion. OSCAL + FedRAMP Excel only.
- Editing parsed POAM items in-app — the canonical record is the uploaded blob; to "fix" a POAM, re-upload.
- Comments / discussion threads on authorizations or POAM items.
- Backfilling org membership for users who don't have one — this is flagged as a precondition that may surface a small number of legacy authorizations needing a SUPER_ADMIN to assign an org. Bootstrap behavior is fail-noisy, not silent.

## Page Restructure — Tabs

Refactor `front-end/src/app/authorizations/authorization/[authorizationId]/page.tsx` so the existing sections (Details, Digital Signature, Conditions of Approval, Authorization Document) become tab `Overview`. Add two new tabs: `Continuous Monitoring` and `Documents`. URL-routable via `?tab=conmon|documents|overview`. Use the existing shadcn/Radix `Tabs` component already in the project. The `Sharing & Access` block (see ACLs section) lives at the bottom of the Overview tab and is visible only to OWNER / ORG_ADMIN / SUPER_ADMIN.

## Architecture

The system breaks into four backend modules and two frontend tabs:

- **Authorization Access Control** (cross-cutting) — `AuthorizationAccessGuard` service centralizes org-scope + role-grant resolution; every read/write path goes through it.
- **Continuous Monitoring** — parsers (OSCAL + FedRAMP Excel) → snapshot/item entities → reconciliation service → analytics queries.
- **Documents** — document entity + upload reusing existing `FileStorageService`.
- **Frontend tabs** — `Overview` (existing), `ContinuousMonitoringTab`, `DocumentsTab`.

Each backend module exposes its own controller. Each frontend tab is its own React component file. Each parser, the reconciliation service, and the access guard are independently unit-testable.

## Component 1: Multi-Tenant Isolation on Authorization

### Schema change

Add `organization_id` (FK to `organizations`, NOT NULL) to:
- `authorizations`
- `authorization_templates`

### Migration: `V1.6__authorization_org_isolation.sql`

1. Add `organization_id BIGINT` (nullable initially) to both tables.
2. Backfill: for each existing row, set `organization_id` to the creator's primary `organization_membership.organization_id` (the lowest-id membership for the user).
3. Identify any rows that still have a NULL `organization_id` after backfill (creator has no membership). Fail the migration with a clear error message listing the affected IDs. SUPER_ADMIN must resolve those manually (assign membership or delete the orphan record) before re-running. **No silent assignment to a "default" org.**
4. Add `NOT NULL` constraint and FK to `organizations(id)`.
5. Index `(organization_id)` on both tables.

### Service changes

- `AuthorizationService` — every list/get/update/delete query filters by the current user's `organization_id`. Out-of-org access returns 404 (not 403, to avoid leaking existence).
- `AuthorizationTemplateService` — same.
- A new `AuthorizationAccessGuard` helper (see ACLs section) is the single chokepoint.

### Controller changes

- `AuthorizationController` and `AuthorizationTemplateController` invoke `AuthorizationAccessGuard.requireRead/Write/Delete(authId, currentUser)` at the top of every handler.

## Component 2: Per-Authorization Role-Based ACLs

### Roles

Code-only enum `AuthorizationRole`:

| Action                                          | OWNER | EDITOR | CONTRIBUTOR | VIEWER |
|-------------------------------------------------|:-----:|:------:|:-----------:|:------:|
| View authorization, ConMon, Documents           |   ✓   |   ✓    |      ✓      |   ✓    |
| Edit details, conditions, signature             |   ✓   |   ✓    |      ✗      |   ✗    |
| Upload ConMon snapshots                         |   ✓   |   ✓    |      ✓      |   ✗    |
| Upload/edit documents                           |   ✓   |   ✓    |      ✓      |   ✗    |
| Delete ConMon snapshots / documents             |   ✓   |   ✓    |   own only  |   ✗    |
| Manage grants (add/remove users, change roles)  |   ✓   |   ✗    |      ✗      |   ✗    |
| Delete the authorization                        |   ✓   |   ✗    |      ✗      |   ✗    |

### Bypass roles

- `SUPER_ADMIN` — full access to everything in every org.
- `ORG_ADMIN` — implicit `OWNER` on every authorization in their own org.
- `Authorization.authorizedBy` — automatically granted `OWNER` on creation.

### Default visibility

New authorizations are **private** — only the creator + ORG_ADMIN + SUPER_ADMIN can see them. Owner must explicitly add grants or toggle "Share with all org members" to broaden access.

### Schema

#### Migration: `V1.7__authorization_grants.sql`

```sql
CREATE TABLE IF NOT EXISTS authorization_grants (
    id              BIGSERIAL PRIMARY KEY,
    authorization_id BIGINT NOT NULL REFERENCES authorizations(id) ON DELETE CASCADE,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role            VARCHAR(32) NOT NULL,
    granted_by      BIGINT NOT NULL REFERENCES users(id),
    granted_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_authorization_grants_user UNIQUE (authorization_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_authorization_grants_user
    ON authorization_grants (user_id);
CREATE INDEX IF NOT EXISTS idx_authorization_grants_auth
    ON authorization_grants (authorization_id);

ALTER TABLE authorizations
    ADD COLUMN IF NOT EXISTS share_with_org_default_role VARCHAR(32) NULL;
```

`share_with_org_default_role` is a convenience column. When set, every member of the authorization's organization has effective access at that role. We do NOT fan out grant rows — the column is consulted at access-resolution time. This keeps the grants table small for the common "share with whole team" case.

### Entities

- `AuthorizationGrant` (new entity in `entity/`).
- `AuthorizationRole` (enum in `entity/`).
- `Authorization` gets a new `shareWithOrgDefaultRole` field (nullable enum) and `@OneToMany` to grants.

### `AuthorizationAccessGuard` (new service)

Centralizes resolution. Public methods:

- `effectiveRole(authorization, user) → AuthorizationRole | null` — first verifies the user's `organization_id` matches the authorization's `organization_id`; if not, returns null (with the SUPER_ADMIN bypass). Otherwise resolves: SUPER_ADMIN → OWNER, ORG_ADMIN of auth's org → OWNER, creator (`authorizedBy`) → OWNER, explicit `AuthorizationGrant` row → that grant's role, `share_with_org_default_role` set → that role, else null. The highest-privilege match wins (e.g., a user with a VIEWER grant who is also ORG_ADMIN gets OWNER).
- `requireRead(authorization, user)` — throws 404 if effectiveRole is null OR user is not in the authorization's org.
- `requireWriteDetails(authorization, user)` — requires OWNER or EDITOR.
- `requireUploadConMon(authorization, user)` — requires OWNER, EDITOR, or CONTRIBUTOR.
- `requireUploadDocument(authorization, user)` — requires OWNER, EDITOR, or CONTRIBUTOR.
- `requireDeleteOwnedItem(authorization, user, ownerUserId)` — OWNER/EDITOR always pass; CONTRIBUTOR passes iff `ownerUserId == user.id`; otherwise throws 403.
- `requireManageGrants(authorization, user)` — requires OWNER (or ORG_ADMIN/SUPER_ADMIN bypass).
- `requireDelete(authorization, user)` — requires OWNER (or ORG_ADMIN/SUPER_ADMIN bypass).

### Endpoints (new, on `AuthorizationController`)

- `GET    /api/authorizations/{id}/grants` — list grants. OWNER+.
- `POST   /api/authorizations/{id}/grants` — body `{ userId, role }`. OWNER+.
- `PATCH  /api/authorizations/{id}/grants/{grantId}` — body `{ role }`. OWNER+.
- `DELETE /api/authorizations/{id}/grants/{grantId}` — OWNER+. Removing any grant is always safe: the creator's OWNER status comes from `authorizedBy`, not from a grant row, so it can't be revoked through this endpoint.
- `PATCH  /api/authorizations/{id}/share-with-org` — body `{ role: "VIEWER" | "CONTRIBUTOR" | "EDITOR" | null }`. OWNER+. `OWNER` is intentionally not allowed as a share-with-org default — you cannot make every org member a co-owner of an authorization.

### Listing scope

`GET /api/authorizations` returns authorizations where:
- `organization_id` matches the current user's organization, AND
- Current user is `authorizedBy` OR has an `AuthorizationGrant` row OR `share_with_org_default_role IS NOT NULL` OR current user is ORG_ADMIN of that org.

`SUPER_ADMIN` sees all authorizations across orgs.

### Frontend

- New "Sharing & Access" card at the bottom of the Overview tab (rendered only when `effectiveRole === OWNER` — which includes the bypass roles SUPER_ADMIN, ORG_ADMIN, and the creator). Shows:
  - List of current explicit grants: user (avatar + name + email), role dropdown, remove button.
  - "Add user" form: user picker scoped to org members, role dropdown.
  - "Share with all org members as: [None | VIEWER | CONTRIBUTOR | EDITOR]" select.
- The two new tabs (Continuous Monitoring, Documents) conditionally render upload/edit/delete controls based on `effectiveRole`. The role is delivered as a field on `AuthorizationResponse`.

## Component 3: Continuous Monitoring

### Entities

#### `ConMonSnapshot`

| field                  | type                  | notes                                                                                  |
|------------------------|-----------------------|----------------------------------------------------------------------------------------|
| id                     | BIGSERIAL PK          |                                                                                        |
| authorization_id       | FK → authorizations   | NOT NULL, indexed                                                                      |
| uploaded_by            | FK → users            | NOT NULL                                                                               |
| uploaded_at            | TIMESTAMP             | NOT NULL DEFAULT now                                                                   |
| source_format          | VARCHAR(32)           | `OSCAL_JSON`, `OSCAL_XML`, `OSCAL_YAML`, `FEDRAMP_XLSX`                                |
| original_filename      | VARCHAR(512)          |                                                                                        |
| file_storage_path      | VARCHAR(1024)         | path in `FileStorageService` (the original blob is preserved)                          |
| oscal_uuid             | VARCHAR(64) NULL      | the POAM document `uuid` for OSCAL inputs                                              |
| oscal_version          | VARCHAR(16) NULL      |                                                                                        |
| metadata_title         | VARCHAR(512) NULL     |                                                                                        |
| metadata_last_modified | TIMESTAMP NULL        |                                                                                        |
| summary_open_count     | INT                   |                                                                                        |
| summary_closed_count   | INT                   |                                                                                        |
| summary_unknown_count  | INT                   |                                                                                        |
| notes                  | TEXT NULL             | user-entered                                                                           |

Index: `(authorization_id, uploaded_at DESC)`.

#### `ConMonPoamItem`

| field                       | type                | notes                                                          |
|-----------------------------|---------------------|----------------------------------------------------------------|
| id                          | BIGSERIAL PK        |                                                                |
| snapshot_id                 | FK → ConMonSnapshot | NOT NULL, ON DELETE CASCADE                                    |
| external_id                 | VARCHAR(128)        | OSCAL `uuid` or FedRAMP "POA&M Item ID"                        |
| title                       | VARCHAR(1024)       |                                                                |
| description                 | TEXT NULL           |                                                                |
| status                      | VARCHAR(16)         | `OPEN`, `CLOSED`, `UNKNOWN`                                    |
| raw_status                  | VARCHAR(64) NULL    | original string before mapping                                 |
| severity                    | VARCHAR(16) NULL    | `LOW`, `MODERATE`, `HIGH`, `CRITICAL`                          |
| weakness_source             | VARCHAR(256) NULL   |                                                                |
| scheduled_completion_date   | DATE NULL           |                                                                |
| actual_completion_date      | DATE NULL           |                                                                |
| point_of_contact            | VARCHAR(256) NULL   |                                                                |
| risk_rating                 | VARCHAR(64) NULL    |                                                                |
| extra_props_json            | JSONB NULL          | catch-all for unmodeled fields                                 |

Indexes: `(snapshot_id, status)`, `(snapshot_id, external_id)`.

#### `ConMonReconciliation`

| field                | type                 |
|----------------------|----------------------|
| id                   | BIGSERIAL PK         |
| snapshot_id          | FK → ConMonSnapshot  |
| previous_snapshot_id | FK → ConMonSnapshot  |
| new_count            | INT                  |
| closed_count         | INT                  |
| reopened_count       | INT                  |
| still_open_count     | INT                  |
| removed_count        | INT                  |
| changed_count        | INT                  |

The detailed per-item diff is computed on demand from the two snapshots' items, not persisted as rows.

### Migration: `V1.8__conmon.sql`

Creates the three tables above with `IF NOT EXISTS`. JPA entities use `@ColumnDefault` for fields with SQL defaults so Hibernate validate matches.

### Status derivation (the hybrid C rule)

For each parsed item, determine status in this order:

1. Look for `prop` with `name = "status"` (any namespace, including `https://fedramp.gov/ns/oscal`):
   - `completed`, `closed`, `false-positive`, `not-applicable` → `CLOSED`
   - `ongoing`, `open`, `risk-accepted`, `operational-requirement`, `pending` → `OPEN`
   - any other value → record `raw_status`, set `status = UNKNOWN`
2. If no status prop and the item references findings/risks → roll up: every linked finding/risk has a closed status → `CLOSED`; any open → `OPEN`.
3. Otherwise → `UNKNOWN`.

For FedRAMP Excel: the sheet name is the source of truth — items in `Open POA&M Items` sheet are `OPEN`; items in `Closed POA&M Items` sheet are `CLOSED`. Status props in those sheets are still recorded in `raw_status`.

### Reconciliation algorithm

When a new snapshot is created and a prior snapshot exists for this authorization:

1. Match items by `external_id`.
2. For each match, compare: `status`, `severity`, `scheduled_completion_date`, `title`. Record `fieldsChanged` list.
3. Categorize:
   - `new` — in current, not in prior
   - `closed` — in both, prior `OPEN` → curr `CLOSED`
   - `reopened` — in both, prior `CLOSED` → curr `OPEN`
   - `still_open` — in both, prior `OPEN` and curr `OPEN`
   - `removed` — in prior, not in current (rare; data quality flag)
   - `changed` — in both, any of the compared fields differs (excluding the status flips already counted above)

Persist counts on `ConMonReconciliation`. The detailed per-item diff is recomputed on demand by `GET /reconciliation`.

### Parsers

- `OscalPoamParser` — uses existing `OscalBindingContext.instance()`. Parses JSON/XML/YAML. Detects format from file extension first, falls back to content sniffing. Walks `plan-of-action-and-milestones.poam-items[]`, extracts modeled fields, applies status derivation, copies unmodeled props into `extra_props_json`.
- `FedrampPoamExcelParser` — Apache POI. Reads `Open POA&M Items` and `Closed POA&M Items` sheets. Header row defines column → field mapping (configurable defaults match FedRAMP template v3+). Unknown columns → `extra_props_json`. If POI is not already a transitive dep of the project, add `org.apache.poi:poi-ooxml`.

Format detection:
- Extension `.json`, `.xml`, `.yaml`, `.yml` → OSCAL
- Extension `.xlsx` → FedRAMP Excel
- Otherwise → reject with `"Unsupported file type. Use OSCAL JSON/XML/YAML or the FedRAMP POA&M Excel template. For other artifacts, use the Documents tab."`

### Endpoints (new `ContinuousMonitoringController`)

- `POST   /api/authorizations/{id}/conmon/snapshots` — multipart `file` + optional `notes`. Returns the created snapshot summary + reconciliation summary if applicable.
- `GET    /api/authorizations/{id}/conmon/snapshots` — list (lightweight).
- `GET    /api/authorizations/{id}/conmon/snapshots/{snapshotId}` — single snapshot detail.
- `GET    /api/authorizations/{id}/conmon/snapshots/{snapshotId}/items?status=OPEN&severity=HIGH&q=...&page=0&size=50` — filtered items.
- `GET    /api/authorizations/{id}/conmon/snapshots/{snapshotId}/reconciliation` — full diff (lazy-computed, summary always present, detailed item lists computed from current+prev tables).
- `GET    /api/authorizations/{id}/conmon/snapshots/{snapshotId}/download` — original blob.
- `DELETE /api/authorizations/{id}/conmon/snapshots/{snapshotId}` — guard via `requireDeleteOwnedItem`.
- `GET    /api/authorizations/{id}/conmon/analytics` — time-series data: `{ openCountSeries[{date, open, closed, unknown}], severitySeriesByDate[{date, low, moderate, high, critical}], agingBuckets[{bucket, count}], meanTimeToCloseDays }`.

### Frontend — Continuous Monitoring tab

Layout, top-down:

1. **Header row** — title + "Upload New Snapshot" button (drag-and-drop dropzone above table also supported).
2. **Hero KPI tiles** — `Open` / `Closed` / `Unknown` / `Last Snapshot` / `Δ vs. prior month`. Sourced from latest snapshot.
3. **Reconciliation banner** (visible when ≥ 2 snapshots exist) — "Since [prev_date]: 12 new, 8 newly closed, 2 reopened, 35 still open, 1 removed." Click → expandable per-item diff table with columns `Status` `Title` `Severity` `Old → New` `Field changed`.
4. **Analytics dashboard** (Recharts via existing `LazyCharts`):
   - Line chart: open POAM count over time (one data point per snapshot).
   - Stacked bar: severity breakdown over time.
   - Donut: current snapshot status breakdown.
   - Bar: aging buckets — `<30d`, `30–60`, `60–90`, `90–180`, `>180` open.
   - Tile: mean time to close.
5. **Snapshot history table** — Date, Uploader, Format, Open/Closed/Unknown, Reconciliation summary, actions (View items → opens drawer, Download original, Delete).
6. **Items drawer** — opens from a row in the history table OR from clicking a KPI tile. Paginated, filterable item list (status, severity, free-text search across `title`/`external_id`).

### Aging bucket cutoffs

`<30d`, `30–60`, `60–90`, `90–180`, `>180` open relative to `scheduled_completion_date` (if set) or `uploaded_at` of first snapshot the item appeared in (if not). User can suggest different cutoffs in review; not blocked on this.

## Component 4: Documents

### Entity

#### `AuthorizationDocument`

| field             | type                  | notes                                                              |
|-------------------|-----------------------|--------------------------------------------------------------------|
| id                | BIGSERIAL PK          |                                                                    |
| authorization_id  | FK → authorizations   | NOT NULL, indexed                                                  |
| uploaded_by       | FK → users            | NOT NULL                                                           |
| uploaded_at       | TIMESTAMP             | NOT NULL DEFAULT now                                               |
| original_filename | VARCHAR(512)          |                                                                    |
| file_size         | BIGINT                |                                                                    |
| content_type      | VARCHAR(128)          |                                                                    |
| storage_path      | VARCHAR(1024)         | path in `FileStorageService`                                       |
| document_type     | VARCHAR(64)           | enum, see below                                                    |
| description       | TEXT NULL             |                                                                    |
| tags              | VARCHAR(512) NULL     | comma-separated free text                                          |
| version           | VARCHAR(64) NULL      | e.g., "v2.1"                                                       |
| effective_date    | DATE NULL             |                                                                    |
| expires_at        | DATE NULL             |                                                                    |

Index: `(authorization_id, uploaded_at DESC)`, `(authorization_id, document_type)`.

#### Document type enum

`VULNERABILITY_SCAN`, `PENETRATION_TEST`, `ASSET_INVENTORY`, `SSP`, `SAR`, `CONFIGURATION_BASELINE`, `CONTINGENCY_PLAN`, `INCIDENT_RESPONSE_PLAN`, `AUDIT_REPORT`, `AUTHORIZATION_LETTER`, `CHANGE_NOTICE_TICKET`, `RISK_ASSESSMENT`, `BUSINESS_CONTINUITY_PLAN`, `DISASTER_RECOVERY_PLAN`, `BUSINESS_IMPACT_ASSESSMENT`, `OTHER`.

### Migration: `V1.9__authorization_documents.sql`

Creates the table with `IF NOT EXISTS`. `@ColumnDefault` annotations on the entity match SQL defaults.

### File constraints

- Max file size: **50 MB** per upload (configurable via `app.documents.max-file-size`, default 50 MB).
- Allowed content types: PDF; Office (`.doc`/`.docx`/`.xls`/`.xlsx`/`.ppt`/`.pptx`); CSV; plain text; images (`.png`/`.jpg`/`.jpeg`); archives (`.zip`); OSCAL formats (`.json`/`.xml`/`.yaml`/`.yml`).
- Blocked: executables (`.exe`/`.bat`/`.sh`/`.dll`/`.so`), scripts that the browser would interpret as HTML.
- Storage path: `authorizations/{authorizationId}/documents/{uuid}-{originalFilename}` via `FileStorageService` (local/Azure/GCS/S3 — already configured per existing pattern).

### Endpoints (new `AuthorizationDocumentsController`)

- `POST   /api/authorizations/{id}/documents` — multipart `file` + JSON metadata (`documentType`, `description`, `tags`, `version`, `effectiveDate`, `expiresAt`).
- `GET    /api/authorizations/{id}/documents?type=...&q=...` — list, filterable.
- `GET    /api/authorizations/{id}/documents/{docId}` — metadata.
- `GET    /api/authorizations/{id}/documents/{docId}/download` — blob.
- `PATCH  /api/authorizations/{id}/documents/{docId}` — edit metadata only (not the blob — re-upload to change content).
- `DELETE /api/authorizations/{id}/documents/{docId}` — guard via `requireDeleteOwnedItem`.

### Frontend — Documents tab

1. **Upload panel** at top — drag-and-drop + "Upload Document" button → modal with file picker + metadata form (Document Type required; description / tags / version / dates optional).
2. **Filter row** — Document Type dropdown, free-text search (matches filename/description/tags), uploader filter.
3. **Documents table** — columns: Type badge, Filename, Description (truncated), Version, Uploaded By, Uploaded At, Effective Date, Expires At (with red "Expired" badge when in the past), Size, actions (Download, Edit metadata, Delete).
4. **Package completeness panel** (left rail or top banner) — checklist of "core" document types: `VULNERABILITY_SCAN`, `PENETRATION_TEST`, `SSP`, `SAR`, `CONTINGENCY_PLAN`, `AUTHORIZATION_LETTER`, `RISK_ASSESSMENT`. Green check if at least one non-expired document of that type exists; red X otherwise. Count badge shows how many are present.

## API Response Shape Changes

`AuthorizationResponse` gains:

- `effectiveRole: "OWNER" | "EDITOR" | "CONTRIBUTOR" | "VIEWER"` — the current user's role on this authorization.
- `organizationId: number`
- `shareWithOrgDefaultRole: AuthorizationRole | null`
- `latestConMonSnapshotSummary: { id, uploadedAt, openCount, closedCount, unknownCount } | null` — convenience for the list page.
- `documentsCount: number` — convenience for the list page.

## Error Handling

- Unsupported upload format → `400` with the message above.
- File too large → `413`.
- Out-of-org authorization access → `404` (don't leak existence).
- Insufficient role for write → `403` with role-name in body.
- Parser failure (corrupt OSCAL, malformed Excel) → `422` with parser error detail; original blob is NOT stored (we don't keep junk).
- Storage backend failure → `500`, transactional rollback (entity not created if blob save fails).
- Reconciliation failure → snapshot is still created; reconciliation row is null and the API returns the snapshot with `reconciliation: null` and a structured warning. (Better to capture the snapshot than lose it.)

## Testing

### Backend

- `OscalPoamParserTest` — sample fixtures for JSON/XML/YAML, including:
  - Items with status props (each FedRAMP value), items without, items with linked findings.
  - Items with no status info → `UNKNOWN`.
  - Edge case: empty `poam-items[]`.
- `FedrampPoamExcelParserTest` — fixtures matching FedRAMP template v3+:
  - Both sheets populated.
  - Only Open sheet populated (no Closed sheet).
  - Custom columns → `extra_props_json`.
  - Missing required columns → `422`.
- `ConMonReconciliationServiceTest` — six diff cases (new, closed, reopened, still_open, removed, changed) plus combinations.
- `AuthorizationAccessGuardTest` — every (role × action) combination from the permissions matrix; bypass behavior for SUPER_ADMIN, ORG_ADMIN, creator; share-with-org default role; out-of-org user → 404; nonexistent authorization → 404.
- `AuthorizationDocumentsControllerTest` — `@SpringBootTest` integration tests for upload, list, filter, download, delete; file-size enforcement; blocked content type; cross-org access denial.
- `ContinuousMonitoringControllerTest` — `@SpringBootTest` for upload (OSCAL + Excel), reconciliation, analytics; cross-org access denial.
- `AuthorizationOrgIsolationTest` — list/get/update never returns or accepts an authorization from a different org.

### Frontend

- Unit tests:
  - `ReconciliationBanner` component renders correct counts and diff table from a given reconciliation payload.
  - Analytics chart data transforms (snapshots → series) correct shape.
  - Package completeness checklist correctly handles expired documents (does not count them as present).
  - Sharing & Access dialog renders only for OWNER+.
- Skip Playwright E2E for v1 — matches what other authorization features ship with.

## Migration Order (single PR or separate?)

Recommended: **separate, sequential PRs** to keep blast radius small:

1. **PR 1** — Multi-tenant isolation on Authorization (`V1.6`). Backfills + adds `organization_id` + service-layer scoping. No user-visible UI change.
2. **PR 2** — Authorization grants + ACL guard (`V1.7`). New "Sharing & Access" UI on Overview tab. Tabbed page refactor lands here.
3. **PR 3** — Documents (`V1.9`). New tab + endpoints + UI.
4. **PR 4** — Continuous Monitoring (`V1.8`). Parsers + reconciliation + analytics.

(Migration version numbers don't need to match merge order — Flyway runs them in version order regardless. Numbering: `V1.6` org-isolation, `V1.7` grants, `V1.8` conmon, `V1.9` documents.)

## Open Questions / Follow-ons

- **Aging bucket cutoffs**: defaulting to `<30/30-60/60-90/90-180/>180`. May want FedRAMP-aligned `<30/30-60/60-90/90-120/>120` instead — easy config knob, not blocking v1.
- **Required-document set for the package completeness panel**: defaulting to `VULNERABILITY_SCAN, PENETRATION_TEST, SSP, SAR, CONTINGENCY_PLAN, AUTHORIZATION_LETTER, RISK_ASSESSMENT`. Should likely be configurable per authorization template later.
- **Notifications**: high-severity new POAMs and expiring documents are obvious follow-on use cases via the existing `EmailService`.
- **Scheduled ingestion**: webhook/SFTP pull from scanners is a future concern.
- **Document version history**: today, "version" is a free-text field on a single document row. A future enhancement could supersede prior documents of the same type/title and show a version timeline.
