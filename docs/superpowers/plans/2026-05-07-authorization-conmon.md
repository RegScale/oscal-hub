# Continuous Monitoring (ConMon) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Continuous Monitoring tab stub with a working surface that lets in-org users upload POAM artifacts (OSCAL JSON/XML/YAML or FedRAMP POA&M `.xlsx`) on a recurring basis, view a snapshot history, see reconciliation against the prior snapshot, and analyze trends across all snapshots. **PR 4 of 4** in the broader Authorizations expansion (spec at `docs/superpowers/specs/2026-05-06-conmon-and-documents-design.md`).

**Architecture:** Each upload is a dated snapshot. Backend parses the upload (OSCAL via existing `OscalBindingContext`, FedRAMP Excel via Apache POI), persists per-item rows with a hybrid status-derivation rule (FedRAMP `status` prop → linked-finding rollup → unknown), then computes a reconciliation row against the prior snapshot. Frontend renders KPI tiles, a reconciliation banner, five Recharts visualizations, a snapshot history table, and an items drawer. Access enforced via the existing `AuthorizationAccessGuard.requireUploadConMon` for writes.

**Tech Stack:** Spring Boot 4.0.6, Flyway, Apache POI 5.2.5 (new dep), `OscalBindingContext` (existing), JUnit 5 + Mockito; Next.js + shadcn UI + Recharts (already wired via `LazyCharts`).

---

## Conventions

- Working tree has unrelated WIP. Stage explicit paths only. NEVER `git add -A`.
- `@DataJpaTest` doesn't exist in Spring Boot 4 — use `@SpringBootTest + @Transactional + @PersistenceContext` for repo integration tests.
- 404 vs 403 leakage matches PR 1-3 conventions.
- Status decisions:
  - **Status enum**: code-level `OPEN | CLOSED | UNKNOWN`.
  - **JSONB**: project has no JSONB pattern; use `TEXT` columns + Jackson `ObjectMapper` (matching existing `Artifact.extractedVariables` pattern).
  - **OSCAL POAM model**: `gov.nist.secauto.oscal.lib.model.PlanOfActionAndMilestones` already accessible via `OscalBindingContext.instance().newDeserializer(...)` (used by `ConversionService`).
  - **POAM item getter names**: implementer must verify against the OSCAL library jar (`mvn dependency:sources` if needed). Plan code uses standard Metaschema-bound names: `getUuid()`, `getTitle()`, `getDescription()`, `getProps()`, `getRelatedFindings()`, `getRelatedRisks()`. Adjust if real names differ.
  - **Multipart**: 50MB limit set in PR 3 — sufficient for POAMs.

---

## File Structure

**New backend:**
- `back-end/src/main/resources/db/migration/V1.9__continuous_monitoring.sql`
- `back-end/src/main/java/gov/nist/oscal/tools/api/entity/ConMonItemStatus.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/entity/ConMonSourceFormat.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/entity/ConMonSnapshot.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/entity/ConMonPoamItem.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/entity/ConMonReconciliation.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/repository/ConMonSnapshotRepository.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/repository/ConMonPoamItemRepository.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/repository/ConMonReconciliationRepository.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/service/conmon/ParsedPoamItem.java` (intermediate parser-output record)
- `back-end/src/main/java/gov/nist/oscal/tools/api/service/conmon/ParsedPoam.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/service/conmon/PoamStatusDeriver.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/service/conmon/OscalPoamParser.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/service/conmon/FedrampPoamExcelParser.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/service/conmon/ConMonReconciliationService.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/service/conmon/ConMonAnalyticsService.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/service/conmon/ConMonService.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/exception/UnsupportedConMonFormatException.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/model/conmon/ConMonSnapshotSummary.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/model/conmon/ConMonPoamItemResponse.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/model/conmon/ConMonReconciliationResponse.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/model/conmon/ConMonAnalyticsResponse.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/controller/ContinuousMonitoringController.java`
- Tests (5): `PoamStatusDeriverTest`, `OscalPoamParserTest`, `FedrampPoamExcelParserTest`, `ConMonReconciliationServiceTest`, `ContinuousMonitoringIntegrationTest`

**Modified backend:**
- `back-end/pom.xml` — add `poi-ooxml`.

**New frontend (under `front-end/src/app/authorizations/authorization/[authorizationId]/_tabs/conmon/`):**
- `kpi-tiles.tsx`, `reconciliation-banner.tsx`, `analytics-dashboard.tsx`, `snapshot-history-table.tsx`, `items-drawer.tsx`, `upload-snapshot-dialog.tsx`

**Modified frontend:**
- `_tabs/conmon-tab.tsx` (replace stub)
- `[authorizationId]/page.tsx` (pass `authorization` prop)
- `front-end/src/types/oscal.ts` (ConMon types)
- `front-end/src/lib/api-client.ts` (ConMon methods)

**New frontend tests (3):** `__tests__/conmon-tab.test.tsx`, `__tests__/upload-snapshot-dialog.test.tsx`, `__tests__/reconciliation-banner.test.tsx`.

---

## Task 1: Migration V1.9

Create `back-end/src/main/resources/db/migration/V1.9__continuous_monitoring.sql`:

```sql
-- V1.9 — Continuous Monitoring snapshots, POAM items, and reconciliations.
-- See docs/superpowers/specs/2026-05-06-conmon-and-documents-design.md.

CREATE TABLE IF NOT EXISTS conmon_snapshots (
    id                     BIGSERIAL PRIMARY KEY,
    authorization_id       BIGINT NOT NULL REFERENCES authorizations(id) ON DELETE CASCADE,
    uploaded_by            BIGINT NOT NULL REFERENCES users(id),
    uploaded_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_format          VARCHAR(32) NOT NULL,
    original_filename      VARCHAR(512) NOT NULL,
    file_storage_path      VARCHAR(1024) NOT NULL,
    oscal_uuid             VARCHAR(64),
    oscal_version          VARCHAR(16),
    metadata_title         VARCHAR(512),
    metadata_last_modified TIMESTAMP,
    summary_open_count     INT NOT NULL DEFAULT 0,
    summary_closed_count   INT NOT NULL DEFAULT 0,
    summary_unknown_count  INT NOT NULL DEFAULT 0,
    notes                  TEXT,
    CONSTRAINT ck_conmon_snapshots_format CHECK (source_format IN
        ('OSCAL_JSON','OSCAL_XML','OSCAL_YAML','FEDRAMP_XLSX'))
);
CREATE INDEX IF NOT EXISTS idx_conmon_snapshots_auth
    ON conmon_snapshots (authorization_id, uploaded_at DESC);

CREATE TABLE IF NOT EXISTS conmon_poam_items (
    id                          BIGSERIAL PRIMARY KEY,
    snapshot_id                 BIGINT NOT NULL REFERENCES conmon_snapshots(id) ON DELETE CASCADE,
    external_id                 VARCHAR(128) NOT NULL,
    title                       VARCHAR(1024) NOT NULL,
    description                 TEXT,
    status                      VARCHAR(16) NOT NULL,
    raw_status                  VARCHAR(64),
    severity                    VARCHAR(16),
    weakness_source             VARCHAR(256),
    scheduled_completion_date   DATE,
    actual_completion_date      DATE,
    point_of_contact            VARCHAR(256),
    risk_rating                 VARCHAR(64),
    extra_props_json            TEXT,
    CONSTRAINT ck_conmon_poam_items_status CHECK (status IN ('OPEN','CLOSED','UNKNOWN')),
    CONSTRAINT ck_conmon_poam_items_severity CHECK (severity IS NULL OR severity IN
        ('LOW','MODERATE','HIGH','CRITICAL'))
);
CREATE INDEX IF NOT EXISTS idx_conmon_poam_items_snap_status
    ON conmon_poam_items (snapshot_id, status);
CREATE INDEX IF NOT EXISTS idx_conmon_poam_items_snap_extid
    ON conmon_poam_items (snapshot_id, external_id);

CREATE TABLE IF NOT EXISTS conmon_reconciliations (
    id                   BIGSERIAL PRIMARY KEY,
    snapshot_id          BIGINT NOT NULL REFERENCES conmon_snapshots(id) ON DELETE CASCADE UNIQUE,
    previous_snapshot_id BIGINT NOT NULL REFERENCES conmon_snapshots(id),
    new_count            INT NOT NULL DEFAULT 0,
    closed_count         INT NOT NULL DEFAULT 0,
    reopened_count       INT NOT NULL DEFAULT 0,
    still_open_count     INT NOT NULL DEFAULT 0,
    removed_count        INT NOT NULL DEFAULT 0,
    changed_count        INT NOT NULL DEFAULT 0
);
```

Commit:
```bash
git add back-end/src/main/resources/db/migration/V1.9__continuous_monitoring.sql
git diff --cached --stat
git commit -m "db(authorizations): V1.9 add continuous_monitoring tables"
```

---

## Task 2: Status + format enums

Create `back-end/src/main/java/gov/nist/oscal/tools/api/entity/ConMonItemStatus.java`:

```java
package gov.nist.oscal.tools.api.entity;

public enum ConMonItemStatus { OPEN, CLOSED, UNKNOWN }
```

Create `back-end/src/main/java/gov/nist/oscal/tools/api/entity/ConMonSourceFormat.java`:

```java
package gov.nist.oscal.tools.api.entity;

public enum ConMonSourceFormat {
    OSCAL_JSON,
    OSCAL_XML,
    OSCAL_YAML,
    FEDRAMP_XLSX;

    public static ConMonSourceFormat fromFilename(String filename) {
        if (filename == null) return null;
        String lower = filename.toLowerCase();
        if (lower.endsWith(".json")) return OSCAL_JSON;
        if (lower.endsWith(".xml")) return OSCAL_XML;
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) return OSCAL_YAML;
        if (lower.endsWith(".xlsx")) return FEDRAMP_XLSX;
        return null;
    }
}
```

Stage both. Commit:
```
feat(conmon): add ConMonItemStatus and ConMonSourceFormat enums
```

---

## Task 3: Three entities (one commit)

### `ConMonSnapshot.java`

```java
package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conmon_snapshots")
public class ConMonSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "authorization_id", nullable = false)
    private Authorization authorization;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "source_format", nullable = false, length = 32)
    private ConMonSourceFormat sourceFormat;

    @Column(name = "original_filename", nullable = false, length = 512)
    private String originalFilename;

    @Column(name = "file_storage_path", nullable = false, length = 1024)
    private String fileStoragePath;

    @Column(name = "oscal_uuid", length = 64)
    private String oscalUuid;

    @Column(name = "oscal_version", length = 16)
    private String oscalVersion;

    @Column(name = "metadata_title", length = 512)
    private String metadataTitle;

    @Column(name = "metadata_last_modified")
    private LocalDateTime metadataLastModified;

    @Column(name = "summary_open_count", nullable = false)
    private int summaryOpenCount;

    @Column(name = "summary_closed_count", nullable = false)
    private int summaryClosedCount;

    @Column(name = "summary_unknown_count", nullable = false)
    private int summaryUnknownCount;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ConMonPoamItem> items = new ArrayList<>();

    public ConMonSnapshot() {}

    // Standard manual getters/setters for all fields above:
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Authorization getAuthorization() { return authorization; }
    public void setAuthorization(Authorization authorization) { this.authorization = authorization; }
    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public ConMonSourceFormat getSourceFormat() { return sourceFormat; }
    public void setSourceFormat(ConMonSourceFormat sourceFormat) { this.sourceFormat = sourceFormat; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String s) { this.originalFilename = s; }
    public String getFileStoragePath() { return fileStoragePath; }
    public void setFileStoragePath(String s) { this.fileStoragePath = s; }
    public String getOscalUuid() { return oscalUuid; }
    public void setOscalUuid(String s) { this.oscalUuid = s; }
    public String getOscalVersion() { return oscalVersion; }
    public void setOscalVersion(String s) { this.oscalVersion = s; }
    public String getMetadataTitle() { return metadataTitle; }
    public void setMetadataTitle(String s) { this.metadataTitle = s; }
    public LocalDateTime getMetadataLastModified() { return metadataLastModified; }
    public void setMetadataLastModified(LocalDateTime t) { this.metadataLastModified = t; }
    public int getSummaryOpenCount() { return summaryOpenCount; }
    public void setSummaryOpenCount(int n) { this.summaryOpenCount = n; }
    public int getSummaryClosedCount() { return summaryClosedCount; }
    public void setSummaryClosedCount(int n) { this.summaryClosedCount = n; }
    public int getSummaryUnknownCount() { return summaryUnknownCount; }
    public void setSummaryUnknownCount(int n) { this.summaryUnknownCount = n; }
    public String getNotes() { return notes; }
    public void setNotes(String s) { this.notes = s; }
    public List<ConMonPoamItem> getItems() { return items; }
    public void setItems(List<ConMonPoamItem> items) { this.items = items; }
}
```

### `ConMonPoamItem.java`

```java
package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "conmon_poam_items",
       indexes = {
           @Index(name = "idx_conmon_poam_items_snap_status", columnList = "snapshot_id, status"),
           @Index(name = "idx_conmon_poam_items_snap_extid", columnList = "snapshot_id, externalId")
       })
public class ConMonPoamItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private ConMonSnapshot snapshot;

    @Column(name = "external_id", nullable = false, length = 128)
    private String externalId;

    @Column(nullable = false, length = 1024)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ConMonItemStatus status;

    @Column(name = "raw_status", length = 64)
    private String rawStatus;

    @Column(length = 16)
    private String severity;

    @Column(name = "weakness_source", length = 256)
    private String weaknessSource;

    @Column(name = "scheduled_completion_date")
    private LocalDate scheduledCompletionDate;

    @Column(name = "actual_completion_date")
    private LocalDate actualCompletionDate;

    @Column(name = "point_of_contact", length = 256)
    private String pointOfContact;

    @Column(name = "risk_rating", length = 64)
    private String riskRating;

    @Column(name = "extra_props_json", columnDefinition = "TEXT")
    private String extraPropsJson;

    public ConMonPoamItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ConMonSnapshot getSnapshot() { return snapshot; }
    public void setSnapshot(ConMonSnapshot s) { this.snapshot = s; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String s) { this.externalId = s; }
    public String getTitle() { return title; }
    public void setTitle(String s) { this.title = s; }
    public String getDescription() { return description; }
    public void setDescription(String s) { this.description = s; }
    public ConMonItemStatus getStatus() { return status; }
    public void setStatus(ConMonItemStatus s) { this.status = s; }
    public String getRawStatus() { return rawStatus; }
    public void setRawStatus(String s) { this.rawStatus = s; }
    public String getSeverity() { return severity; }
    public void setSeverity(String s) { this.severity = s; }
    public String getWeaknessSource() { return weaknessSource; }
    public void setWeaknessSource(String s) { this.weaknessSource = s; }
    public LocalDate getScheduledCompletionDate() { return scheduledCompletionDate; }
    public void setScheduledCompletionDate(LocalDate d) { this.scheduledCompletionDate = d; }
    public LocalDate getActualCompletionDate() { return actualCompletionDate; }
    public void setActualCompletionDate(LocalDate d) { this.actualCompletionDate = d; }
    public String getPointOfContact() { return pointOfContact; }
    public void setPointOfContact(String s) { this.pointOfContact = s; }
    public String getRiskRating() { return riskRating; }
    public void setRiskRating(String s) { this.riskRating = s; }
    public String getExtraPropsJson() { return extraPropsJson; }
    public void setExtraPropsJson(String s) { this.extraPropsJson = s; }
}
```

### `ConMonReconciliation.java`

```java
package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "conmon_reconciliations")
public class ConMonReconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false, unique = true)
    private ConMonSnapshot snapshot;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_snapshot_id", nullable = false)
    private ConMonSnapshot previousSnapshot;

    @Column(name = "new_count", nullable = false) private int newCount;
    @Column(name = "closed_count", nullable = false) private int closedCount;
    @Column(name = "reopened_count", nullable = false) private int reopenedCount;
    @Column(name = "still_open_count", nullable = false) private int stillOpenCount;
    @Column(name = "removed_count", nullable = false) private int removedCount;
    @Column(name = "changed_count", nullable = false) private int changedCount;

    public ConMonReconciliation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ConMonSnapshot getSnapshot() { return snapshot; }
    public void setSnapshot(ConMonSnapshot s) { this.snapshot = s; }
    public ConMonSnapshot getPreviousSnapshot() { return previousSnapshot; }
    public void setPreviousSnapshot(ConMonSnapshot s) { this.previousSnapshot = s; }
    public int getNewCount() { return newCount; }
    public void setNewCount(int n) { this.newCount = n; }
    public int getClosedCount() { return closedCount; }
    public void setClosedCount(int n) { this.closedCount = n; }
    public int getReopenedCount() { return reopenedCount; }
    public void setReopenedCount(int n) { this.reopenedCount = n; }
    public int getStillOpenCount() { return stillOpenCount; }
    public void setStillOpenCount(int n) { this.stillOpenCount = n; }
    public int getRemovedCount() { return removedCount; }
    public void setRemovedCount(int n) { this.removedCount = n; }
    public int getChangedCount() { return changedCount; }
    public void setChangedCount(int n) { this.changedCount = n; }
}
```

`mvn compile -DskipTests` → BUILD SUCCESS.

Commit (3 files):
```
feat(conmon): add ConMonSnapshot, ConMonPoamItem, ConMonReconciliation entities
```

---

## Task 4: Repositories (one commit)

### `ConMonSnapshotRepository.java`

```java
package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.ConMonSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConMonSnapshotRepository extends JpaRepository<ConMonSnapshot, Long> {
    List<ConMonSnapshot> findByAuthorizationOrderByUploadedAtDesc(Authorization authorization);
    Optional<ConMonSnapshot> findByIdAndAuthorization(Long id, Authorization authorization);
    Optional<ConMonSnapshot> findFirstByAuthorizationOrderByUploadedAtDesc(Authorization authorization);
}
```

### `ConMonPoamItemRepository.java`

```java
package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import gov.nist.oscal.tools.api.entity.ConMonPoamItem;
import gov.nist.oscal.tools.api.entity.ConMonSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConMonPoamItemRepository extends JpaRepository<ConMonPoamItem, Long> {
    List<ConMonPoamItem> findBySnapshot(ConMonSnapshot snapshot);

    @Query("SELECT i FROM ConMonPoamItem i WHERE i.snapshot = :snapshot " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (:severity IS NULL OR i.severity = :severity) " +
           "AND (:q IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "                OR LOWER(i.externalId) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<ConMonPoamItem> search(
            @Param("snapshot") ConMonSnapshot snapshot,
            @Param("status") ConMonItemStatus status,
            @Param("severity") String severity,
            @Param("q") String q,
            Pageable pageable);
}
```

### `ConMonReconciliationRepository.java`

```java
package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.ConMonReconciliation;
import gov.nist.oscal.tools.api.entity.ConMonSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConMonReconciliationRepository extends JpaRepository<ConMonReconciliation, Long> {
    Optional<ConMonReconciliation> findBySnapshot(ConMonSnapshot snapshot);
}
```

`mvn compile -DskipTests` → BUILD SUCCESS.

Commit (3 files):
```
feat(conmon): add ConMon repositories
```

---

## Task 5: ParsedPoam intermediate types

Two simple records that decouple parsers from JPA entities.

### `service/conmon/ParsedPoamItem.java`

```java
package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;

import java.time.LocalDate;
import java.util.Map;

/**
 * Parser output for a single POAM item — independent of JPA entities so
 * parsers and the persistence layer can be tested separately.
 */
public record ParsedPoamItem(
        String externalId,
        String title,
        String description,
        ConMonItemStatus status,
        String rawStatus,
        String severity,                 // LOW/MODERATE/HIGH/CRITICAL or null
        String weaknessSource,
        LocalDate scheduledCompletionDate,
        LocalDate actualCompletionDate,
        String pointOfContact,
        String riskRating,
        Map<String, Object> extraProps   // Unmodeled fields, serialized to extra_props_json
) {}
```

### `service/conmon/ParsedPoam.java`

```java
package gov.nist.oscal.tools.api.service.conmon;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Parser output for an entire POAM document.
 */
public record ParsedPoam(
        String oscalUuid,           // null for FedRAMP_XLSX
        String oscalVersion,        // null for FedRAMP_XLSX
        String metadataTitle,       // optional
        LocalDateTime metadataLastModified, // optional
        List<ParsedPoamItem> items
) {}
```

`mvn compile -DskipTests` → BUILD SUCCESS.

Commit (2 files):
```
feat(conmon): add ParsedPoam and ParsedPoamItem records
```

---

## Task 6: PoamStatusDeriver (TDD)

Decides `OPEN | CLOSED | UNKNOWN` per the hybrid rule.

### Test first: `back-end/src/test/java/gov/nist/oscal/tools/api/service/conmon/PoamStatusDeriverTest.java`

```java
package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PoamStatusDeriverTest {

    @Test
    void closedKeyword_yieldsClosed() {
        var r = PoamStatusDeriver.derive("closed", List.of());
        assertThat(r.status()).isEqualTo(ConMonItemStatus.CLOSED);
        assertThat(r.rawStatus()).isEqualTo("closed");
    }

    @Test
    void completedKeyword_yieldsClosed() {
        assertThat(PoamStatusDeriver.derive("Completed", List.of()).status())
                .isEqualTo(ConMonItemStatus.CLOSED);
    }

    @Test
    void falsePositive_yieldsClosed() {
        assertThat(PoamStatusDeriver.derive("False-Positive", List.of()).status())
                .isEqualTo(ConMonItemStatus.CLOSED);
    }

    @Test
    void riskAccepted_yieldsOpen() {
        assertThat(PoamStatusDeriver.derive("Risk Accepted", List.of()).status())
                .isEqualTo(ConMonItemStatus.OPEN);
    }

    @Test
    void ongoing_yieldsOpen() {
        assertThat(PoamStatusDeriver.derive("ongoing", List.of()).status())
                .isEqualTo(ConMonItemStatus.OPEN);
    }

    @Test
    void unrecognized_yieldsUnknown() {
        var r = PoamStatusDeriver.derive("flibbertigibbet", List.of());
        assertThat(r.status()).isEqualTo(ConMonItemStatus.UNKNOWN);
        assertThat(r.rawStatus()).isEqualTo("flibbertigibbet");
    }

    @Test
    void nullStatus_emptyFindings_yieldsUnknown() {
        assertThat(PoamStatusDeriver.derive(null, List.of()).status())
                .isEqualTo(ConMonItemStatus.UNKNOWN);
    }

    @Test
    void nullStatus_allFindingsClosed_yieldsClosed() {
        var r = PoamStatusDeriver.derive(null, List.of("closed", "completed"));
        assertThat(r.status()).isEqualTo(ConMonItemStatus.CLOSED);
    }

    @Test
    void nullStatus_anyFindingOpen_yieldsOpen() {
        var r = PoamStatusDeriver.derive(null, List.of("closed", "ongoing"));
        assertThat(r.status()).isEqualTo(ConMonItemStatus.OPEN);
    }
}
```

### Implementation: `back-end/src/main/java/gov/nist/oscal/tools/api/service/conmon/PoamStatusDeriver.java`

```java
package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;

import java.util.List;
import java.util.Set;

/**
 * Hybrid status derivation per spec:
 *   1. status keyword on poam-item → mapped (FedRAMP-style values)
 *   2. linked findings/risks → roll up
 *   3. otherwise → UNKNOWN
 */
public final class PoamStatusDeriver {

    private static final Set<String> CLOSED_KEYWORDS = Set.of(
            "completed", "closed", "false-positive", "false positive", "not-applicable", "not applicable");
    private static final Set<String> OPEN_KEYWORDS = Set.of(
            "ongoing", "open", "risk-accepted", "risk accepted",
            "operational-requirement", "operational requirement", "pending");

    public record DerivedStatus(ConMonItemStatus status, String rawStatus) {}

    private PoamStatusDeriver() {}

    public static DerivedStatus derive(String statusKeyword, List<String> findingStatuses) {
        if (statusKeyword != null && !statusKeyword.isBlank()) {
            String normalized = statusKeyword.trim().toLowerCase();
            if (CLOSED_KEYWORDS.contains(normalized)) return new DerivedStatus(ConMonItemStatus.CLOSED, statusKeyword);
            if (OPEN_KEYWORDS.contains(normalized)) return new DerivedStatus(ConMonItemStatus.OPEN, statusKeyword);
            return new DerivedStatus(ConMonItemStatus.UNKNOWN, statusKeyword);
        }
        if (findingStatuses != null && !findingStatuses.isEmpty()) {
            boolean anyOpen = findingStatuses.stream().anyMatch(s -> {
                if (s == null) return false;
                String n = s.trim().toLowerCase();
                return OPEN_KEYWORDS.contains(n);
            });
            boolean anyKnown = findingStatuses.stream().anyMatch(s -> {
                if (s == null) return false;
                String n = s.trim().toLowerCase();
                return OPEN_KEYWORDS.contains(n) || CLOSED_KEYWORDS.contains(n);
            });
            if (anyOpen) return new DerivedStatus(ConMonItemStatus.OPEN, null);
            if (anyKnown) return new DerivedStatus(ConMonItemStatus.CLOSED, null);
        }
        return new DerivedStatus(ConMonItemStatus.UNKNOWN, null);
    }
}
```

Run test: `mvn surefire:test -Dtest=PoamStatusDeriverTest -DfailIfNoTests=false` → 9/9 pass.

Commit (2 files):
```
feat(conmon): add PoamStatusDeriver with hybrid status rule
```

---

## Task 7: Add Apache POI dependency

Modify `back-end/pom.xml`. Find the `<dependencies>` block. Add:

```xml
        <!-- Apache POI for FedRAMP POA&M Excel parsing (PR 4 ConMon) -->
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>5.2.5</version>
        </dependency>
```

`mvn compile -DskipTests` → BUILD SUCCESS (will download POI on first compile).

**Vigilance:** the user has unrelated WIP. Before staging, run:
```bash
git diff -- back-end/pom.xml | head -40
```
Verify only your 5-line addition. Restore-and-reapply if WIP is mixed in.

Commit:
```
build(conmon): add Apache POI 5.2.5 for FedRAMP POA&M parsing
```

---

## Task 8: OscalPoamParser (TDD)

Parses OSCAL POAM JSON/XML/YAML using `OscalBindingContext`. Produces a `ParsedPoam`.

### Test first: `OscalPoamParserTest.java`

The CLI has fixtures at `cli/src/test/resources/cli/example_poam_valid.{json,xml,yaml}`. Copy the JSON fixture into `back-end/src/test/resources/conmon/example_poam_valid.json` for use here.

Step 8a: Copy fixture:

```bash
cp cli/src/test/resources/cli/example_poam_valid.json \
   back-end/src/test/resources/conmon/example_poam_valid.json
```

(Create the `conmon/` dir if needed.)

Step 8b: Test:

```java
package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import gov.nist.oscal.tools.api.entity.ConMonSourceFormat;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class OscalPoamParserTest {

    @Test
    void parsesValidJsonFixture() throws Exception {
        OscalPoamParser parser = new OscalPoamParser();
        try (InputStream in = getClass().getResourceAsStream("/conmon/example_poam_valid.json")) {
            assertThat(in).as("fixture must be on classpath").isNotNull();
            ParsedPoam parsed = parser.parse(in, ConMonSourceFormat.OSCAL_JSON);

            assertThat(parsed.oscalUuid()).isEqualTo("51657392-cc1f-4e77-977c-91528f690be2");
            assertThat(parsed.oscalVersion()).isEqualTo("1.1.1");
            assertThat(parsed.items()).hasSize(1);

            ParsedPoamItem item = parsed.items().get(0);
            assertThat(item.externalId()).isEqualTo("c8d39ca5-7563-45d8-b90b-969f8c38bb48");
            assertThat(item.title()).contains("Example PO");
            // Fixture has no status prop and no findings → UNKNOWN
            assertThat(item.status()).isEqualTo(ConMonItemStatus.UNKNOWN);
        }
    }

    @Test
    void emptyContent_throws() {
        OscalPoamParser parser = new OscalPoamParser();
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> parser.parse(new java.io.ByteArrayInputStream(new byte[0]), ConMonSourceFormat.OSCAL_JSON))
                .isInstanceOf(RuntimeException.class);
    }
}
```

### Implementation: `OscalPoamParser.java`

```java
package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import gov.nist.oscal.tools.api.entity.ConMonSourceFormat;
import gov.nist.secauto.metaschema.databind.io.Format;
import gov.nist.secauto.oscal.lib.OscalBindingContext;
import gov.nist.secauto.oscal.lib.model.PlanOfActionAndMilestones;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses an OSCAL POAM document (JSON, XML, or YAML) into the parser's
 * intermediate ParsedPoam shape. Reflection is used for traversing the
 * Metaschema-bound model to keep this resilient to library version
 * differences in field-name casing or wrapper classes.
 */
@Component
public class OscalPoamParser {

    public ParsedPoam parse(InputStream input, ConMonSourceFormat sourceFormat) {
        Format mFormat = switch (sourceFormat) {
            case OSCAL_JSON -> Format.JSON;
            case OSCAL_XML -> Format.XML;
            case OSCAL_YAML -> Format.YAML;
            default -> throw new IllegalArgumentException("Not an OSCAL format: " + sourceFormat);
        };

        Path tmp;
        try {
            tmp = Files.createTempFile("conmon-poam-", "." + mFormat.name().toLowerCase());
            Files.copy(input, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to buffer upload to temp file", e);
        }

        try {
            PlanOfActionAndMilestones poam = OscalBindingContext.instance()
                    .newDeserializer(mFormat, PlanOfActionAndMilestones.class)
                    .deserialize(tmp);

            return toParsedPoam(poam);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OSCAL POAM: " + e.getMessage(), e);
        } finally {
            try { Files.deleteIfExists(tmp); } catch (IOException ignore) {}
        }
    }

    private static ParsedPoam toParsedPoam(PlanOfActionAndMilestones poam) {
        String uuid = invokeStringGetter(poam, "getUuid");
        Object metadata = invokeGetter(poam, "getMetadata");
        String oscalVersion = metadata == null ? null : invokeStringGetter(metadata, "getOscalVersion");
        String title = metadata == null ? null : invokeStringGetter(metadata, "getTitle");
        Object lastModifiedRaw = metadata == null ? null : invokeGetter(metadata, "getLastModified");
        LocalDateTime lastModified = parseInstantToLocal(lastModifiedRaw);

        Object itemsRaw = invokeGetter(poam, "getPoamItems");
        List<?> rawItems = itemsRaw instanceof List<?> ? (List<?>) itemsRaw : List.of();

        List<ParsedPoamItem> items = new ArrayList<>(rawItems.size());
        for (Object raw : rawItems) {
            items.add(toParsedItem(raw));
        }
        return new ParsedPoam(uuid, oscalVersion, title, lastModified, items);
    }

    private static ParsedPoamItem toParsedItem(Object raw) {
        String externalId = invokeStringGetter(raw, "getUuid");
        String itemTitle = invokeStringGetter(raw, "getTitle");
        String description = invokeStringGetter(raw, "getDescription");

        // Look for FedRAMP-style status prop
        Object propsRaw = invokeGetter(raw, "getProps");
        List<?> props = propsRaw instanceof List<?> ? (List<?>) propsRaw : List.of();
        String statusKeyword = null;
        Map<String, Object> extra = new HashMap<>();
        for (Object p : props) {
            String name = invokeStringGetter(p, "getName");
            String value = invokeStringGetter(p, "getValue");
            if ("status".equalsIgnoreCase(name)) {
                statusKeyword = value;
            } else if (name != null) {
                extra.put("prop:" + name, value);
            }
        }

        // Linked-finding rollup (best-effort; many POAMs don't include this)
        List<String> findingStatuses = new ArrayList<>();
        Object related = invokeGetter(raw, "getRelatedFindings");
        if (related instanceof List<?> rl) {
            for (Object rf : rl) {
                String fStatus = invokeStringGetter(rf, "getStatus");
                if (fStatus != null) findingStatuses.add(fStatus);
            }
        }

        var derived = PoamStatusDeriver.derive(statusKeyword, findingStatuses);

        return new ParsedPoamItem(
                externalId,
                itemTitle == null ? "(untitled)" : itemTitle,
                description,
                derived.status(),
                derived.rawStatus(),
                null,   // severity not standard on poam-item
                null,
                null,
                null,
                null,
                null,
                extra
        );
    }

    private static Object invokeGetter(Object target, String name) {
        if (target == null) return null;
        try {
            var m = target.getClass().getMethod(name);
            return m.invoke(target);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static String invokeStringGetter(Object target, String name) {
        Object v = invokeGetter(target, name);
        return v == null ? null : v.toString();
    }

    private static LocalDateTime parseInstantToLocal(Object instant) {
        if (instant == null) return null;
        try {
            if (instant instanceof java.time.Instant i) return LocalDateTime.ofInstant(i, ZoneOffset.UTC);
            if (instant instanceof java.time.OffsetDateTime o) return o.toLocalDateTime();
            if (instant instanceof java.time.ZonedDateTime z) return z.toLocalDateTime();
        } catch (Exception ignore) {}
        return null;
    }
}
```

Reflection makes this resilient to differences in OSCAL library version naming. If the parser test fails because a getter has a different name than expected (e.g., `getPoamItems` vs `getPoaMItems`), check the deserialized object via `mvn dependency:sources -DincludeArtifactIds=liboscal-java` and inspect the generated class — adjust the reflection name in `invokeGetter` calls.

Run test: `mvn surefire:test -Dtest=OscalPoamParserTest -DfailIfNoTests=false` → 2/2 pass.

Commit (3 files: parser + test + fixture):
```
feat(conmon): add OscalPoamParser using OscalBindingContext
```

---

## Task 9: FedrampPoamExcelParser (TDD)

Parses the FedRAMP `.xlsx` template using Apache POI.

### Test first: `FedrampPoamExcelParserTest.java`

We'll synthesize a minimal valid FedRAMP-shaped workbook in the test rather than ship a binary fixture.

```java
package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class FedrampPoamExcelParserTest {

    @Test
    void parsesOpenAndClosedSheets() throws Exception {
        byte[] xlsx = buildWorkbook(true, true);
        ParsedPoam parsed = new FedrampPoamExcelParser().parse(new ByteArrayInputStream(xlsx));

        assertThat(parsed.items()).hasSize(2);

        var open = parsed.items().stream().filter(i -> "P-1".equals(i.externalId())).findFirst().orElseThrow();
        assertThat(open.status()).isEqualTo(ConMonItemStatus.OPEN);
        assertThat(open.title()).isEqualTo("Open weakness");
        assertThat(open.severity()).isEqualTo("HIGH");

        var closed = parsed.items().stream().filter(i -> "P-2".equals(i.externalId())).findFirst().orElseThrow();
        assertThat(closed.status()).isEqualTo(ConMonItemStatus.CLOSED);
    }

    @Test
    void parsesOnlyOpenSheet() throws Exception {
        byte[] xlsx = buildWorkbook(true, false);
        ParsedPoam parsed = new FedrampPoamExcelParser().parse(new ByteArrayInputStream(xlsx));
        assertThat(parsed.items()).hasSize(1);
        assertThat(parsed.items().get(0).status()).isEqualTo(ConMonItemStatus.OPEN);
    }

    @Test
    void rejectsWorkbookWithoutPoamSheets() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            wb.createSheet("Cover Page");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            byte[] xlsx = out.toByteArray();

            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> new FedrampPoamExcelParser().parse(new ByteArrayInputStream(xlsx)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("POA&M");
        }
    }

    private byte[] buildWorkbook(boolean openSheet, boolean closedSheet) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            if (openSheet) {
                Sheet s = wb.createSheet("Open POA&M Items");
                writeHeader(s);
                Row r = s.createRow(1);
                r.createCell(0).setCellValue("P-1");
                r.createCell(1).setCellValue("Open weakness");
                r.createCell(2).setCellValue("Description here");
                r.createCell(3).setCellValue("High");
                r.createCell(4).setCellValue("");      // scheduled
                r.createCell(5).setCellValue("");      // actual
                r.createCell(6).setCellValue("alice"); // POC
                r.createCell(7).setCellValue("Ongoing"); // raw status
            }
            if (closedSheet) {
                Sheet s = wb.createSheet("Closed POA&M Items");
                writeHeader(s);
                Row r = s.createRow(1);
                r.createCell(0).setCellValue("P-2");
                r.createCell(1).setCellValue("Closed weakness");
                r.createCell(2).setCellValue("Description here");
                r.createCell(3).setCellValue("Moderate");
                r.createCell(4).setCellValue("");
                r.createCell(5).setCellValue("");
                r.createCell(6).setCellValue("alice");
                r.createCell(7).setCellValue("Completed");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private void writeHeader(Sheet s) {
        Row h = s.createRow(0);
        String[] cols = {
            "POA&M Item ID", "Weakness Name", "Weakness Description", "Severity",
            "Scheduled Completion Date", "Actual Completion Date", "Point of Contact", "Status"
        };
        for (int i = 0; i < cols.length; i++) h.createCell(i).setCellValue(cols[i]);
    }
}
```

### Implementation: `FedrampPoamExcelParser.java`

```java
package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a FedRAMP POA&M Excel template (.xlsx). Looks for sheets whose names
 * contain "Open POA&M Items" and/or "Closed POA&M Items" (case-insensitive).
 * Header row matched permissively against known columns.
 */
@Component
public class FedrampPoamExcelParser {

    public ParsedPoam parse(InputStream input) {
        try (Workbook wb = WorkbookFactory.create(input)) {
            Sheet open = findSheet(wb, "open poa&m items");
            Sheet closed = findSheet(wb, "closed poa&m items");
            if (open == null && closed == null) {
                throw new IllegalArgumentException(
                        "Workbook does not contain expected POA&M sheets " +
                        "(\"Open POA&M Items\" or \"Closed POA&M Items\").");
            }

            List<ParsedPoamItem> items = new ArrayList<>();
            if (open != null) {
                items.addAll(parseSheet(open, ConMonItemStatus.OPEN));
            }
            if (closed != null) {
                items.addAll(parseSheet(closed, ConMonItemStatus.CLOSED));
            }
            return new ParsedPoam(null, null, null, null, items);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel workbook: " + e.getMessage(), e);
        }
    }

    private Sheet findSheet(Workbook wb, String needle) {
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet s = wb.getSheetAt(i);
            if (s.getSheetName() != null && s.getSheetName().toLowerCase().contains(needle)) return s;
        }
        return null;
    }

    private List<ParsedPoamItem> parseSheet(Sheet sheet, ConMonItemStatus sheetStatus) {
        List<ParsedPoamItem> rows = new ArrayList<>();
        if (sheet.getLastRowNum() < 1) return rows;

        Row header = sheet.getRow(0);
        if (header == null) return rows;

        Map<String, Integer> col = new LinkedHashMap<>();
        for (int i = 0; i < header.getLastCellNum(); i++) {
            Cell c = header.getCell(i);
            if (c != null) {
                String v = stringValue(c);
                if (v != null) col.put(v.toLowerCase().trim(), i);
            }
        }

        Integer cId = matchColumn(col, "poa&m item id", "item id", "id");
        Integer cTitle = matchColumn(col, "weakness name", "title", "weakness");
        Integer cDesc = matchColumn(col, "weakness description", "description");
        Integer cSev = matchColumn(col, "severity");
        Integer cSched = matchColumn(col, "scheduled completion date", "scheduled");
        Integer cActual = matchColumn(col, "actual completion date", "actual");
        Integer cPoc = matchColumn(col, "point of contact", "poc");
        Integer cStatus = matchColumn(col, "status");

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String externalId = cellString(row, cId);
            if (externalId == null || externalId.isBlank()) continue;
            String title = cellString(row, cTitle);
            String description = cellString(row, cDesc);
            String severityRaw = cellString(row, cSev);
            String severity = normalizeSeverity(severityRaw);
            LocalDate sched = cellDate(row, cSched);
            LocalDate actual = cellDate(row, cActual);
            String poc = cellString(row, cPoc);
            String rawStatus = cellString(row, cStatus);

            Map<String, Object> extra = new HashMap<>();
            if (severityRaw != null && severity == null) extra.put("rawSeverity", severityRaw);

            rows.add(new ParsedPoamItem(
                    externalId,
                    title == null ? "(untitled)" : title,
                    description,
                    sheetStatus,
                    rawStatus,
                    severity,
                    null,
                    sched,
                    actual,
                    poc,
                    null,
                    extra));
        }
        return rows;
    }

    private static Integer matchColumn(Map<String, Integer> headers, String... candidates) {
        for (String c : candidates) {
            for (var e : headers.entrySet()) {
                if (e.getKey().contains(c)) return e.getValue();
            }
        }
        return null;
    }

    private static String stringValue(Cell c) {
        if (c == null) return null;
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(c)
                    ? c.getDateCellValue().toInstant().atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()
                    : Double.toString(c.getNumericCellValue());
            case BOOLEAN -> Boolean.toString(c.getBooleanCellValue());
            default -> null;
        };
    }

    private static String cellString(Row row, Integer idx) {
        if (idx == null) return null;
        return stringValue(row.getCell(idx));
    }

    private static LocalDate cellDate(Row row, Integer idx) {
        if (idx == null) return null;
        Cell c = row.getCell(idx);
        if (c == null) return null;
        if (c.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
            return c.getDateCellValue().toInstant().atZone(java.time.ZoneOffset.UTC).toLocalDate();
        }
        String s = stringValue(c);
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s);
        } catch (Exception ignore) {
            return null;
        }
    }

    private static String normalizeSeverity(String raw) {
        if (raw == null) return null;
        String n = raw.trim().toLowerCase();
        return switch (n) {
            case "low" -> "LOW";
            case "moderate", "medium", "med" -> "MODERATE";
            case "high" -> "HIGH";
            case "critical" -> "CRITICAL";
            default -> null;
        };
    }
}
```

Run test: `mvn surefire:test -Dtest=FedrampPoamExcelParserTest -DfailIfNoTests=false` → 3/3 pass.

Commit (2 files):
```
feat(conmon): add FedrampPoamExcelParser using Apache POI
```

---

## Task 10: ConMonReconciliationService (TDD)

Computes the diff between current and previous snapshot's items.

### Test first: `ConMonReconciliationServiceTest.java`

```java
package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import gov.nist.oscal.tools.api.entity.ConMonPoamItem;
import gov.nist.oscal.tools.api.entity.ConMonReconciliation;
import gov.nist.oscal.tools.api.entity.ConMonSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConMonReconciliationServiceTest {

    private final ConMonReconciliationService service = new ConMonReconciliationService();

    @Test
    void detectsAllSixCategories() {
        ConMonSnapshot prev = snap();
        ConMonSnapshot curr = snap();

        // P-1: still_open  (open in both)
        addItem(prev, "P-1", "Still open", ConMonItemStatus.OPEN, "HIGH", null);
        addItem(curr, "P-1", "Still open", ConMonItemStatus.OPEN, "HIGH", null);

        // P-2: closed (open → closed)
        addItem(prev, "P-2", "Will close", ConMonItemStatus.OPEN, "HIGH", null);
        addItem(curr, "P-2", "Will close", ConMonItemStatus.CLOSED, "HIGH", null);

        // P-3: reopened (closed → open)
        addItem(prev, "P-3", "Came back", ConMonItemStatus.CLOSED, "MODERATE", null);
        addItem(curr, "P-3", "Came back", ConMonItemStatus.OPEN, "MODERATE", null);

        // P-4: changed (severity bumped)
        addItem(prev, "P-4", "Bumped", ConMonItemStatus.OPEN, "MODERATE", null);
        addItem(curr, "P-4", "Bumped", ConMonItemStatus.OPEN, "HIGH", null);

        // P-5: new (only in curr)
        addItem(curr, "P-5", "Brand new", ConMonItemStatus.OPEN, "LOW", null);

        // P-6: removed (only in prev)
        addItem(prev, "P-6", "Vanished", ConMonItemStatus.OPEN, "LOW", null);

        ConMonReconciliation rec = service.compute(curr, prev);

        assertThat(rec.getNewCount()).isEqualTo(1);
        assertThat(rec.getClosedCount()).isEqualTo(1);
        assertThat(rec.getReopenedCount()).isEqualTo(1);
        assertThat(rec.getStillOpenCount()).isEqualTo(1);
        assertThat(rec.getRemovedCount()).isEqualTo(1);
        assertThat(rec.getChangedCount()).isEqualTo(1);
        assertThat(rec.getSnapshot()).isSameAs(curr);
        assertThat(rec.getPreviousSnapshot()).isSameAs(prev);
    }

    @Test
    void titleChangeIsAlsoChanged() {
        ConMonSnapshot prev = snap();
        ConMonSnapshot curr = snap();
        addItem(prev, "P-1", "Old title", ConMonItemStatus.OPEN, "HIGH", null);
        addItem(curr, "P-1", "New title", ConMonItemStatus.OPEN, "HIGH", null);

        ConMonReconciliation rec = service.compute(curr, prev);
        assertThat(rec.getChangedCount()).isEqualTo(1);
        assertThat(rec.getStillOpenCount()).isEqualTo(0);
    }

    @Test
    void emptyPrev_allItemsAreNew() {
        ConMonSnapshot prev = snap();
        ConMonSnapshot curr = snap();
        addItem(curr, "P-1", "x", ConMonItemStatus.OPEN, "HIGH", null);
        addItem(curr, "P-2", "y", ConMonItemStatus.CLOSED, null, null);

        ConMonReconciliation rec = service.compute(curr, prev);
        assertThat(rec.getNewCount()).isEqualTo(2);
    }

    private ConMonSnapshot snap() {
        ConMonSnapshot s = new ConMonSnapshot();
        s.setItems(new ArrayList<>());
        return s;
    }

    private void addItem(ConMonSnapshot s, String extId, String title,
                         ConMonItemStatus status, String severity, java.time.LocalDate sched) {
        ConMonPoamItem i = new ConMonPoamItem();
        i.setSnapshot(s);
        i.setExternalId(extId);
        i.setTitle(title);
        i.setStatus(status);
        i.setSeverity(severity);
        i.setScheduledCompletionDate(sched);
        s.getItems().add(i);
    }
}
```

### Implementation: `ConMonReconciliationService.java`

```java
package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import gov.nist.oscal.tools.api.entity.ConMonPoamItem;
import gov.nist.oscal.tools.api.entity.ConMonReconciliation;
import gov.nist.oscal.tools.api.entity.ConMonSnapshot;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Computes a six-category diff between a current snapshot and the immediate
 * prior snapshot. Items match across snapshots by external_id.
 */
@Service
public class ConMonReconciliationService {

    public ConMonReconciliation compute(ConMonSnapshot current, ConMonSnapshot previous) {
        Objects.requireNonNull(current);
        Objects.requireNonNull(previous);

        Map<String, ConMonPoamItem> prevByExt = new HashMap<>();
        for (ConMonPoamItem p : previous.getItems()) {
            if (p.getExternalId() != null) prevByExt.put(p.getExternalId(), p);
        }
        Map<String, ConMonPoamItem> currByExt = new HashMap<>();
        for (ConMonPoamItem c : current.getItems()) {
            if (c.getExternalId() != null) currByExt.put(c.getExternalId(), c);
        }

        int newCount = 0, closedCount = 0, reopenedCount = 0;
        int stillOpen = 0, removed = 0, changed = 0;

        for (var entry : currByExt.entrySet()) {
            ConMonPoamItem curr = entry.getValue();
            ConMonPoamItem prev = prevByExt.get(entry.getKey());
            if (prev == null) {
                newCount++;
                continue;
            }
            // Status transitions
            ConMonItemStatus pStatus = prev.getStatus();
            ConMonItemStatus cStatus = curr.getStatus();
            if (pStatus == ConMonItemStatus.OPEN && cStatus == ConMonItemStatus.CLOSED) {
                closedCount++;
            } else if (pStatus == ConMonItemStatus.CLOSED && cStatus == ConMonItemStatus.OPEN) {
                reopenedCount++;
            } else if (pStatus == ConMonItemStatus.OPEN && cStatus == ConMonItemStatus.OPEN
                    && fieldsEqual(prev, curr)) {
                stillOpen++;
            } else if (fieldsDiffer(prev, curr)) {
                changed++;
            }
        }
        for (String prevExt : prevByExt.keySet()) {
            if (!currByExt.containsKey(prevExt)) removed++;
        }

        ConMonReconciliation rec = new ConMonReconciliation();
        rec.setSnapshot(current);
        rec.setPreviousSnapshot(previous);
        rec.setNewCount(newCount);
        rec.setClosedCount(closedCount);
        rec.setReopenedCount(reopenedCount);
        rec.setStillOpenCount(stillOpen);
        rec.setRemovedCount(removed);
        rec.setChangedCount(changed);
        return rec;
    }

    private static boolean fieldsEqual(ConMonPoamItem a, ConMonPoamItem b) {
        return Objects.equals(a.getTitle(), b.getTitle())
                && Objects.equals(a.getSeverity(), b.getSeverity())
                && Objects.equals(a.getScheduledCompletionDate(), b.getScheduledCompletionDate())
                && Objects.equals(a.getStatus(), b.getStatus());
    }

    private static boolean fieldsDiffer(ConMonPoamItem a, ConMonPoamItem b) {
        return !fieldsEqual(a, b);
    }
}
```

Run test: `mvn surefire:test -Dtest=ConMonReconciliationServiceTest -DfailIfNoTests=false` → 3/3 pass.

Commit (2 files):
```
feat(conmon): add ConMonReconciliationService computing 6-category diff
```

---

## Task 11: Exception + DTOs

### `UnsupportedConMonFormatException.java`

```java
package gov.nist.oscal.tools.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnsupportedConMonFormatException extends RuntimeException {
    public UnsupportedConMonFormatException(String filename) {
        super("Unsupported ConMon file: " + filename
                + ". Use OSCAL JSON/XML/YAML or the FedRAMP POA&M Excel template (.xlsx). "
                + "For other artifacts, use the Documents tab.");
    }
}
```

### `model/conmon/ConMonSnapshotSummary.java`

```java
package gov.nist.oscal.tools.api.model.conmon;

import gov.nist.oscal.tools.api.entity.ConMonReconciliation;
import gov.nist.oscal.tools.api.entity.ConMonSnapshot;
import gov.nist.oscal.tools.api.entity.ConMonSourceFormat;

import java.time.LocalDateTime;

public class ConMonSnapshotSummary {
    private Long id;
    private Long authorizationId;
    private LocalDateTime uploadedAt;
    private String uploadedByUsername;
    private ConMonSourceFormat sourceFormat;
    private String originalFilename;
    private String oscalUuid;
    private String oscalVersion;
    private String metadataTitle;
    private LocalDateTime metadataLastModified;
    private int openCount;
    private int closedCount;
    private int unknownCount;
    private String notes;

    /** Reconciliation counts vs the prior snapshot, or null if no prior. */
    private ReconciliationCounts reconciliation;

    public ConMonSnapshotSummary() {}

    public ConMonSnapshotSummary(ConMonSnapshot s, ConMonReconciliation rec) {
        this.id = s.getId();
        this.authorizationId = s.getAuthorization().getId();
        this.uploadedAt = s.getUploadedAt();
        this.uploadedByUsername = s.getUploadedBy() != null ? s.getUploadedBy().getUsername() : null;
        this.sourceFormat = s.getSourceFormat();
        this.originalFilename = s.getOriginalFilename();
        this.oscalUuid = s.getOscalUuid();
        this.oscalVersion = s.getOscalVersion();
        this.metadataTitle = s.getMetadataTitle();
        this.metadataLastModified = s.getMetadataLastModified();
        this.openCount = s.getSummaryOpenCount();
        this.closedCount = s.getSummaryClosedCount();
        this.unknownCount = s.getSummaryUnknownCount();
        this.notes = s.getNotes();
        this.reconciliation = rec == null ? null : new ReconciliationCounts(rec);
    }

    public static class ReconciliationCounts {
        private int newCount, closedCount, reopenedCount, stillOpenCount, removedCount, changedCount;
        private Long previousSnapshotId;
        public ReconciliationCounts() {}
        public ReconciliationCounts(ConMonReconciliation r) {
            this.newCount = r.getNewCount();
            this.closedCount = r.getClosedCount();
            this.reopenedCount = r.getReopenedCount();
            this.stillOpenCount = r.getStillOpenCount();
            this.removedCount = r.getRemovedCount();
            this.changedCount = r.getChangedCount();
            this.previousSnapshotId = r.getPreviousSnapshot() != null ? r.getPreviousSnapshot().getId() : null;
        }
        public int getNewCount() { return newCount; } public void setNewCount(int n) { this.newCount = n; }
        public int getClosedCount() { return closedCount; } public void setClosedCount(int n) { this.closedCount = n; }
        public int getReopenedCount() { return reopenedCount; } public void setReopenedCount(int n) { this.reopenedCount = n; }
        public int getStillOpenCount() { return stillOpenCount; } public void setStillOpenCount(int n) { this.stillOpenCount = n; }
        public int getRemovedCount() { return removedCount; } public void setRemovedCount(int n) { this.removedCount = n; }
        public int getChangedCount() { return changedCount; } public void setChangedCount(int n) { this.changedCount = n; }
        public Long getPreviousSnapshotId() { return previousSnapshotId; }
        public void setPreviousSnapshotId(Long id) { this.previousSnapshotId = id; }
    }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getAuthorizationId() { return authorizationId; } public void setAuthorizationId(Long id) { this.authorizationId = id; }
    public LocalDateTime getUploadedAt() { return uploadedAt; } public void setUploadedAt(LocalDateTime t) { this.uploadedAt = t; }
    public String getUploadedByUsername() { return uploadedByUsername; } public void setUploadedByUsername(String s) { this.uploadedByUsername = s; }
    public ConMonSourceFormat getSourceFormat() { return sourceFormat; } public void setSourceFormat(ConMonSourceFormat f) { this.sourceFormat = f; }
    public String getOriginalFilename() { return originalFilename; } public void setOriginalFilename(String s) { this.originalFilename = s; }
    public String getOscalUuid() { return oscalUuid; } public void setOscalUuid(String s) { this.oscalUuid = s; }
    public String getOscalVersion() { return oscalVersion; } public void setOscalVersion(String s) { this.oscalVersion = s; }
    public String getMetadataTitle() { return metadataTitle; } public void setMetadataTitle(String s) { this.metadataTitle = s; }
    public LocalDateTime getMetadataLastModified() { return metadataLastModified; } public void setMetadataLastModified(LocalDateTime t) { this.metadataLastModified = t; }
    public int getOpenCount() { return openCount; } public void setOpenCount(int n) { this.openCount = n; }
    public int getClosedCount() { return closedCount; } public void setClosedCount(int n) { this.closedCount = n; }
    public int getUnknownCount() { return unknownCount; } public void setUnknownCount(int n) { this.unknownCount = n; }
    public String getNotes() { return notes; } public void setNotes(String s) { this.notes = s; }
    public ReconciliationCounts getReconciliation() { return reconciliation; }
    public void setReconciliation(ReconciliationCounts r) { this.reconciliation = r; }
}
```

### `model/conmon/ConMonPoamItemResponse.java`

```java
package gov.nist.oscal.tools.api.model.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import gov.nist.oscal.tools.api.entity.ConMonPoamItem;

import java.time.LocalDate;

public class ConMonPoamItemResponse {
    private Long id;
    private String externalId;
    private String title;
    private String description;
    private ConMonItemStatus status;
    private String rawStatus;
    private String severity;
    private String weaknessSource;
    private LocalDate scheduledCompletionDate;
    private LocalDate actualCompletionDate;
    private String pointOfContact;
    private String riskRating;

    public ConMonPoamItemResponse() {}

    public ConMonPoamItemResponse(ConMonPoamItem i) {
        this.id = i.getId();
        this.externalId = i.getExternalId();
        this.title = i.getTitle();
        this.description = i.getDescription();
        this.status = i.getStatus();
        this.rawStatus = i.getRawStatus();
        this.severity = i.getSeverity();
        this.weaknessSource = i.getWeaknessSource();
        this.scheduledCompletionDate = i.getScheduledCompletionDate();
        this.actualCompletionDate = i.getActualCompletionDate();
        this.pointOfContact = i.getPointOfContact();
        this.riskRating = i.getRiskRating();
    }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getExternalId() { return externalId; } public void setExternalId(String s) { this.externalId = s; }
    public String getTitle() { return title; } public void setTitle(String s) { this.title = s; }
    public String getDescription() { return description; } public void setDescription(String s) { this.description = s; }
    public ConMonItemStatus getStatus() { return status; } public void setStatus(ConMonItemStatus s) { this.status = s; }
    public String getRawStatus() { return rawStatus; } public void setRawStatus(String s) { this.rawStatus = s; }
    public String getSeverity() { return severity; } public void setSeverity(String s) { this.severity = s; }
    public String getWeaknessSource() { return weaknessSource; } public void setWeaknessSource(String s) { this.weaknessSource = s; }
    public LocalDate getScheduledCompletionDate() { return scheduledCompletionDate; } public void setScheduledCompletionDate(LocalDate d) { this.scheduledCompletionDate = d; }
    public LocalDate getActualCompletionDate() { return actualCompletionDate; } public void setActualCompletionDate(LocalDate d) { this.actualCompletionDate = d; }
    public String getPointOfContact() { return pointOfContact; } public void setPointOfContact(String s) { this.pointOfContact = s; }
    public String getRiskRating() { return riskRating; } public void setRiskRating(String s) { this.riskRating = s; }
}
```

### `model/conmon/ConMonReconciliationResponse.java`

```java
package gov.nist.oscal.tools.api.model.conmon;

import java.util.List;

public class ConMonReconciliationResponse {
    private Long snapshotId;
    private Long previousSnapshotId;
    private int newCount;
    private int closedCount;
    private int reopenedCount;
    private int stillOpenCount;
    private int removedCount;
    private int changedCount;
    private List<ConMonPoamItemResponse> newItems;
    private List<ConMonPoamItemResponse> newlyClosedItems;
    private List<ConMonPoamItemResponse> reopenedItems;
    private List<ConMonPoamItemResponse> removedItems;
    private List<ChangedItem> changedItems;

    public static class ChangedItem {
        private ConMonPoamItemResponse current;
        private ConMonPoamItemResponse previous;
        private List<String> fieldsChanged;
        public ChangedItem() {}
        public ChangedItem(ConMonPoamItemResponse curr, ConMonPoamItemResponse prev, List<String> fields) {
            this.current = curr; this.previous = prev; this.fieldsChanged = fields;
        }
        public ConMonPoamItemResponse getCurrent() { return current; }
        public void setCurrent(ConMonPoamItemResponse c) { this.current = c; }
        public ConMonPoamItemResponse getPrevious() { return previous; }
        public void setPrevious(ConMonPoamItemResponse p) { this.previous = p; }
        public List<String> getFieldsChanged() { return fieldsChanged; }
        public void setFieldsChanged(List<String> f) { this.fieldsChanged = f; }
    }

    public ConMonReconciliationResponse() {}

    public Long getSnapshotId() { return snapshotId; } public void setSnapshotId(Long id) { this.snapshotId = id; }
    public Long getPreviousSnapshotId() { return previousSnapshotId; } public void setPreviousSnapshotId(Long id) { this.previousSnapshotId = id; }
    public int getNewCount() { return newCount; } public void setNewCount(int n) { this.newCount = n; }
    public int getClosedCount() { return closedCount; } public void setClosedCount(int n) { this.closedCount = n; }
    public int getReopenedCount() { return reopenedCount; } public void setReopenedCount(int n) { this.reopenedCount = n; }
    public int getStillOpenCount() { return stillOpenCount; } public void setStillOpenCount(int n) { this.stillOpenCount = n; }
    public int getRemovedCount() { return removedCount; } public void setRemovedCount(int n) { this.removedCount = n; }
    public int getChangedCount() { return changedCount; } public void setChangedCount(int n) { this.changedCount = n; }
    public List<ConMonPoamItemResponse> getNewItems() { return newItems; } public void setNewItems(List<ConMonPoamItemResponse> l) { this.newItems = l; }
    public List<ConMonPoamItemResponse> getNewlyClosedItems() { return newlyClosedItems; } public void setNewlyClosedItems(List<ConMonPoamItemResponse> l) { this.newlyClosedItems = l; }
    public List<ConMonPoamItemResponse> getReopenedItems() { return reopenedItems; } public void setReopenedItems(List<ConMonPoamItemResponse> l) { this.reopenedItems = l; }
    public List<ConMonPoamItemResponse> getRemovedItems() { return removedItems; } public void setRemovedItems(List<ConMonPoamItemResponse> l) { this.removedItems = l; }
    public List<ChangedItem> getChangedItems() { return changedItems; } public void setChangedItems(List<ChangedItem> l) { this.changedItems = l; }
}
```

### `model/conmon/ConMonAnalyticsResponse.java`

```java
package gov.nist.oscal.tools.api.model.conmon;

import java.time.LocalDate;
import java.util.List;

public class ConMonAnalyticsResponse {

    public record TimeSeriesPoint(LocalDate date, int open, int closed, int unknown) {}
    public record SeveritySeriesPoint(LocalDate date, int low, int moderate, int high, int critical) {}
    public record DonutSegment(String label, int count) {}
    public record AgingBucket(String bucket, int count) {}

    private List<TimeSeriesPoint> openCountSeries;
    private List<SeveritySeriesPoint> severitySeriesByDate;
    private List<DonutSegment> currentStatusBreakdown;
    private List<AgingBucket> agingBuckets;
    private Double meanTimeToCloseDays;

    public ConMonAnalyticsResponse() {}

    public List<TimeSeriesPoint> getOpenCountSeries() { return openCountSeries; }
    public void setOpenCountSeries(List<TimeSeriesPoint> l) { this.openCountSeries = l; }
    public List<SeveritySeriesPoint> getSeveritySeriesByDate() { return severitySeriesByDate; }
    public void setSeveritySeriesByDate(List<SeveritySeriesPoint> l) { this.severitySeriesByDate = l; }
    public List<DonutSegment> getCurrentStatusBreakdown() { return currentStatusBreakdown; }
    public void setCurrentStatusBreakdown(List<DonutSegment> l) { this.currentStatusBreakdown = l; }
    public List<AgingBucket> getAgingBuckets() { return agingBuckets; }
    public void setAgingBuckets(List<AgingBucket> l) { this.agingBuckets = l; }
    public Double getMeanTimeToCloseDays() { return meanTimeToCloseDays; }
    public void setMeanTimeToCloseDays(Double d) { this.meanTimeToCloseDays = d; }
}
```

`mvn compile -DskipTests` → BUILD SUCCESS.

Commit (5 files):
```
feat(conmon): add ConMon DTOs and UnsupportedConMonFormatException
```

---

## Task 12: ConMonAnalyticsService

```java
package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import gov.nist.oscal.tools.api.entity.ConMonPoamItem;
import gov.nist.oscal.tools.api.entity.ConMonSnapshot;
import gov.nist.oscal.tools.api.model.conmon.ConMonAnalyticsResponse;
import gov.nist.oscal.tools.api.model.conmon.ConMonAnalyticsResponse.AgingBucket;
import gov.nist.oscal.tools.api.model.conmon.ConMonAnalyticsResponse.DonutSegment;
import gov.nist.oscal.tools.api.model.conmon.ConMonAnalyticsResponse.SeveritySeriesPoint;
import gov.nist.oscal.tools.api.model.conmon.ConMonAnalyticsResponse.TimeSeriesPoint;
import gov.nist.oscal.tools.api.repository.ConMonSnapshotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConMonAnalyticsService {

    private final ConMonSnapshotRepository snapshotRepository;

    public ConMonAnalyticsService(ConMonSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    public ConMonAnalyticsResponse forAuthorization(Authorization authorization) {
        List<ConMonSnapshot> snaps = snapshotRepository.findByAuthorizationOrderByUploadedAtDesc(authorization);
        ConMonAnalyticsResponse r = new ConMonAnalyticsResponse();
        r.setOpenCountSeries(timeSeries(snaps));
        r.setSeveritySeriesByDate(severitySeries(snaps));
        r.setCurrentStatusBreakdown(currentDonut(snaps));
        r.setAgingBuckets(agingBuckets(snaps));
        r.setMeanTimeToCloseDays(meanTimeToClose(snaps));
        return r;
    }

    private List<TimeSeriesPoint> timeSeries(List<ConMonSnapshot> snaps) {
        List<TimeSeriesPoint> out = new ArrayList<>(snaps.size());
        for (int i = snaps.size() - 1; i >= 0; i--) {
            ConMonSnapshot s = snaps.get(i);
            out.add(new TimeSeriesPoint(
                    s.getUploadedAt().toLocalDate(),
                    s.getSummaryOpenCount(),
                    s.getSummaryClosedCount(),
                    s.getSummaryUnknownCount()));
        }
        return out;
    }

    private List<SeveritySeriesPoint> severitySeries(List<ConMonSnapshot> snaps) {
        List<SeveritySeriesPoint> out = new ArrayList<>(snaps.size());
        for (int i = snaps.size() - 1; i >= 0; i--) {
            ConMonSnapshot s = snaps.get(i);
            int low = 0, mod = 0, high = 0, crit = 0;
            for (ConMonPoamItem it : s.getItems()) {
                if (it.getStatus() != ConMonItemStatus.OPEN) continue;
                String sev = it.getSeverity();
                if ("LOW".equals(sev)) low++;
                else if ("MODERATE".equals(sev)) mod++;
                else if ("HIGH".equals(sev)) high++;
                else if ("CRITICAL".equals(sev)) crit++;
            }
            out.add(new SeveritySeriesPoint(s.getUploadedAt().toLocalDate(), low, mod, high, crit));
        }
        return out;
    }

    private List<DonutSegment> currentDonut(List<ConMonSnapshot> snaps) {
        if (snaps.isEmpty()) return List.of();
        ConMonSnapshot latest = snaps.get(0);
        return List.of(
                new DonutSegment("Open", latest.getSummaryOpenCount()),
                new DonutSegment("Closed", latest.getSummaryClosedCount()),
                new DonutSegment("Unknown", latest.getSummaryUnknownCount())
        );
    }

    private List<AgingBucket> agingBuckets(List<ConMonSnapshot> snaps) {
        if (snaps.isEmpty()) return List.of();
        ConMonSnapshot latest = snaps.get(0);
        LocalDate today = LocalDate.now();

        int[] buckets = new int[5]; // <30, 30-60, 60-90, 90-180, >180
        for (ConMonPoamItem it : latest.getItems()) {
            if (it.getStatus() != ConMonItemStatus.OPEN) continue;
            LocalDate baseline = it.getScheduledCompletionDate();
            if (baseline == null) baseline = latest.getUploadedAt().toLocalDate();
            long days = Math.abs(ChronoUnit.DAYS.between(baseline, today));
            if (days < 30) buckets[0]++;
            else if (days < 60) buckets[1]++;
            else if (days < 90) buckets[2]++;
            else if (days < 180) buckets[3]++;
            else buckets[4]++;
        }
        return List.of(
                new AgingBucket("<30d", buckets[0]),
                new AgingBucket("30–60", buckets[1]),
                new AgingBucket("60–90", buckets[2]),
                new AgingBucket("90–180", buckets[3]),
                new AgingBucket(">180", buckets[4])
        );
    }

    /**
     * Best-effort mean time to close: across all snapshots, average days
     * between scheduledCompletionDate and actualCompletionDate on closed items.
     * Returns null if no data.
     */
    private Double meanTimeToClose(List<ConMonSnapshot> snaps) {
        long totalDays = 0;
        long count = 0;
        for (ConMonSnapshot s : snaps) {
            for (ConMonPoamItem it : s.getItems()) {
                if (it.getStatus() != ConMonItemStatus.CLOSED) continue;
                LocalDate sched = it.getScheduledCompletionDate();
                LocalDate actual = it.getActualCompletionDate();
                if (sched == null || actual == null) continue;
                totalDays += Math.abs(ChronoUnit.DAYS.between(sched, actual));
                count++;
            }
        }
        return count == 0 ? null : ((double) totalDays) / count;
    }
}
```

Commit:
```
feat(conmon): add ConMonAnalyticsService for time-series + aging analytics
```

---

## Task 13: ConMonService (orchestrator)

The orchestrator: takes a multipart upload, dispatches to the right parser, persists snapshot + items, computes reconciliation. Stage in two parts to keep this readable: first the upload + persist flow, then helpers.

Create `back-end/src/main/java/gov/nist/oscal/tools/api/service/conmon/ConMonService.java`:

```java
package gov.nist.oscal.tools.api.service.conmon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import gov.nist.oscal.tools.api.entity.ConMonPoamItem;
import gov.nist.oscal.tools.api.entity.ConMonReconciliation;
import gov.nist.oscal.tools.api.entity.ConMonSnapshot;
import gov.nist.oscal.tools.api.entity.ConMonSourceFormat;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.exception.UnsupportedConMonFormatException;
import gov.nist.oscal.tools.api.repository.ConMonPoamItemRepository;
import gov.nist.oscal.tools.api.repository.ConMonReconciliationRepository;
import gov.nist.oscal.tools.api.repository.ConMonSnapshotRepository;
import gov.nist.oscal.tools.api.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConMonService {

    private final ConMonSnapshotRepository snapshotRepository;
    private final ConMonPoamItemRepository itemRepository;
    private final ConMonReconciliationRepository reconciliationRepository;
    private final OscalPoamParser oscalParser;
    private final FedrampPoamExcelParser excelParser;
    private final ConMonReconciliationService reconciliationService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    public ConMonService(ConMonSnapshotRepository snapshotRepository,
                         ConMonPoamItemRepository itemRepository,
                         ConMonReconciliationRepository reconciliationRepository,
                         OscalPoamParser oscalParser,
                         FedrampPoamExcelParser excelParser,
                         ConMonReconciliationService reconciliationService,
                         FileStorageService fileStorageService,
                         ObjectMapper objectMapper) {
        this.snapshotRepository = snapshotRepository;
        this.itemRepository = itemRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.oscalParser = oscalParser;
        this.excelParser = excelParser;
        this.reconciliationService = reconciliationService;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ConMonSnapshot upload(Authorization authorization, User uploader,
                                 MultipartFile file, String notes) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }
        ConMonSourceFormat format = ConMonSourceFormat.fromFilename(file.getOriginalFilename());
        if (format == null) {
            throw new UnsupportedConMonFormatException(file.getOriginalFilename());
        }

        ParsedPoam parsed;
        try {
            parsed = (format == ConMonSourceFormat.FEDRAMP_XLSX)
                    ? excelParser.parse(file.getInputStream())
                    : oscalParser.parse(file.getInputStream(), format);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read upload stream", e);
        }

        // Persist the original blob via existing FileStorageService binary primitives (PR 3).
        String storagePath = "authorizations/" + authorization.getId()
                + "/conmon/" + UUID.randomUUID() + "-" + sanitizeFilename(file.getOriginalFilename());
        try {
            fileStorageService.saveBinary(storagePath, file.getBytes(), file.getContentType());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read upload bytes for storage", e);
        }

        ConMonSnapshot snapshot = new ConMonSnapshot();
        snapshot.setAuthorization(authorization);
        snapshot.setUploadedBy(uploader);
        snapshot.setSourceFormat(format);
        snapshot.setOriginalFilename(file.getOriginalFilename());
        snapshot.setFileStoragePath(storagePath);
        snapshot.setOscalUuid(parsed.oscalUuid());
        snapshot.setOscalVersion(parsed.oscalVersion());
        snapshot.setMetadataTitle(parsed.metadataTitle());
        snapshot.setMetadataLastModified(parsed.metadataLastModified());
        snapshot.setNotes(notes);

        int open = 0, closed = 0, unknown = 0;
        for (ParsedPoamItem pi : parsed.items()) {
            ConMonPoamItem item = toItem(pi, snapshot);
            snapshot.getItems().add(item);
            switch (item.getStatus()) {
                case OPEN -> open++;
                case CLOSED -> closed++;
                case UNKNOWN -> unknown++;
            }
        }
        snapshot.setSummaryOpenCount(open);
        snapshot.setSummaryClosedCount(closed);
        snapshot.setSummaryUnknownCount(unknown);

        ConMonSnapshot saved = snapshotRepository.save(snapshot);

        // Compute reconciliation against the prior (now second-newest) snapshot
        Optional<ConMonSnapshot> prev = findPriorSnapshot(authorization, saved.getId());
        if (prev.isPresent()) {
            ConMonReconciliation rec = reconciliationService.compute(saved, prev.get());
            reconciliationRepository.save(rec);
        }
        return saved;
    }

    public Optional<ConMonReconciliation> findReconciliation(ConMonSnapshot snapshot) {
        return reconciliationRepository.findBySnapshot(snapshot);
    }

    @Transactional
    public void delete(ConMonSnapshot snapshot) {
        // The blob lives outside the DB — best-effort delete (idempotent).
        try {
            fileStorageService.deleteBinary(snapshot.getFileStoragePath());
        } catch (RuntimeException ignored) {
            // Don't block row deletion on storage hiccups.
        }
        snapshotRepository.delete(snapshot);
    }

    public byte[] downloadOriginal(ConMonSnapshot snapshot) {
        byte[] bytes = fileStorageService.loadBinary(snapshot.getFileStoragePath());
        if (bytes == null) {
            throw new RuntimeException("Original blob missing for snapshot " + snapshot.getId());
        }
        return bytes;
    }

    private Optional<ConMonSnapshot> findPriorSnapshot(Authorization authorization, Long currentId) {
        return snapshotRepository.findByAuthorizationOrderByUploadedAtDesc(authorization).stream()
                .filter(s -> !s.getId().equals(currentId))
                .findFirst();
    }

    private ConMonPoamItem toItem(ParsedPoamItem pi, ConMonSnapshot snapshot) {
        ConMonPoamItem item = new ConMonPoamItem();
        item.setSnapshot(snapshot);
        item.setExternalId(pi.externalId() == null ? UUID.randomUUID().toString() : pi.externalId());
        item.setTitle(pi.title());
        item.setDescription(pi.description());
        item.setStatus(pi.status() == null ? ConMonItemStatus.UNKNOWN : pi.status());
        item.setRawStatus(pi.rawStatus());
        item.setSeverity(pi.severity());
        item.setWeaknessSource(pi.weaknessSource());
        item.setScheduledCompletionDate(pi.scheduledCompletionDate());
        item.setActualCompletionDate(pi.actualCompletionDate());
        item.setPointOfContact(pi.pointOfContact());
        item.setRiskRating(pi.riskRating());
        if (pi.extraProps() != null && !pi.extraProps().isEmpty()) {
            try {
                item.setExtraPropsJson(objectMapper.writeValueAsString(pi.extraProps()));
            } catch (JsonProcessingException ignored) {}
        }
        return item;
    }

    private static String sanitizeFilename(String raw) {
        if (raw == null || raw.isBlank()) return "file";
        String trimmed = raw.replace("\\", "/");
        int slash = trimmed.lastIndexOf('/');
        String basename = slash < 0 ? trimmed : trimmed.substring(slash + 1);
        return basename.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
```

`mvn compile -DskipTests` → BUILD SUCCESS.

Commit:
```
feat(conmon): add ConMonService orchestrating upload + reconciliation
```

---

## Task 14: ContinuousMonitoringController

Endpoints (all under `/api/authorizations/{authorizationId}/conmon`):
- `POST /snapshots` — multipart upload (CONTRIBUTOR+)
- `GET /snapshots` — list lightweight summaries
- `GET /snapshots/{snapshotId}` — single snapshot summary + reconciliation counts
- `GET /snapshots/{snapshotId}/items` — paginated/filtered items
- `GET /snapshots/{snapshotId}/reconciliation` — full diff with per-item lists
- `GET /snapshots/{snapshotId}/download` — original blob
- `DELETE /snapshots/{snapshotId}` — owner/editor delete (no own-only contributors here; ConMon snapshots are organization-shared artifacts)
- `GET /analytics` — time-series + aging + MTTC

Create `back-end/src/main/java/gov/nist/oscal/tools/api/controller/ContinuousMonitoringController.java`:

```java
package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import gov.nist.oscal.tools.api.entity.ConMonPoamItem;
import gov.nist.oscal.tools.api.entity.ConMonReconciliation;
import gov.nist.oscal.tools.api.entity.ConMonSnapshot;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.conmon.ConMonAnalyticsResponse;
import gov.nist.oscal.tools.api.model.conmon.ConMonPoamItemResponse;
import gov.nist.oscal.tools.api.model.conmon.ConMonReconciliationResponse;
import gov.nist.oscal.tools.api.model.conmon.ConMonSnapshotSummary;
import gov.nist.oscal.tools.api.repository.ConMonPoamItemRepository;
import gov.nist.oscal.tools.api.repository.ConMonSnapshotRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.AuthorizationAccessGuard;
import gov.nist.oscal.tools.api.service.AuthorizationService;
import gov.nist.oscal.tools.api.service.conmon.ConMonAnalyticsService;
import gov.nist.oscal.tools.api.service.conmon.ConMonService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/authorizations/{authorizationId}/conmon")
@Tag(name = "Continuous Monitoring", description = "POAM snapshot upload, reconciliation, and analytics")
public class ContinuousMonitoringController {

    private final AuthorizationService authorizationService;
    private final AuthorizationAccessGuard accessGuard;
    private final ConMonService conMonService;
    private final ConMonAnalyticsService analyticsService;
    private final ConMonSnapshotRepository snapshotRepository;
    private final ConMonPoamItemRepository itemRepository;
    private final UserRepository userRepository;

    public ContinuousMonitoringController(AuthorizationService authorizationService,
                                          AuthorizationAccessGuard accessGuard,
                                          ConMonService conMonService,
                                          ConMonAnalyticsService analyticsService,
                                          ConMonSnapshotRepository snapshotRepository,
                                          ConMonPoamItemRepository itemRepository,
                                          UserRepository userRepository) {
        this.authorizationService = authorizationService;
        this.accessGuard = accessGuard;
        this.conMonService = conMonService;
        this.analyticsService = analyticsService;
        this.snapshotRepository = snapshotRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    @PostMapping(value = "/snapshots", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConMonSnapshotSummary> upload(
            @PathVariable Long authorizationId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "notes", required = false) String notes,
            Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        User currentUser = requireCurrentUser(principal);
        accessGuard.requireUploadConMon(authorization, currentUser);

        ConMonSnapshot snapshot = conMonService.upload(authorization, currentUser, file, notes);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ConMonSnapshotSummary(snapshot, conMonService.findReconciliation(snapshot).orElse(null)));
    }

    @GetMapping("/snapshots")
    public ResponseEntity<List<ConMonSnapshotSummary>> list(@PathVariable Long authorizationId,
                                                            Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        List<ConMonSnapshot> snaps = snapshotRepository.findByAuthorizationOrderByUploadedAtDesc(authorization);
        List<ConMonSnapshotSummary> out = new ArrayList<>(snaps.size());
        for (ConMonSnapshot s : snaps) {
            out.add(new ConMonSnapshotSummary(s, conMonService.findReconciliation(s).orElse(null)));
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/snapshots/{snapshotId}")
    public ResponseEntity<ConMonSnapshotSummary> get(@PathVariable Long authorizationId,
                                                     @PathVariable Long snapshotId,
                                                     Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        ConMonSnapshot snap = requireSnapshot(authorization, snapshotId);
        return ResponseEntity.ok(new ConMonSnapshotSummary(snap, conMonService.findReconciliation(snap).orElse(null)));
    }

    @GetMapping("/snapshots/{snapshotId}/items")
    public ResponseEntity<Map<String, Object>> items(@PathVariable Long authorizationId,
                                                     @PathVariable Long snapshotId,
                                                     @RequestParam(value = "status", required = false) ConMonItemStatus status,
                                                     @RequestParam(value = "severity", required = false) String severity,
                                                     @RequestParam(value = "q", required = false) String q,
                                                     @RequestParam(value = "page", defaultValue = "0") int page,
                                                     @RequestParam(value = "size", defaultValue = "50") int size,
                                                     Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        ConMonSnapshot snap = requireSnapshot(authorization, snapshotId);

        Page<ConMonPoamItem> result = itemRepository.search(snap, status, severity, q,
                PageRequest.of(page, Math.min(size, 200)));

        List<ConMonPoamItemResponse> rows = result.stream().map(ConMonPoamItemResponse::new).toList();
        Map<String, Object> body = new HashMap<>();
        body.put("items", rows);
        body.put("totalElements", result.getTotalElements());
        body.put("totalPages", result.getTotalPages());
        body.put("page", result.getNumber());
        body.put("size", result.getSize());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/snapshots/{snapshotId}/reconciliation")
    public ResponseEntity<ConMonReconciliationResponse> reconciliation(@PathVariable Long authorizationId,
                                                                       @PathVariable Long snapshotId,
                                                                       Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        ConMonSnapshot current = requireSnapshot(authorization, snapshotId);
        ConMonReconciliation rec = conMonService.findReconciliation(current).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No reconciliation for snapshot " + snapshotId + " (likely the first snapshot)."));

        ConMonSnapshot prev = rec.getPreviousSnapshot();
        return ResponseEntity.ok(buildDiff(current, prev, rec));
    }

    @GetMapping("/snapshots/{snapshotId}/download")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long authorizationId,
                                                      @PathVariable Long snapshotId,
                                                      Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        ConMonSnapshot snap = requireSnapshot(authorization, snapshotId);
        byte[] bytes = conMonService.downloadOriginal(snap);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(snap.getOriginalFilename()).build());
        headers.setContentLength(bytes.length);
        return ResponseEntity.ok().headers(headers).body(new ByteArrayResource(bytes));
    }

    @DeleteMapping("/snapshots/{snapshotId}")
    public ResponseEntity<Void> delete(@PathVariable Long authorizationId,
                                       @PathVariable Long snapshotId,
                                       Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        User currentUser = requireCurrentUser(principal);
        ConMonSnapshot snap = requireSnapshot(authorization, snapshotId);
        // ConMon snapshots are org-level artifacts: any EDITOR+ may delete them;
        // CONTRIBUTORs can delete only their own (matches the documents pattern).
        accessGuard.requireDeleteOwnedItem(authorization, currentUser, snap.getUploadedBy().getId());
        conMonService.delete(snap);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/analytics")
    public ResponseEntity<ConMonAnalyticsResponse> analytics(@PathVariable Long authorizationId,
                                                             Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        return ResponseEntity.ok(analyticsService.forAuthorization(authorization));
    }

    private ConMonReconciliationResponse buildDiff(ConMonSnapshot current, ConMonSnapshot previous,
                                                   ConMonReconciliation rec) {
        Map<String, ConMonPoamItem> prevByExt = new HashMap<>();
        for (ConMonPoamItem p : previous.getItems()) prevByExt.put(p.getExternalId(), p);
        Map<String, ConMonPoamItem> currByExt = new HashMap<>();
        for (ConMonPoamItem c : current.getItems()) currByExt.put(c.getExternalId(), c);

        List<ConMonPoamItemResponse> news = new ArrayList<>();
        List<ConMonPoamItemResponse> closed = new ArrayList<>();
        List<ConMonPoamItemResponse> reopened = new ArrayList<>();
        List<ConMonPoamItemResponse> removed = new ArrayList<>();
        List<ConMonReconciliationResponse.ChangedItem> changed = new ArrayList<>();

        for (var e : currByExt.entrySet()) {
            ConMonPoamItem curr = e.getValue();
            ConMonPoamItem prev = prevByExt.get(e.getKey());
            if (prev == null) { news.add(new ConMonPoamItemResponse(curr)); continue; }
            if (prev.getStatus() == ConMonItemStatus.OPEN && curr.getStatus() == ConMonItemStatus.CLOSED) {
                closed.add(new ConMonPoamItemResponse(curr));
            } else if (prev.getStatus() == ConMonItemStatus.CLOSED && curr.getStatus() == ConMonItemStatus.OPEN) {
                reopened.add(new ConMonPoamItemResponse(curr));
            } else {
                List<String> diffs = diffFields(prev, curr);
                if (!diffs.isEmpty()) {
                    changed.add(new ConMonReconciliationResponse.ChangedItem(
                            new ConMonPoamItemResponse(curr),
                            new ConMonPoamItemResponse(prev),
                            diffs));
                }
            }
        }
        for (var e : prevByExt.entrySet()) {
            if (!currByExt.containsKey(e.getKey())) removed.add(new ConMonPoamItemResponse(e.getValue()));
        }

        ConMonReconciliationResponse r = new ConMonReconciliationResponse();
        r.setSnapshotId(current.getId());
        r.setPreviousSnapshotId(previous.getId());
        r.setNewCount(rec.getNewCount());
        r.setClosedCount(rec.getClosedCount());
        r.setReopenedCount(rec.getReopenedCount());
        r.setStillOpenCount(rec.getStillOpenCount());
        r.setRemovedCount(rec.getRemovedCount());
        r.setChangedCount(rec.getChangedCount());
        r.setNewItems(news);
        r.setNewlyClosedItems(closed);
        r.setReopenedItems(reopened);
        r.setRemovedItems(removed);
        r.setChangedItems(changed);
        return r;
    }

    private static List<String> diffFields(ConMonPoamItem a, ConMonPoamItem b) {
        List<String> diffs = new ArrayList<>();
        if (!java.util.Objects.equals(a.getTitle(), b.getTitle())) diffs.add("title");
        if (!java.util.Objects.equals(a.getSeverity(), b.getSeverity())) diffs.add("severity");
        if (!java.util.Objects.equals(a.getScheduledCompletionDate(), b.getScheduledCompletionDate()))
            diffs.add("scheduledCompletionDate");
        if (!java.util.Objects.equals(a.getStatus(), b.getStatus())) diffs.add("status");
        return diffs;
    }

    private ConMonSnapshot requireSnapshot(Authorization authorization, Long snapshotId) {
        return snapshotRepository.findByIdAndAuthorization(snapshotId, authorization)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Snapshot " + snapshotId + " not found on authorization " + authorization.getId()));
    }

    private User requireCurrentUser(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User '" + principal.getName() + "' not found."));
    }
}
```

`mvn compile -DskipTests` → BUILD SUCCESS.

Commit:
```
feat(conmon): add ContinuousMonitoringController with 8 endpoints
```

---

## Task 15: Backend integration tests

End-to-end via `@SpringBootTest`. Pattern after `AuthorizationDocumentsIntegrationTest` (commit `bf7efd2`).

Create `back-end/src/test/java/gov/nist/oscal/tools/api/integration/ContinuousMonitoringIntegrationTest.java`:

```java
package gov.nist.oscal.tools.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationGrant;
import gov.nist.oscal.tools.api.entity.AuthorizationRole;
import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.User.GlobalRole;
import gov.nist.oscal.tools.api.repository.AuthorizationGrantRepository;
import gov.nist.oscal.tools.api.repository.AuthorizationRepository;
import gov.nist.oscal.tools.api.repository.AuthorizationTemplateRepository;
import gov.nist.oscal.tools.api.repository.ConMonSnapshotRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Continuous Monitoring end-to-end")
class ContinuousMonitoringIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private OrganizationMembershipRepository membershipRepository;
    @Autowired private AuthorizationTemplateRepository templateRepository;
    @Autowired private AuthorizationRepository authorizationRepository;
    @Autowired private AuthorizationGrantRepository grantRepository;
    @Autowired private ConMonSnapshotRepository snapshotRepository;
    @Autowired private EntityManager entityManager;

    private Organization orgA;
    private User alice, bob, carol;
    private Authorization authA;

    @BeforeEach
    void setUp() {
        snapshotRepository.deleteAll();
        grantRepository.deleteAll();
        authorizationRepository.deleteAll();
        templateRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        orgA = newOrg("Org A");
        Organization orgB = newOrg("Org B");
        alice = newUser("alice");
        bob = newUser("bob");
        carol = newUser("carol");
        joinOrg(alice, orgA, OrganizationRole.USER);
        joinOrg(bob, orgA, OrganizationRole.USER);
        joinOrg(carol, orgB, OrganizationRole.USER);
        AuthorizationTemplate t = newTemplate("T", alice, orgA);
        authA = newAuthorization("A", alice, t, orgA);

        entityManager.flush();
        entityManager.clear();

        alice = userRepository.findByUsername("alice").orElseThrow();
        bob = userRepository.findByUsername("bob").orElseThrow();
        carol = userRepository.findByUsername("carol").orElseThrow();
        authA = authorizationRepository.findById(authA.getId()).orElseThrow();
    }

    @Nested
    @DisplayName("POST snapshots")
    class Upload {

        @Test @WithMockUser("alice")
        @DisplayName("OWNER uploads a FedRAMP xlsx — 201 with reconciliation null")
        void owner_uploadsXlsx_201() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "poam.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    fedrampWorkbook(1, 0));

            mockMvc.perform(multipart("/api/authorizations/" + authA.getId() + "/conmon/snapshots")
                            .file(file).with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sourceFormat").value("FEDRAMP_XLSX"))
                    .andExpect(jsonPath("$.openCount").value(1))
                    .andExpect(jsonPath("$.reconciliation").doesNotExist());
        }

        @Test @WithMockUser("bob")
        @DisplayName("VIEWER blocked — 403")
        void viewer_blocked() throws Exception {
            grant(authA, bob, AuthorizationRole.VIEWER, alice);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "poam.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    fedrampWorkbook(1, 0));

            mockMvc.perform(multipart("/api/authorizations/" + authA.getId() + "/conmon/snapshots")
                            .file(file).with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test @WithMockUser("alice")
        @DisplayName("Unsupported file type — 400")
        void unsupportedExt_400() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "evil.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "x".getBytes());

            mockMvc.perform(multipart("/api/authorizations/" + authA.getId() + "/conmon/snapshots")
                            .file(file).with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test @WithMockUser("carol")
        @DisplayName("cross-org user — 404")
        void crossOrg_404() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "poam.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    fedrampWorkbook(1, 0));

            mockMvc.perform(multipart("/api/authorizations/" + authA.getId() + "/conmon/snapshots")
                            .file(file).with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Reconciliation between two snapshots")
    class Reconciliation {

        @Test @WithMockUser("alice")
        @DisplayName("Second snapshot triggers reconciliation row")
        void secondSnapshot_reconciles() throws Exception {
            // First snapshot: 1 open
            uploadXlsx("first.xlsx", 1, 0);
            // Second snapshot: 0 open, 1 closed (the same ID, transition open→closed)
            Long secondId = uploadXlsx("second.xlsx", 0, 1);

            mockMvc.perform(get("/api/authorizations/" + authA.getId()
                            + "/conmon/snapshots/" + secondId + "/reconciliation"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.snapshotId").value(secondId));
            // closedCount should be 1 (P-1 went open→closed in the synthetic data)
        }

        @Test @WithMockUser("alice")
        @DisplayName("First snapshot has no reconciliation — 404")
        void firstSnapshot_noReconciliation_404() throws Exception {
            Long id = uploadXlsx("first.xlsx", 1, 0);
            mockMvc.perform(get("/api/authorizations/" + authA.getId()
                            + "/conmon/snapshots/" + id + "/reconciliation"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Listing and analytics")
    class Listing {

        @Test @WithMockUser("alice")
        @DisplayName("Lists snapshots newest-first")
        void lists_newestFirst() throws Exception {
            uploadXlsx("a.xlsx", 1, 0);
            uploadXlsx("b.xlsx", 1, 0);

            mockMvc.perform(get("/api/authorizations/" + authA.getId() + "/conmon/snapshots"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test @WithMockUser("alice")
        @DisplayName("Analytics endpoint returns time series + donut")
        void analytics_returnsExpected() throws Exception {
            uploadXlsx("a.xlsx", 2, 0);

            mockMvc.perform(get("/api/authorizations/" + authA.getId() + "/conmon/analytics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.openCountSeries").isArray())
                    .andExpect(jsonPath("$.currentStatusBreakdown").isArray());
        }
    }

    // --- helpers ---

    private Long uploadXlsx(String filename, int openRows, int closedRows) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", filename,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                fedrampWorkbook(openRows, closedRows));
        MvcResult result = mockMvc.perform(multipart("/api/authorizations/" + authA.getId() + "/conmon/snapshots")
                        .file(file).with(csrf())
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user("alice")))
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private byte[] fedrampWorkbook(int openRows, int closedRows) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            if (openRows > 0) {
                Sheet s = wb.createSheet("Open POA&M Items");
                writeHeader(s);
                for (int i = 0; i < openRows; i++) {
                    Row r = s.createRow(1 + i);
                    r.createCell(0).setCellValue("P-" + (i + 1));
                    r.createCell(1).setCellValue("Open weakness " + (i + 1));
                    r.createCell(3).setCellValue("High");
                    r.createCell(7).setCellValue("Ongoing");
                }
            }
            if (closedRows > 0) {
                Sheet s = wb.createSheet("Closed POA&M Items");
                writeHeader(s);
                for (int i = 0; i < closedRows; i++) {
                    Row r = s.createRow(1 + i);
                    // Use the same external IDs as the open sheet to drive transitions
                    r.createCell(0).setCellValue("P-" + (i + 1));
                    r.createCell(1).setCellValue("Closed weakness " + (i + 1));
                    r.createCell(3).setCellValue("High");
                    r.createCell(7).setCellValue("Completed");
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private void writeHeader(Sheet s) {
        Row h = s.createRow(0);
        String[] cols = {
            "POA&M Item ID", "Weakness Name", "Weakness Description", "Severity",
            "Scheduled Completion Date", "Actual Completion Date", "Point of Contact", "Status"
        };
        for (int i = 0; i < cols.length; i++) h.createCell(i).setCellValue(cols[i]);
    }

    private Organization newOrg(String name) {
        Organization o = new Organization();
        o.setName(name);
        return organizationRepository.save(o);
    }

    private User newUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@test");
        u.setPassword("x");
        u.setGlobalRole(GlobalRole.USER);
        return userRepository.save(u);
    }

    private void joinOrg(User user, Organization org, OrganizationRole role) {
        OrganizationMembership m = new OrganizationMembership();
        m.setUser(user); m.setOrganization(org); m.setRole(role);
        m.setStatus(MembershipStatus.ACTIVE); m.setJoinedAt(LocalDateTime.now());
        membershipRepository.save(m);
    }

    private AuthorizationTemplate newTemplate(String name, User creator, Organization org) {
        AuthorizationTemplate t = new AuthorizationTemplate();
        t.setName(name); t.setContent("body"); t.setCreatedBy(creator);
        t.setCreatedAt(LocalDateTime.now()); t.setLastUpdatedAt(LocalDateTime.now());
        t.setOrganization(org);
        return templateRepository.save(t);
    }

    private Authorization newAuthorization(String name, User creator, AuthorizationTemplate template, Organization org) {
        Authorization a = new Authorization();
        a.setName(name); a.setSspItemId("ssp-" + name); a.setTemplate(template);
        a.setAuthorizedBy(creator); a.setAuthorizedAt(LocalDateTime.now()); a.setCreatedAt(LocalDateTime.now());
        a.setVariableValues(new HashMap<>()); a.setOrganization(org);
        a.setDateExpired(LocalDate.now().plusYears(1));
        a.setSystemOwner("o"); a.setSecurityManager("sm"); a.setAuthorizingOfficial("ao");
        a.setCompletedContent("body");
        return authorizationRepository.save(a);
    }

    private void grant(Authorization auth, User user, AuthorizationRole role, User grantedBy) {
        AuthorizationGrant g = new AuthorizationGrant(auth, user, role, grantedBy);
        grantRepository.save(g);
    }
}
```

Run:

```bash
cd back-end && mvn surefire:test -Dtest=ContinuousMonitoringIntegrationTest -DfailIfNoTests=false 2>&1 | tail -30
```

Expected: ~9 tests pass.

Commit:
```
test(conmon): RBAC + reconciliation end-to-end tests
```

---

## Task 16: Frontend types

Modify `front-end/src/types/oscal.ts`. Add near the existing authorization types:

```typescript
export type ConMonItemStatus = 'OPEN' | 'CLOSED' | 'UNKNOWN';
export type ConMonSourceFormat = 'OSCAL_JSON' | 'OSCAL_XML' | 'OSCAL_YAML' | 'FEDRAMP_XLSX';

export interface ConMonReconciliationCounts {
  newCount: number;
  closedCount: number;
  reopenedCount: number;
  stillOpenCount: number;
  removedCount: number;
  changedCount: number;
  previousSnapshotId?: number | null;
}

export interface ConMonSnapshotSummary {
  id: number;
  authorizationId: number;
  uploadedAt: string;
  uploadedByUsername?: string | null;
  sourceFormat: ConMonSourceFormat;
  originalFilename: string;
  oscalUuid?: string | null;
  oscalVersion?: string | null;
  metadataTitle?: string | null;
  metadataLastModified?: string | null;
  openCount: number;
  closedCount: number;
  unknownCount: number;
  notes?: string | null;
  reconciliation?: ConMonReconciliationCounts | null;
}

export interface ConMonPoamItem {
  id: number;
  externalId: string;
  title: string;
  description?: string | null;
  status: ConMonItemStatus;
  rawStatus?: string | null;
  severity?: 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL' | null;
  weaknessSource?: string | null;
  scheduledCompletionDate?: string | null;
  actualCompletionDate?: string | null;
  pointOfContact?: string | null;
  riskRating?: string | null;
}

export interface ConMonChangedItem {
  current: ConMonPoamItem;
  previous: ConMonPoamItem;
  fieldsChanged: string[];
}

export interface ConMonReconciliationDetail {
  snapshotId: number;
  previousSnapshotId: number;
  newCount: number;
  closedCount: number;
  reopenedCount: number;
  stillOpenCount: number;
  removedCount: number;
  changedCount: number;
  newItems: ConMonPoamItem[];
  newlyClosedItems: ConMonPoamItem[];
  reopenedItems: ConMonPoamItem[];
  removedItems: ConMonPoamItem[];
  changedItems: ConMonChangedItem[];
}

export interface ConMonAnalytics {
  openCountSeries: Array<{ date: string; open: number; closed: number; unknown: number }>;
  severitySeriesByDate: Array<{ date: string; low: number; moderate: number; high: number; critical: number }>;
  currentStatusBreakdown: Array<{ label: string; count: number }>;
  agingBuckets: Array<{ bucket: string; count: number }>;
  meanTimeToCloseDays?: number | null;
}
```

Vigilance: `git diff -- front-end/src/types/oscal.ts | head -120` must show only additions. Restore-and-reapply if WIP is mixed.

Commit:
```
feat(conmon): add ConMon types to frontend
```

---

## Task 17: API client methods

Modify `front-end/src/lib/api-client.ts`. Extend type imports with `ConMonSnapshotSummary`, `ConMonReconciliationDetail`, `ConMonAnalytics`. Add methods (match existing try/catch + console.error style):

```typescript
  async listConMonSnapshots(authorizationId: number): Promise<ConMonSnapshotSummary[]> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/authorizations/${authorizationId}/conmon/snapshots`,
      { method: 'GET', headers: this.getAuthHeaders() }, 8000);
    if (!response.ok) throw new Error(`Failed to list snapshots: ${response.status}`);
    return await response.json();
  }

  async uploadConMonSnapshot(authorizationId: number, file: File, notes?: string): Promise<ConMonSnapshotSummary> {
    const formData = new FormData();
    formData.append('file', file);
    if (notes) formData.append('notes', notes);
    const token = typeof localStorage !== 'undefined' ? localStorage.getItem('token') : null;
    const headers: HeadersInit = token ? { Authorization: `Bearer ${token}` } : {};
    const response = await fetch(
      `${API_BASE_URL}/authorizations/${authorizationId}/conmon/snapshots`,
      { method: 'POST', headers, body: formData });
    if (!response.ok) throw new Error(`Failed to upload snapshot: ${response.status}`);
    return await response.json();
  }

  async getConMonReconciliation(authorizationId: number, snapshotId: number): Promise<ConMonReconciliationDetail> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/authorizations/${authorizationId}/conmon/snapshots/${snapshotId}/reconciliation`,
      { method: 'GET', headers: this.getAuthHeaders() }, 8000);
    if (!response.ok) throw new Error(`Failed to load reconciliation: ${response.status}`);
    return await response.json();
  }

  async listConMonItems(authorizationId: number, snapshotId: number,
                        opts?: { status?: 'OPEN'|'CLOSED'|'UNKNOWN'; severity?: string; q?: string;
                                 page?: number; size?: number }): Promise<{
    items: import('@/types/oscal').ConMonPoamItem[];
    totalElements: number;
    totalPages: number;
    page: number;
    size: number;
  }> {
    const params = new URLSearchParams();
    if (opts?.status) params.set('status', opts.status);
    if (opts?.severity) params.set('severity', opts.severity);
    if (opts?.q) params.set('q', opts.q);
    if (opts?.page !== undefined) params.set('page', String(opts.page));
    if (opts?.size !== undefined) params.set('size', String(opts.size));
    const qs = params.toString();
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/authorizations/${authorizationId}/conmon/snapshots/${snapshotId}/items${qs ? `?${qs}` : ''}`,
      { method: 'GET', headers: this.getAuthHeaders() }, 8000);
    if (!response.ok) throw new Error(`Failed to list items: ${response.status}`);
    return await response.json();
  }

  async deleteConMonSnapshot(authorizationId: number, snapshotId: number): Promise<void> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/authorizations/${authorizationId}/conmon/snapshots/${snapshotId}`,
      { method: 'DELETE', headers: this.getAuthHeaders() }, 8000);
    if (!response.ok) throw new Error(`Failed to delete snapshot: ${response.status}`);
  }

  async downloadConMonSnapshot(authorizationId: number, snapshotId: number): Promise<Blob> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/authorizations/${authorizationId}/conmon/snapshots/${snapshotId}/download`,
      { method: 'GET', headers: this.getAuthHeaders() }, 30000);
    if (!response.ok) throw new Error(`Failed to download snapshot: ${response.status}`);
    return await response.blob();
  }

  async getConMonAnalytics(authorizationId: number): Promise<ConMonAnalytics> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/authorizations/${authorizationId}/conmon/analytics`,
      { method: 'GET', headers: this.getAuthHeaders() }, 8000);
    if (!response.ok) throw new Error(`Failed to load analytics: ${response.status}`);
    return await response.json();
  }
```

Vigilance check + commit:
```
feat(conmon): add ConMon methods to api client
```

---

## Task 18: KPI tiles component

Create `front-end/src/app/authorizations/authorization/[authorizationId]/_tabs/conmon/kpi-tiles.tsx`:

```tsx
'use client';

import { Card } from '@/components/ui/card';
import type { ConMonSnapshotSummary } from '@/types/oscal';

interface Props {
  latest: ConMonSnapshotSummary | null;
}

export function KpiTiles({ latest }: Props) {
  if (!latest) {
    return (
      <Card className="p-4 text-center text-sm text-muted-foreground">
        No snapshots yet. Upload one to see open/closed counts.
      </Card>
    );
  }

  const tiles = [
    { label: 'Open', value: latest.openCount, color: 'text-amber-600' },
    { label: 'Closed', value: latest.closedCount, color: 'text-green-600' },
    { label: 'Unknown', value: latest.unknownCount, color: 'text-muted-foreground' },
    { label: 'Last snapshot', value: new Date(latest.uploadedAt).toLocaleDateString(), color: '' },
  ];

  return (
    <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
      {tiles.map((t) => (
        <Card key={t.label} className="p-4">
          <div className="text-xs uppercase text-muted-foreground">{t.label}</div>
          <div className={`mt-1 text-2xl font-semibold ${t.color}`}>{t.value}</div>
        </Card>
      ))}
    </div>
  );
}
```

Commit:
```
feat(conmon): add KPI tiles component
```

---

## Task 19: Reconciliation banner

Create `front-end/src/app/authorizations/authorization/[authorizationId]/_tabs/conmon/reconciliation-banner.tsx`:

```tsx
'use client';

import { useState } from 'react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { ChevronDown, ChevronUp } from 'lucide-react';
import type { ConMonReconciliationCounts, ConMonReconciliationDetail } from '@/types/oscal';

interface Props {
  counts: ConMonReconciliationCounts;
  previousSnapshotDate: string | null;
  onLoadDetail: () => Promise<ConMonReconciliationDetail>;
}

export function ReconciliationBanner({ counts, previousSnapshotDate, onLoadDetail }: Props) {
  const [expanded, setExpanded] = useState(false);
  const [detail, setDetail] = useState<ConMonReconciliationDetail | null>(null);

  const handleToggle = async () => {
    if (!expanded && !detail) {
      try {
        setDetail(await onLoadDetail());
      } catch {
        // surfaced as toast upstream
      }
    }
    setExpanded(!expanded);
  };

  return (
    <Card className="p-4">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h3 className="text-sm font-semibold">Since last snapshot</h3>
          <p className="text-xs text-muted-foreground">
            {previousSnapshotDate ? `Previous upload: ${new Date(previousSnapshotDate).toLocaleDateString()}` : ''}
          </p>
          <p className="mt-1 text-sm">
            <span className="font-medium">{counts.newCount}</span> new
            {' · '}
            <span className="font-medium text-green-600">{counts.closedCount}</span> closed
            {' · '}
            <span className="font-medium text-amber-600">{counts.reopenedCount}</span> reopened
            {' · '}
            <span className="font-medium">{counts.stillOpenCount}</span> still open
            {counts.removedCount > 0 && (
              <> {' · '} <span className="font-medium text-destructive">{counts.removedCount}</span> removed</>
            )}
            {counts.changedCount > 0 && (
              <> {' · '} <span className="font-medium">{counts.changedCount}</span> changed</>
            )}
          </p>
        </div>
        <Button variant="ghost" size="sm" onClick={handleToggle}>
          {expanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
          {expanded ? 'Hide details' : 'Show details'}
        </Button>
      </div>

      {expanded && detail && (
        <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2">
          <DetailList title="New" items={detail.newItems.map((i) => i.title)} />
          <DetailList title="Newly closed" items={detail.newlyClosedItems.map((i) => i.title)} />
          <DetailList title="Reopened" items={detail.reopenedItems.map((i) => i.title)} />
          <DetailList title="Removed" items={detail.removedItems.map((i) => i.title)} />
          <DetailList
            title="Changed"
            items={detail.changedItems.map((c) => `${c.current.title} (${c.fieldsChanged.join(', ')})`)}
          />
        </div>
      )}
    </Card>
  );
}

function DetailList({ title, items }: { title: string; items: string[] }) {
  if (items.length === 0) return null;
  return (
    <div className="rounded-md border p-2">
      <div className="mb-1 text-xs font-semibold uppercase text-muted-foreground">{title}</div>
      <ul className="space-y-0.5 text-xs">
        {items.slice(0, 10).map((s, i) => <li key={i} className="truncate">{s}</li>)}
        {items.length > 10 && <li className="text-muted-foreground">…and {items.length - 10} more</li>}
      </ul>
    </div>
  );
}
```

Commit:
```
feat(conmon): add ReconciliationBanner component
```

---

## Task 20: Analytics dashboard

Create `front-end/src/app/authorizations/authorization/[authorizationId]/_tabs/conmon/analytics-dashboard.tsx`:

```tsx
'use client';

import { Card } from '@/components/ui/card';
import {
  LazyResponsiveContainer,
  LazyLineChart,
  LazyBarChart,
  LazyPieChart,
} from '@/components/lazy/LazyCharts';
import { Line, Bar, Pie, Cell, CartesianGrid, XAxis, YAxis, Tooltip, Legend } from 'recharts';
import type { ConMonAnalytics } from '@/types/oscal';

interface Props {
  analytics: ConMonAnalytics | null;
  loading: boolean;
}

const STATUS_COLORS: Record<string, string> = {
  Open: '#F59E0B',
  Closed: '#10B981',
  Unknown: '#9CA3AF',
};

const SEVERITY_COLORS = {
  low: '#3B82F6',
  moderate: '#F59E0B',
  high: '#EF4444',
  critical: '#7C2D12',
};

export function AnalyticsDashboard({ analytics, loading }: Props) {
  if (loading) {
    return <Card className="p-6 text-sm text-muted-foreground">Loading analytics…</Card>;
  }
  if (!analytics || analytics.openCountSeries.length === 0) {
    return (
      <Card className="p-6 text-sm text-muted-foreground">
        Upload a few snapshots to see trends and aging analytics here.
      </Card>
    );
  }

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <Card className="p-4">
        <h3 className="mb-2 text-sm font-semibold">Open POAM count over time</h3>
        <LazyResponsiveContainer width="100%" height={240}>
          <LazyLineChart data={analytics.openCountSeries}>
            <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />
            <XAxis dataKey="date" stroke="#9CA3AF" tick={{ fontSize: 11 }} />
            <YAxis stroke="#9CA3AF" tick={{ fontSize: 11 }} />
            <Tooltip contentStyle={tooltipStyle} />
            <Line type="monotone" dataKey="open" stroke="#F59E0B" strokeWidth={2} dot={false} />
            <Line type="monotone" dataKey="closed" stroke="#10B981" strokeWidth={2} dot={false} />
          </LazyLineChart>
        </LazyResponsiveContainer>
      </Card>

      <Card className="p-4">
        <h3 className="mb-2 text-sm font-semibold">Open POAMs by severity over time</h3>
        <LazyResponsiveContainer width="100%" height={240}>
          <LazyBarChart data={analytics.severitySeriesByDate}>
            <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />
            <XAxis dataKey="date" stroke="#9CA3AF" tick={{ fontSize: 11 }} />
            <YAxis stroke="#9CA3AF" tick={{ fontSize: 11 }} />
            <Tooltip contentStyle={tooltipStyle} />
            <Legend />
            <Bar dataKey="critical" stackId="sev" fill={SEVERITY_COLORS.critical} />
            <Bar dataKey="high" stackId="sev" fill={SEVERITY_COLORS.high} />
            <Bar dataKey="moderate" stackId="sev" fill={SEVERITY_COLORS.moderate} />
            <Bar dataKey="low" stackId="sev" fill={SEVERITY_COLORS.low} />
          </LazyBarChart>
        </LazyResponsiveContainer>
      </Card>

      <Card className="p-4">
        <h3 className="mb-2 text-sm font-semibold">Current status breakdown</h3>
        <LazyResponsiveContainer width="100%" height={240}>
          <LazyPieChart>
            <Pie
              data={analytics.currentStatusBreakdown}
              cx="50%" cy="50%"
              innerRadius={50} outerRadius={80}
              paddingAngle={2}
              dataKey="count"
              nameKey="label"
              label={(p: any) => `${p.label || ''} (${((Number(p.percent) || 0) * 100).toFixed(0)}%)`}
              labelLine={false}
            >
              {analytics.currentStatusBreakdown.map((seg, i) => (
                <Cell key={i} fill={STATUS_COLORS[seg.label] || '#6B7280'} />
              ))}
            </Pie>
            <Tooltip contentStyle={tooltipStyle} />
          </LazyPieChart>
        </LazyResponsiveContainer>
      </Card>

      <Card className="p-4">
        <h3 className="mb-2 text-sm font-semibold">Aging — open items</h3>
        <LazyResponsiveContainer width="100%" height={240}>
          <LazyBarChart data={analytics.agingBuckets}>
            <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />
            <XAxis dataKey="bucket" stroke="#9CA3AF" tick={{ fontSize: 11 }} />
            <YAxis stroke="#9CA3AF" tick={{ fontSize: 11 }} />
            <Tooltip contentStyle={tooltipStyle} />
            <Bar dataKey="count" fill="#3B82F6" radius={[4, 4, 0, 0]} />
          </LazyBarChart>
        </LazyResponsiveContainer>
        {analytics.meanTimeToCloseDays != null && (
          <p className="mt-3 text-xs text-muted-foreground">
            Mean time to close: <span className="font-medium text-foreground">
              {analytics.meanTimeToCloseDays.toFixed(1)} days
            </span>
          </p>
        )}
      </Card>
    </div>
  );
}

const tooltipStyle = {
  backgroundColor: '#1F2937',
  border: '1px solid #374151',
  borderRadius: '8px',
  color: '#F9FAFB',
} as const;
```

Commit:
```
feat(conmon): add AnalyticsDashboard with 4 Recharts visualizations
```

---

## Task 21: Snapshot history table + items drawer + upload dialog

These three are stylistically parallel to PR 3's Documents components. Create three new files:

### `_tabs/conmon/upload-snapshot-dialog.tsx`

```tsx
'use client';

import { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { apiClient } from '@/lib/api-client';

interface Props {
  authorizationId: number;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onUploaded: () => void;
}

export function UploadSnapshotDialog({ authorizationId, open, onOpenChange, onUploaded }: Props) {
  const [file, setFile] = useState<File | null>(null);
  const [notes, setNotes] = useState('');
  const [uploading, setUploading] = useState(false);

  const handleSubmit = async () => {
    if (!file) { toast.error('Pick a file first'); return; }
    setUploading(true);
    try {
      await apiClient.uploadConMonSnapshot(authorizationId, file, notes || undefined);
      toast.success(`Uploaded ${file.name}`);
      setFile(null); setNotes('');
      onUploaded();
      onOpenChange(false);
    } catch {
      toast.error('Upload failed');
    } finally {
      setUploading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(v) => { if (!uploading) onOpenChange(v); }}>
      <DialogContent className="max-w-xl">
        <DialogHeader>
          <DialogTitle>Upload POAM snapshot</DialogTitle>
          <DialogDescription>
            OSCAL JSON/XML/YAML or FedRAMP POA&amp;M Excel template (.xlsx). For other artifacts use the Documents tab.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div>
            <Label htmlFor="cm-file">File</Label>
            <Input id="cm-file" type="file"
                   accept=".json,.xml,.yaml,.yml,.xlsx"
                   onChange={(e) => setFile(e.target.files?.[0] ?? null)} disabled={uploading} />
          </div>
          <div>
            <Label htmlFor="cm-notes">Notes (optional)</Label>
            <Textarea id="cm-notes" rows={2} value={notes}
                      onChange={(e) => setNotes(e.target.value)} disabled={uploading}
                      placeholder="What's notable about this upload?" />
          </div>
        </div>

        <DialogFooter>
          <Button variant="ghost" onClick={() => onOpenChange(false)} disabled={uploading}>Cancel</Button>
          <Button onClick={handleSubmit} disabled={!file || uploading}>
            {uploading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Upload
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
```

### `_tabs/conmon/snapshot-history-table.tsx`

```tsx
'use client';

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Download, Eye, Trash2 } from 'lucide-react';
import type { ConMonSnapshotSummary } from '@/types/oscal';

interface Props {
  snapshots: ConMonSnapshotSummary[];
  canDelete: (s: ConMonSnapshotSummary) => boolean;
  onView: (s: ConMonSnapshotSummary) => void;
  onDownload: (s: ConMonSnapshotSummary) => void;
  onDelete: (s: ConMonSnapshotSummary) => void;
}

export function SnapshotHistoryTable({ snapshots, canDelete, onView, onDownload, onDelete }: Props) {
  if (snapshots.length === 0) {
    return <p className="py-8 text-center text-sm text-muted-foreground">No snapshots yet.</p>;
  }
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Uploaded</TableHead>
          <TableHead>By</TableHead>
          <TableHead>Format</TableHead>
          <TableHead className="text-right">Open</TableHead>
          <TableHead className="text-right">Closed</TableHead>
          <TableHead className="text-right">Unknown</TableHead>
          <TableHead>Reconciliation</TableHead>
          <TableHead className="text-right">Actions</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {snapshots.map((s) => (
          <TableRow key={s.id}>
            <TableCell className="text-sm">{new Date(s.uploadedAt).toLocaleString()}</TableCell>
            <TableCell className="text-sm text-muted-foreground">{s.uploadedByUsername ?? '—'}</TableCell>
            <TableCell><Badge variant="secondary">{s.sourceFormat.replace('_', ' ')}</Badge></TableCell>
            <TableCell className="text-right">{s.openCount}</TableCell>
            <TableCell className="text-right">{s.closedCount}</TableCell>
            <TableCell className="text-right">{s.unknownCount}</TableCell>
            <TableCell className="text-xs text-muted-foreground">
              {s.reconciliation
                ? `+${s.reconciliation.newCount} new, -${s.reconciliation.closedCount} closed, ${s.reconciliation.reopenedCount} reopened`
                : '—'}
            </TableCell>
            <TableCell className="text-right">
              <div className="flex justify-end gap-1">
                <Button variant="ghost" size="icon" onClick={() => onView(s)} aria-label="View items">
                  <Eye className="h-4 w-4" />
                </Button>
                <Button variant="ghost" size="icon" onClick={() => onDownload(s)} aria-label="Download original">
                  <Download className="h-4 w-4" />
                </Button>
                {canDelete(s) && (
                  <Button variant="ghost" size="icon" onClick={() => onDelete(s)} aria-label="Delete snapshot">
                    <Trash2 className="h-4 w-4" />
                  </Button>
                )}
              </div>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
```

### `_tabs/conmon/items-drawer.tsx`

```tsx
'use client';

import { useEffect, useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { apiClient } from '@/lib/api-client';
import type { ConMonItemStatus, ConMonPoamItem } from '@/types/oscal';

interface Props {
  authorizationId: number;
  snapshotId: number | null;
  onClose: () => void;
}

export function ItemsDrawer({ authorizationId, snapshotId, onClose }: Props) {
  const [status, setStatus] = useState<ConMonItemStatus | 'ALL'>('ALL');
  const [q, setQ] = useState('');
  const [items, setItems] = useState<ConMonPoamItem[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!snapshotId) return;
    setLoading(true);
    apiClient.listConMonItems(authorizationId, snapshotId, {
      status: status === 'ALL' ? undefined : status,
      q: q || undefined,
      size: 100,
    }).then((r) => setItems(r.items)).finally(() => setLoading(false));
  }, [authorizationId, snapshotId, status, q]);

  return (
    <Dialog open={snapshotId !== null} onOpenChange={(v) => { if (!v) onClose(); }}>
      <DialogContent className="max-w-4xl">
        <DialogHeader><DialogTitle>POAM items</DialogTitle></DialogHeader>

        <div className="mb-3 flex flex-wrap items-center gap-2">
          <Select value={status} onValueChange={(v) => setStatus(v as any)}>
            <SelectTrigger className="w-40"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All statuses</SelectItem>
              <SelectItem value="OPEN">Open</SelectItem>
              <SelectItem value="CLOSED">Closed</SelectItem>
              <SelectItem value="UNKNOWN">Unknown</SelectItem>
            </SelectContent>
          </Select>
          <Input className="max-w-xs" placeholder="Search title or ID" value={q}
                 onChange={(e) => setQ(e.target.value)} />
        </div>

        {loading ? <p className="py-6 text-center text-sm text-muted-foreground">Loading…</p>
         : items.length === 0 ? <p className="py-6 text-center text-sm text-muted-foreground">No items match.</p>
         : (
          <div className="max-h-[60vh] overflow-y-auto divide-y">
            {items.map((it) => (
              <div key={it.id} className="py-2">
                <div className="flex items-center justify-between gap-2">
                  <div className="font-medium text-sm">{it.title}</div>
                  <div className="flex gap-1">
                    <Badge variant={it.status === 'OPEN' ? 'destructive' : 'secondary'}>{it.status}</Badge>
                    {it.severity && <Badge variant="outline">{it.severity}</Badge>}
                  </div>
                </div>
                <div className="text-xs text-muted-foreground">{it.externalId}</div>
                {it.description && <div className="mt-1 text-xs text-muted-foreground line-clamp-2">{it.description}</div>}
              </div>
            ))}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
```

Commit (3 files):
```
feat(conmon): add upload dialog, snapshot history table, and items drawer
```

---

## Task 22: Replace ConMon tab stub

Modify `front-end/src/app/authorizations/authorization/[authorizationId]/_tabs/conmon-tab.tsx`. Overwrite the stub with:

```tsx
'use client';

import { useEffect, useState } from 'react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Plus, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { apiClient } from '@/lib/api-client';
import { KpiTiles } from './conmon/kpi-tiles';
import { ReconciliationBanner } from './conmon/reconciliation-banner';
import { AnalyticsDashboard } from './conmon/analytics-dashboard';
import { SnapshotHistoryTable } from './conmon/snapshot-history-table';
import { ItemsDrawer } from './conmon/items-drawer';
import { UploadSnapshotDialog } from './conmon/upload-snapshot-dialog';
import type { AuthorizationResponse, ConMonAnalytics, ConMonSnapshotSummary } from '@/types/oscal';

interface Props {
  authorization: AuthorizationResponse;
}

export function ContinuousMonitoringTab({ authorization }: Props) {
  const [snapshots, setSnapshots] = useState<ConMonSnapshotSummary[]>([]);
  const [analytics, setAnalytics] = useState<ConMonAnalytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [analyticsLoading, setAnalyticsLoading] = useState(true);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [drawerSnapshotId, setDrawerSnapshotId] = useState<number | null>(null);

  const role = authorization.effectiveRole;
  const canUpload = role === 'OWNER' || role === 'EDITOR' || role === 'CONTRIBUTOR';

  const refresh = async () => {
    setLoading(true);
    try {
      setSnapshots(await apiClient.listConMonSnapshots(authorization.id));
    } catch {
      toast.error('Failed to load snapshots');
    } finally {
      setLoading(false);
    }
  };

  const refreshAnalytics = async () => {
    setAnalyticsLoading(true);
    try {
      setAnalytics(await apiClient.getConMonAnalytics(authorization.id));
    } catch {
      // non-fatal
    } finally {
      setAnalyticsLoading(false);
    }
  };

  useEffect(() => { void refresh(); /* eslint-disable-next-line react-hooks/exhaustive-deps */ }, [authorization.id]);
  useEffect(() => { void refreshAnalytics(); /* eslint-disable-next-line react-hooks/exhaustive-deps */ }, [authorization.id, snapshots.length]);

  const latest = snapshots[0] ?? null;
  const previousDate = (() => {
    if (!latest?.reconciliation) return null;
    const prev = snapshots.find((s) => s.id === latest.reconciliation?.previousSnapshotId);
    return prev?.uploadedAt ?? null;
  })();

  const handleDownload = async (s: ConMonSnapshotSummary) => {
    try {
      const blob = await apiClient.downloadConMonSnapshot(authorization.id, s.id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = s.originalFilename; a.click();
      URL.revokeObjectURL(url);
    } catch { toast.error('Download failed'); }
  };

  const handleDelete = async (s: ConMonSnapshotSummary) => {
    if (!confirm(`Delete snapshot uploaded ${new Date(s.uploadedAt).toLocaleString()}? This cannot be undone.`)) return;
    try {
      await apiClient.deleteConMonSnapshot(authorization.id, s.id);
      toast.success('Snapshot deleted');
      await refresh();
    } catch { toast.error('Delete failed'); }
  };

  const canDelete = (s: ConMonSnapshotSummary) => {
    if (role === 'OWNER' || role === 'EDITOR') return true;
    if (role === 'CONTRIBUTOR') return s.uploadedByUsername === currentUsername();
    return false;
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-2">
        <h2 className="text-lg font-semibold">Continuous Monitoring</h2>
        {canUpload && (
          <Button onClick={() => setUploadOpen(true)}>
            <Plus className="mr-1 h-4 w-4" />
            Upload snapshot
          </Button>
        )}
      </div>

      <KpiTiles latest={latest} />

      {latest?.reconciliation && (
        <ReconciliationBanner
          counts={latest.reconciliation}
          previousSnapshotDate={previousDate}
          onLoadDetail={() => apiClient.getConMonReconciliation(authorization.id, latest.id)}
        />
      )}

      <AnalyticsDashboard analytics={analytics} loading={analyticsLoading} />

      <Card className="p-4">
        <h3 className="mb-2 text-sm font-semibold">Snapshot history</h3>
        {loading ? (
          <div className="flex items-center justify-center py-6 text-sm text-muted-foreground">
            <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Loading…
          </div>
        ) : (
          <SnapshotHistoryTable
            snapshots={snapshots}
            canDelete={canDelete}
            onView={(s) => setDrawerSnapshotId(s.id)}
            onDownload={(s) => void handleDownload(s)}
            onDelete={(s) => void handleDelete(s)}
          />
        )}
      </Card>

      <UploadSnapshotDialog
        authorizationId={authorization.id}
        open={uploadOpen}
        onOpenChange={setUploadOpen}
        onUploaded={refresh}
      />

      <ItemsDrawer
        authorizationId={authorization.id}
        snapshotId={drawerSnapshotId}
        onClose={() => setDrawerSnapshotId(null)}
      />
    </div>
  );
}

function currentUsername(): string | null {
  if (typeof localStorage === 'undefined') return null;
  try {
    const raw = localStorage.getItem('user');
    if (!raw) return null;
    return JSON.parse(raw).username ?? null;
  } catch { return null; }
}
```

Update `[authorizationId]/page.tsx`: find `<ContinuousMonitoringTab />` and replace with `<ContinuousMonitoringTab authorization={authorization!} />`. Verify diff scope — restore-and-reapply if WIP contamination.

Stage both files, commit:
```
feat(conmon): replace ConMon tab stub with full implementation
```

---

## Task 23: Frontend tests

Create three test files under `_tabs/conmon/__tests__/`:

### `__tests__/upload-snapshot-dialog.test.tsx`

```tsx
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { UploadSnapshotDialog } from '../upload-snapshot-dialog';
import { apiClient } from '@/lib/api-client';

vi.mock('@/lib/api-client', () => ({ apiClient: { uploadConMonSnapshot: vi.fn() } }));
vi.mock('sonner', () => ({ toast: { success: vi.fn(), error: vi.fn() } }));

describe('UploadSnapshotDialog', () => {
  beforeEach(() => vi.clearAllMocks());

  it('renders nothing when closed', () => {
    render(<UploadSnapshotDialog authorizationId={1} open={false} onOpenChange={vi.fn()} onUploaded={vi.fn()} />);
    expect(screen.queryByText('Upload POAM snapshot')).not.toBeInTheDocument();
  });

  it('renders the form when open', () => {
    render(<UploadSnapshotDialog authorizationId={1} open={true} onOpenChange={vi.fn()} onUploaded={vi.fn()} />);
    expect(screen.getByText('Upload POAM snapshot')).toBeInTheDocument();
  });

  it('disables Upload until file is picked', () => {
    render(<UploadSnapshotDialog authorizationId={1} open={true} onOpenChange={vi.fn()} onUploaded={vi.fn()} />);
    expect(screen.getByRole('button', { name: /^Upload$/ })).toBeDisabled();
  });

  it('calls uploadConMonSnapshot when submitted', async () => {
    (apiClient.uploadConMonSnapshot as any).mockResolvedValue({ id: 1 });
    const onUploaded = vi.fn();
    const onOpenChange = vi.fn();
    render(<UploadSnapshotDialog authorizationId={42} open={true} onOpenChange={onOpenChange} onUploaded={onUploaded} />);

    const file = new File(['{}'], 'p.json', { type: 'application/json' });
    const input = screen.getByLabelText(/^File$/) as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: [file] });
    fireEvent.change(input);

    fireEvent.click(screen.getByRole('button', { name: /^Upload$/ }));
    await waitFor(() => {
      expect(apiClient.uploadConMonSnapshot).toHaveBeenCalledWith(42, file, undefined);
      expect(onUploaded).toHaveBeenCalled();
      expect(onOpenChange).toHaveBeenCalledWith(false);
    });
  });
});
```

### `__tests__/reconciliation-banner.test.tsx`

```tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ReconciliationBanner } from '../reconciliation-banner';

const counts = {
  newCount: 2, closedCount: 1, reopenedCount: 0, stillOpenCount: 5, removedCount: 0, changedCount: 1,
  previousSnapshotId: 99,
};

describe('ReconciliationBanner', () => {
  it('renders counts inline', () => {
    render(<ReconciliationBanner counts={counts} previousSnapshotDate={null} onLoadDetail={vi.fn()} />);
    expect(screen.getByText(/Since last snapshot/)).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument(); // newCount
    expect(screen.getByText('1', { selector: '.text-green-600' })).toBeInTheDocument();
  });

  it('loads detail on expand', async () => {
    const onLoad = vi.fn().mockResolvedValue({
      snapshotId: 100, previousSnapshotId: 99,
      newCount: 1, closedCount: 0, reopenedCount: 0, stillOpenCount: 0, removedCount: 0, changedCount: 0,
      newItems: [{ id: 1, externalId: 'X', title: 'New finding', status: 'OPEN' } as any],
      newlyClosedItems: [], reopenedItems: [], removedItems: [], changedItems: [],
    });
    render(<ReconciliationBanner counts={counts} previousSnapshotDate={null} onLoadDetail={onLoad} />);
    fireEvent.click(screen.getByRole('button', { name: /Show details/i }));
    await waitFor(() => {
      expect(onLoad).toHaveBeenCalled();
      expect(screen.getByText('New finding')).toBeInTheDocument();
    });
  });
});
```

### `__tests__/conmon-tab.test.tsx`

```tsx
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { ContinuousMonitoringTab } from '../../conmon-tab';
import { apiClient } from '@/lib/api-client';
import type { AuthorizationResponse } from '@/types/oscal';

vi.mock('@/lib/api-client', () => ({
  apiClient: {
    listConMonSnapshots: vi.fn(),
    getConMonAnalytics: vi.fn(),
    downloadConMonSnapshot: vi.fn(),
    deleteConMonSnapshot: vi.fn(),
    getConMonReconciliation: vi.fn(),
  },
}));
vi.mock('sonner', () => ({ toast: { success: vi.fn(), error: vi.fn() } }));
vi.mock('../conmon/upload-snapshot-dialog', () => ({
  UploadSnapshotDialog: ({ open }: { open: boolean }) =>
    open ? <div data-testid="upload-dialog">upload</div> : null,
}));
vi.mock('../conmon/items-drawer', () => ({
  ItemsDrawer: () => null,
}));
vi.mock('../conmon/analytics-dashboard', () => ({
  AnalyticsDashboard: () => <div data-testid="analytics" />,
}));
vi.mock('../conmon/reconciliation-banner', () => ({
  ReconciliationBanner: () => <div data-testid="reconciliation" />,
}));

function makeAuth(role?: 'OWNER' | 'EDITOR' | 'CONTRIBUTOR' | 'VIEWER'): AuthorizationResponse {
  return {
    id: 1, organizationId: 100, name: 'A', sspItemId: 'ssp', templateId: 1, templateName: 'T',
    variableValues: {}, completedContent: '', authorizedBy: 'alice', authorizedAt: '', createdAt: '',
    effectiveRole: role,
  } as AuthorizationResponse;
}

describe('ContinuousMonitoringTab', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (apiClient.listConMonSnapshots as any).mockResolvedValue([]);
    (apiClient.getConMonAnalytics as any).mockResolvedValue({ openCountSeries: [], severitySeriesByDate: [], currentStatusBreakdown: [], agingBuckets: [] });
  });

  it('shows Upload button for CONTRIBUTOR', async () => {
    render(<ContinuousMonitoringTab authorization={makeAuth('CONTRIBUTOR')} />);
    await waitFor(() => expect(apiClient.listConMonSnapshots).toHaveBeenCalled());
    expect(screen.getByRole('button', { name: /Upload snapshot/i })).toBeInTheDocument();
  });

  it('hides Upload button for VIEWER', async () => {
    render(<ContinuousMonitoringTab authorization={makeAuth('VIEWER')} />);
    await waitFor(() => expect(apiClient.listConMonSnapshots).toHaveBeenCalled());
    expect(screen.queryByRole('button', { name: /Upload snapshot/i })).not.toBeInTheDocument();
  });
});
```

Run:

```bash
cd front-end && npx vitest run \
    'src/app/authorizations/authorization/[authorizationId]/_tabs/conmon/__tests__/' \
    2>&1 | tail -25
```

Commit:
```
test(conmon): frontend tests for ConMon tab and dialogs
```

---

## Task 24: Final verification

Run full backend and frontend test sweeps:

```bash
cd back-end && mvn surefire:test \
    -Dtest='PoamStatusDeriverTest,OscalPoamParserTest,FedrampPoamExcelParserTest,ConMonReconciliationServiceTest,ContinuousMonitoringIntegrationTest,AuthorizationAccessGuardTest,AuthorizationDocumentsIntegrationTest,AuthorizationAclIntegrationTest' \
    -DfailIfNoTests=false 2>&1 | tail -20
```

```bash
cd front-end && npx vitest run 2>&1 | tail -10
cd front-end && npx tsc --noEmit 2>&1 | tail -10
```

If pass, hard-refresh `http://localhost:3010/authorizations/authorization/<id>?tab=conmon` and exercise:
- Upload a synthetic FedRAMP `.xlsx` (or use the CLI's `cli/src/test/resources/cli/example_poam_valid.json`).
- See the KPI tiles populate.
- Upload a second snapshot — reconciliation banner appears.
- Click View Items on a snapshot → drawer opens, filter by status works.
- Analytics charts render once you have ≥1 snapshot.
- VIEWER role: no Upload button, no Delete buttons.

---

## Self-Review

- [ ] V1.9 migration creates 3 tables ✓ (Task 1)
- [ ] Status enum + format enum + 3 entities + 3 repos ✓ (Tasks 2-4)
- [ ] Hybrid status derivation rule (status keyword → finding rollup → unknown) ✓ (Task 6)
- [ ] OSCAL parser using `OscalBindingContext` ✓ (Task 8)
- [ ] FedRAMP Excel parser using POI ✓ (Task 9)
- [ ] Reconciliation algorithm (6 categories) ✓ (Task 10)
- [ ] Analytics service (line/stacked-bar/donut/aging/MTTC) ✓ (Task 12)
- [ ] Service orchestrator + Apache POI dep + multipart 50MB (already PR 3) ✓ (Tasks 7, 13)
- [ ] 8 endpoints with full RBAC ✓ (Task 14)
- [ ] Backend integration tests ✓ (Task 15)
- [ ] Frontend types, API client, KPI tiles, banner, dashboard, history table, items drawer, upload dialog, ConMon tab ✓ (Tasks 16-22)
- [ ] Frontend tests ✓ (Task 23)
- [ ] Final verification ✓ (Task 24)

## Out of Scope (acknowledged follow-ups)

- POI library transitive dependency conflicts (rare but possible) — verify on first compile.
- "Removed" items in reconciliation surface as a data-quality flag, not folded into "closed" (per spec).
- Aging bucket cutoffs hardcoded `<30/30-60/60-90/90-180/>180`; FedRAMP-specific cutoffs are a future config knob.
- Per-item reconciliation in the snapshot summary list payload would be expensive — current API returns per-snapshot counts only and lazy-loads detail on banner expand.
- Email notifications, scheduled ingestion, CSV ingestion, in-app POAM editing — explicitly out of scope per spec.
