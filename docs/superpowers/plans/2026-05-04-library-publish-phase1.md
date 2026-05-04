# Library Publish — Phase 1: Visibility + Save-to-Library Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add three-tier visibility (`PRIVATE | ORGANIZATION | PUBLIC`) to library items, wire a "Save to Library" action into every builder (catalog / profile / component / SSP / AP / AR / POA&M), and enforce visibility on every existing library read. No public-facing surface in this phase — that's Phase 2. No API keys — that's Phase 3.

**Architecture:** Schema migration adds `visibility`, `organization_id`, `source_type`, `source_id`, `published_at`, `last_published_at` to `library_items`. A new `LibraryIngestService` performs idempotent saves keyed on `(creator, source_type, source_id)` — first save creates the item + version 1, subsequent saves append a new `library_versions` row. A `canRead` predicate gates all reads. A new `PATCH /api/library/{itemId}/visibility` endpoint covers publish / unpublish / share-with-org. Each builder controller gets a `POST /save-to-library` endpoint that delegates to `LibraryIngestService` via a per-builder `SourceContentResolver`.

**Tech Stack:** Spring Boot 3.5.9 (existing), JPA/Hibernate with `ddl-auto=validate`, Flyway, PostgreSQL, JUnit 5 + Spring Boot Test, Next.js 13+ App Router, React, TypeScript.

**Spec:** `docs/superpowers/specs/2026-05-04-library-publishing-design.md`

**Branch policy:** Work continues on `ai-foundation`. Commit after each task. CLAUDE.md authorizes Maven and npm builds (per memory `feedback_build_policy.md`).

---

## Phase 1 task map

| # | Task | Group |
|---|---|---|
| 1 | Add Visibility + SourceType enums | Backend foundation |
| 2 | V1.1 migration: visibility/source/published columns + indexes | Backend foundation |
| 3 | Extend `LibraryItem` entity with new fields | Backend foundation |
| 4 | Backfill existing rows to PRIVATE (verified by integration test) | Backend foundation |
| 5 | Add `findByCreatorAndSource` to `LibraryItemRepository` | Backend foundation |
| 6 | Add new `AuditEventType` enum values | Backend foundation |
| 7 | V1.2 migration: extend `audit_events` CHECK constraint | Backend foundation |
| 8 | `canRead` helper + unit tests | Visibility enforcement |
| 9 | Apply visibility filter to list/search/recent/popular endpoints | Visibility enforcement |
| 10 | Apply `canRead` to single-item endpoints (404 on miss) | Visibility enforcement |
| 11 | Integration test: visibility matrix end-to-end | Visibility enforcement |
| 12 | `VisibilityChangeRequest` DTO | Visibility endpoint |
| 13 | `PATCH /api/library/{itemId}/visibility` endpoint + service + tests | Visibility endpoint |
| 14 | `SourceContentResolver` interface + `SourceContent` record | Save-to-Library backend |
| 15 | `CatalogSourceResolver` + unit test | Save-to-Library backend |
| 16 | `ProfileSourceResolver` + unit test | Save-to-Library backend |
| 17 | `ComponentDefinitionSourceResolver` + unit test | Save-to-Library backend |
| 18 | `OscalDocumentSourceResolver` (SSP/AP/AR/POAM) + unit test | Save-to-Library backend |
| 19 | `LibraryIngestService` + unit tests | Save-to-Library backend |
| 20 | `SaveToLibraryRequest` DTO + DTO test | Save-to-Library backend |
| 21 | `POST /api/build/catalogs/{id}/save-to-library` endpoint + integration test | Save-to-Library backend |
| 22 | `POST /api/build/profiles/{id}/save-to-library` endpoint + integration test | Save-to-Library backend |
| 23 | `POST /api/build/component-definitions/{id}/save-to-library` endpoint + integration test | Save-to-Library backend |
| 24 | `POST /api/build/oscal-documents/{id}/save-to-library` endpoint + integration test | Save-to-Library backend |
| 25 | Frontend API client: visibility & save-to-library methods | Frontend |
| 26 | `<VisibilityBadge>` component + test | Frontend |
| 27 | `<SaveToLibraryModal>` component + test | Frontend |
| 28 | Visibility column + filter on `/library` page | Frontend |
| 29 | Visibility action menu (publish/unpublish/share-with-org) on `/library` rows | Frontend |
| 30 | "This item is public" banner on owned PUBLIC items | Frontend |
| 31 | Wire `<SaveToLibraryModal>` into the catalog builder | Frontend |
| 32 | Wire `<SaveToLibraryModal>` into the profile builder | Frontend |
| 33 | Wire `<SaveToLibraryModal>` into the component builder | Frontend |
| 34 | Wire `<SaveToLibraryModal>` into the SSP/AP/AR/POAM builder | Frontend |
| 35 | Smoke test: full round-trip from each builder to library | Verification |

**Migration version numbers:** the next two migrations are `V1.1` and `V1.2`. Confirm before writing by running `ls back-end/src/main/resources/db/migration/`. If the highest existing is something else, increment from there.

---

## File structure

### Backend — new files

```
back-end/src/main/java/gov/nist/oscal/tools/api/
├── entity/
│   ├── Visibility.java                                  # enum: PRIVATE, ORGANIZATION, PUBLIC
│   └── SourceType.java                                  # enum: CATALOG, PROFILE, SSP, AP, AR, POAM, COMPONENT_DEFINITION
├── model/library/
│   ├── SaveToLibraryRequest.java                        # title, description, tags, visibility, organizationId
│   ├── VisibilityChangeRequest.java                     # visibility, organizationId, reason
│   └── SourceContent.java                               # record (byte[] bytes, String format, String filename)
├── service/library/
│   ├── LibraryIngestService.java                        # orchestrates save-to-library
│   ├── SourceContentResolver.java                       # interface
│   ├── CatalogSourceResolver.java
│   ├── ProfileSourceResolver.java
│   ├── ComponentDefinitionSourceResolver.java
│   └── OscalDocumentSourceResolver.java                 # serves SSP, AP, AR, POAM
└── (test mirrors)
    └── service/library/
        ├── LibraryIngestServiceTest.java
        ├── CatalogSourceResolverTest.java
        ├── ProfileSourceResolverTest.java
        ├── ComponentDefinitionSourceResolverTest.java
        └── OscalDocumentSourceResolverTest.java

back-end/src/main/resources/db/migration/
├── V1.1__library_visibility_and_source.sql
└── V1.2__library_audit_event_types.sql

back-end/src/test/java/gov/nist/oscal/tools/api/
├── service/LibraryServiceVisibilityTest.java
└── controller/
    ├── LibraryVisibilityControllerTest.java
    └── SaveToLibraryControllerTest.java
```

### Backend — modified files

```
back-end/src/main/java/gov/nist/oscal/tools/api/
├── entity/LibraryItem.java                              # add fields + getters/setters
├── repository/LibraryItemRepository.java                # add findByCreatorAndSource + visibility-aware queries
├── service/LibraryService.java                          # canRead + visibility-aware list/get
├── controller/LibraryController.java                    # PATCH /visibility endpoint
├── controller/CatalogController.java                    # POST /save-to-library
├── controller/ProfileController.java                    # POST /save-to-library
├── controller/ComponentDefinitionController.java        # POST /save-to-library
├── controller/OscalDocumentController.java              # POST /save-to-library
└── model/AuditEventType.java                            # add 4 new enum values
```

### Frontend — new files

```
front-end/src/
├── components/library/
│   ├── SaveToLibraryModal.tsx
│   ├── VisibilityBadge.tsx
│   └── VisibilityActionMenu.tsx
├── lib/api/library.ts                                   # if not already present, otherwise extend
└── (test mirrors)
    └── __tests__/components/library/
        ├── SaveToLibraryModal.test.tsx
        └── VisibilityBadge.test.tsx
```

### Frontend — modified files

```
front-end/src/
├── app/library/page.tsx                                 # visibility column + filter + action menu
└── app/build/...                                        # wire SaveToLibraryModal into each builder's save step
    ├── (catalog)/                                       # exact path determined during Task 31
    ├── (profile)/
    ├── (component)/
    └── (SSP/AP/AR/POAM)/
```

---

## Group A — Backend foundation

### Task 1: Add `Visibility` and `SourceType` enums

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/Visibility.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/SourceType.java`

- [ ] **Step 1: Create `Visibility.java`**

```java
package gov.nist.oscal.tools.api.entity;

/**
 * Three-tier access control for library items.
 * PRIVATE: only the creator
 * ORGANIZATION: anyone in the same organization
 * PUBLIC: visible at the public catalog and via the public API
 */
public enum Visibility {
    PRIVATE,
    ORGANIZATION,
    PUBLIC
}
```

- [ ] **Step 2: Create `SourceType.java`**

```java
package gov.nist.oscal.tools.api.entity;

/**
 * Identifies which builder produced a library item. Soft pointer — the source
 * row may be deleted independently and the library item survives.
 */
public enum SourceType {
    CATALOG,
    PROFILE,
    SSP,
    AP,
    AR,
    POAM,
    COMPONENT_DEFINITION
}
```

- [ ] **Step 3: Verify compile**

```bash
cd back-end && mvn -q compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/entity/Visibility.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/entity/SourceType.java
git commit -m "feat(library): add Visibility and SourceType enums"
```

---

### Task 2: V1.1 migration — visibility, source pointer, published timestamps

**Files:**
- Create: `back-end/src/main/resources/db/migration/V1.1__library_visibility_and_source.sql`

- [ ] **Step 1: Confirm next migration version**

```bash
ls back-end/src/main/resources/db/migration/
```

Expected: `V1.0__baseline.sql` is the only file. New migrations will be `V1.1` and `V1.2`. If anything else exists, bump accordingly throughout this plan.

- [ ] **Step 2: Write the migration**

```sql
-- V1.1__library_visibility_and_source.sql
-- Adds three-tier visibility, source-pointer linkage to builder rows,
-- and publish timestamps to library_items.

ALTER TABLE library_items
    ADD COLUMN IF NOT EXISTS visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    ADD COLUMN IF NOT EXISTS organization_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(40) NULL,
    ADD COLUMN IF NOT EXISTS source_id UUID NULL,
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMP NULL,
    ADD COLUMN IF NOT EXISTS last_published_at TIMESTAMP NULL;

ALTER TABLE library_items
    DROP CONSTRAINT IF EXISTS library_items_visibility_check;
ALTER TABLE library_items
    ADD CONSTRAINT library_items_visibility_check
    CHECK (visibility IN ('PRIVATE', 'ORGANIZATION', 'PUBLIC'));

ALTER TABLE library_items
    DROP CONSTRAINT IF EXISTS library_items_source_type_check;
ALTER TABLE library_items
    ADD CONSTRAINT library_items_source_type_check
    CHECK (source_type IS NULL OR source_type IN
        ('CATALOG','PROFILE','SSP','AP','AR','POAM','COMPONENT_DEFINITION'));

ALTER TABLE library_items
    DROP CONSTRAINT IF EXISTS library_items_organization_fk;
ALTER TABLE library_items
    ADD CONSTRAINT library_items_organization_fk
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_library_items_visibility_type
    ON library_items(visibility, oscal_type);
CREATE INDEX IF NOT EXISTS idx_library_items_visibility_org
    ON library_items(visibility, organization_id);
CREATE INDEX IF NOT EXISTS idx_library_items_source
    ON library_items(created_by, source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_library_items_search_fts
    ON library_items USING GIN (
        to_tsvector('english', coalesce(title,'') || ' ' || coalesce(description,''))
    );
```

**Note:** existing rows are backfilled to `'PRIVATE'` automatically via the column's `DEFAULT`. The `organization_id` requirement when `visibility='ORGANIZATION'` is enforced in service code, not via SQL — see spec §6.1.

- [ ] **Step 3: Run the application to apply the migration**

```bash
./stop.sh && ./dev.sh
```

Watch the backend log for `Successfully applied 1 migration to schema "public"`. Backend should reach `Started OscalCliApiApplication` without `SchemaManagementException`.

- [ ] **Step 4: Verify the schema**

```bash
docker exec oscal-postgres-dev psql -U oscal_user -d oscal_dev -c \
  "SELECT column_name, data_type, column_default FROM information_schema.columns \
   WHERE table_name='library_items' AND column_name IN \
   ('visibility','organization_id','source_type','source_id','published_at','last_published_at');"
```

Expected: 6 rows. `visibility` has default `'PRIVATE'::character varying`.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/resources/db/migration/V1.1__library_visibility_and_source.sql
git commit -m "feat(db): add visibility/source/publish columns to library_items"
```

---

### Task 3: Extend `LibraryItem` entity

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/LibraryItem.java`

- [ ] **Step 1: Add the imports**

Add near the top of the file:

```java
import org.hibernate.annotations.ColumnDefault;
import java.util.UUID;
```

- [ ] **Step 2: Add fields after `viewCount` (current end of fields, around line 64)**

```java
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'PRIVATE'")
    private Visibility visibility = Visibility.PRIVATE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 40)
    private SourceType sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "last_published_at")
    private LocalDateTime lastPublishedAt;
```

- [ ] **Step 3: Add getters/setters at the end of the class**

```java
    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
        this.updatedAt = LocalDateTime.now();
    }

    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }

    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }

    public UUID getSourceId() { return sourceId; }
    public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public LocalDateTime getLastPublishedAt() { return lastPublishedAt; }
    public void setLastPublishedAt(LocalDateTime lastPublishedAt) { this.lastPublishedAt = lastPublishedAt; }
```

- [ ] **Step 4: Restart and confirm the entity validates against the schema**

```bash
./stop.sh && ./dev.sh
```

Backend must start without `SchemaManagementException: Schema-validation: missing column ...`. If that error appears, the entity's `@Column(name=...)` does not match the migration — fix.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/entity/LibraryItem.java
git commit -m "feat(library): add visibility, source pointer, publish timestamps to LibraryItem"
```

---

### Task 4: Verify backfill — integration test

**Files:**
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/service/LibraryItemBackfillTest.java`

- [ ] **Step 1: Write the integration test**

```java
package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.Visibility;
import gov.nist.oscal.tools.api.repository.LibraryItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LibraryItemBackfillTest {

    @Autowired
    LibraryItemRepository repo;

    @Test
    void allExistingRowsDefaultToPrivate() {
        List<LibraryItem> all = repo.findAll();
        // The point of this test is to assert the migration's DEFAULT clause did
        // its job. If the project ships with seed data, all of it must be PRIVATE.
        // If empty, the assertion is vacuously true and that's fine.
        assertThat(all).allMatch(i -> i.getVisibility() == Visibility.PRIVATE);
    }
}
```

- [ ] **Step 2: Run the test**

```bash
cd back-end && mvn -q test -Dtest=LibraryItemBackfillTest
```

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add back-end/src/test/java/gov/nist/oscal/tools/api/service/LibraryItemBackfillTest.java
git commit -m "test(library): verify migration backfills existing rows to PRIVATE"
```

---

### Task 5: Repository — `findByCreatorAndSource`

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/LibraryItemRepository.java`

- [ ] **Step 1: Read the file to find the right place to insert**

```bash
grep -n "interface LibraryItemRepository" back-end/src/main/java/gov/nist/oscal/tools/api/repository/LibraryItemRepository.java
```

- [ ] **Step 2: Add the query method inside the interface**

```java
    /**
     * Looks up a library item by its creator and the builder source it was saved from.
     * Used by LibraryIngestService to decide between create-new vs append-version.
     */
    java.util.Optional<LibraryItem> findByCreatedBy_IdAndSourceTypeAndSourceId(
        Long createdById,
        gov.nist.oscal.tools.api.entity.SourceType sourceType,
        java.util.UUID sourceId);
```

- [ ] **Step 3: Verify compile**

```bash
cd back-end && mvn -q compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/repository/LibraryItemRepository.java
git commit -m "feat(library): add findByCreatedBy_IdAndSourceTypeAndSourceId query"
```

---

### Task 6: Add new `AuditEventType` enum values

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/model/AuditEventType.java`

- [ ] **Step 1: Locate the closing entry**

The current last entry is `ORG_CREATED`. Replace its trailing `;` with `,` and append the new entries before the field declarations (around line 424–425).

- [ ] **Step 2: Add the new enum values**

```java
    /**
     * Library item visibility changed to PUBLIC by its creator.
     * <p>Risk Level: MEDIUM (data exposure)</p>
     * <p>Retention: LONG (compliance, content provenance)</p>
     */
    LIBRARY_ITEM_PUBLISHED("Library", "Library item published", "MEDIUM"),

    /**
     * Library item visibility changed away from PUBLIC by its creator.
     * <p>Risk Level: LOW</p>
     * <p>Retention: LONG (content provenance)</p>
     */
    LIBRARY_ITEM_UNPUBLISHED("Library", "Library item unpublished by creator", "LOW"),

    /**
     * Library item force-unpublished by SUPER_ADMIN.
     * <p>Risk Level: HIGH (admin override)</p>
     * <p>Retention: LONG (compliance, takedown traceability)</p>
     */
    LIBRARY_ITEM_FORCE_UNPUBLISHED("Library", "Library item force-unpublished by admin", "HIGH"),

    /**
     * Library item visibility changed (any transition not covered above).
     * <p>Risk Level: LOW</p>
     * <p>Retention: MEDIUM</p>
     */
    LIBRARY_ITEM_VISIBILITY_CHANGED("Library", "Library item visibility changed", "LOW");
```

(Note: the previous `ORG_CREATED("...", "...", "LOW");` becomes `ORG_CREATED("...", "...", "LOW"),`.)

- [ ] **Step 3: Compile**

```bash
cd back-end && mvn -q compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/model/AuditEventType.java
git commit -m "feat(audit): add LIBRARY_ITEM_* event types"
```

---

### Task 7: V1.2 migration — extend `audit_events.event_type` CHECK constraint

**Files:**
- Create: `back-end/src/main/resources/db/migration/V1.2__library_audit_event_types.sql`

- [ ] **Step 1: Capture the existing CHECK values**

```bash
grep "audit_events_event_type_check CHECK" \
  back-end/src/main/resources/db/migration/V1.0__baseline.sql
```

Note the full list of values currently allowed.

- [ ] **Step 2: Write the migration**

```sql
-- V1.2__library_audit_event_types.sql
-- Extends audit_events.event_type CHECK constraint with library-related events.
-- Mirrors values added to gov.nist.oscal.tools.api.model.AuditEventType.

ALTER TABLE audit_events
    DROP CONSTRAINT IF EXISTS audit_events_event_type_check;

ALTER TABLE audit_events
    ADD CONSTRAINT audit_events_event_type_check
    CHECK (event_type IN (
        -- Existing values from V1.0 (preserved verbatim)
        'AUTH_REGISTER_SUCCESS', 'AUTH_REGISTER_FAILURE',
        'AUTH_LOGIN_SUCCESS', 'AUTH_LOGIN_FAILURE',
        'AUTH_LOGOUT', 'AUTH_TOKEN_REFRESH',
        'AUTH_SERVICE_TOKEN_GENERATED', 'AUTH_ORG_SELECTION',
        'AUTHZ_ACCESS_DENIED', 'AUTHZ_ACCESS_GRANTED', 'AUTHZ_PERMISSION_CHANGED',
        'DATA_FILE_UPLOAD', 'DATA_FILE_ACCESS', 'DATA_FILE_DELETE',
        'DATA_FILE_MODIFY', 'DATA_VALIDATION', 'DATA_CONVERSION', 'DATA_PROFILE_RESOLVE',
        'CONFIG_PROFILE_UPDATE', 'CONFIG_PASSWORD_CHANGE',
        'CONFIG_LOGO_UPLOAD', 'CONFIG_SYSTEM_CHANGE',
        'SECURITY_ACCOUNT_LOCKED', 'SECURITY_ACCOUNT_UNLOCKED',
        'SECURITY_IP_BLOCKED', 'SECURITY_PASSWORD_RESET_REQUEST',
        'SECURITY_PASSWORD_RESET_COMPLETE', 'SECURITY_SUSPICIOUS_ACTIVITY',
        'SECURITY_RATE_LIMIT_EXCEEDED', 'SECURITY_INVALID_FILE_UPLOAD',
        'SYSTEM_STARTUP', 'SYSTEM_SHUTDOWN', 'SYSTEM_ERROR',
        'SYSTEM_DATABASE_ERROR', 'SYSTEM_EXTERNAL_API_ERROR',
        -- New values added in this migration
        'LIBRARY_ITEM_PUBLISHED',
        'LIBRARY_ITEM_UNPUBLISHED',
        'LIBRARY_ITEM_FORCE_UNPUBLISHED',
        'LIBRARY_ITEM_VISIBILITY_CHANGED'
    ));
```

**Important:** if the `grep` in Step 1 shows additional values not listed here (e.g. `MFA_*`, `EMAIL_*`, `INVITATION_*`, `ORG_CREATED`), include them — copy-paste the full set the database currently uses.

- [ ] **Step 3: Apply the migration**

```bash
./stop.sh && ./dev.sh
```

Watch for `Successfully applied 1 migration`.

- [ ] **Step 4: Verify the constraint**

```bash
docker exec oscal-postgres-dev psql -U oscal_user -d oscal_dev -c \
  "SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint \
   WHERE conname = 'audit_events_event_type_check';"
```

Expected: the printed constraint contains `LIBRARY_ITEM_PUBLISHED`.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/resources/db/migration/V1.2__library_audit_event_types.sql
git commit -m "feat(db): allow LIBRARY_ITEM_* values in audit_events.event_type"
```

---

## Group B — Visibility enforcement

### Task 8: `canRead` helper + unit tests

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/LibraryService.java`
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/service/LibraryServiceCanReadTest.java`

- [ ] **Step 1: Write the test (TDD)**

Create `LibraryServiceCanReadTest.java`:

```java
package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.Visibility;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LibraryServiceCanReadTest {

    private final LibraryService svc = new LibraryService();  // canRead is pure — no deps needed

    private LibraryItem item(Visibility v, User creator, Organization org) {
        LibraryItem it = new LibraryItem();
        it.setVisibility(v);
        it.setCreatedBy(creator);
        it.setOrganization(org);
        return it;
    }
    private User user(long id, String username, Long orgId) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        if (orgId != null) {
            Organization o = new Organization();
            o.setId(orgId);
            u.setOrganization(o);  // adjust to actual setter on User; see Step 1.5 below
        }
        return u;
    }
    private Organization org(long id) { Organization o = new Organization(); o.setId(id); return o; }

    @Test
    void publicItemReadableByAnyone() {
        LibraryItem item = item(Visibility.PUBLIC, user(1, "alice", 10L), null);
        assertThat(svc.canRead(item, null)).isTrue();
        assertThat(svc.canRead(item, user(2, "bob", 20L))).isTrue();
    }

    @Test
    void privateItemReadableOnlyByCreator() {
        User alice = user(1, "alice", 10L);
        LibraryItem item = item(Visibility.PRIVATE, alice, null);
        assertThat(svc.canRead(item, null)).isFalse();
        assertThat(svc.canRead(item, alice)).isTrue();
        assertThat(svc.canRead(item, user(2, "bob", 10L))).isFalse();  // same org, still no
    }

    @Test
    void organizationItemReadableByOrgMembers() {
        User alice = user(1, "alice", 10L);
        Organization acme = org(10);
        LibraryItem item = item(Visibility.ORGANIZATION, alice, acme);
        assertThat(svc.canRead(item, null)).isFalse();
        assertThat(svc.canRead(item, alice)).isTrue();
        assertThat(svc.canRead(item, user(2, "bob", 10L))).isTrue();   // same org
        assertThat(svc.canRead(item, user(3, "carol", 99L))).isFalse(); // different org
    }
}
```

**Step 1.5 — adjust to actual `User` shape:**

```bash
grep -E "Organization|getOrganizationId|setOrganization" back-end/src/main/java/gov/nist/oscal/tools/api/entity/User.java | head
```

Use the real getter/setter the `User` class exposes (it may be `getPrimaryOrganization` or backed by `OrganizationMembership` rather than a direct field). Adjust the test helpers and the canRead implementation to match.

- [ ] **Step 2: Run the test — should fail (no `canRead` method yet)**

```bash
cd back-end && mvn -q test -Dtest=LibraryServiceCanReadTest
```

Expected: COMPILATION FAILURE — `cannot find symbol method canRead(LibraryItem,User)`.

- [ ] **Step 3: Implement `canRead` in `LibraryService`**

Add to `LibraryService.java`:

```java
    /**
     * Visibility predicate used by every read path. PUBLIC items are readable by
     * anyone (including null caller). Otherwise, only creator or same-org members.
     */
    public boolean canRead(LibraryItem item, User caller) {
        if (item.getVisibility() == Visibility.PUBLIC) return true;
        if (caller == null) return false;
        if (item.getCreatedBy() != null
                && item.getCreatedBy().getId().equals(caller.getId())) return true;
        if (item.getVisibility() == Visibility.ORGANIZATION) {
            Long itemOrg = item.getOrganization() != null ? item.getOrganization().getId() : null;
            Long callerOrg = resolveOrgId(caller);
            return itemOrg != null && itemOrg.equals(callerOrg);
        }
        return false;
    }

    private Long resolveOrgId(User user) {
        // Adjust to match the User → Organization relationship in this codebase.
        // If User has multiple memberships, prefer the active/primary one.
        return user.getOrganization() != null ? user.getOrganization().getId() : null;
    }
```

If `User` doesn't have `getOrganization()` directly, replace `resolveOrgId` with the appropriate accessor (e.g., iterate `user.getMemberships()` and pick the active one).

- [ ] **Step 4: Run tests — should pass**

```bash
cd back-end && mvn -q test -Dtest=LibraryServiceCanReadTest
```

Expected: 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/LibraryService.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/LibraryServiceCanReadTest.java
git commit -m "feat(library): add canRead visibility predicate with unit tests"
```

---

### Task 9: Apply visibility filter to list/search/recent/popular

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/LibraryItemRepository.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/LibraryService.java`
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/service/LibraryListVisibilityTest.java`

- [ ] **Step 1: Write the integration test (TDD)**

```java
package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.*;
import gov.nist.oscal.tools.api.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LibraryListVisibilityTest {

    @Autowired LibraryService libraryService;
    @Autowired UserRepository userRepo;
    @Autowired LibraryItemRepository libraryRepo;

    @Test
    void listForUserShowsOwnPlusOrgPlusPublic() {
        // Set up 4 items: own-private, others-private (hidden),
        // org-shared by another user (visible), public (visible).
        // Use existing test fixtures or @TestConfiguration to seed.
        // Assert exactly 3 items returned to the calling user.
        // Skeleton — adapt to existing test fixture patterns:
        // User alice = ...; User bob = ...;  Organization acme = ...;
        // libraryRepo.save(buildItem(alice, Visibility.PRIVATE, acme));
        // libraryRepo.save(buildItem(bob,   Visibility.PRIVATE, acme));   // hidden
        // libraryRepo.save(buildItem(bob,   Visibility.ORGANIZATION, acme));
        // libraryRepo.save(buildItem(bob,   Visibility.PUBLIC, acme));

        // Page<LibraryItem> page = libraryService.listForUser(alice, PageRequest.of(0, 50));
        // assertThat(page.getContent()).extracting(LibraryItem::getVisibility)
        //         .containsExactlyInAnyOrder(
        //             Visibility.PRIVATE, Visibility.ORGANIZATION, Visibility.PUBLIC);
    }
}
```

The test skeleton is intentionally pseudo-code in places; adapt to whatever fixture conventions the repo uses (e.g. `@Sql`, builder helpers, `@DataJpaTest`). The point is to cover the visibility matrix end-to-end against real Postgres.

- [ ] **Step 2: Add visibility-aware repository query**

```java
    /**
     * Returns items the user is allowed to see: own + same-org ORGANIZATION + all PUBLIC.
     */
    @org.springframework.data.jpa.repository.Query("""
        SELECT li FROM LibraryItem li
        WHERE li.visibility = gov.nist.oscal.tools.api.entity.Visibility.PUBLIC
           OR li.createdBy.id = :userId
           OR (li.visibility = gov.nist.oscal.tools.api.entity.Visibility.ORGANIZATION
                AND li.organization.id = :orgId)
        """)
    org.springframework.data.domain.Page<LibraryItem> findVisibleTo(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("orgId") Long orgId,
            org.springframework.data.domain.Pageable pageable);
```

Add similar variants for search-with-keyword (combine FTS predicate `to_tsvector(...) @@ plainto_tsquery(...)` with the visibility OR), recent, popular. Each is a small JPQL/native query mirroring the existing one with the visibility OR added.

- [ ] **Step 3: Update `LibraryService.listForUser`/`searchForUser`/`recentForUser`/`popularForUser`**

Each existing list method gets a `User caller` parameter (or reads the `Principal` and resolves to a `User`). It calls the new `findVisibleTo`-style query instead of `findAll`. The controller passes the principal through.

Key change: any `LibraryItemRepository.findAll(...)` or `findByOscalType(...)` call in the service must be replaced or augmented with a visibility-aware variant.

- [ ] **Step 4: Run the test**

```bash
cd back-end && mvn -q test -Dtest=LibraryListVisibilityTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/repository/LibraryItemRepository.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/LibraryService.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/LibraryListVisibilityTest.java
git commit -m "feat(library): filter list/search/recent/popular by visibility"
```

---

### Task 10: Apply `canRead` to single-item endpoints

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/LibraryService.java`

- [ ] **Step 1: Write the test**

Add to `back-end/src/test/java/gov/nist/oscal/tools/api/controller/LibraryVisibilityControllerTest.java` (new file):

```java
package gov.nist.oscal.tools.api.controller;

// imports omitted for brevity — Spring Boot test, MockMvc, JWT helpers
@org.springframework.boot.test.context.SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class LibraryVisibilityControllerTest {

    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.test.web.servlet.MockMvc mvc;

    @org.junit.jupiter.api.Test
    void privateItemReturns404ToNonCreator() throws Exception {
        // Seed: alice creates a PRIVATE item with itemId "alice-private-item".
        // bob authenticates and GETs /api/library/alice-private-item.
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/library/alice-private-item")
                .header("Authorization", "Bearer " + bobToken()))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        // 404, not 403 — to avoid leaking item existence.
    }

    private String bobToken() { /* fetch a JWT for bob via existing test helper */ return ""; }
}
```

- [ ] **Step 2: Modify `LibraryService` single-item methods**

In `getById`, `getContent`, `getVersions`, `getVersionContent`:

```java
    public LibraryItem getById(String itemId, User caller) {
        LibraryItem item = repo.findByItemId(itemId)
            .orElseThrow(() -> new NotFoundException("library item not found"));
        if (!canRead(item, caller)) {
            throw new NotFoundException("library item not found");  // do NOT throw 403 — same response as truly missing
        }
        return item;
    }
```

Apply the same pattern to the other three methods. The controller catches `NotFoundException` and returns 404 (existing behavior).

- [ ] **Step 3: Run the test**

```bash
cd back-end && mvn -q test -Dtest=LibraryVisibilityControllerTest
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/LibraryService.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/controller/LibraryVisibilityControllerTest.java
git commit -m "feat(library): enforce canRead on single-item endpoints (404 on miss)"
```

---

### Task 11: Visibility matrix integration test

**Files:**
- Modify: `back-end/src/test/java/gov/nist/oscal/tools/api/controller/LibraryVisibilityControllerTest.java`

- [ ] **Step 1: Add the matrix tests**

Cover:

| Caller | Item visibility | Item creator | Same org? | Expected |
|---|---|---|---|---|
| anonymous | PUBLIC | anyone | n/a | 200 |
| anonymous | ORGANIZATION | anyone | n/a | 401 (existing endpoint requires JWT) |
| anonymous | PRIVATE | anyone | n/a | 401 |
| user | PUBLIC | other | yes | 200 |
| user | PUBLIC | other | no | 200 |
| user | ORGANIZATION | other | yes | 200 |
| user | ORGANIZATION | other | no | 404 |
| user | ORGANIZATION | self | yes | 200 |
| user | PRIVATE | other | yes | 404 |
| user | PRIVATE | self | yes | 200 |

Note that anonymous calls to `/api/library/*` return 401 from the JWT filter, not 200/404. The PUBLIC anonymous case here should be skipped (or remarked TODO Phase 2) since `/api/library/*` is currently JWT-required. Phase 2 introduces `/api/public/catalog/*` for the anonymous path.

- [ ] **Step 2: Run all tests**

```bash
cd back-end && mvn -q test -Dtest=LibraryVisibilityControllerTest
```

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add back-end/src/test/java/gov/nist/oscal/tools/api/controller/LibraryVisibilityControllerTest.java
git commit -m "test(library): full visibility matrix end-to-end"
```

---

## Group C — Visibility change endpoint

### Task 12: `VisibilityChangeRequest` DTO

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/library/VisibilityChangeRequest.java`

- [ ] **Step 1: Write the class**

```java
package gov.nist.oscal.tools.api.model.library;

import gov.nist.oscal.tools.api.entity.Visibility;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VisibilityChangeRequest {

    @NotNull
    private Visibility visibility;

    private Long organizationId;       // required when visibility=ORGANIZATION

    @Size(max = 500)
    private String reason;             // required when SUPER_ADMIN force-unpublishes

    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
```

- [ ] **Step 2: Compile**

```bash
cd back-end && mvn -q compile
```

- [ ] **Step 3: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/model/library/VisibilityChangeRequest.java
git commit -m "feat(library): add VisibilityChangeRequest DTO"
```

---

### Task 13: `PATCH /api/library/{itemId}/visibility` — endpoint, service, audit, tests

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/LibraryService.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/LibraryController.java`
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/controller/LibraryVisibilityChangeTest.java`

- [ ] **Step 1: Write the controller test (TDD)**

```java
package gov.nist.oscal.tools.api.controller;

// imports omitted
@org.springframework.boot.test.context.SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class LibraryVisibilityChangeTest {

    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.test.web.servlet.MockMvc mvc;

    @org.junit.jupiter.api.Test
    void creatorCanPublishItem() throws Exception {
        // Seed alice's PRIVATE item "alice-item-1"
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/library/alice-item-1/visibility")
                .header("Authorization", "Bearer " + aliceToken())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"visibility\":\"PUBLIC\"}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        // Verify DB: visibility=PUBLIC, published_at != NULL, last_published_at != NULL,
        // and an audit_events row of type LIBRARY_ITEM_PUBLISHED exists.
    }

    @org.junit.jupiter.api.Test
    void nonCreatorRegularUserCannotPublish() throws Exception {
        // Seed alice's item, call PATCH as bob → 403 or 404.
    }

    @org.junit.jupiter.api.Test
    void superAdminCanForceUnpublishWithReason() throws Exception {
        // Seed alice's PUBLIC item, call PATCH as super_admin with
        // {visibility:"PRIVATE", reason:"violates policy"} → 200,
        // audit row LIBRARY_ITEM_FORCE_UNPUBLISHED with reason captured.
    }

    @org.junit.jupiter.api.Test
    void organizationVisibilityRequiresOrganizationId() throws Exception {
        // PATCH {visibility:"ORGANIZATION"} without organizationId → 400.
    }

    private String aliceToken() { return ""; }
}
```

- [ ] **Step 2: Add the service method**

In `LibraryService`:

```java
    public LibraryItem changeVisibility(String itemId, VisibilityChangeRequest req, User caller) {
        LibraryItem item = repo.findByItemId(itemId)
            .orElseThrow(() -> new NotFoundException("library item not found"));

        boolean isCreator = caller != null
            && item.getCreatedBy() != null
            && item.getCreatedBy().getId().equals(caller.getId());
        boolean isSuperAdmin = caller != null
            && "SUPER_ADMIN".equals(caller.getGlobalRole());  // adjust to actual accessor

        if (!isCreator && !isSuperAdmin) {
            throw new NotFoundException("library item not found");  // hide existence
        }

        Visibility prev = item.getVisibility();
        Visibility next = req.getVisibility();

        if (next == Visibility.ORGANIZATION) {
            if (req.getOrganizationId() == null) {
                throw new ValidationException("organizationId required when visibility=ORGANIZATION");
            }
            if (!isSuperAdmin) {
                Long callerOrg = resolveOrgId(caller);
                if (!req.getOrganizationId().equals(callerOrg)) {
                    throw new ForbiddenException("cannot share to organization you don't belong to");
                }
            }
            item.setOrganization(orgRepo.findById(req.getOrganizationId())
                .orElseThrow(() -> new ValidationException("unknown organizationId")));
        } else {
            item.setOrganization(null);
        }

        item.setVisibility(next);
        LocalDateTime now = LocalDateTime.now();
        if (next == Visibility.PUBLIC) {
            if (item.getPublishedAt() == null) item.setPublishedAt(now);
            item.setLastPublishedAt(now);
        }

        repo.save(item);

        AuditEventType type = chooseAuditType(prev, next, isCreator, isSuperAdmin);
        auditService.record(type, caller, "LibraryItem", item.getItemId(),
            req.getReason());  // adjust to actual auditService method signature

        return item;
    }

    private AuditEventType chooseAuditType(Visibility prev, Visibility next,
                                            boolean isCreator, boolean isSuperAdmin) {
        if (next == Visibility.PUBLIC && prev != Visibility.PUBLIC)
            return AuditEventType.LIBRARY_ITEM_PUBLISHED;
        if (prev == Visibility.PUBLIC && next != Visibility.PUBLIC) {
            return isSuperAdmin && !isCreator
                ? AuditEventType.LIBRARY_ITEM_FORCE_UNPUBLISHED
                : AuditEventType.LIBRARY_ITEM_UNPUBLISHED;
        }
        return AuditEventType.LIBRARY_ITEM_VISIBILITY_CHANGED;
    }
```

Resolve `auditService.record(...)` to whatever the existing `AuditLogService` (or equivalent) exposes. If it doesn't currently accept a free-form `reason`, store it in the `metadata` JSON column instead.

- [ ] **Step 3: Add the controller endpoint**

In `LibraryController`:

```java
    @PatchMapping("/{itemId}/visibility")
    public ResponseEntity<?> changeVisibility(
            @PathVariable String itemId,
            @Valid @RequestBody VisibilityChangeRequest req,
            Principal principal) {
        try {
            User caller = userService.requireByUsername(principal.getName());
            LibraryItem item = libraryService.changeVisibility(itemId, req, caller);
            return ResponseEntity.ok(LibraryItemDto.fromEntity(item));
        } catch (NotFoundException nf) {
            return ResponseEntity.notFound().build();
        } catch (ForbiddenException fe) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (ValidationException ve) {
            return ResponseEntity.badRequest().body(Map.of("error", ve.getMessage()));
        }
    }
```

- [ ] **Step 4: Run the tests**

```bash
cd back-end && mvn -q test -Dtest=LibraryVisibilityChangeTest
```

Expected: 4 tests pass.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/LibraryService.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/controller/LibraryController.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/controller/LibraryVisibilityChangeTest.java
git commit -m "feat(library): PATCH /visibility endpoint with audit + role gate"
```

---

## Group D — Save-to-Library backend

### Task 14: `SourceContentResolver` interface and `SourceContent` record

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/library/SourceContent.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/library/SourceContentResolver.java`

- [ ] **Step 1: Write `SourceContent`**

```java
package gov.nist.oscal.tools.api.model.library;

/**
 * Bytes + metadata loaded from a builder row, ready to write into the library blob.
 * @param bytes serialized content (typically JSON)
 * @param format "json" / "xml" / "yaml"
 * @param filename suggested filename, e.g. "my-catalog-v3.json"
 * @param oscalType "catalog" / "profile" / etc.
 * @param sourceId the builder row's UUID — becomes library_items.source_id
 * @param defaultTitle title to suggest when the user didn't override
 */
public record SourceContent(
        byte[] bytes,
        String format,
        String filename,
        String oscalType,
        java.util.UUID sourceId,
        String defaultTitle
) {}
```

- [ ] **Step 2: Write the interface**

```java
package gov.nist.oscal.tools.api.service.library;

import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.model.library.SourceContent;

public interface SourceContentResolver {

    /** Which builder this resolver handles. Used by Spring to register the resolver in a map. */
    SourceType supportedType();

    /**
     * Loads content for the given builder row id. The id is the builder table's
     * primary key (Long), not the library item id.
     *
     * @throws IllegalArgumentException if no row with that id exists
     * @throws SecurityException if the caller is not authorized to read the source row
     */
    SourceContent resolve(Long builderRowId, String callerUsername);
}
```

- [ ] **Step 3: Compile**

```bash
cd back-end && mvn -q compile
```

- [ ] **Step 4: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/model/library/SourceContent.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/library/SourceContentResolver.java
git commit -m "feat(library): SourceContentResolver interface + SourceContent record"
```

---

### Task 15: `CatalogSourceResolver`

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/library/CatalogSourceResolver.java`
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/service/library/CatalogSourceResolverTest.java`

- [ ] **Step 1: Write the unit test (TDD)**

```java
package gov.nist.oscal.tools.api.service.library;

import gov.nist.oscal.tools.api.entity.Catalog;
import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.repository.CatalogRepository;
import gov.nist.oscal.tools.api.service.StorageService;
import gov.nist.oscal.tools.api.model.library.SourceContent;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogSourceResolverTest {

    @Mock CatalogRepository catalogRepo;
    @Mock StorageService storage;
    CatalogSourceResolver resolver;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        resolver = new CatalogSourceResolver(catalogRepo, storage);
    }

    @Test
    void resolveReturnsContentFromStorage() {
        Catalog c = new Catalog();
        c.setId(42L);
        c.setOscalUuid(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        c.setTitle("My Catalog");
        c.setStoragePath("alice/catalog/my-catalog.json");
        c.setFilename("my-catalog.json");
        // assume creator username is alice; method on Catalog tbd — adapt
        when(catalogRepo.findById(42L)).thenReturn(Optional.of(c));
        when(storage.readBytes("alice/catalog/my-catalog.json"))
            .thenReturn("{\"x\":1}".getBytes());

        SourceContent sc = resolver.resolve(42L, "alice");

        assertThat(sc.bytes()).isEqualTo("{\"x\":1}".getBytes());
        assertThat(sc.format()).isEqualTo("json");
        assertThat(sc.filename()).isEqualTo("my-catalog.json");
        assertThat(sc.oscalType()).isEqualTo("catalog");
        assertThat(sc.sourceId()).isEqualTo(c.getOscalUuid());
        assertThat(sc.defaultTitle()).isEqualTo("My Catalog");
    }

    @Test
    void resolveThrowsWhenRowMissing() {
        when(catalogRepo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> resolver.resolve(99L, "alice"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void supportedTypeIsCatalog() {
        assertThat(resolver.supportedType()).isEqualTo(SourceType.CATALOG);
    }
}
```

- [ ] **Step 2: Run — fails with `cannot find symbol class CatalogSourceResolver`**

```bash
cd back-end && mvn -q test -Dtest=CatalogSourceResolverTest
```

- [ ] **Step 3: Implement the resolver**

```java
package gov.nist.oscal.tools.api.service.library;

import gov.nist.oscal.tools.api.entity.Catalog;
import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.model.library.SourceContent;
import gov.nist.oscal.tools.api.repository.CatalogRepository;
import gov.nist.oscal.tools.api.service.StorageService;
import org.springframework.stereotype.Component;

@Component
public class CatalogSourceResolver implements SourceContentResolver {

    private final CatalogRepository catalogRepo;
    private final StorageService storage;

    public CatalogSourceResolver(CatalogRepository catalogRepo, StorageService storage) {
        this.catalogRepo = catalogRepo;
        this.storage = storage;
    }

    @Override public SourceType supportedType() { return SourceType.CATALOG; }

    @Override
    public SourceContent resolve(Long builderRowId, String callerUsername) {
        Catalog c = catalogRepo.findById(builderRowId)
            .orElseThrow(() -> new IllegalArgumentException("catalog not found: " + builderRowId));
        // Authorization: only the creator may read their own row. SUPER_ADMIN bypass
        // is not required here — admins use the existing library re-upload flow.
        if (!c.getCreatedBy().equals(callerUsername)) {
            throw new SecurityException("not your catalog");
        }
        byte[] bytes = storage.readBytes(c.getStoragePath());
        return new SourceContent(
            bytes, "json", c.getFilename(), "catalog", c.getOscalUuid(), c.getTitle());
    }
}
```

Adjust to match the actual `Catalog` accessor names (e.g., `getStoragePath` vs `getFilePath`, `getCreatedBy` returning a `User` vs a String) discovered via `grep` in the entity file.

- [ ] **Step 4: Run — pass**

```bash
cd back-end && mvn -q test -Dtest=CatalogSourceResolverTest
```

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/library/CatalogSourceResolver.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/library/CatalogSourceResolverTest.java
git commit -m "feat(library): CatalogSourceResolver"
```

---

### Task 16: `ProfileSourceResolver`

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/library/ProfileSourceResolver.java`
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/service/library/ProfileSourceResolverTest.java`

- [ ] **Step 1: Write the test**

Identical structure to `CatalogSourceResolverTest`. Substitute:
- `Profile` for `Catalog`
- `ProfileRepository` for `CatalogRepository`
- `SourceType.PROFILE` for `SourceType.CATALOG`
- `"profile"` for `"catalog"` in `oscalType()`

- [ ] **Step 2: Run — fail**

```bash
cd back-end && mvn -q test -Dtest=ProfileSourceResolverTest
```

- [ ] **Step 3: Implement**

```java
package gov.nist.oscal.tools.api.service.library;

import gov.nist.oscal.tools.api.entity.Profile;
import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.model.library.SourceContent;
import gov.nist.oscal.tools.api.repository.ProfileRepository;
import gov.nist.oscal.tools.api.service.StorageService;
import org.springframework.stereotype.Component;

@Component
public class ProfileSourceResolver implements SourceContentResolver {

    private final ProfileRepository profileRepo;
    private final StorageService storage;

    public ProfileSourceResolver(ProfileRepository profileRepo, StorageService storage) {
        this.profileRepo = profileRepo;
        this.storage = storage;
    }

    @Override public SourceType supportedType() { return SourceType.PROFILE; }

    @Override
    public SourceContent resolve(Long builderRowId, String callerUsername) {
        Profile p = profileRepo.findById(builderRowId)
            .orElseThrow(() -> new IllegalArgumentException("profile not found: " + builderRowId));
        if (!p.getCreatedBy().equals(callerUsername)) {
            throw new SecurityException("not your profile");
        }
        byte[] bytes = storage.readBytes(p.getStoragePath());
        return new SourceContent(
            bytes, "json", p.getFilename(), "profile", p.getOscalUuid(), p.getTitle());
    }
}
```

- [ ] **Step 4: Run — pass**

```bash
cd back-end && mvn -q test -Dtest=ProfileSourceResolverTest
```

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/library/ProfileSourceResolver.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/library/ProfileSourceResolverTest.java
git commit -m "feat(library): ProfileSourceResolver"
```

---

### Task 17: `ComponentDefinitionSourceResolver`

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/library/ComponentDefinitionSourceResolver.java`
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/service/library/ComponentDefinitionSourceResolverTest.java`

- [ ] **Step 1: Write the test**

Mirror Task 15. Substitutions:
- `ComponentDefinition` for `Catalog`
- `ComponentDefinitionRepository`
- `SourceType.COMPONENT_DEFINITION`
- `"component-definition"` (or whatever the existing `oscalType` string is in the database — confirm by inspecting the existing `ComponentDefinitionService.create*` method).

- [ ] **Step 2: Run — fail.**

- [ ] **Step 3: Implement (analogous to Task 15/16, swapping types).**

```java
package gov.nist.oscal.tools.api.service.library;

import gov.nist.oscal.tools.api.entity.ComponentDefinition;
import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.model.library.SourceContent;
import gov.nist.oscal.tools.api.repository.ComponentDefinitionRepository;
import gov.nist.oscal.tools.api.service.StorageService;
import org.springframework.stereotype.Component;

@Component
public class ComponentDefinitionSourceResolver implements SourceContentResolver {

    private final ComponentDefinitionRepository repo;
    private final StorageService storage;

    public ComponentDefinitionSourceResolver(ComponentDefinitionRepository repo, StorageService storage) {
        this.repo = repo;
        this.storage = storage;
    }

    @Override public SourceType supportedType() { return SourceType.COMPONENT_DEFINITION; }

    @Override
    public SourceContent resolve(Long builderRowId, String callerUsername) {
        ComponentDefinition cd = repo.findById(builderRowId)
            .orElseThrow(() -> new IllegalArgumentException("component-definition not found: " + builderRowId));
        if (!cd.getCreatedBy().equals(callerUsername)) {
            throw new SecurityException("not your component-definition");
        }
        byte[] bytes = storage.readBytes(cd.getStoragePath());
        return new SourceContent(
            bytes, "json", cd.getFilename(), "component-definition",
            cd.getOscalUuid(), cd.getTitle());
    }
}
```

- [ ] **Step 4: Run — pass.**

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/library/ComponentDefinitionSourceResolver.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/library/ComponentDefinitionSourceResolverTest.java
git commit -m "feat(library): ComponentDefinitionSourceResolver"
```

---

### Task 18: `OscalDocumentSourceResolver` (SSP / AP / AR / POAM)

The four document types share the `oscal_documents` table; one resolver covers them all. The resolver chooses its `SourceType` and `oscalType` string based on `OscalDocument.modelType`.

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/library/OscalDocumentSourceResolver.java`
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/service/library/OscalDocumentSourceResolverTest.java`

**Important architectural note:** Because one resolver covers multiple `SourceType` values, `LibraryIngestService` must look up resolvers by `SourceType` enum *and* understand that the same Spring bean serves four entries. Two patterns work:
- (a) Register the same bean four times in a `Map<SourceType, SourceContentResolver>` via a `@Configuration` that wires the bean once and adds it under each of `SSP`, `AP`, `AR`, `POAM`.
- (b) Have `OscalDocumentSourceResolver.supportedType()` return one of the four (say `SSP`) and add a `supportedTypes()` method returning `Set.of(SSP, AP, AR, POAM)` to the interface, then update `LibraryIngestService` to register each resolver under all its supported types.

Choose (b) — extend the interface with a default method:

```java
default java.util.Set<SourceType> supportedTypes() {
    return java.util.Set.of(supportedType());
}
```

`OscalDocumentSourceResolver` overrides `supportedTypes()` to return all four.

- [ ] **Step 1: Add `supportedTypes()` default method to `SourceContentResolver`** (modify the file from Task 14).

- [ ] **Step 2: Write the resolver test**

```java
package gov.nist.oscal.tools.api.service.library;

import gov.nist.oscal.tools.api.entity.OscalDocument;
import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.repository.OscalDocumentRepository;
import gov.nist.oscal.tools.api.service.StorageService;
import gov.nist.oscal.tools.api.model.library.SourceContent;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OscalDocumentSourceResolverTest {

    @Mock OscalDocumentRepository repo;
    @Mock StorageService storage;
    OscalDocumentSourceResolver resolver;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        resolver = new OscalDocumentSourceResolver(repo, storage);
    }

    @Test
    void supportedTypesCoversAllFourDocumentKinds() {
        assertThat(resolver.supportedTypes())
            .containsExactlyInAnyOrder(SourceType.SSP, SourceType.AP, SourceType.AR, SourceType.POAM);
    }

    @Test
    void resolveMapsModelTypeToOscalTypeString() {
        OscalDocument d = new OscalDocument();
        d.setId(7L);
        d.setOscalUuid(UUID.randomUUID());
        d.setTitle("My SSP");
        d.setModelType("SYSTEM_SECURITY_PLAN");
        d.setStoragePath("alice/ssp/my-ssp.json");
        d.setFilename("my-ssp.json");
        d.setCreatedBy("alice");
        when(repo.findById(7L)).thenReturn(Optional.of(d));
        when(storage.readBytes("alice/ssp/my-ssp.json")).thenReturn(new byte[]{1,2,3});

        SourceContent sc = resolver.resolve(7L, "alice");

        assertThat(sc.oscalType()).isEqualTo("ssp");
        assertThat(sc.bytes()).containsExactly(1,2,3);
    }
}
```

- [ ] **Step 3: Run — fail.**

- [ ] **Step 4: Implement**

```java
package gov.nist.oscal.tools.api.service.library;

import gov.nist.oscal.tools.api.entity.OscalDocument;
import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.model.library.SourceContent;
import gov.nist.oscal.tools.api.repository.OscalDocumentRepository;
import gov.nist.oscal.tools.api.service.StorageService;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class OscalDocumentSourceResolver implements SourceContentResolver {

    private final OscalDocumentRepository repo;
    private final StorageService storage;

    public OscalDocumentSourceResolver(OscalDocumentRepository repo, StorageService storage) {
        this.repo = repo;
        this.storage = storage;
    }

    @Override public SourceType supportedType() { return SourceType.SSP; }   // primary

    @Override public Set<SourceType> supportedTypes() {
        return Set.of(SourceType.SSP, SourceType.AP, SourceType.AR, SourceType.POAM);
    }

    @Override
    public SourceContent resolve(Long builderRowId, String callerUsername) {
        OscalDocument d = repo.findById(builderRowId)
            .orElseThrow(() -> new IllegalArgumentException("oscal-document not found: " + builderRowId));
        if (!d.getCreatedBy().equals(callerUsername)) {
            throw new SecurityException("not your oscal-document");
        }
        String oscalType = switch (d.getModelType()) {
            case "SYSTEM_SECURITY_PLAN" -> "ssp";
            case "ASSESSMENT_PLAN" -> "ap";
            case "ASSESSMENT_RESULTS" -> "ar";
            case "PLAN_OF_ACTION_AND_MILESTONES" -> "poam";
            default -> throw new IllegalStateException("unknown model_type " + d.getModelType());
        };
        byte[] bytes = storage.readBytes(d.getStoragePath());
        return new SourceContent(bytes, "json", d.getFilename(), oscalType, d.getOscalUuid(), d.getTitle());
    }
}
```

- [ ] **Step 5: Run — pass.**

- [ ] **Step 6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/library/SourceContentResolver.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/library/OscalDocumentSourceResolver.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/library/OscalDocumentSourceResolverTest.java
git commit -m "feat(library): OscalDocumentSourceResolver covers SSP/AP/AR/POAM"
```

---

### Task 19: `LibraryIngestService` — orchestration

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/library/LibraryIngestService.java`
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/service/library/LibraryIngestServiceTest.java`

- [ ] **Step 1: Write the unit tests**

```java
package gov.nist.oscal.tools.api.service.library;

import gov.nist.oscal.tools.api.entity.*;
import gov.nist.oscal.tools.api.model.library.SourceContent;
import gov.nist.oscal.tools.api.repository.*;
import gov.nist.oscal.tools.api.service.LibraryStorageService;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class LibraryIngestServiceTest {

    @Mock LibraryItemRepository itemRepo;
    @Mock LibraryVersionRepository versionRepo;
    @Mock LibraryStorageService libraryStorage;
    @Mock SourceContentResolver catalogResolver;
    @Mock UserRepository userRepo;
    @Mock LibraryTagRepository libraryTagRepository;

    LibraryIngestService svc;
    User alice;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        when(catalogResolver.supportedType()).thenReturn(SourceType.CATALOG);
        when(catalogResolver.supportedTypes()).thenReturn(Set.of(SourceType.CATALOG));
        svc = new LibraryIngestService(
            itemRepo, versionRepo, libraryStorage,
            List.of(catalogResolver), userRepo, libraryTagRepository);

        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(alice));
    }

    private SourceContent sampleContent() {
        return new SourceContent(
            "{\"x\":1}".getBytes(), "json", "my.json", "catalog",
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "Sample Catalog");
    }

    @Test
    void firstSaveCreatesItemAndVersion1() {
        when(catalogResolver.resolve(42L, "alice")).thenReturn(sampleContent());
        when(itemRepo.findByCreatedBy_IdAndSourceTypeAndSourceId(eq(1L), eq(SourceType.CATALOG), any()))
            .thenReturn(Optional.empty());
        when(libraryStorage.write(any(), any(), any())).thenReturn("library/alice/<uuid>/v1.json");
        when(itemRepo.save(any(LibraryItem.class)))
            .thenAnswer(inv -> { LibraryItem li = inv.getArgument(0); li.setId(100L); return li; });
        when(versionRepo.save(any(LibraryVersion.class)))
            .thenAnswer(inv -> { LibraryVersion v = inv.getArgument(0); v.setId(500L); return v; });

        LibraryItem result = svc.saveToLibrary(
            SourceType.CATALOG, 42L,
            "Title", "Desc", Set.of("compliance"), Visibility.PRIVATE, null,
            "alice");

        assertThat(result.getVisibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(result.getSourceType()).isEqualTo(SourceType.CATALOG);
        verify(itemRepo, times(1)).save(any(LibraryItem.class));
        verify(versionRepo, times(1)).save(any(LibraryVersion.class));
        verify(libraryStorage, times(1)).write(any(), any(), any());
    }

    @Test
    void secondSaveAppendsNewVersionToExistingItem() {
        LibraryItem existing = new LibraryItem();
        existing.setId(100L);
        existing.setCreatedBy(alice);
        existing.setSourceType(SourceType.CATALOG);
        existing.setSourceId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        LibraryVersion v1 = new LibraryVersion();
        v1.setId(500L);
        v1.setVersionNumber(1);
        existing.setCurrentVersion(v1);

        when(catalogResolver.resolve(42L, "alice")).thenReturn(sampleContent());
        when(itemRepo.findByCreatedBy_IdAndSourceTypeAndSourceId(eq(1L), eq(SourceType.CATALOG), any()))
            .thenReturn(Optional.of(existing));
        when(versionRepo.save(any(LibraryVersion.class)))
            .thenAnswer(inv -> {
                LibraryVersion v = inv.getArgument(0);
                v.setId(501L);
                return v;
            });

        LibraryItem result = svc.saveToLibrary(
            SourceType.CATALOG, 42L,
            null, null, null, null, null, "alice");

        assertThat(result.getId()).isEqualTo(100L);
        verify(itemRepo, never()).save(argThat(li -> li.getId() == null));  // no NEW item
        // versionNumber should advance to 2
        verify(versionRepo).save(argThat(v -> v.getVersionNumber() == 2));
    }

    @Test
    void organizationVisibilityRequiresOrgId() {
        when(catalogResolver.resolve(42L, "alice")).thenReturn(sampleContent());
        when(itemRepo.findByCreatedBy_IdAndSourceTypeAndSourceId(any(), any(), any()))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.saveToLibrary(
            SourceType.CATALOG, 42L,
            "T", "D", Set.of(), Visibility.ORGANIZATION, null, "alice"))
            .hasMessageContaining("organizationId");
    }

    @Test
    void unknownSourceTypeThrows() {
        // simulate a resolver list that doesn't include PROFILE
        assertThatThrownBy(() -> svc.saveToLibrary(
            SourceType.PROFILE, 42L,
            "T", "D", Set.of(), Visibility.PRIVATE, null, "alice"))
            .hasMessageContaining("no resolver");
    }
}
```

- [ ] **Step 2: Run — fail.**

- [ ] **Step 3: Implement `LibraryIngestService`**

```java
package gov.nist.oscal.tools.api.service.library;

import gov.nist.oscal.tools.api.entity.*;
import gov.nist.oscal.tools.api.model.library.SourceContent;
import gov.nist.oscal.tools.api.repository.*;
import gov.nist.oscal.tools.api.service.LibraryStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class LibraryIngestService {

    private final LibraryItemRepository itemRepo;
    private final LibraryVersionRepository versionRepo;
    private final LibraryStorageService libraryStorage;
    private final UserRepository userRepo;
    private final LibraryTagRepository libraryTagRepository;
    private final Map<SourceType, SourceContentResolver> resolvers;

    @Autowired
    public LibraryIngestService(LibraryItemRepository itemRepo,
                                 LibraryVersionRepository versionRepo,
                                 LibraryStorageService libraryStorage,
                                 List<SourceContentResolver> resolverList,
                                 UserRepository userRepo,
                                 LibraryTagRepository libraryTagRepository) {
        this.itemRepo = itemRepo;
        this.versionRepo = versionRepo;
        this.libraryStorage = libraryStorage;
        this.userRepo = userRepo;
        this.libraryTagRepository = libraryTagRepository;
        Map<SourceType, SourceContentResolver> map = new EnumMap<>(SourceType.class);
        for (SourceContentResolver r : resolverList) {
            for (SourceType t : r.supportedTypes()) map.put(t, r);
        }
        this.resolvers = Collections.unmodifiableMap(map);
    }

    @Transactional
    public LibraryItem saveToLibrary(
            SourceType sourceType, Long builderRowId,
            String title, String description, Set<String> tagNames,
            Visibility visibility, Long organizationId,
            String callerUsername) {

        SourceContentResolver r = resolvers.get(sourceType);
        if (r == null) throw new IllegalArgumentException("no resolver for " + sourceType);

        SourceContent sc = r.resolve(builderRowId, callerUsername);
        User caller = userRepo.findByUsername(callerUsername)
            .orElseThrow(() -> new IllegalStateException("caller not found: " + callerUsername));

        Visibility effectiveVisibility = visibility != null ? visibility : Visibility.PRIVATE;
        if (effectiveVisibility == Visibility.ORGANIZATION && organizationId == null) {
            throw new IllegalArgumentException("organizationId required when visibility=ORGANIZATION");
        }

        Optional<LibraryItem> existingOpt =
            itemRepo.findByCreatedBy_IdAndSourceTypeAndSourceId(caller.getId(), sourceType, sc.sourceId());

        LibraryItem item;
        int nextVersion;
        if (existingOpt.isPresent()) {
            item = existingOpt.get();
            nextVersion = (item.getCurrentVersion() != null
                ? item.getCurrentVersion().getVersionNumber() : 0) + 1;
            // Title/description/tags: only update when caller provided non-null values.
            if (title != null) item.setTitle(title);
            if (description != null) item.setDescription(description);
            // Tags: replace only if non-null set passed.
            if (tagNames != null) item.setTags(resolveTags(tagNames));
        } else {
            item = new LibraryItem();
            item.setItemId(UUID.randomUUID().toString());
            item.setTitle(title != null ? title : sc.defaultTitle());
            item.setDescription(description);
            item.setOscalType(sc.oscalType());
            item.setCreatedBy(caller);
            item.setSourceType(sourceType);
            item.setSourceId(sc.sourceId());
            item.setVisibility(effectiveVisibility);
            if (effectiveVisibility == Visibility.ORGANIZATION) {
                Organization o = new Organization();
                o.setId(organizationId);
                item.setOrganization(o);
            }
            if (effectiveVisibility == Visibility.PUBLIC) {
                item.setPublishedAt(LocalDateTime.now());
                item.setLastPublishedAt(LocalDateTime.now());
            }
            if (tagNames != null) item.setTags(resolveTags(tagNames));
            nextVersion = 1;
        }
        item = itemRepo.save(item);

        // Write the blob.
        String storagePath = libraryStorage.write(
            item.getItemId(), nextVersion, sc.bytes());

        LibraryVersion v = new LibraryVersion();
        v.setLibraryItem(item);
        v.setVersionNumber(nextVersion);
        v.setVersionId(UUID.randomUUID().toString());
        v.setFileName(sc.filename());
        v.setFilePath(storagePath);
        v.setFileSize((long) sc.bytes().length);
        v.setFormat(sc.format());
        v.setOscalTypeSnapshot(sc.oscalType());
        v.setUploadedAt(LocalDateTime.now());
        v.setUploadedBy(caller);
        v = versionRepo.save(v);

        item.setCurrentVersion(v);
        if (existingOpt.isPresent() && item.getVisibility() == Visibility.PUBLIC) {
            item.setLastPublishedAt(LocalDateTime.now());
        }
        return itemRepo.save(item);
    }

    private Set<LibraryTag> resolveTags(Set<String> names) {
        if (names == null || names.isEmpty()) return Collections.emptySet();
        Set<LibraryTag> result = new java.util.HashSet<>();
        for (String name : names) {
            LibraryTag tag = libraryTagRepository.findByName(name)
                .orElseGet(() -> {
                    LibraryTag t = new LibraryTag();
                    t.setName(name);
                    return libraryTagRepository.save(t);
                });
            result.add(tag);
        }
        return result;
    }
}
```

**Tag resolution discovery step:** before writing this method, run:

```bash
grep -rn "class LibraryTagService\|interface LibraryTagRepository\|findByName" \
  back-end/src/main/java/gov/nist/oscal/tools/api/repository/LibraryTagRepository.java \
  back-end/src/main/java/gov/nist/oscal/tools/api/service/ 2>/dev/null
```

If `LibraryTagService.findOrCreate(String)` exists, inject and call it instead of duplicating the logic. If only `LibraryTagRepository` exists, add `findByName` if missing and use the inline implementation above. Either way, inject the dependency in the constructor (`LibraryTagRepository libraryTagRepository` or `LibraryTagService libraryTagService`) — the service must compile and tags must persist before this task completes.

Add a fifth unit test verifying tags persist:

```java
    @Test
    void savePersistsTagsByName() {
        when(catalogResolver.resolve(42L, "alice")).thenReturn(sampleContent());
        when(itemRepo.findByCreatedBy_IdAndSourceTypeAndSourceId(any(), any(), any()))
            .thenReturn(Optional.empty());
        // Stub the tag repo/service to return tag entities for given names.
        // Assert: the saved LibraryItem has tags whose names equal the input set.
    }
```

- [ ] **Step 4: Run — pass.**

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/library/LibraryIngestService.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/library/LibraryIngestServiceTest.java
git commit -m "feat(library): LibraryIngestService — first/append save semantics"
```

---

### Task 20: `SaveToLibraryRequest` DTO

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/library/SaveToLibraryRequest.java`

- [ ] **Step 1: Write the DTO**

```java
package gov.nist.oscal.tools.api.model.library;

import gov.nist.oscal.tools.api.entity.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public class SaveToLibraryRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String description;

    private Set<String> tags;

    private Visibility visibility = Visibility.PRIVATE;   // default

    private Long organizationId;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }
    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
}
```

- [ ] **Step 2: Compile**

```bash
cd back-end && mvn -q compile
```

- [ ] **Step 3: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/model/library/SaveToLibraryRequest.java
git commit -m "feat(library): SaveToLibraryRequest DTO"
```

---

## Group E — Builder save-to-library endpoints

### Task 21: `POST /api/build/catalogs/{id}/save-to-library`

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/CatalogController.java`
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/controller/CatalogSaveToLibraryTest.java`

- [ ] **Step 1: Write the integration test**

```java
package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.*;
import gov.nist.oscal.tools.api.repository.LibraryItemRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CatalogSaveToLibraryTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired LibraryItemRepository libraryRepo;

    @Test
    void firstSaveCreatesLibraryItemLinkedToCatalog() throws Exception {
        // Seed: create a catalog as alice, get its id.
        // Pseudo: Long catalogId = createCatalogAsAlice("My Catalog");
        Long catalogId = 1L;  // replace with actual seeding helper

        Map<String,Object> body = Map.of(
            "title", "My Library Catalog",
            "description", "First save",
            "tags", List.of("compliance"),
            "visibility", "PRIVATE");

        mvc.perform(MockMvcRequestBuilders
                .post("/api/build/catalogs/" + catalogId + "/save-to-library")
                .header("Authorization", "Bearer " + aliceToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.itemId").exists());

        // Verify a LibraryItem exists with sourceType=CATALOG and source_id matching the catalog's UUID.
        List<LibraryItem> items = libraryRepo.findAll();
        assertThat(items).anySatisfy(li -> {
            assertThat(li.getSourceType()).isEqualTo(SourceType.CATALOG);
            assertThat(li.getSourceId()).isNotNull();
        });
    }

    private String aliceToken() { return ""; /* hook to existing JWT helper */ }
}
```

- [ ] **Step 2: Run — should fail (no endpoint).**

- [ ] **Step 3: Add the controller method**

In `CatalogController.java`:

```java
    @Autowired
    private gov.nist.oscal.tools.api.service.library.LibraryIngestService libraryIngestService;

    @Operation(summary = "Save this catalog to the user's library")
    @PostMapping("/{catalogId}/save-to-library")
    public ResponseEntity<?> saveToLibrary(
            @PathVariable Long catalogId,
            @Valid @RequestBody gov.nist.oscal.tools.api.model.library.SaveToLibraryRequest req,
            Principal principal) {
        try {
            gov.nist.oscal.tools.api.entity.LibraryItem saved = libraryIngestService.saveToLibrary(
                gov.nist.oscal.tools.api.entity.SourceType.CATALOG,
                catalogId,
                req.getTitle(), req.getDescription(), req.getTags(),
                req.getVisibility(), req.getOrganizationId(),
                principal.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(gov.nist.oscal.tools.api.model.LibraryItemDto.fromEntity(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
```

Adjust `LibraryItemDto` import to whatever DTO the existing `LibraryController` uses for the response shape.

- [ ] **Step 4: Run — pass.**

```bash
cd back-end && mvn -q test -Dtest=CatalogSaveToLibraryTest
```

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/controller/CatalogController.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/controller/CatalogSaveToLibraryTest.java
git commit -m "feat(catalog): POST /save-to-library endpoint"
```

---

### Task 22: `POST /api/build/profiles/{id}/save-to-library`

Mirror Task 21 with `ProfileController` and `SourceType.PROFILE`.

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/ProfileController.java`
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/controller/ProfileSaveToLibraryTest.java`

- [ ] **Step 1: Test** — copy `CatalogSaveToLibraryTest` skeleton, replace catalog with profile.
- [ ] **Step 2: Endpoint** — same body as Task 21 Step 3, replacing `SourceType.CATALOG` with `SourceType.PROFILE`, path with `/api/build/profiles/{profileId}/save-to-library`.
- [ ] **Step 3: Run + commit** with message `feat(profile): POST /save-to-library endpoint`.

---

### Task 23: `POST /api/build/component-definitions/{id}/save-to-library`

Mirror Task 21 with `ComponentDefinitionController` and `SourceType.COMPONENT_DEFINITION`.

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/ComponentDefinitionController.java`
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/controller/ComponentDefinitionSaveToLibraryTest.java`

- [ ] **Step 1–3:** structurally identical to Task 21 — copy the test skeleton, substitute `ComponentDefinitionController` for `CatalogController`, `SourceType.COMPONENT_DEFINITION` for `SourceType.CATALOG`, path `/api/build/component-definitions/{componentId}/save-to-library`. Commit message: `feat(component-definition): POST /save-to-library endpoint`.

---

### Task 24: `POST /api/build/oscal-documents/{id}/save-to-library`

This endpoint covers SSP, AP, AR, and POAM. The controller resolves the right `SourceType` from `OscalDocument.modelType` before calling the service.

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/OscalDocumentController.java`
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/controller/OscalDocumentSaveToLibraryTest.java`

- [ ] **Step 1: Test — covers all four document kinds**

The test creates one oscal_documents row per `modelType` and runs `POST /save-to-library` against each. Assertion: each one creates a `LibraryItem` with the correct `sourceType` and `oscalType`.

- [ ] **Step 2: Add the controller method**

```java
    @Autowired
    private gov.nist.oscal.tools.api.service.library.LibraryIngestService libraryIngestService;

    @PostMapping("/{documentId}/save-to-library")
    public ResponseEntity<?> saveToLibrary(
            @PathVariable Long documentId,
            @Valid @RequestBody gov.nist.oscal.tools.api.model.library.SaveToLibraryRequest req,
            Principal principal) {
        try {
            gov.nist.oscal.tools.api.entity.OscalDocument doc =
                oscalDocumentService.requireById(documentId);
            gov.nist.oscal.tools.api.entity.SourceType st = switch (doc.getModelType()) {
                case "SYSTEM_SECURITY_PLAN" -> gov.nist.oscal.tools.api.entity.SourceType.SSP;
                case "ASSESSMENT_PLAN" -> gov.nist.oscal.tools.api.entity.SourceType.AP;
                case "ASSESSMENT_RESULTS" -> gov.nist.oscal.tools.api.entity.SourceType.AR;
                case "PLAN_OF_ACTION_AND_MILESTONES" -> gov.nist.oscal.tools.api.entity.SourceType.POAM;
                default -> throw new IllegalStateException("unknown model_type " + doc.getModelType());
            };
            gov.nist.oscal.tools.api.entity.LibraryItem saved =
                libraryIngestService.saveToLibrary(st, documentId,
                    req.getTitle(), req.getDescription(), req.getTags(),
                    req.getVisibility(), req.getOrganizationId(),
                    principal.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(gov.nist.oscal.tools.api.model.LibraryItemDto.fromEntity(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
```

- [ ] **Step 3: Run + commit** with message `feat(oscal-document): POST /save-to-library endpoint covering SSP/AP/AR/POAM`.

---

## Group F — Frontend

### Task 25: Frontend API client — visibility & save-to-library methods

**Files:**
- Modify (or create if absent): `front-end/src/lib/api/library.ts`

- [ ] **Step 1: Add typed methods**

```typescript
import { apiClient } from "./client";   // existing axios/fetch wrapper

export type Visibility = "PRIVATE" | "ORGANIZATION" | "PUBLIC";

export interface SaveToLibraryRequest {
  title: string;
  description?: string;
  tags?: string[];
  visibility: Visibility;
  organizationId?: number;
}

export interface VisibilityChangeRequest {
  visibility: Visibility;
  organizationId?: number;
  reason?: string;
}

export const libraryApi = {
  saveCatalog: (catalogId: number, body: SaveToLibraryRequest) =>
    apiClient.post(`/build/catalogs/${catalogId}/save-to-library`, body),
  saveProfile: (profileId: number, body: SaveToLibraryRequest) =>
    apiClient.post(`/build/profiles/${profileId}/save-to-library`, body),
  saveComponentDefinition: (componentId: number, body: SaveToLibraryRequest) =>
    apiClient.post(`/build/component-definitions/${componentId}/save-to-library`, body),
  saveOscalDocument: (documentId: number, body: SaveToLibraryRequest) =>
    apiClient.post(`/build/oscal-documents/${documentId}/save-to-library`, body),
  changeVisibility: (itemId: string, body: VisibilityChangeRequest) =>
    apiClient.patch(`/library/${itemId}/visibility`, body),
};
```

Adjust import path of `apiClient` to whatever the existing client export is named (`apiClient`, `api`, or similar). If `front-end/src/lib/api/library.ts` already exists with a different shape, append to it instead of replacing.

- [ ] **Step 2: Compile / typecheck**

```bash
cd front-end && npx tsc --noEmit
```

Expected: no type errors.

- [ ] **Step 3: Commit**

```bash
git add front-end/src/lib/api/library.ts
git commit -m "feat(library): frontend API client for save-to-library + visibility"
```

---

### Task 26: `<VisibilityBadge>` component

**Files:**
- Create: `front-end/src/components/library/VisibilityBadge.tsx`
- Create: `front-end/__tests__/components/library/VisibilityBadge.test.tsx`

- [ ] **Step 1: Write the test**

```tsx
import { render, screen } from "@testing-library/react";
import { VisibilityBadge } from "@/components/library/VisibilityBadge";

describe("<VisibilityBadge>", () => {
  it("renders Private with gray styling", () => {
    render(<VisibilityBadge visibility="PRIVATE" />);
    const el = screen.getByText(/private/i);
    expect(el).toBeInTheDocument();
    expect(el.className).toMatch(/gray|slate/);
  });
  it("renders Organization with blue styling", () => {
    render(<VisibilityBadge visibility="ORGANIZATION" />);
    expect(screen.getByText(/organization/i).className).toMatch(/blue/);
  });
  it("renders Public with green styling", () => {
    render(<VisibilityBadge visibility="PUBLIC" />);
    expect(screen.getByText(/public/i).className).toMatch(/green/);
  });
});
```

- [ ] **Step 2: Run — fail**

```bash
cd front-end && npm test -- VisibilityBadge
```

- [ ] **Step 3: Implement**

```tsx
import type { Visibility } from "@/lib/api/library";

const styles: Record<Visibility, string> = {
  PRIVATE: "bg-slate-200 text-slate-700",
  ORGANIZATION: "bg-blue-100 text-blue-800",
  PUBLIC: "bg-green-100 text-green-800",
};
const labels: Record<Visibility, string> = {
  PRIVATE: "Private",
  ORGANIZATION: "Organization",
  PUBLIC: "Public",
};

export function VisibilityBadge({ visibility }: { visibility: Visibility }) {
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${styles[visibility]}`}>
      {labels[visibility]}
    </span>
  );
}
```

- [ ] **Step 4: Run — pass.**

- [ ] **Step 5: Commit**

```bash
git add front-end/src/components/library/VisibilityBadge.tsx \
        front-end/__tests__/components/library/VisibilityBadge.test.tsx
git commit -m "feat(library): VisibilityBadge component"
```

---

### Task 27: `<SaveToLibraryModal>` component

**Files:**
- Create: `front-end/src/components/library/SaveToLibraryModal.tsx`
- Create: `front-end/__tests__/components/library/SaveToLibraryModal.test.tsx`

- [ ] **Step 1: Write the test**

```tsx
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { SaveToLibraryModal } from "@/components/library/SaveToLibraryModal";

describe("<SaveToLibraryModal>", () => {
  it("requires a title before submit is enabled", async () => {
    const onSubmit = jest.fn();
    render(<SaveToLibraryModal open onClose={() => {}} onSubmit={onSubmit}
                                defaultTitle="" />);
    const button = screen.getByRole("button", { name: /save to library/i });
    expect(button).toBeDisabled();
    await userEvent.type(screen.getByLabelText(/title/i), "Hello");
    expect(button).not.toBeDisabled();
  });

  it("submits the form payload", async () => {
    const onSubmit = jest.fn();
    render(<SaveToLibraryModal open onClose={() => {}} onSubmit={onSubmit}
                                defaultTitle="My Catalog" />);
    await userEvent.click(screen.getByLabelText(/public/i));
    fireEvent.click(screen.getByRole("button", { name: /save to library/i }));
    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      title: "My Catalog",
      visibility: "PUBLIC",
    })));
  });

  it("disables Organization radio when user has no organization", () => {
    render(<SaveToLibraryModal open onClose={() => {}} onSubmit={() => {}}
                                defaultTitle="" userOrganizationId={null} />);
    expect(screen.getByLabelText(/organization/i)).toBeDisabled();
  });
});
```

- [ ] **Step 2: Run — fail.**

- [ ] **Step 3: Implement**

```tsx
"use client";
import { useState } from "react";
import type { SaveToLibraryRequest, Visibility } from "@/lib/api/library";

interface Props {
  open: boolean;
  onClose: () => void;
  onSubmit: (req: SaveToLibraryRequest) => Promise<void> | void;
  defaultTitle: string;
  defaultDescription?: string;
  userOrganizationId?: number | null;
}

export function SaveToLibraryModal({
  open, onClose, onSubmit, defaultTitle, defaultDescription, userOrganizationId,
}: Props) {
  const [title, setTitle] = useState(defaultTitle);
  const [description, setDescription] = useState(defaultDescription ?? "");
  const [tagInput, setTagInput] = useState("");
  const [tags, setTags] = useState<string[]>([]);
  const [visibility, setVisibility] = useState<Visibility>("PRIVATE");
  const [submitting, setSubmitting] = useState(false);

  if (!open) return null;

  const valid = title.trim().length > 0 &&
    (visibility !== "ORGANIZATION" || userOrganizationId != null);

  const submit = async () => {
    setSubmitting(true);
    try {
      await onSubmit({
        title: title.trim(),
        description: description.trim() || undefined,
        tags: tags.length ? tags : undefined,
        visibility,
        organizationId: visibility === "ORGANIZATION" ? userOrganizationId ?? undefined : undefined,
      });
      onClose();
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div role="dialog" aria-modal className="fixed inset-0 bg-black/40 flex items-center justify-center">
      <div className="bg-white rounded-lg shadow-lg max-w-md w-full p-6">
        <h2 className="text-lg font-semibold mb-4">Save to Library</h2>

        <label className="block text-sm font-medium mb-1" htmlFor="stl-title">Title</label>
        <input id="stl-title" className="w-full border rounded px-2 py-1 mb-3"
               value={title} onChange={e => setTitle(e.target.value)} />

        <label className="block text-sm font-medium mb-1" htmlFor="stl-desc">Description</label>
        <textarea id="stl-desc" className="w-full border rounded px-2 py-1 mb-3" rows={3}
                  value={description} onChange={e => setDescription(e.target.value)} />

        <label className="block text-sm font-medium mb-1">Tags</label>
        <div className="flex flex-wrap gap-1 mb-2">
          {tags.map(t => (
            <span key={t} className="bg-slate-100 px-2 py-0.5 rounded text-xs">
              {t}
              <button className="ml-1" onClick={() => setTags(tags.filter(x => x !== t))}>×</button>
            </span>
          ))}
        </div>
        <input className="w-full border rounded px-2 py-1 mb-3" placeholder="Add tag and press Enter"
               value={tagInput}
               onChange={e => setTagInput(e.target.value)}
               onKeyDown={e => {
                 if (e.key === "Enter" && tagInput.trim()) {
                   e.preventDefault();
                   setTags([...tags, tagInput.trim()]);
                   setTagInput("");
                 }
               }} />

        <fieldset className="mb-4">
          <legend className="block text-sm font-medium mb-1">Visibility</legend>
          <label className="block"><input type="radio" name="vis" value="PRIVATE"
            checked={visibility === "PRIVATE"} onChange={() => setVisibility("PRIVATE")} /> Private — only me</label>
          <label className="block"><input type="radio" name="vis" value="ORGANIZATION"
            checked={visibility === "ORGANIZATION"}
            disabled={userOrganizationId == null}
            onChange={() => setVisibility("ORGANIZATION")} /> Organization — my team</label>
          <label className="block"><input type="radio" name="vis" value="PUBLIC"
            checked={visibility === "PUBLIC"} onChange={() => setVisibility("PUBLIC")} /> Public — visible at /catalog</label>
        </fieldset>

        <div className="flex justify-end gap-2">
          <button className="px-3 py-1 border rounded" onClick={onClose}>Cancel</button>
          <button className="px-3 py-1 bg-blue-600 text-white rounded disabled:opacity-50"
                  disabled={!valid || submitting} onClick={submit}>
            Save to Library
          </button>
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Run — pass.**

- [ ] **Step 5: Commit**

```bash
git add front-end/src/components/library/SaveToLibraryModal.tsx \
        front-end/__tests__/components/library/SaveToLibraryModal.test.tsx
git commit -m "feat(library): SaveToLibraryModal component"
```

---

### Task 28: Visibility column + filter on `/library` page

**Files:**
- Modify: `front-end/src/app/library/page.tsx`

- [ ] **Step 1: Locate the table render and the existing filter bar**

```bash
grep -n "oscalType\|filter\|Search" front-end/src/app/library/page.tsx | head -30
```

- [ ] **Step 2: Add `visibility` column rendering and filter UI**

In the listing table:

```tsx
<th>Visibility</th>
{/* ... per-row: */}
<td><VisibilityBadge visibility={item.visibility} /></td>
```

In the filter bar, add a select:

```tsx
<select value={visibilityFilter} onChange={e => setVisibilityFilter(e.target.value as VisibilityFilter)}>
  <option value="">All visibilities</option>
  <option value="PRIVATE">Private</option>
  <option value="ORGANIZATION">Organization</option>
  <option value="PUBLIC">Public</option>
</select>
```

Pass the filter to the existing query call: `libraryApi.list({ visibility: visibilityFilter || undefined, ... })`. Add a corresponding `visibility` query param to the backend list endpoint if it doesn't already accept it (small backend change — extend `LibraryService.listForUser` and the controller to forward the filter).

- [ ] **Step 3: Manual verify**

Run `./dev.sh`, log in, visit `/library`, switch the filter to PUBLIC, confirm only PUBLIC items show.

- [ ] **Step 4: Commit**

```bash
git add front-end/src/app/library/page.tsx \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/LibraryService.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/controller/LibraryController.java
git commit -m "feat(library): visibility column + filter on library page"
```

---

### Task 29: Visibility action menu

**Files:**
- Create: `front-end/src/components/library/VisibilityActionMenu.tsx`
- Modify: `front-end/src/app/library/page.tsx`

- [ ] **Step 1: Write the menu**

```tsx
"use client";
import { libraryApi, type Visibility } from "@/lib/api/library";
import { useState } from "react";

interface Props {
  itemId: string;
  currentVisibility: Visibility;
  isCreator: boolean;
  isSuperAdmin: boolean;
  onChanged: () => void;
}

export function VisibilityActionMenu({
  itemId, currentVisibility, isCreator, isSuperAdmin, onChanged,
}: Props) {
  const [pending, setPending] = useState(false);

  const change = async (next: Visibility, reason?: string) => {
    setPending(true);
    try {
      await libraryApi.changeVisibility(itemId, { visibility: next, reason });
      onChanged();
    } finally {
      setPending(false);
    }
  };

  if (!isCreator && !isSuperAdmin) return null;

  return (
    <div className="inline-flex gap-1">
      {currentVisibility !== "PRIVATE" && (
        <button disabled={pending} onClick={() => change("PRIVATE")}
                className="text-xs px-2 py-0.5 border rounded">Make Private</button>
      )}
      {currentVisibility !== "ORGANIZATION" && isCreator && (
        <button disabled={pending} onClick={() => change("ORGANIZATION")}
                className="text-xs px-2 py-0.5 border rounded">Share with Org</button>
      )}
      {currentVisibility !== "PUBLIC" && (
        <button disabled={pending} onClick={() => change("PUBLIC")}
                className="text-xs px-2 py-0.5 border rounded bg-green-50">Publish</button>
      )}
      {currentVisibility === "PUBLIC" && isSuperAdmin && !isCreator && (
        <button disabled={pending} onClick={() => {
          const reason = window.prompt("Reason for force-unpublish (required):");
          if (reason && reason.trim()) change("PRIVATE", reason);
        }}
                className="text-xs px-2 py-0.5 border rounded bg-red-50">Force unpublish</button>
      )}
    </div>
  );
}
```

- [ ] **Step 2: Wire into the table row**

In `library/page.tsx`, render the action menu in a new "Actions" column. Pass `isCreator = item.createdBy.id === currentUser.id`, `isSuperAdmin = currentUser.globalRole === 'SUPER_ADMIN'`. The `onChanged` callback should refetch the list.

- [ ] **Step 3: Manual verify**

Hit each action against an item you own and against an item owned by another user (where you're SUPER_ADMIN). Confirm audit events are written via:

```bash
docker exec oscal-postgres-dev psql -U oscal_user -d oscal_dev -c \
  "SELECT event_type, resource, metadata FROM audit_events \
   WHERE event_type LIKE 'LIBRARY_ITEM%' ORDER BY id DESC LIMIT 5;"
```

- [ ] **Step 4: Commit**

```bash
git add front-end/src/components/library/VisibilityActionMenu.tsx \
        front-end/src/app/library/page.tsx
git commit -m "feat(library): visibility action menu (publish / unpublish / share)"
```

---

### Task 30: "Public" banner on owned PUBLIC items

**Files:**
- Modify: `front-end/src/app/library/page.tsx` (and any `/library/[itemId]` detail page if one exists)

- [ ] **Step 1: Render the banner**

Above the item title, when `item.visibility === 'PUBLIC' && item.createdBy.id === currentUser.id`:

```tsx
{item.visibility === "PUBLIC" && item.createdBy.id === currentUser?.id && (
  <div className="bg-green-50 border-l-4 border-green-500 p-3 mb-3 text-sm">
    This item is public — visible at <a className="underline" href={`/catalog/${item.itemId}`}>/catalog/{item.itemId}</a>.
  </div>
)}
```

The `/catalog/<itemId>` route doesn't exist yet (Phase 2). Linking to it now is fine — it'll 404 in Phase 1, then start working when Phase 2 lands.

- [ ] **Step 2: Manual verify** the banner appears on a PUBLIC item.

- [ ] **Step 3: Commit**

```bash
git add front-end/src/app/library/page.tsx
git commit -m "feat(library): show public banner on items owned by the user"
```

---

### Task 31: Wire `<SaveToLibraryModal>` into the catalog builder

**Files:**
- Modify: the catalog builder's save/finish step. Locate via:

```bash
grep -rn "CatalogBuilderWizard\|onFinish\|saveCatalog" front-end/src/app/build/ | head
```

- [ ] **Step 1: Add a "Save to Library" button to the final step**

Next to the existing save/finish buttons:

```tsx
const [showSaveToLib, setShowSaveToLib] = useState(false);

<button className="px-3 py-1 border rounded" onClick={() => setShowSaveToLib(true)}>
  Save to Library
</button>

{showSaveToLib && (
  <SaveToLibraryModal
    open
    onClose={() => setShowSaveToLib(false)}
    defaultTitle={catalog.title}
    defaultDescription={catalog.description}
    userOrganizationId={currentUser?.organizationId ?? null}
    onSubmit={async (req) => {
      await libraryApi.saveCatalog(catalog.id, req);
      toast.success("Saved to Library");
    }}
  />
)}
```

- [ ] **Step 2: Manual verify**

Build a catalog → finish → click Save to Library → fill the modal → submit → visit `/library` → confirm the item appears with the right visibility.

- [ ] **Step 3: Commit**

```bash
git add front-end/src/app/build/...
git commit -m "feat(catalog): Save to Library button in catalog builder"
```

---

### Task 32: Wire `<SaveToLibraryModal>` into the profile builder

Mirror Task 31. Use `libraryApi.saveProfile(profile.id, req)`.

- [ ] **Step 1–3:** as Task 31. Commit message: `feat(profile): Save to Library button in profile builder`.

---

### Task 33: Wire `<SaveToLibraryModal>` into the component builder

Mirror Task 31. Use `libraryApi.saveComponentDefinition(componentDef.id, req)`. Locate via `grep -rn "ComponentBuilderWizard" front-end/src/app/build/`.

- [ ] **Step 1–3:** as Task 31. Commit message: `feat(component): Save to Library button in component builder`.

---

### Task 34: Wire `<SaveToLibraryModal>` into the SSP/AP/AR/POAM builder

The SSP/AP/AR/POAM builders use the shared `OscalDocumentWizard` component. One modification covers all four. Use `libraryApi.saveOscalDocument(document.id, req)`.

- [ ] **Step 1–3:** as Task 31. Commit message: `feat(oscal-document): Save to Library button in SSP/AP/AR/POAM builder`.

---

## Group G — Verification

### Task 35: End-to-end smoke test

- [ ] **Step 1: Stop, restart, and log in**

```bash
./stop.sh && ./dev.sh
```

Wait for both ports to be ready. Browse to `http://localhost:3010`, log in.

- [ ] **Step 2: For each builder, run the round-trip**

For Catalog, Profile, Component, SSP, AP, AR, POAM:
1. Build a minimal example.
2. Click "Save to Library" — set visibility to PRIVATE first.
3. Visit `/library`, confirm the item appears with `Private` badge.
4. Click "Publish" → confirm badge flips to `Public` and the public banner appears.
5. Click "Make Private" → confirm reversion.
6. Build a v2 of the same source → click "Save to Library" → confirm in `/library` that no new item appears (linked update).
7. Confirm version count incremented:
```bash
docker exec oscal-postgres-dev psql -U oscal_user -d oscal_dev -c \
  "SELECT i.title, COUNT(v.id) FROM library_items i JOIN library_versions v ON v.library_item_id=i.id GROUP BY i.title;"
```

- [ ] **Step 3: Audit log verification**

```bash
docker exec oscal-postgres-dev psql -U oscal_user -d oscal_dev -c \
  "SELECT event_type, resource, timestamp FROM audit_events \
   WHERE event_type LIKE 'LIBRARY_ITEM%' ORDER BY id DESC LIMIT 20;"
```

Expected: `LIBRARY_ITEM_PUBLISHED` and `LIBRARY_ITEM_UNPUBLISHED` rows for each publish/unpublish action.

- [ ] **Step 4: Commit smoke-test notes (if any tweaks needed)**

If verification surfaces small gaps, fix them and commit with `fix(library): <whatever>`.

- [ ] **Step 5: Tag and announce**

```bash
git log --oneline ai-foundation -- back-end/src/main/java/.../service/library back-end/src/main/resources/db/migration/V1.1__library_visibility_and_source.sql back-end/src/main/resources/db/migration/V1.2__library_audit_event_types.sql front-end/src/components/library | head -50
```

Verify the commit history is clean. Tell the user: "Phase 1 complete. Ready for Phase 2 (public catalog UI) plan when you are."

---

## What Phase 1 deliberately doesn't ship

These belong to Phases 2 and 3, not this plan:

- `/api/public/catalog/*` and `/api/public/v1/*` endpoints
- `(public)` Next.js route group, `/catalog` and `/catalog/[itemId]` pages
- `api_keys` table, `ApiKeyService`, `ApiKeyAuthenticationFilter`
- `RateLimitFilter` extension for API-key buckets
- `/account/api-keys` page with example code
- Any change to the public navigation

PUBLIC items will accumulate in the database during Phase 1 but won't be reachable to anonymous users until Phase 2 lands. The "This item is public" banner links to a route (`/catalog/{itemId}`) that 404s in Phase 1; that's intentional — the link starts working when Phase 2 ships.
