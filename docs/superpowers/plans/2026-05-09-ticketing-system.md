# Ticketing System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an in-app ticketing system: users open bug/feature tickets from the avatar menu, hold a threaded conversation with the super admin (with attachments), and track status. Super admin gets a search/filter/analytics panel. Per-event email notifications via SendGrid. Daily auto-close job.

**Architecture:** Standard Spring Boot CRUD mirroring the existing `Profile` pattern — three new tables (`tickets`, `ticket_comments`, `ticket_attachments`), one service, two controllers (user + admin), five new email templates. Next.js front-end with four new pages and a `UserAvatarMenu` extension. Attachment storage via a new `TicketAttachmentStorageService` that wraps the existing `GcsStorageService`.

**Tech Stack:** Java 21, Spring Boot 3.5.9, Spring Data JPA, PostgreSQL via Flyway (next version `V1.10`), SendGrid (existing wiring), Next.js 13+ App Router, TypeScript, JUnit 5, Mockito, Jest + Testing Library.

**Spec:** [`docs/superpowers/specs/2026-05-08-ticketing-system-design.md`](../specs/2026-05-08-ticketing-system-design.md).

---

## Conventions used by this plan

- **Backend root:** `back-end/src/main/java/gov/nist/oscal/tools/api/`
- **Frontend root:** `front-end/src/`
- **Java tests root:** `back-end/src/test/java/gov/nist/oscal/tools/api/`
- **Frontend tests:** colocated `__tests__/` next to source.
- **Commit style:** matches recent history (`feat(ticketing): …`, `test(ticketing): …`, `fix(ticketing): …`). Trailers include `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>`.
- **Build commands:** `cd back-end && mvn -DskipTests=false test -Dtest=<TestClass>` for a single backend test class. `cd front-end && npm test -- <pattern>` for a single frontend test. Per CLAUDE.md the user normally drives builds; for executing this plan the auto-memory override allows running these autonomously.

---

# Phase 1 — Backend foundation

Produces a working backend that an admin can drive end-to-end via Swagger UI: create ticket, list mine, get one, add comment, change status. No UI yet.

## Task 1.1 — Flyway migration V1.10

**Files:**
- Create: `back-end/src/main/resources/db/migration/V1.10__ticketing.sql`

- [ ] **Step 1: Create the migration file**

```sql
-- V1.10__ticketing.sql
-- Ticketing system: tickets, threaded comments, file attachments.

CREATE TABLE IF NOT EXISTS tickets (
    id              BIGSERIAL PRIMARY KEY,
    reporter_id     BIGINT NOT NULL REFERENCES users(id),
    type            VARCHAR(16) NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT NOT NULL,
    priority        VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    resolved_at     TIMESTAMP(6) WITHOUT TIME ZONE NULL,
    CONSTRAINT tickets_type_check CHECK (type IN ('BUG','FEATURE')),
    CONSTRAINT tickets_status_check CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','CLOSED','WONT_FIX','DUPLICATE')),
    CONSTRAINT tickets_priority_check CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL'))
);

CREATE INDEX IF NOT EXISTS idx_tickets_reporter_status ON tickets(reporter_id, status);
CREATE INDEX IF NOT EXISTS idx_tickets_status_created ON tickets(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_tickets_type_status ON tickets(type, status);
CREATE INDEX IF NOT EXISTS idx_tickets_updated ON tickets(updated_at DESC);

CREATE TABLE IF NOT EXISTS ticket_comments (
    id                  BIGSERIAL PRIMARY KEY,
    ticket_id           BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    author_id           BIGINT NOT NULL REFERENCES users(id),
    body                TEXT NOT NULL,
    is_status_change    BOOLEAN NOT NULL DEFAULT false,
    old_status          VARCHAR(16) NULL,
    new_status          VARCHAR(16) NULL,
    created_at          TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ticket_comments_ticket_created ON ticket_comments(ticket_id, created_at);

CREATE TABLE IF NOT EXISTS ticket_attachments (
    id              BIGSERIAL PRIMARY KEY,
    ticket_id       BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    comment_id      BIGINT NULL REFERENCES ticket_comments(id) ON DELETE CASCADE,
    uploader_id     BIGINT NOT NULL REFERENCES users(id),
    filename        VARCHAR(255) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    size_bytes      BIGINT NOT NULL,
    storage_path    VARCHAR(512) NOT NULL,
    created_at      TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ticket_attachments_ticket ON ticket_attachments(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_attachments_comment ON ticket_attachments(comment_id);
```

- [ ] **Step 2: Verify migration is the next version**

Run: `ls back-end/src/main/resources/db/migration/ | sort -V | tail -3`
Expected: ends with `V1.9__continuous_monitoring.sql` and `V1.10__ticketing.sql`.

- [ ] **Step 3: Apply migration locally**

Run: `cd back-end && mvn flyway:migrate -Dflyway.configFiles=src/main/resources/application.properties`

If that's not how this repo runs Flyway in isolation, instead boot the backend (`./dev.sh`) and watch the logs for `Migrating schema "public" to version "1.10 - ticketing"`. Either way, expected: migration applied, no errors.

- [ ] **Step 4: Verify tables exist**

Run: `docker exec oscal-postgres-dev psql -U oscal_user -d oscal_dev -c "\d tickets"`
Expected: shows the `tickets` table with the columns above.

Repeat for `ticket_comments` and `ticket_attachments`.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/resources/db/migration/V1.10__ticketing.sql
git commit -m "feat(ticketing): add V1.10 migration for tickets, comments, attachments"
```

---

## Task 1.2 — Enum classes

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/TicketType.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/TicketStatus.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/TicketPriority.java`

- [ ] **Step 1: Write `TicketType.java`**

```java
package gov.nist.oscal.tools.api.entity;

public enum TicketType {
    BUG,
    FEATURE
}
```

- [ ] **Step 2: Write `TicketStatus.java`**

```java
package gov.nist.oscal.tools.api.entity;

import java.util.Set;

public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    WONT_FIX,
    DUPLICATE;

    private static final Set<TicketStatus> TERMINAL =
        Set.of(CLOSED, WONT_FIX, DUPLICATE);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    public boolean isResolvedNotClosed() {
        return this == RESOLVED;
    }
}
```

- [ ] **Step 3: Write `TicketPriority.java`**

```java
package gov.nist.oscal.tools.api.entity;

public enum TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
```

- [ ] **Step 4: Add a unit test for `TicketStatus` helpers**

Create: `back-end/src/test/java/gov/nist/oscal/tools/api/entity/TicketStatusTest.java`

```java
package gov.nist.oscal.tools.api.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TicketStatusTest {

    @Test
    void terminalStates() {
        assertTrue(TicketStatus.CLOSED.isTerminal());
        assertTrue(TicketStatus.WONT_FIX.isTerminal());
        assertTrue(TicketStatus.DUPLICATE.isTerminal());
        assertFalse(TicketStatus.OPEN.isTerminal());
        assertFalse(TicketStatus.IN_PROGRESS.isTerminal());
        assertFalse(TicketStatus.RESOLVED.isTerminal());
    }

    @Test
    void resolvedNotClosed() {
        assertTrue(TicketStatus.RESOLVED.isResolvedNotClosed());
        assertFalse(TicketStatus.CLOSED.isResolvedNotClosed());
        assertFalse(TicketStatus.OPEN.isResolvedNotClosed());
    }
}
```

- [ ] **Step 5: Run test**

Run: `cd back-end && mvn test -Dtest=TicketStatusTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/entity/TicketType.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/entity/TicketStatus.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/entity/TicketPriority.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/entity/TicketStatusTest.java
git commit -m "feat(ticketing): add TicketType, TicketStatus, TicketPriority enums"
```

---

## Task 1.3 — `Ticket` entity

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/Ticket.java`

- [ ] **Step 1: Write the entity**

```java
package gov.nist.oscal.tools.api.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TicketType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TicketStatus status = TicketStatus.OPEN;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public Ticket() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Ticket(User reporter, TicketType type, String title, String description) {
        this();
        this.reporter = reporter;
        this.type = type;
        this.title = title;
        this.description = description;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }
    public TicketType getType() { return type; }
    public void setType(TicketType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata == null ? new HashMap<>() : metadata; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
```

- [ ] **Step 2: Verify the JSONB type dependency exists**

Run: `grep -r "hypersistence-utils" back-end/pom.xml back-end/../pom.xml`
Expected: a dependency line is present. If not, add to `back-end/pom.xml` inside `<dependencies>`:

```xml
<dependency>
    <groupId>io.hypersistence</groupId>
    <artifactId>hypersistence-utils-hibernate-63</artifactId>
    <version>3.7.0</version>
</dependency>
```

(Use `hypersistence-utils-hibernate-63` for Hibernate 6.3+. If the project is on a different Hibernate version, look at an existing JSONB-using entity in the repo and mirror its annotation; if no such entity exists yet, the dependency-add above is correct for this repo's Spring Boot 3.5.9 / Hibernate 6.x baseline.)

- [ ] **Step 3: Boot backend to validate schema match**

Run: `./dev.sh` and watch the logs.
Expected: backend starts cleanly. No `SchemaManagementException: Schema-validation: missing column ...` errors. If validation fails, the migration in Task 1.1 doesn't match the entity — fix the column type/length to match.

Stop the backend (`./stop.sh`) before continuing.

- [ ] **Step 4: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/entity/Ticket.java back-end/pom.xml
git commit -m "feat(ticketing): add Ticket JPA entity"
```

---

## Task 1.4 — `TicketComment` entity

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/TicketComment.java`

- [ ] **Step 1: Write the entity**

```java
package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_comments")
public class TicketComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_status_change", nullable = false)
    private boolean statusChange = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 16)
    private TicketStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 16)
    private TicketStatus newStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TicketComment() {
        this.createdAt = LocalDateTime.now();
    }

    public TicketComment(Ticket ticket, User author, String body) {
        this();
        this.ticket = ticket;
        this.author = author;
        this.body = body;
    }

    /** Factory for system-generated status-change comments. */
    public static TicketComment statusChange(Ticket ticket, User actor, TicketStatus from, TicketStatus to) {
        TicketComment c = new TicketComment(ticket, actor,
            "Status changed from " + from.name() + " to " + to.name() + ".");
        c.statusChange = true;
        c.oldStatus = from;
        c.newStatus = to;
        return c;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public boolean isStatusChange() { return statusChange; }
    public void setStatusChange(boolean statusChange) { this.statusChange = statusChange; }
    public TicketStatus getOldStatus() { return oldStatus; }
    public void setOldStatus(TicketStatus oldStatus) { this.oldStatus = oldStatus; }
    public TicketStatus getNewStatus() { return newStatus; }
    public void setNewStatus(TicketStatus newStatus) { this.newStatus = newStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 2: Boot backend, verify schema validates**

Run `./dev.sh`, confirm clean boot, then `./stop.sh`.

- [ ] **Step 3: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/entity/TicketComment.java
git commit -m "feat(ticketing): add TicketComment JPA entity with statusChange factory"
```

---

## Task 1.5 — `TicketAttachment` entity

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/TicketAttachment.java`

- [ ] **Step 1: Write the entity**

```java
package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_attachments")
public class TicketAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne
    @JoinColumn(name = "comment_id")
    private TicketComment comment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TicketAttachment() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }
    public TicketComment getComment() { return comment; }
    public void setComment(TicketComment comment) { this.comment = comment; }
    public User getUploader() { return uploader; }
    public void setUploader(User uploader) { this.uploader = uploader; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 2: Boot backend, verify schema validates**

`./dev.sh` clean boot, then `./stop.sh`.

- [ ] **Step 3: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/entity/TicketAttachment.java
git commit -m "feat(ticketing): add TicketAttachment JPA entity"
```

---

## Task 1.6 — Repositories

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/TicketRepository.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/TicketCommentRepository.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/TicketAttachmentRepository.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/repository/TicketRepositoryTest.java`

- [ ] **Step 1: Write `TicketRepository.java`**

```java
package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Ticket;
import gov.nist.oscal.tools.api.entity.TicketStatus;
import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketRepository
        extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    Page<Ticket> findByReporter(User reporter, Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE t.status = :status AND t.resolvedAt < :before")
    List<Ticket> findResolvedBefore(@Param("status") TicketStatus status,
                                    @Param("before") LocalDateTime before);
}
```

- [ ] **Step 2: Write `TicketCommentRepository.java`**

```java
package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Ticket;
import gov.nist.oscal.tools.api.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {
    List<TicketComment> findByTicketOrderByCreatedAtAsc(Ticket ticket);
}
```

- [ ] **Step 3: Write `TicketAttachmentRepository.java`**

```java
package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Ticket;
import gov.nist.oscal.tools.api.entity.TicketAttachment;
import gov.nist.oscal.tools.api.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {
    List<TicketAttachment> findByTicket(Ticket ticket);
    List<TicketAttachment> findByComment(TicketComment comment);
    List<TicketAttachment> findByTicketAndCommentIsNull(Ticket ticket);
}
```

- [ ] **Step 4: Write a `@DataJpaTest` against `TicketRepository`**

Create: `back-end/src/test/java/gov/nist/oscal/tools/api/repository/TicketRepositoryTest.java`

Look first at an existing `@DataJpaTest` in this repo (e.g., search `find back-end/src/test -name "*RepositoryTest.java" | head -3`) and mirror its annotations and test database setup. The skeleton:

```java
package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TicketRepositoryTest {

    @Autowired private TicketRepository tickets;
    @Autowired private UserRepository users;

    @Test
    void findResolvedBeforeReturnsExpiredOnly() {
        User u = users.save(newUser("alice"));
        Ticket fresh = tickets.save(newTicket(u, TicketStatus.RESOLVED, LocalDateTime.now()));
        Ticket old   = tickets.save(newTicket(u, TicketStatus.RESOLVED, LocalDateTime.now().minusDays(8)));

        var result = tickets.findResolvedBefore(TicketStatus.RESOLVED, LocalDateTime.now().minusDays(7));

        assertThat(result).extracting(Ticket::getId).containsExactly(old.getId());
    }

    @Test
    void findByReporterReturnsOnlyReportersTickets() {
        User alice = users.save(newUser("alice"));
        User bob   = users.save(newUser("bob"));
        tickets.save(newTicket(alice, TicketStatus.OPEN, null));
        tickets.save(newTicket(bob,   TicketStatus.OPEN, null));

        Page<Ticket> result = tickets.findByReporter(alice, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getReporter().getId()).isEqualTo(alice.getId());
    }

    private User newUser(String username) {
        // Mirror the existing test User-creation helper from another *RepositoryTest;
        // populate required NOT NULL columns on `users`.
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        // ... (set any other required fields the User entity demands)
        return u;
    }

    private Ticket newTicket(User reporter, TicketStatus status, LocalDateTime resolvedAt) {
        Ticket t = new Ticket(reporter, TicketType.BUG, "title", "description");
        t.setStatus(status);
        t.setResolvedAt(resolvedAt);
        return t;
    }
}
```

- [ ] **Step 5: Run repository tests**

Run: `cd back-end && mvn test -Dtest=TicketRepositoryTest`
Expected: PASS. If `newUser` complains about missing required columns, add them — model after another `*RepositoryTest` in the codebase.

- [ ] **Step 6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/repository/TicketRepository.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/repository/TicketCommentRepository.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/repository/TicketAttachmentRepository.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/repository/TicketRepositoryTest.java
git commit -m "feat(ticketing): add ticket repositories with finder methods"
```

---

## Task 1.7 — `TicketAttachmentStorageService`

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/TicketAttachmentStorageService.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/TicketAttachmentStorageServiceTest.java`

This wraps the existing `GcsStorageService` (or `FileStorageService` locally) for the `tickets/` prefix and enforces the file size / count / extension limits.

- [ ] **Step 1: Inspect existing storage service for the right method to call**

Run: `grep -n "uploadComponent\|uploadArtifact\|uploadLibrary\|public.*upload" back-end/src/main/java/gov/nist/oscal/tools/api/service/GcsStorageService.java back-end/src/main/java/gov/nist/oscal/tools/api/service/FileStorageService.java`
Note the method that takes raw bytes / `InputStream` and a path — that's what we'll call. Likely `uploadComponent` or a generic helper. If only domain-named methods exist, pick the one whose signature matches and reuse it (the path discriminates the bucket location).

- [ ] **Step 2: Write the failing test**

```java
package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.service.TicketAttachmentStorageService.AttachmentUpload;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketAttachmentStorageServiceTest {

    @Test
    void rejectsOversizeFile() {
        TicketAttachmentStorageService svc =
            new TicketAttachmentStorageService(/* underlying storage */ null);
        byte[] eleven_mb = new byte[11 * 1024 * 1024];
        MultipartFile f = new MockMultipartFile("f", "big.png", "image/png", eleven_mb);

        assertThatThrownBy(() -> svc.validate(f))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("10");
    }

    @Test
    void rejectsForbiddenExtension() {
        TicketAttachmentStorageService svc = new TicketAttachmentStorageService(null);
        MultipartFile f = new MockMultipartFile("f", "evil.exe", "application/octet-stream", new byte[10]);

        assertThatThrownBy(() -> svc.validate(f))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not allowed");
    }

    @Test
    void acceptsValidPng() {
        TicketAttachmentStorageService svc = new TicketAttachmentStorageService(null);
        MultipartFile f = new MockMultipartFile("f", "ok.png", "image/png", new byte[100]);
        svc.validate(f); // no throw
    }
}
```

- [ ] **Step 3: Run test, confirm it fails**

Run: `cd back-end && mvn test -Dtest=TicketAttachmentStorageServiceTest`
Expected: FAIL — class doesn't exist yet.

- [ ] **Step 4: Implement the service**

```java
package gov.nist.oscal.tools.api.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class TicketAttachmentStorageService {

    public static final long MAX_BYTES = 10L * 1024 * 1024; // 10 MB
    public static final int MAX_FILES_PER_REQUEST = 5;
    public static final Set<String> ALLOWED_EXT = Set.of(
        "png","jpg","jpeg","gif","pdf","txt","log","json","xml","yaml","yml");

    private final StorageService storage;

    public TicketAttachmentStorageService(StorageService storage) {
        this.storage = storage;
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException(
                "File exceeds 10 MB limit: " + file.getOriginalFilename());
        }
        String ext = extension(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException(
                "File type ." + ext + " is not allowed");
        }
    }

    /** Returns the GCS storage path. */
    public AttachmentUpload upload(Long ticketId, MultipartFile file) throws IOException {
        validate(file);
        String safeName = file.getOriginalFilename().replaceAll("[^A-Za-z0-9._-]", "_");
        String path = "tickets/" + ticketId + "/" + System.currentTimeMillis() + "-" + safeName;
        Map<String, String> metadata = new HashMap<>();
        metadata.put("ticketId", String.valueOf(ticketId));
        // Reuse the existing storage abstraction. If StorageService has a generic
        // "upload bytes to path" method, call it here. If it only exposes
        // domain-named methods, add a generic `uploadBytes(String path, byte[] data,
        // String contentType, Map<String,String> metadata)` to the interface and
        // implement it on each StorageService impl (GCS/S3/File). Mirror what
        // ArtifactStorageService does — it likely already added such a method.
        storage.uploadBytes(path, file.getBytes(), file.getContentType(), metadata);
        return new AttachmentUpload(path, file.getSize(), file.getContentType(), file.getOriginalFilename());
    }

    public byte[] download(String storagePath) throws IOException {
        return storage.downloadBytes(storagePath);
    }

    public record AttachmentUpload(String storagePath, long sizeBytes,
                                   String contentType, String originalFilename) {}

    private static String extension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }
}
```

- [ ] **Step 5: Add `uploadBytes` / `downloadBytes` to `StorageService`**

Open `back-end/src/main/java/gov/nist/oscal/tools/api/service/StorageService.java` and add:

```java
/** Generic byte upload, used by feature-specific storage services. */
String uploadBytes(String path, byte[] data, String contentType, Map<String, String> metadata);

/** Generic byte download. */
byte[] downloadBytes(String path);
```

Then implement these in **all** existing impls (`GcsStorageService.java`, `FileStorageService.java`, `S3StorageService.java`). Each impl already has SDK-specific code for byte uploads — extract the common pattern from one of the existing `upload*` methods. For `FileStorageService` it's `Files.write(Paths.get(localRoot, path), data)`; for `GcsStorageService` it's `storage.create(BlobInfo.newBuilder(bucket, path).setContentType(contentType).setMetadata(metadata).build(), data)`; for `S3StorageService` it's `s3.putObject(PutObjectRequest.builder().bucket(bucket).key(path).contentType(contentType).metadata(metadata).build(), RequestBody.fromBytes(data))`.

If any impl doesn't conveniently support metadata, pass `null` and document the limitation in a one-line comment.

- [ ] **Step 6: Run the test**

Run: `cd back-end && mvn test -Dtest=TicketAttachmentStorageServiceTest`
Expected: PASS.

- [ ] **Step 7: Run a wider test pass to check nothing else broke**

Run: `cd back-end && mvn test`
Expected: All previously-passing tests still pass.

- [ ] **Step 8: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/TicketAttachmentStorageService.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/StorageService.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/GcsStorageService.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/FileStorageService.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/S3StorageService.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/TicketAttachmentStorageServiceTest.java
git commit -m "feat(ticketing): add TicketAttachmentStorageService with size/extension limits"
```

---

## Task 1.8 — Email templates and `EmailService` interface additions

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/email/EmailService.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/email/SendGridEmailService.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/email/NoOpEmailService.java`
- Create: `back-end/src/main/resources/email-templates/ticket-created-admin.html`
- Create: `back-end/src/main/resources/email-templates/ticket-created-admin.txt`
- Create: `back-end/src/main/resources/email-templates/ticket-created-reporter.html`
- Create: `back-end/src/main/resources/email-templates/ticket-created-reporter.txt`
- Create: `back-end/src/main/resources/email-templates/ticket-comment-added.html`
- Create: `back-end/src/main/resources/email-templates/ticket-comment-added.txt`
- Create: `back-end/src/main/resources/email-templates/ticket-status-changed.html`
- Create: `back-end/src/main/resources/email-templates/ticket-status-changed.txt`
- Create: `back-end/src/main/resources/email-templates/ticket-reopened.html`
- Create: `back-end/src/main/resources/email-templates/ticket-reopened.txt`
- Modify: `back-end/src/main/resources/application.properties` (add `app.support.email`)

- [ ] **Step 1: Add the recipient property**

Open `back-end/src/main/resources/application.properties` and add:

```properties
# Where ticketing system notifications go for the super admin.
app.support.email=thowerton@regscale.com
```

- [ ] **Step 2: Add five method signatures to `EmailService.java`**

Open `back-end/src/main/java/gov/nist/oscal/tools/api/email/EmailService.java`, add imports for `Ticket`, `TicketComment`, `TicketStatus`, and append:

```java
void sendTicketCreatedToAdmin(Ticket ticket);
void sendTicketCreatedToReporter(Ticket ticket);
void sendTicketCommentAdded(Ticket ticket, TicketComment comment, User recipient);
void sendTicketStatusChanged(Ticket ticket, TicketStatus oldStatus, TicketStatus newStatus, String adminNote);
void sendTicketReopened(Ticket ticket, TicketComment reopenComment);
```

- [ ] **Step 3: Implement the methods in `SendGridEmailService.java`**

At the top of the class, inject the support-email value via a new constructor parameter `String supportEmail`. Update `EmailConfig.java` to pass `${app.support.email}` to the constructor.

Add the methods:

```java
@Override
public void sendTicketCreatedToAdmin(Ticket t) {
    Map<String, String> vars = ticketVars(t);
    send("ticket-created-admin", supportEmail,
        "[OSCAL Hub] New " + t.getType().name() + ": TKT-" + t.getId() + " — " + t.getTitle(),
        vars);
}

@Override
public void sendTicketCreatedToReporter(Ticket t) {
    Map<String, String> vars = ticketVars(t);
    send("ticket-created-reporter", t.getReporter().getEmail(),
        "[OSCAL Hub] We received your " + humanType(t.getType()) + " — TKT-" + t.getId(),
        vars);
}

@Override
public void sendTicketCommentAdded(Ticket t, TicketComment c, User recipient) {
    Map<String, String> vars = ticketVars(t);
    vars.put("authorName", c.getAuthor().getUsername());
    vars.put("commentBody", nullSafe(c.getBody()));
    send("ticket-comment-added", recipient.getEmail(),
        "[OSCAL Hub] New comment on TKT-" + t.getId(),
        vars);
}

@Override
public void sendTicketStatusChanged(Ticket t, TicketStatus oldStatus, TicketStatus newStatus, String adminNote) {
    Map<String, String> vars = ticketVars(t);
    vars.put("oldStatus", oldStatus.name());
    vars.put("newStatus", newStatus.name());
    vars.put("adminNote", nullSafe(adminNote));
    send("ticket-status-changed", t.getReporter().getEmail(),
        "[OSCAL Hub] TKT-" + t.getId() + " is now " + newStatus.name(),
        vars);
}

@Override
public void sendTicketReopened(Ticket t, TicketComment reopenComment) {
    Map<String, String> vars = ticketVars(t);
    vars.put("reopenBody", nullSafe(reopenComment.getBody()));
    send("ticket-reopened", supportEmail,
        "[OSCAL Hub] Reopened: TKT-" + t.getId() + " — " + t.getTitle(),
        vars);
}

private Map<String, String> ticketVars(Ticket t) {
    Map<String, String> vars = new HashMap<>();
    vars.put("ticketId", "TKT-" + t.getId());
    vars.put("title", t.getTitle());
    vars.put("type", humanType(t.getType()));
    vars.put("priority", t.getPriority().name());
    vars.put("status", t.getStatus().name());
    vars.put("description", nullSafe(t.getDescription()));
    vars.put("reporterName", t.getReporter().getUsername());
    vars.put("ticketUrl", baseUrl + "/tickets/" + t.getId());
    return vars;
}

private String humanType(TicketType type) {
    return type == TicketType.BUG ? "Bug Report" : "Feature Request";
}
```

(Wrap email failures the same way the existing methods do — log and swallow per spec.)

- [ ] **Step 4: Implement no-op stubs in `NoOpEmailService.java`**

Add the same five methods, each a logged no-op:

```java
@Override public void sendTicketCreatedToAdmin(Ticket t) { log("sendTicketCreatedToAdmin", t.getId()); }
@Override public void sendTicketCreatedToReporter(Ticket t) { log("sendTicketCreatedToReporter", t.getId()); }
@Override public void sendTicketCommentAdded(Ticket t, TicketComment c, User r) { log("sendTicketCommentAdded", t.getId()); }
@Override public void sendTicketStatusChanged(Ticket t, TicketStatus o, TicketStatus n, String note) { log("sendTicketStatusChanged", t.getId()); }
@Override public void sendTicketReopened(Ticket t, TicketComment c) { log("sendTicketReopened", t.getId()); }

private void log(String method, Long ticketId) {
    LoggerFactory.getLogger(NoOpEmailService.class).debug("NoOpEmail: {} ticketId={}", method, ticketId);
}
```

- [ ] **Step 5: Write the five `.txt` templates**

Each is a short plain-text version. Example for `ticket-created-admin.txt`:

```
A new {{type}} was reported on OSCAL Hub.

  {{ticketId}} — {{title}}
  Reporter: {{reporterName}}
  Priority: {{priority}}

Description:
{{description}}

View ticket: {{ticketUrl}}
```

Repeat for `ticket-created-reporter.txt`, `ticket-comment-added.txt` (uses `{{authorName}}` and `{{commentBody}}`), `ticket-status-changed.txt` (uses `{{oldStatus}}`, `{{newStatus}}`, `{{adminNote}}`), `ticket-reopened.txt` (uses `{{reopenBody}}`). Mirror the writing tone of existing templates like `welcome.txt`.

- [ ] **Step 6: Write the five `.html` templates**

Each is the HTML mirror of the txt. Look at `welcome.html` for the wrapper style — keep it minimal: a single styled `<div>` with the same content the txt has, formatted as paragraphs and a styled link button to `{{ticketUrl}}`.

- [ ] **Step 7: Update `EmailConfig.java`**

Wire the new constructor argument: read `app.support.email` and pass it to `new SendGridEmailService(...)`.

- [ ] **Step 8: Sanity test — boot the backend**

Run: `./dev.sh`
Expected: backend starts cleanly. If `EmailConfig` injection fails, the missing property is the usual cause.
Then `./stop.sh`.

- [ ] **Step 9: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/email/ \
        back-end/src/main/java/gov/nist/oscal/tools/api/config/EmailConfig.java \
        back-end/src/main/resources/email-templates/ticket-*.html \
        back-end/src/main/resources/email-templates/ticket-*.txt \
        back-end/src/main/resources/application.properties
git commit -m "feat(ticketing): add 5 email templates and EmailService methods"
```

---

## Task 1.9 — `TicketService.createTicket` (TDD)

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/TicketService.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/TicketServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.email.EmailService;
import gov.nist.oscal.tools.api.entity.*;
import gov.nist.oscal.tools.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TicketServiceTest {

    private TicketRepository tickets;
    private TicketCommentRepository comments;
    private TicketAttachmentRepository attachments;
    private TicketAttachmentStorageService storage;
    private EmailService email;
    private UserRepository users;

    private TicketService svc;

    @BeforeEach
    void setUp() {
        tickets = mock(TicketRepository.class);
        comments = mock(TicketCommentRepository.class);
        attachments = mock(TicketAttachmentRepository.class);
        storage = mock(TicketAttachmentStorageService.class);
        email = mock(EmailService.class);
        users = mock(UserRepository.class);
        svc = new TicketService(tickets, comments, attachments, storage, email, users);
    }

    @Test
    void createTicket_persistsAndSendsBothEmails() {
        User reporter = userWithUsername("alice");
        when(tickets.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(42L);
            return t;
        });

        Ticket created = svc.createTicket(
            reporter, TicketType.BUG, "It crashed", "Steps...",
            TicketPriority.HIGH, Map.of("severity", "MAJOR"), List.of());

        assertThat(created.getId()).isEqualTo(42L);
        assertThat(created.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(created.getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(created.getMetadata()).containsEntry("severity", "MAJOR");
        verify(email).sendTicketCreatedToAdmin(created);
        verify(email).sendTicketCreatedToReporter(created);
    }

    @Test
    void createTicket_uploadsAttachments() throws Exception {
        User reporter = userWithUsername("alice");
        when(tickets.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0); t.setId(1L); return t;
        });
        MultipartFile file = new MockMultipartFile("f", "screenshot.png", "image/png", new byte[10]);
        when(storage.upload(eq(1L), eq(file)))
            .thenReturn(new TicketAttachmentStorageService.AttachmentUpload(
                "tickets/1/x-screenshot.png", 10L, "image/png", "screenshot.png"));

        svc.createTicket(reporter, TicketType.BUG, "t", "d",
            TicketPriority.MEDIUM, Map.of(), List.of(file));

        ArgumentCaptor<TicketAttachment> cap = ArgumentCaptor.forClass(TicketAttachment.class);
        verify(attachments).save(cap.capture());
        assertThat(cap.getValue().getStoragePath()).isEqualTo("tickets/1/x-screenshot.png");
        assertThat(cap.getValue().getComment()).isNull();
    }

    @Test
    void createTicket_rejectsMoreThanFiveFiles() {
        User reporter = userWithUsername("alice");
        var sixFiles = List.<MultipartFile>of(
            mockFile(), mockFile(), mockFile(), mockFile(), mockFile(), mockFile());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> svc.createTicket(reporter, TicketType.BUG, "t", "d",
                TicketPriority.MEDIUM, Map.of(), sixFiles));
        verifyNoInteractions(tickets);
    }

    private MultipartFile mockFile() {
        return new MockMultipartFile("f", "x.png", "image/png", new byte[1]);
    }

    private User userWithUsername(String u) {
        User user = new User();
        user.setId(1L);
        user.setUsername(u);
        user.setEmail(u + "@example.com");
        return user;
    }
}
```

- [ ] **Step 2: Run test, confirm failure (`TicketService` doesn't exist)**

Run: `cd back-end && mvn test -Dtest=TicketServiceTest#createTicket_persistsAndSendsBothEmails`
Expected: FAIL — class not found.

- [ ] **Step 3: Implement `TicketService` skeleton with `createTicket`**

```java
package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.email.EmailService;
import gov.nist.oscal.tools.api.entity.*;
import gov.nist.oscal.tools.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class TicketService {

    private final TicketRepository tickets;
    private final TicketCommentRepository comments;
    private final TicketAttachmentRepository attachments;
    private final TicketAttachmentStorageService storage;
    private final EmailService email;
    private final UserRepository users;

    public TicketService(TicketRepository tickets,
                         TicketCommentRepository comments,
                         TicketAttachmentRepository attachments,
                         TicketAttachmentStorageService storage,
                         EmailService email,
                         UserRepository users) {
        this.tickets = tickets;
        this.comments = comments;
        this.attachments = attachments;
        this.storage = storage;
        this.email = email;
        this.users = users;
    }

    @Transactional
    public Ticket createTicket(User reporter, TicketType type, String title, String description,
                               TicketPriority priority, Map<String, Object> metadata,
                               List<MultipartFile> files) {
        if (files != null && files.size() > TicketAttachmentStorageService.MAX_FILES_PER_REQUEST) {
            throw new IllegalArgumentException(
                "Max " + TicketAttachmentStorageService.MAX_FILES_PER_REQUEST + " files per request");
        }
        Ticket t = new Ticket(reporter, type, title, description);
        t.setPriority(priority == null ? TicketPriority.MEDIUM : priority);
        if (metadata != null) t.setMetadata(metadata);
        Ticket saved = tickets.save(t);

        if (files != null) {
            for (MultipartFile f : files) {
                try {
                    var up = storage.upload(saved.getId(), f);
                    TicketAttachment a = new TicketAttachment();
                    a.setTicket(saved);
                    a.setUploader(reporter);
                    a.setFilename(up.originalFilename());
                    a.setContentType(up.contentType());
                    a.setSizeBytes(up.sizeBytes());
                    a.setStoragePath(up.storagePath());
                    attachments.save(a);
                } catch (IOException e) {
                    throw new RuntimeException("Attachment upload failed", e);
                }
            }
        }

        try { email.sendTicketCreatedToAdmin(saved); } catch (Exception ignored) {}
        try { email.sendTicketCreatedToReporter(saved); } catch (Exception ignored) {}
        return saved;
    }
}
```

- [ ] **Step 4: Run all tests in `TicketServiceTest`**

Run: `cd back-end && mvn test -Dtest=TicketServiceTest`
Expected: PASS for all three tests.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/TicketService.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/TicketServiceTest.java
git commit -m "feat(ticketing): TicketService.createTicket with attachments and emails"
```

---

## Task 1.10 — `TicketService.getTicket` and `listMyTickets` (auth check)

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/TicketService.java`
- Modify: `back-end/src/test/java/gov/nist/oscal/tools/api/service/TicketServiceTest.java`

- [ ] **Step 1: Add failing tests**

Append to `TicketServiceTest`:

```java
@Test
void getTicket_returnsForReporter() {
    User alice = userWithUsername("alice");
    Ticket t = new Ticket(alice, TicketType.BUG, "x", "y");
    t.setId(7L);
    when(tickets.findById(7L)).thenReturn(java.util.Optional.of(t));

    Ticket got = svc.getTicket(7L, alice, /*isAdmin*/ false);
    assertThat(got.getId()).isEqualTo(7L);
}

@Test
void getTicket_returnsForAdminEvenIfNotReporter() {
    User alice = userWithUsername("alice");
    User adminBob = userWithUsername("bob");
    Ticket t = new Ticket(alice, TicketType.BUG, "x", "y");
    t.setId(7L);
    when(tickets.findById(7L)).thenReturn(java.util.Optional.of(t));

    assertThat(svc.getTicket(7L, adminBob, true).getId()).isEqualTo(7L);
}

@Test
void getTicket_throwsForOtherUserNotAdmin() {
    User alice = userWithUsername("alice");
    User mallory = userWithUsername("mallory");
    Ticket t = new Ticket(alice, TicketType.BUG, "x", "y");
    t.setId(7L);
    when(tickets.findById(7L)).thenReturn(java.util.Optional.of(t));

    org.junit.jupiter.api.Assertions.assertThrows(
        org.springframework.security.access.AccessDeniedException.class,
        () -> svc.getTicket(7L, mallory, false));
}

@Test
void listMyTickets_filtersToReporter() {
    User alice = userWithUsername("alice");
    org.springframework.data.domain.Pageable p = org.springframework.data.domain.PageRequest.of(0, 25);
    when(tickets.findByReporter(alice, p)).thenReturn(org.springframework.data.domain.Page.empty());
    svc.listMyTickets(alice, p);
    verify(tickets).findByReporter(alice, p);
}
```

- [ ] **Step 2: Run, confirm 4 failures**

Run: `cd back-end && mvn test -Dtest=TicketServiceTest`
Expected: 4 new failures (`getTicket` / `listMyTickets` undefined).

- [ ] **Step 3: Implement**

Add to `TicketService`:

```java
@Transactional(readOnly = true)
public Ticket getTicket(Long id, User caller, boolean isAdmin) {
    Ticket t = tickets.findById(id)
        .orElseThrow(() -> new java.util.NoSuchElementException("Ticket " + id));
    if (!isAdmin && !t.getReporter().getId().equals(caller.getId())) {
        throw new org.springframework.security.access.AccessDeniedException(
            "Not your ticket");
    }
    return t;
}

@Transactional(readOnly = true)
public org.springframework.data.domain.Page<Ticket> listMyTickets(
        User reporter, org.springframework.data.domain.Pageable pageable) {
    return tickets.findByReporter(reporter, pageable);
}
```

- [ ] **Step 4: Run and confirm pass**

Run: `cd back-end && mvn test -Dtest=TicketServiceTest`
Expected: all PASS.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/TicketService.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/TicketServiceTest.java
git commit -m "feat(ticketing): TicketService.getTicket with reporter-or-admin auth"
```

---

## Task 1.11 — `TicketService.addComment` with reopen logic

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/TicketService.java`
- Modify: `back-end/src/test/java/gov/nist/oscal/tools/api/service/TicketServiceTest.java`

- [ ] **Step 1: Add failing tests**

```java
@Test
void addComment_byUser_emailsAdmin() {
    User alice = userWithUsername("alice");
    Ticket t = new Ticket(alice, TicketType.BUG, "x", "y");
    t.setId(1L); t.setStatus(TicketStatus.OPEN);
    when(tickets.findById(1L)).thenReturn(java.util.Optional.of(t));
    when(comments.save(any())).thenAnswer(inv -> inv.getArgument(0));

    TicketComment c = svc.addComment(1L, alice, /*isAdmin*/ false, "More info", List.of());

    assertThat(c.getBody()).isEqualTo("More info");
    verify(email).sendTicketCommentAdded(eq(t), eq(c), any(User.class));
    verify(email, never()).sendTicketReopened(any(), any());
}

@Test
void addComment_byAdmin_emailsReporter() {
    User alice = userWithUsername("alice");
    User adminBob = userWithUsername("bob");
    Ticket t = new Ticket(alice, TicketType.BUG, "x", "y");
    t.setId(1L); t.setStatus(TicketStatus.IN_PROGRESS);
    when(tickets.findById(1L)).thenReturn(java.util.Optional.of(t));
    when(comments.save(any())).thenAnswer(inv -> inv.getArgument(0));

    svc.addComment(1L, adminBob, true, "working on it", List.of());

    ArgumentCaptor<User> recip = ArgumentCaptor.forClass(User.class);
    verify(email).sendTicketCommentAdded(eq(t), any(), recip.capture());
    assertThat(recip.getValue().getId()).isEqualTo(alice.getId());
}

@Test
void addComment_byReporterOnResolved_reopens() {
    User alice = userWithUsername("alice");
    Ticket t = new Ticket(alice, TicketType.BUG, "x", "y");
    t.setId(1L); t.setStatus(TicketStatus.RESOLVED);
    t.setResolvedAt(java.time.LocalDateTime.now().minusDays(1));
    when(tickets.findById(1L)).thenReturn(java.util.Optional.of(t));
    when(comments.save(any())).thenAnswer(inv -> inv.getArgument(0));

    svc.addComment(1L, alice, false, "still broken", List.of());

    assertThat(t.getStatus()).isEqualTo(TicketStatus.OPEN);
    assertThat(t.getResolvedAt()).isNull();
    verify(email).sendTicketReopened(eq(t), any());
    // The reporter's own comment alone does NOT also fire a CommentAdded email,
    // because Reopened replaces it. (Spec: notify Admin only on reopen.)
    verify(email, never()).sendTicketCommentAdded(any(), any(), any());
}

@Test
void addComment_byReporterOnTerminalState_doesNotReopen() {
    User alice = userWithUsername("alice");
    Ticket t = new Ticket(alice, TicketType.BUG, "x", "y");
    t.setId(1L); t.setStatus(TicketStatus.CLOSED);
    when(tickets.findById(1L)).thenReturn(java.util.Optional.of(t));
    when(comments.save(any())).thenAnswer(inv -> inv.getArgument(0));

    svc.addComment(1L, alice, false, "any updates?", List.of());

    assertThat(t.getStatus()).isEqualTo(TicketStatus.CLOSED);
    verify(email, never()).sendTicketReopened(any(), any());
    verify(email).sendTicketCommentAdded(any(), any(), any());
}
```

- [ ] **Step 2: Run, confirm 4 failures**

Run: `cd back-end && mvn test -Dtest=TicketServiceTest`
Expected: 4 new failures.

- [ ] **Step 3: Implement `addComment`**

Add to `TicketService`:

```java
@Transactional
public TicketComment addComment(Long ticketId, User caller, boolean isAdmin,
                                String body, List<MultipartFile> files) {
    if (files != null && files.size() > TicketAttachmentStorageService.MAX_FILES_PER_REQUEST) {
        throw new IllegalArgumentException(
            "Max " + TicketAttachmentStorageService.MAX_FILES_PER_REQUEST + " files per request");
    }
    Ticket t = getTicket(ticketId, caller, isAdmin);

    boolean reporterReopening = !isAdmin
        && caller.getId().equals(t.getReporter().getId())
        && t.getStatus().isResolvedNotClosed();

    TicketStatus oldStatus = t.getStatus();
    if (reporterReopening) {
        t.setStatus(TicketStatus.OPEN);
        t.setResolvedAt(null);
    }
    t.setUpdatedAt(java.time.LocalDateTime.now());
    tickets.save(t);

    TicketComment c = comments.save(new TicketComment(t, caller, body));

    if (reporterReopening) {
        comments.save(TicketComment.statusChange(t, caller, oldStatus, TicketStatus.OPEN));
    }

    if (files != null) {
        for (MultipartFile f : files) {
            try {
                var up = storage.upload(t.getId(), f);
                TicketAttachment a = new TicketAttachment();
                a.setTicket(t); a.setComment(c); a.setUploader(caller);
                a.setFilename(up.originalFilename());
                a.setContentType(up.contentType());
                a.setSizeBytes(up.sizeBytes());
                a.setStoragePath(up.storagePath());
                attachments.save(a);
            } catch (IOException e) {
                throw new RuntimeException("Attachment upload failed", e);
            }
        }
    }

    try {
        if (reporterReopening) {
            email.sendTicketReopened(t, c);
        } else {
            User recipient = isAdmin ? t.getReporter() : null; // resolved below
            if (!isAdmin) {
                // user comment (non-reopen) → admin gets it; recipient is "support"
                // but EmailService.sendTicketCommentAdded already routes by support.email
                // when the recipient is the admin. We pass `null` to indicate "the admin
                // address from configuration". For the SendGrid impl, treat null as
                // "support email"; alternatively load a designated admin user.
            }
            email.sendTicketCommentAdded(t, c, recipient);
        }
    } catch (Exception ignored) {}

    return c;
}
```

**Important:** the admin-recipient question (how to look up "the admin user object" when emailing the support address) needs a small refinement. Two options:
- (a) Change `EmailService.sendTicketCommentAdded` signature so the recipient is an email string, not a `User`. Then pass `supportEmail` directly when caller is the user.
- (b) Add a `User` lookup helper that returns a synthetic `User` with the support email and a placeholder username.

Pick (a) — it's cleaner. Adjust the interface and tests accordingly. Update the test `addComment_byUser_emailsAdmin`'s assertion to match: `verify(email).sendTicketCommentAdded(eq(t), eq(c), eq(/* the support email string */));`.

If you change the signature, also update `sendTicketCommentAdded` in `SendGridEmailService` and `NoOpEmailService` from `(Ticket, TicketComment, User)` to `(Ticket, TicketComment, String recipientEmail)`. Update the tests in this task to use a `String` arg (e.g., a constructor-injected `supportEmail` field on the service, or a config lookup). Inject `@Value("${app.support.email}") String supportEmail` into `TicketService`.

- [ ] **Step 4: Apply the signature change**

In `TicketService`, inject `@Value("${app.support.email}") String supportEmail` and change the email call to `email.sendTicketCommentAdded(t, c, isAdmin ? t.getReporter().getEmail() : supportEmail);`. Update `EmailService` interface, `SendGridEmailService`, `NoOpEmailService`, and the test mocks accordingly. Also update Task 1.8's signature retroactively in your mental model — the spec's signature `(Ticket, TicketComment, User recipient)` is superseded by `(Ticket, TicketComment, String recipientEmail)`.

- [ ] **Step 5: Run and confirm pass**

Run: `cd back-end && mvn test -Dtest=TicketServiceTest`
Expected: all PASS.

- [ ] **Step 6: Commit**

```bash
git add -A back-end/src/main/java back-end/src/test/java
git commit -m "feat(ticketing): TicketService.addComment with reopen-on-RESOLVED logic"
```

---

## Task 1.12 — `TicketService.changeStatus`

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/TicketService.java`
- Modify: `back-end/src/test/java/gov/nist/oscal/tools/api/service/TicketServiceTest.java`

- [ ] **Step 1: Add failing tests**

```java
@Test
void changeStatus_writesSystemCommentAndEmailsReporter() {
    User alice = userWithUsername("alice");
    User adminBob = userWithUsername("bob");
    Ticket t = new Ticket(alice, TicketType.BUG, "x", "y");
    t.setId(1L); t.setStatus(TicketStatus.OPEN);
    when(tickets.findById(1L)).thenReturn(java.util.Optional.of(t));
    when(comments.save(any())).thenAnswer(inv -> inv.getArgument(0));

    svc.changeStatus(1L, adminBob, TicketStatus.IN_PROGRESS, null);

    assertThat(t.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    ArgumentCaptor<TicketComment> cap = ArgumentCaptor.forClass(TicketComment.class);
    verify(comments).save(cap.capture());
    assertThat(cap.getValue().isStatusChange()).isTrue();
    assertThat(cap.getValue().getOldStatus()).isEqualTo(TicketStatus.OPEN);
    assertThat(cap.getValue().getNewStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    verify(email).sendTicketStatusChanged(t, TicketStatus.OPEN, TicketStatus.IN_PROGRESS, null);
}

@Test
void changeStatus_terminal_setsResolvedAt() {
    User alice = userWithUsername("alice");
    User adminBob = userWithUsername("bob");
    Ticket t = new Ticket(alice, TicketType.BUG, "x", "y");
    t.setId(1L); t.setStatus(TicketStatus.IN_PROGRESS);
    when(tickets.findById(1L)).thenReturn(java.util.Optional.of(t));
    when(comments.save(any())).thenAnswer(inv -> inv.getArgument(0));

    svc.changeStatus(1L, adminBob, TicketStatus.RESOLVED, "Fixed in v2.1.4");

    assertThat(t.getStatus()).isEqualTo(TicketStatus.RESOLVED);
    assertThat(t.getResolvedAt()).isNotNull();
    // expect 2 comments saved: 1 system status-change + 1 admin note
    verify(comments, times(2)).save(any());
    verify(email).sendTicketStatusChanged(t, TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED, "Fixed in v2.1.4");
}

@Test
void changeStatus_noopWhenSameStatus() {
    User alice = userWithUsername("alice");
    User adminBob = userWithUsername("bob");
    Ticket t = new Ticket(alice, TicketType.BUG, "x", "y");
    t.setId(1L); t.setStatus(TicketStatus.OPEN);
    when(tickets.findById(1L)).thenReturn(java.util.Optional.of(t));

    svc.changeStatus(1L, adminBob, TicketStatus.OPEN, null);

    verify(comments, never()).save(any());
    verify(email, never()).sendTicketStatusChanged(any(), any(), any(), any());
}
```

- [ ] **Step 2: Run, confirm 3 failures**

Run: `cd back-end && mvn test -Dtest=TicketServiceTest`

- [ ] **Step 3: Implement**

```java
@Transactional
public Ticket changeStatus(Long ticketId, User adminCaller, TicketStatus newStatus, String adminNote) {
    Ticket t = tickets.findById(ticketId)
        .orElseThrow(() -> new java.util.NoSuchElementException("Ticket " + ticketId));
    TicketStatus oldStatus = t.getStatus();
    if (oldStatus == newStatus) return t;

    t.setStatus(newStatus);
    if (newStatus == TicketStatus.RESOLVED || newStatus.isTerminal()) {
        t.setResolvedAt(java.time.LocalDateTime.now());
    } else {
        t.setResolvedAt(null);
    }
    t.setUpdatedAt(java.time.LocalDateTime.now());
    tickets.save(t);

    comments.save(TicketComment.statusChange(t, adminCaller, oldStatus, newStatus));
    if (adminNote != null && !adminNote.isBlank()) {
        comments.save(new TicketComment(t, adminCaller, adminNote));
    }

    try { email.sendTicketStatusChanged(t, oldStatus, newStatus, adminNote); } catch (Exception ignored) {}
    return t;
}
```

- [ ] **Step 4: Run and confirm pass**

Run: `cd back-end && mvn test -Dtest=TicketServiceTest`

- [ ] **Step 5: Commit**

```bash
git add -A back-end/src/main/java back-end/src/test/java
git commit -m "feat(ticketing): TicketService.changeStatus with system comment + email"
```

---

## Task 1.13 — DTOs

**Files:**
- Create:
  - `back-end/src/main/java/gov/nist/oscal/tools/api/dto/CreateTicketRequest.java`
  - `back-end/src/main/java/gov/nist/oscal/tools/api/dto/UpdateStatusRequest.java`
  - `back-end/src/main/java/gov/nist/oscal/tools/api/dto/TicketSummaryResponse.java`
  - `back-end/src/main/java/gov/nist/oscal/tools/api/dto/TicketDetailResponse.java`
  - `back-end/src/main/java/gov/nist/oscal/tools/api/dto/CommentResponse.java`
  - `back-end/src/main/java/gov/nist/oscal/tools/api/dto/AttachmentResponse.java`

- [ ] **Step 1: Write `CreateTicketRequest`**

```java
package gov.nist.oscal.tools.api.dto;

import gov.nist.oscal.tools.api.entity.TicketPriority;
import gov.nist.oscal.tools.api.entity.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public class CreateTicketRequest {
    @NotNull private TicketType type;
    @NotBlank @Size(max = 200) private String title;
    @NotBlank private String description;
    private TicketPriority priority = TicketPriority.MEDIUM;
    private Map<String, Object> metadata;

    public TicketType getType() { return type; }
    public void setType(TicketType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
```

- [ ] **Step 2: Write `UpdateStatusRequest`**

```java
package gov.nist.oscal.tools.api.dto;

import gov.nist.oscal.tools.api.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateStatusRequest {
    @NotNull private TicketStatus status;
    private String note;

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
```

- [ ] **Step 3: Write `AttachmentResponse`**

```java
package gov.nist.oscal.tools.api.dto;

import gov.nist.oscal.tools.api.entity.TicketAttachment;

public record AttachmentResponse(Long id, String filename, String contentType, long sizeBytes) {
    public static AttachmentResponse from(TicketAttachment a) {
        return new AttachmentResponse(a.getId(), a.getFilename(), a.getContentType(), a.getSizeBytes());
    }
}
```

- [ ] **Step 4: Write `CommentResponse`**

```java
package gov.nist.oscal.tools.api.dto;

import gov.nist.oscal.tools.api.entity.TicketComment;
import gov.nist.oscal.tools.api.entity.TicketStatus;
import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        String authorUsername,
        String body,
        boolean statusChange,
        TicketStatus oldStatus,
        TicketStatus newStatus,
        LocalDateTime createdAt,
        List<AttachmentResponse> attachments) {

    public static CommentResponse from(TicketComment c, List<AttachmentResponse> atts) {
        return new CommentResponse(
            c.getId(),
            c.getAuthor().getUsername(),
            c.getBody(),
            c.isStatusChange(),
            c.getOldStatus(),
            c.getNewStatus(),
            c.getCreatedAt(),
            atts);
    }
}
```

- [ ] **Step 5: Write `TicketSummaryResponse`**

```java
package gov.nist.oscal.tools.api.dto;

import gov.nist.oscal.tools.api.entity.Ticket;
import gov.nist.oscal.tools.api.entity.TicketPriority;
import gov.nist.oscal.tools.api.entity.TicketStatus;
import gov.nist.oscal.tools.api.entity.TicketType;
import java.time.LocalDateTime;

public record TicketSummaryResponse(
        Long id,
        TicketType type,
        String title,
        TicketPriority priority,
        TicketStatus status,
        String reporterUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static TicketSummaryResponse from(Ticket t) {
        return new TicketSummaryResponse(
            t.getId(), t.getType(), t.getTitle(), t.getPriority(), t.getStatus(),
            t.getReporter().getUsername(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
```

- [ ] **Step 6: Write `TicketDetailResponse`**

```java
package gov.nist.oscal.tools.api.dto;

import gov.nist.oscal.tools.api.entity.Ticket;
import gov.nist.oscal.tools.api.entity.TicketPriority;
import gov.nist.oscal.tools.api.entity.TicketStatus;
import gov.nist.oscal.tools.api.entity.TicketType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record TicketDetailResponse(
        Long id,
        TicketType type,
        String title,
        String description,
        TicketPriority priority,
        TicketStatus status,
        Map<String, Object> metadata,
        String reporterUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt,
        List<AttachmentResponse> originalAttachments,
        List<CommentResponse> comments) {

    public static TicketDetailResponse from(Ticket t,
                                            List<AttachmentResponse> originalAttachments,
                                            List<CommentResponse> comments) {
        return new TicketDetailResponse(
            t.getId(), t.getType(), t.getTitle(), t.getDescription(),
            t.getPriority(), t.getStatus(), t.getMetadata(),
            t.getReporter().getUsername(),
            t.getCreatedAt(), t.getUpdatedAt(), t.getResolvedAt(),
            originalAttachments, comments);
    }
}
```

- [ ] **Step 7: Compile**

Run: `cd back-end && mvn compile`
Expected: clean compile.

- [ ] **Step 8: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/dto/
git commit -m "feat(ticketing): add request/response DTOs"
```

---

## Task 1.14 — `TicketController` (user endpoints)

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/TicketController.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/controller/TicketControllerTest.java`

- [ ] **Step 1: Write the controller**

```java
package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.dto.*;
import gov.nist.oscal.tools.api.entity.*;
import gov.nist.oscal.tools.api.repository.*;
import gov.nist.oscal.tools.api.service.TicketAttachmentStorageService;
import gov.nist.oscal.tools.api.service.TicketService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "User-facing ticket APIs")
public class TicketController {

    private final TicketService service;
    private final UserRepository users;
    private final TicketAttachmentRepository attachments;
    private final TicketCommentRepository comments;
    private final TicketAttachmentStorageService storage;
    private final ObjectMapper mapper = new ObjectMapper();

    public TicketController(TicketService service, UserRepository users,
                            TicketAttachmentRepository attachments,
                            TicketCommentRepository comments,
                            TicketAttachmentStorageService storage) {
        this.service = service;
        this.users = users;
        this.attachments = attachments;
        this.comments = comments;
        this.storage = storage;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketSummaryResponse> create(
            @RequestParam("type") TicketType type,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam(value = "priority", defaultValue = "MEDIUM") TicketPriority priority,
            @RequestParam(value = "metadata", required = false) String metadataJson,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            Principal principal) throws JsonProcessingException {

        User reporter = users.findByUsername(principal.getName()).orElseThrow();
        Map<String, Object> metadata = metadataJson == null ? Map.of()
            : mapper.readValue(metadataJson, new TypeReference<>() {});
        Ticket t = service.createTicket(reporter, type, title, description, priority, metadata, files);
        return ResponseEntity.status(201).body(TicketSummaryResponse.from(t));
    }

    @GetMapping("/mine")
    public Page<TicketSummaryResponse> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Principal principal) {
        User reporter = users.findByUsername(principal.getName()).orElseThrow();
        return service.listMyTickets(reporter, PageRequest.of(page, Math.min(size, 100)))
            .map(TicketSummaryResponse::from);
    }

    @GetMapping("/{id}")
    public TicketDetailResponse get(@PathVariable Long id, Principal principal) {
        User caller = users.findByUsername(principal.getName()).orElseThrow();
        boolean isAdmin = isSuperAdmin(caller);
        Ticket t = service.getTicket(id, caller, isAdmin);

        List<AttachmentResponse> origAtts = attachments
            .findByTicketAndCommentIsNull(t).stream()
            .map(AttachmentResponse::from).toList();
        List<CommentResponse> threadedComments = comments
            .findByTicketOrderByCreatedAtAsc(t).stream()
            .map(c -> CommentResponse.from(c, attachments.findByComment(c).stream()
                .map(AttachmentResponse::from).toList()))
            .toList();
        return TicketDetailResponse.from(t, origAtts, threadedComments);
    }

    @PostMapping(value = "/{id}/comments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long id,
            @RequestParam("body") String body,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            Principal principal) {
        User caller = users.findByUsername(principal.getName()).orElseThrow();
        boolean isAdmin = isSuperAdmin(caller);
        TicketComment c = service.addComment(id, caller, isAdmin, body, files);
        List<AttachmentResponse> atts = attachments.findByComment(c).stream()
            .map(AttachmentResponse::from).toList();
        return ResponseEntity.status(201).body(CommentResponse.from(c, atts));
    }

    @GetMapping("/attachments/{id}")
    public ResponseEntity<ByteArrayResource> downloadAttachment(
            @PathVariable Long id, Principal principal) throws IOException {
        TicketAttachment a = attachments.findById(id)
            .orElseThrow(() -> new java.util.NoSuchElementException("Attachment " + id));
        User caller = users.findByUsername(principal.getName()).orElseThrow();
        boolean isAdmin = isSuperAdmin(caller);
        // Reuse the same authorization rule as getTicket
        service.getTicket(a.getTicket().getId(), caller, isAdmin);

        byte[] bytes = storage.download(a.getStoragePath());
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(a.getContentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + a.getFilename() + "\"")
            .body(new ByteArrayResource(bytes));
    }

    private boolean isSuperAdmin(User u) {
        // Adjust to whatever User exposes — likely "SUPER_ADMIN".equals(u.getGlobalRole())
        // or u.getGlobalRole() == GlobalRole.SUPER_ADMIN. Mirror existing checks
        // in another controller (search: `globalRole`).
        return "SUPER_ADMIN".equals(String.valueOf(u.getGlobalRole()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Void> forbidden() { return ResponseEntity.status(403).build(); }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<Void> notFound() { return ResponseEntity.status(404).build(); }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
```

- [ ] **Step 2: Write controller tests**

```java
package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.*;
import gov.nist.oscal.tools.api.repository.*;
import gov.nist.oscal.tools.api.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired private MockMvc mvc;
    @MockBean private TicketService service;
    @MockBean private UserRepository users;
    @MockBean private TicketAttachmentRepository attachments;
    @MockBean private TicketCommentRepository comments;
    @MockBean private TicketAttachmentStorageService storage;

    private User makeUser() {
        User u = new User();
        u.setId(1L); u.setUsername("alice"); u.setEmail("alice@example.com");
        return u;
    }

    @Test
    @WithMockUser(username = "alice")
    void create_returns201() throws Exception {
        User u = makeUser();
        when(users.findByUsername("alice")).thenReturn(Optional.of(u));
        Ticket t = new Ticket(u, TicketType.BUG, "boom", "details");
        t.setId(42L);
        when(service.createTicket(eq(u), eq(TicketType.BUG), eq("boom"), eq("details"),
                                  eq(TicketPriority.MEDIUM), any(), any())).thenReturn(t);

        mvc.perform(multipart("/api/tickets")
                .file(new MockMultipartFile("files", "x.png", "image/png", new byte[10]))
                .param("type", "BUG").param("title", "boom")
                .param("description", "details").param("priority", "MEDIUM")
                .with(csrf -> { csrf.setMethod("POST"); return csrf; }))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(42));
    }

    @Test
    @WithMockUser(username = "alice")
    void get_returns200ForReporter() throws Exception {
        User u = makeUser();
        when(users.findByUsername("alice")).thenReturn(Optional.of(u));
        Ticket t = new Ticket(u, TicketType.BUG, "x", "y"); t.setId(7L);
        when(service.getTicket(eq(7L), eq(u), eq(false))).thenReturn(t);
        when(attachments.findByTicketAndCommentIsNull(t)).thenReturn(List.of());
        when(comments.findByTicketOrderByCreatedAtAsc(t)).thenReturn(List.of());

        mvc.perform(get("/api/tickets/7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.title").value("x"));
    }

    @Test
    @WithMockUser(username = "mallory")
    void get_returns403ForNonReporter() throws Exception {
        User m = new User(); m.setId(2L); m.setUsername("mallory");
        when(users.findByUsername("mallory")).thenReturn(Optional.of(m));
        when(service.getTicket(eq(7L), eq(m), eq(false)))
            .thenThrow(new AccessDeniedException("nope"));

        mvc.perform(get("/api/tickets/7")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice")
    void mine_returnsPagedSummaries() throws Exception {
        User u = makeUser();
        when(users.findByUsername("alice")).thenReturn(Optional.of(u));
        when(service.listMyTickets(eq(u), any()))
            .thenReturn(org.springframework.data.domain.Page.empty());

        mvc.perform(get("/api/tickets/mine"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
    }
}
```

(If `@WebMvcTest` clashes with this project's Spring Security wiring — a common case where the slice can't bootstrap the JwtAuthenticationFilter — instead use `@SpringBootTest @AutoConfigureMockMvc` and add `.with(jwt())` from `spring-security-test`. Check one existing controller test in this repo to confirm which style it uses, then mirror it.)

- [ ] **Step 3: Run controller tests**

Run: `cd back-end && mvn test -Dtest=TicketControllerTest`
Expected: PASS.

- [ ] **Step 4: Smoke-test in Swagger UI**

Run: `./dev.sh`. Open `http://localhost:8090/swagger-ui.html`. Authorize with a JWT (log in via `/api/auth/login` first, copy the token). Try `POST /api/tickets` with type=BUG, title, description. Confirm it returns 201 with an id. Try `GET /api/tickets/{id}`. Stop with `./stop.sh`.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/controller/TicketController.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/controller/TicketControllerTest.java
git commit -m "feat(ticketing): TicketController with user-facing ticket endpoints"
```

---

## Task 1.15 — `AdminTicketController` skeleton (status change endpoint only)

The list/search/analytics endpoints land in Phases 3 and 4 — this task only adds the status-change endpoint so admins can drive the workflow via Swagger immediately.

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AdminTicketController.java`

- [ ] **Step 1: Write the controller**

```java
package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.dto.TicketSummaryResponse;
import gov.nist.oscal.tools.api.dto.UpdateStatusRequest;
import gov.nist.oscal.tools.api.entity.Ticket;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.TicketService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/admin/tickets")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin Tickets", description = "Super-admin-only ticket APIs")
public class AdminTicketController {

    private final TicketService service;
    private final UserRepository users;

    public AdminTicketController(TicketService service, UserRepository users) {
        this.service = service;
        this.users = users;
    }

    // PATCH lives at /api/tickets/{id}/status per the spec, but Spring routing
    // here puts it under /api/admin/tickets/{id}/status to keep all admin
    // operations under one prefix. Either is fine — pick one and update the
    // frontend accordingly. This plan uses /api/admin/tickets/{id}/status.
    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketSummaryResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest req,
            Principal principal) {
        User admin = users.findByUsername(principal.getName()).orElseThrow();
        Ticket t = service.changeStatus(id, admin, req.getStatus(), req.getNote());
        return ResponseEntity.ok(TicketSummaryResponse.from(t));
    }
}
```

- [ ] **Step 2: Boot and smoke-test**

Run `./dev.sh`. With a SUPER_ADMIN JWT in Swagger, hit `PATCH /api/admin/tickets/{id}/status` with `{ "status": "IN_PROGRESS" }` against a ticket created in Task 1.14. Confirm 200 and that `GET /api/tickets/{id}` shows the new status plus a system status-change comment in the thread. Confirm an email lands at thowerton@regscale.com (or in the configured SendGrid sandbox / dev mailcatcher). Stop with `./stop.sh`.

- [ ] **Step 3: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/controller/AdminTicketController.java
git commit -m "feat(ticketing): AdminTicketController with PATCH /status endpoint"
```

---

# Phase 2 — Frontend ticket flow

User opens a ticket from the avatar menu, sees their list, opens a detail page, replies, attaches files. No admin pages yet.

## Task 2.1 — Types and API client

**Files:**
- Create: `front-end/src/types/ticket.ts`
- Create: `front-end/src/lib/api/tickets.ts`

- [ ] **Step 1: Write `ticket.ts` types**

```ts
export type TicketType = 'BUG' | 'FEATURE';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type TicketStatus =
  | 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'WONT_FIX' | 'DUPLICATE';

export interface AttachmentResponse {
  id: number;
  filename: string;
  contentType: string;
  sizeBytes: number;
}

export interface CommentResponse {
  id: number;
  authorUsername: string;
  body: string;
  statusChange: boolean;
  oldStatus: TicketStatus | null;
  newStatus: TicketStatus | null;
  createdAt: string;
  attachments: AttachmentResponse[];
}

export interface TicketSummaryResponse {
  id: number;
  type: TicketType;
  title: string;
  priority: TicketPriority;
  status: TicketStatus;
  reporterUsername: string;
  createdAt: string;
  updatedAt: string;
}

export interface TicketDetailResponse {
  id: number;
  type: TicketType;
  title: string;
  description: string;
  priority: TicketPriority;
  status: TicketStatus;
  metadata: Record<string, unknown>;
  reporterUsername: string;
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
  originalAttachments: AttachmentResponse[];
  comments: CommentResponse[];
}

export interface BugMetadata {
  stepsToReproduce?: string;
  expectedBehavior?: string;
  actualBehavior?: string;
  severity?: 'MINOR' | 'MAJOR' | 'CRITICAL';
  browser?: string;
  viewport?: string;
  url?: string;
}

export interface FeatureMetadata {
  useCase?: string;
}
```

- [ ] **Step 2: Write `lib/api/tickets.ts`**

Mirror an existing API client file in `front-end/src/lib/api/` for auth and request style. Skeleton:

```ts
import type {
  TicketDetailResponse, TicketSummaryResponse,
  TicketType, TicketPriority, TicketStatus,
} from '@/types/ticket';

const BASE = '/api/tickets';
const ADMIN_BASE = '/api/admin/tickets';

function authHeaders(): HeadersInit {
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function createTicket(form: {
  type: TicketType; title: string; description: string;
  priority: TicketPriority; metadata: Record<string, unknown>;
  files: File[];
}): Promise<TicketSummaryResponse> {
  const fd = new FormData();
  fd.set('type', form.type);
  fd.set('title', form.title);
  fd.set('description', form.description);
  fd.set('priority', form.priority);
  fd.set('metadata', JSON.stringify(form.metadata));
  form.files.forEach(f => fd.append('files', f));
  const res = await fetch(BASE, { method: 'POST', headers: authHeaders(), body: fd });
  if (!res.ok) throw new Error(`Create failed: ${res.status}`);
  return res.json();
}

export async function listMyTickets(
  page = 0, size = 25,
): Promise<{ content: TicketSummaryResponse[]; totalElements: number }> {
  const res = await fetch(`${BASE}/mine?page=${page}&size=${size}`, { headers: authHeaders() });
  if (!res.ok) throw new Error(`List failed: ${res.status}`);
  return res.json();
}

export async function getTicket(id: number): Promise<TicketDetailResponse> {
  const res = await fetch(`${BASE}/${id}`, { headers: authHeaders() });
  if (!res.ok) throw new Error(`Get failed: ${res.status}`);
  return res.json();
}

export async function addComment(
  ticketId: number, body: string, files: File[]
): Promise<void> {
  const fd = new FormData();
  fd.set('body', body);
  files.forEach(f => fd.append('files', f));
  const res = await fetch(`${BASE}/${ticketId}/comments`, {
    method: 'POST', headers: authHeaders(), body: fd,
  });
  if (!res.ok) throw new Error(`Comment failed: ${res.status}`);
}

export async function changeStatus(
  ticketId: number, status: TicketStatus, note?: string
): Promise<TicketSummaryResponse> {
  const res = await fetch(`${ADMIN_BASE}/${ticketId}/status`, {
    method: 'PATCH',
    headers: { ...authHeaders(), 'Content-Type': 'application/json' },
    body: JSON.stringify({ status, note: note ?? null }),
  });
  if (!res.ok) throw new Error(`Status change failed: ${res.status}`);
  return res.json();
}

export function attachmentDownloadUrl(attachmentId: number): string {
  return `${BASE}/attachments/${attachmentId}`;
}
```

- [ ] **Step 3: Type-check**

Run: `cd front-end && npx tsc --noEmit`
Expected: clean.

- [ ] **Step 4: Commit**

```bash
git add front-end/src/types/ticket.ts front-end/src/lib/api/tickets.ts
git commit -m "feat(ticketing): frontend ticket types and API client"
```

---

## Task 2.2 — Badge components

**Files:**
- Create: `front-end/src/components/tickets/TicketStatusBadge.tsx`
- Create: `front-end/src/components/tickets/TicketTypeBadge.tsx`
- Create: `front-end/src/components/tickets/TicketPriorityBadge.tsx`

- [ ] **Step 1: Inspect existing badge style**

Run: `grep -rn "Badge\|badge" front-end/src/components/ui/ | head -5`. If a `Badge` component exists, reuse it. If not, the components below render as small styled `<span>`s.

- [ ] **Step 2: `TicketStatusBadge.tsx`**

```tsx
'use client';
import type { TicketStatus } from '@/types/ticket';

const STATUS_LABEL: Record<TicketStatus, string> = {
  OPEN: 'Open', IN_PROGRESS: 'In Progress', RESOLVED: 'Resolved',
  CLOSED: 'Closed', WONT_FIX: "Won't Fix", DUPLICATE: 'Duplicate',
};
const STATUS_COLOR: Record<TicketStatus, string> = {
  OPEN: 'bg-blue-100 text-blue-800',
  IN_PROGRESS: 'bg-amber-100 text-amber-800',
  RESOLVED: 'bg-green-100 text-green-800',
  CLOSED: 'bg-gray-200 text-gray-800',
  WONT_FIX: 'bg-rose-100 text-rose-800',
  DUPLICATE: 'bg-purple-100 text-purple-800',
};

export function TicketStatusBadge({ status }: { status: TicketStatus }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_COLOR[status]}`}>
      {STATUS_LABEL[status]}
    </span>
  );
}
```

- [ ] **Step 3: `TicketTypeBadge.tsx` and `TicketPriorityBadge.tsx`**

Same pattern; types and priorities mapped to short labels and colors:
- Type: `BUG` → "Bug" red, `FEATURE` → "Feature" blue.
- Priority: `LOW`/`MEDIUM`/`HIGH`/`CRITICAL` → green/yellow/orange/red.

- [ ] **Step 4: Render-smoke test**

Add a Jest+RTL test:

```tsx
import { render, screen } from '@testing-library/react';
import { TicketStatusBadge } from './TicketStatusBadge';

test('renders status label', () => {
  render(<TicketStatusBadge status="IN_PROGRESS" />);
  expect(screen.getByText('In Progress')).toBeInTheDocument();
});
```

- [ ] **Step 5: Run test**

Run: `cd front-end && npm test -- TicketStatusBadge`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add front-end/src/components/tickets/
git commit -m "feat(ticketing): badge components for status, type, priority"
```

---

## Task 2.3 — `UserAvatarMenu` update

**Files:**
- Modify: `front-end/src/components/UserAvatarMenu.tsx`

- [ ] **Step 1: Add the two menu items**

Edit `front-end/src/components/UserAvatarMenu.tsx`. Import `Bug` and `Inbox` icons from `lucide-react`. Insert these `<Link>`s into the menu after the "Manage Profile" link and before the "Org Admin Panel" / "Admin Dashboard" conditionals (around line 65):

```tsx
<Link
  href="/tickets/new"
  onClick={() => setOpen(false)}
  className="flex items-center gap-2 rounded-md px-3 py-2 text-sm transition-colors hover:bg-accent hover:text-accent-foreground"
>
  <Bug className="h-4 w-4" />
  Open Ticket
</Link>
<Link
  href="/tickets"
  onClick={() => setOpen(false)}
  className="flex items-center gap-2 rounded-md px-3 py-2 text-sm transition-colors hover:bg-accent hover:text-accent-foreground"
>
  <Inbox className="h-4 w-4" />
  My Tickets
</Link>
```

- [ ] **Step 2: Update import line**

Change:
```tsx
import { Cog, LogOut, Settings, UserCog } from 'lucide-react';
```
to:
```tsx
import { Bug, Cog, Inbox, LogOut, Settings, UserCog } from 'lucide-react';
```

- [ ] **Step 3: Visual verify**

Run: `./dev.sh`. Log in. Click the avatar. Confirm "Open Ticket" and "My Tickets" appear between Manage Profile and Admin Dashboard. Click "Open Ticket" — should 404 for now (page lands in next task).

- [ ] **Step 4: Commit**

```bash
git add front-end/src/components/UserAvatarMenu.tsx
git commit -m "feat(ticketing): add Open Ticket and My Tickets to avatar menu"
```

---

## Task 2.4 — `/tickets/new` page

**Files:**
- Create: `front-end/src/app/tickets/new/page.tsx`

- [ ] **Step 1: Write the page**

```tsx
'use client';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { createTicket } from '@/lib/api/tickets';
import type { TicketType, TicketPriority, BugMetadata, FeatureMetadata } from '@/types/ticket';

export default function NewTicketPage() {
  const router = useRouter();
  const [type, setType] = useState<TicketType>('BUG');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<TicketPriority>('MEDIUM');
  const [bug, setBug] = useState<BugMetadata>({ severity: 'MAJOR' });
  const [feature, setFeature] = useState<FeatureMetadata>({});
  const [files, setFiles] = useState<File[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function captureEnv(): BugMetadata {
    if (typeof window === 'undefined') return {};
    return {
      browser: navigator.userAgent,
      viewport: `${window.innerWidth}x${window.innerHeight}`,
      url: window.location.pathname,
    };
  }

  function onFilesChange(e: React.ChangeEvent<HTMLInputElement>) {
    const list = Array.from(e.target.files ?? []);
    if (list.length > 5) { setError('Max 5 files'); return; }
    if (list.some(f => f.size > 10 * 1024 * 1024)) { setError('Max 10 MB per file'); return; }
    setError(null);
    setFiles(list);
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true); setError(null);
    try {
      const metadata = type === 'BUG'
        ? { ...bug, ...captureEnv() }
        : feature;
      const created = await createTicket({ type, title, description, priority, metadata, files });
      router.push(`/tickets/${created.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-2xl p-6">
      <h1 className="text-2xl font-semibold mb-6">Open a Ticket</h1>
      <form onSubmit={onSubmit} className="space-y-5">
        <fieldset className="flex gap-6" aria-label="Ticket type">
          <label className="flex items-center gap-2">
            <input type="radio" checked={type === 'BUG'} onChange={() => setType('BUG')} /> Bug Report
          </label>
          <label className="flex items-center gap-2">
            <input type="radio" checked={type === 'FEATURE'} onChange={() => setType('FEATURE')} /> Feature Request
          </label>
        </fieldset>

        <label className="block">
          <span className="text-sm font-medium">Title</span>
          <input className="mt-1 w-full rounded border px-3 py-2"
                 maxLength={200} required value={title}
                 onChange={e => setTitle(e.target.value)} />
        </label>

        <label className="block">
          <span className="text-sm font-medium">Description</span>
          <textarea className="mt-1 w-full rounded border px-3 py-2 min-h-[120px]"
                    required value={description}
                    placeholder={type === 'BUG'
                      ? 'What you saw, briefly. Use the structured fields below for steps and expected behavior.'
                      : 'What problem this solves and who it helps.'}
                    onChange={e => setDescription(e.target.value)} />
        </label>

        <label className="block">
          <span className="text-sm font-medium">Priority</span>
          <select className="mt-1 w-full rounded border px-3 py-2"
                  value={priority} onChange={e => setPriority(e.target.value as TicketPriority)}>
            {(['LOW','MEDIUM','HIGH','CRITICAL'] as TicketPriority[]).map(p =>
              <option key={p} value={p}>{p}</option>)}
          </select>
        </label>

        {type === 'BUG' && (
          <div className="space-y-3">
            <label className="block">
              <span className="text-sm font-medium">Steps to Reproduce</span>
              <textarea className="mt-1 w-full rounded border px-3 py-2"
                        value={bug.stepsToReproduce ?? ''}
                        onChange={e => setBug({ ...bug, stepsToReproduce: e.target.value })} />
            </label>
            <label className="block">
              <span className="text-sm font-medium">Expected Behavior</span>
              <textarea className="mt-1 w-full rounded border px-3 py-2"
                        value={bug.expectedBehavior ?? ''}
                        onChange={e => setBug({ ...bug, expectedBehavior: e.target.value })} />
            </label>
            <label className="block">
              <span className="text-sm font-medium">Actual Behavior</span>
              <textarea className="mt-1 w-full rounded border px-3 py-2"
                        value={bug.actualBehavior ?? ''}
                        onChange={e => setBug({ ...bug, actualBehavior: e.target.value })} />
            </label>
            <label className="block">
              <span className="text-sm font-medium">Severity</span>
              <select className="mt-1 w-full rounded border px-3 py-2"
                      value={bug.severity ?? 'MAJOR'}
                      onChange={e => setBug({ ...bug, severity: e.target.value as BugMetadata['severity'] })}>
                {['MINOR','MAJOR','CRITICAL'].map(s => <option key={s}>{s}</option>)}
              </select>
            </label>
          </div>
        )}

        {type === 'FEATURE' && (
          <label className="block">
            <span className="text-sm font-medium">Use Case</span>
            <textarea className="mt-1 w-full rounded border px-3 py-2 min-h-[100px]"
                      placeholder="Why does this matter? What problem does it solve?"
                      value={feature.useCase ?? ''}
                      onChange={e => setFeature({ ...feature, useCase: e.target.value })} />
          </label>
        )}

        <label className="block">
          <span className="text-sm font-medium">Attachments (max 5 files, 10 MB each)</span>
          <input type="file" multiple onChange={onFilesChange}
                 accept=".png,.jpg,.jpeg,.gif,.pdf,.txt,.log,.json,.xml,.yaml,.yml" />
        </label>

        {error && <p className="text-sm text-rose-600">{error}</p>}

        <button type="submit" disabled={submitting || !title || !description}
                className="rounded bg-blue-600 px-4 py-2 text-white disabled:opacity-50">
          {submitting ? 'Submitting…' : 'Submit Ticket'}
        </button>
      </form>
    </div>
  );
}
```

- [ ] **Step 2: Smoke test in browser**

`./dev.sh`. Click avatar → Open Ticket. Fill out a Bug. Submit. Confirm browser navigates to `/tickets/{id}` (the page may 404 for now — that's expected; page lands in 2.6). Confirm via `GET /api/tickets/mine` that the ticket exists.

- [ ] **Step 3: Commit**

```bash
git add front-end/src/app/tickets/new/page.tsx
git commit -m "feat(ticketing): /tickets/new page with bug/feature forms"
```

---

## Task 2.5 — `/tickets` (My Tickets) list page

**Files:**
- Create: `front-end/src/app/tickets/page.tsx`

- [ ] **Step 1: Write the page**

```tsx
'use client';
import Link from 'next/link';
import { useEffect, useState } from 'react';
import { listMyTickets } from '@/lib/api/tickets';
import type { TicketSummaryResponse } from '@/types/ticket';
import { TicketStatusBadge } from '@/components/tickets/TicketStatusBadge';
import { TicketTypeBadge } from '@/components/tickets/TicketTypeBadge';
import { TicketPriorityBadge } from '@/components/tickets/TicketPriorityBadge';

export default function MyTicketsPage() {
  const [items, setItems] = useState<TicketSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    listMyTickets(0, 50).then(p => { setItems(p.content); setLoading(false); });
  }, []);

  if (loading) return <div className="p-6">Loading…</div>;

  if (items.length === 0) {
    return (
      <div className="mx-auto max-w-3xl p-6 text-center">
        <h1 className="text-2xl font-semibold mb-3">My Tickets</h1>
        <p className="text-muted-foreground mb-6">You haven't opened any tickets yet.</p>
        <Link href="/tickets/new" className="rounded bg-blue-600 px-4 py-2 text-white">
          Open Your First Ticket
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl p-6">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold">My Tickets</h1>
        <Link href="/tickets/new" className="rounded bg-blue-600 px-4 py-2 text-white text-sm">
          Open New Ticket
        </Link>
      </div>
      <ul className="divide-y rounded border">
        {items.map(t => (
          <li key={t.id} className="hover:bg-accent">
            <Link href={`/tickets/${t.id}`} className="flex items-center gap-3 p-4">
              <span className="text-xs text-muted-foreground tabular-nums">TKT-{t.id}</span>
              <TicketTypeBadge type={t.type} />
              <span className="flex-1 truncate font-medium">{t.title}</span>
              <TicketPriorityBadge priority={t.priority} />
              <TicketStatusBadge status={t.status} />
              <span className="text-xs text-muted-foreground">
                {new Date(t.updatedAt).toLocaleDateString()}
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
```

- [ ] **Step 2: Smoke test**

`./dev.sh`. Avatar → My Tickets. Confirm the ticket from 2.4 shows up. Click a row — 404 is OK (next task).

- [ ] **Step 3: Commit**

```bash
git add front-end/src/app/tickets/page.tsx
git commit -m "feat(ticketing): /tickets My Tickets list page"
```

---

## Task 2.6 — `/tickets/[id]` detail page

**Files:**
- Create: `front-end/src/app/tickets/[id]/page.tsx`

- [ ] **Step 1: Write the page**

```tsx
'use client';
import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'next/navigation';
import {
  getTicket, addComment, attachmentDownloadUrl,
} from '@/lib/api/tickets';
import type { TicketDetailResponse } from '@/types/ticket';
import { TicketStatusBadge } from '@/components/tickets/TicketStatusBadge';
import { TicketTypeBadge } from '@/components/tickets/TicketTypeBadge';
import { TicketPriorityBadge } from '@/components/tickets/TicketPriorityBadge';
import { useAuth } from '@/contexts/AuthContext';

export default function TicketDetailPage() {
  const { id } = useParams<{ id: string }>();
  const ticketId = Number(id);
  const { user } = useAuth();
  const [t, setT] = useState<TicketDetailResponse | null>(null);
  const [body, setBody] = useState('');
  const [files, setFiles] = useState<File[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(() => getTicket(ticketId).then(setT), [ticketId]);

  useEffect(() => { reload(); }, [reload]);

  if (!t) return <div className="p-6">Loading…</div>;

  const isAdmin = user?.globalRole === 'SUPER_ADMIN';
  const isReporter = user?.username === t.reporterUsername;
  const reopenHint = isReporter && t.status === 'RESOLVED';

  async function submitReply(e: React.FormEvent) {
    e.preventDefault(); setBusy(true); setError(null);
    try { await addComment(ticketId, body, files); setBody(''); setFiles([]); await reload(); }
    catch (err) { setError(err instanceof Error ? err.message : String(err)); }
    finally { setBusy(false); }
  }

  return (
    <div className="mx-auto max-w-3xl p-6 space-y-6">
      <header className="space-y-2">
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <span>TKT-{t.id}</span>
          <TicketTypeBadge type={t.type} />
          <TicketPriorityBadge priority={t.priority} />
          <TicketStatusBadge status={t.status} />
        </div>
        <h1 className="text-2xl font-semibold">{t.title}</h1>
        <div className="text-xs text-muted-foreground">
          Opened {new Date(t.createdAt).toLocaleString()} · last updated {new Date(t.updatedAt).toLocaleString()}
        </div>
      </header>

      <section className="rounded border p-4 whitespace-pre-wrap">
        {t.description}
        {Object.keys(t.metadata).length > 0 && (
          <dl className="mt-4 text-sm">
            {Object.entries(t.metadata).map(([k, v]) => (
              <div key={k} className="flex gap-2"><dt className="font-medium">{k}:</dt><dd>{String(v)}</dd></div>
            ))}
          </dl>
        )}
        {t.originalAttachments.length > 0 && (
          <ul className="mt-3 text-sm">
            {t.originalAttachments.map(a => (
              <li key={a.id}>
                <a href={attachmentDownloadUrl(a.id)} className="text-blue-600 underline">{a.filename}</a>
                <span className="text-muted-foreground"> ({Math.round(a.sizeBytes/1024)} KB)</span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="space-y-3">
        {t.comments.map(c => c.statusChange ? (
          <div key={c.id} className="text-xs italic text-muted-foreground">
            {c.body} · {new Date(c.createdAt).toLocaleString()}
          </div>
        ) : (
          <article key={c.id} className="rounded border p-3">
            <header className="flex items-center justify-between text-xs text-muted-foreground mb-2">
              <span className="font-medium text-foreground">{c.authorUsername}</span>
              <span>{new Date(c.createdAt).toLocaleString()}</span>
            </header>
            <p className="whitespace-pre-wrap">{c.body}</p>
            {c.attachments.length > 0 && (
              <ul className="mt-2 text-sm">
                {c.attachments.map(a => (
                  <li key={a.id}>
                    <a href={attachmentDownloadUrl(a.id)} className="text-blue-600 underline">{a.filename}</a>
                  </li>
                ))}
              </ul>
            )}
          </article>
        ))}
      </section>

      <form onSubmit={submitReply} className="space-y-3">
        {reopenHint && (
          <p className="text-xs text-amber-700">Posting a reply will reopen this ticket.</p>
        )}
        <textarea className="w-full rounded border px-3 py-2 min-h-[100px]"
                  placeholder="Write a reply…" required
                  value={body} onChange={e => setBody(e.target.value)} />
        <input type="file" multiple
               accept=".png,.jpg,.jpeg,.gif,.pdf,.txt,.log,.json,.xml,.yaml,.yml"
               onChange={e => setFiles(Array.from(e.target.files ?? []))} />
        {error && <p className="text-sm text-rose-600">{error}</p>}
        <button type="submit" disabled={busy || !body}
                className="rounded bg-blue-600 px-4 py-2 text-white disabled:opacity-50">
          {busy ? 'Sending…' : 'Reply'}
        </button>
      </form>
    </div>
  );
}
```

- [ ] **Step 2: Smoke test the full user flow**

`./dev.sh`. Avatar → Open Ticket. Submit a bug. You land on `/tickets/{id}`. Reply with a comment. Confirm it appears in the thread. Confirm an email lands at thowerton@regscale.com (or in the configured dev mail catcher).

- [ ] **Step 3: Commit**

```bash
git add front-end/src/app/tickets/[id]/page.tsx
git commit -m "feat(ticketing): /tickets/[id] detail page with thread and reply composer"
```

---

## Task 2.7 — Admin status dropdown on detail page

**Files:**
- Modify: `front-end/src/app/tickets/[id]/page.tsx`
- Create: `front-end/src/components/tickets/StatusChangeControl.tsx`

- [ ] **Step 1: Write `StatusChangeControl.tsx`**

```tsx
'use client';
import { useState } from 'react';
import type { TicketStatus } from '@/types/ticket';
import { changeStatus } from '@/lib/api/tickets';

const ALL_STATUSES: TicketStatus[] = [
  'OPEN','IN_PROGRESS','RESOLVED','CLOSED','WONT_FIX','DUPLICATE',
];
const TERMINAL: TicketStatus[] = ['CLOSED','WONT_FIX','DUPLICATE'];

export function StatusChangeControl({
  ticketId, current, onChange,
}: { ticketId: number; current: TicketStatus; onChange: () => void }) {
  const [next, setNext] = useState<TicketStatus>(current);
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit() {
    if (next === current) return;
    if (TERMINAL.includes(next) &&
        !confirm(`Set status to ${next}? This is terminal and the user will not be able to reopen.`)) {
      return;
    }
    setBusy(true);
    try { await changeStatus(ticketId, next, note || undefined); setNote(''); onChange(); }
    finally { setBusy(false); }
  }

  return (
    <div className="flex flex-wrap items-center gap-2 rounded border p-3 bg-muted/30">
      <label className="text-sm font-medium">Set status:</label>
      <select className="rounded border px-2 py-1 text-sm"
              value={next} onChange={e => setNext(e.target.value as TicketStatus)}>
        {ALL_STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
      </select>
      <input className="flex-1 rounded border px-2 py-1 text-sm"
             placeholder="Optional note (becomes a comment)…"
             value={note} onChange={e => setNote(e.target.value)} />
      <button onClick={submit} disabled={busy || next === current}
              className="rounded bg-blue-600 px-3 py-1 text-sm text-white disabled:opacity-50">
        Apply
      </button>
    </div>
  );
}
```

- [ ] **Step 2: Wire it into the detail page**

In `front-end/src/app/tickets/[id]/page.tsx`, import and render the control above the comment thread, only when `isAdmin`:

```tsx
import { StatusChangeControl } from '@/components/tickets/StatusChangeControl';
// ...
{isAdmin && (
  <StatusChangeControl ticketId={t.id} current={t.status} onChange={reload} />
)}
```

- [ ] **Step 3: Smoke test as admin**

`./dev.sh`. Log in as a SUPER_ADMIN user. Open a ticket. Move it through OPEN → IN_PROGRESS → RESOLVED. Confirm system status-change comments appear in the thread; confirm reporter email arrives.

Then log in as the reporter. Comment on the RESOLVED ticket. Confirm the ticket flips to OPEN, an additional system comment appears, and an email lands for the admin.

- [ ] **Step 4: Commit**

```bash
git add front-end/src/components/tickets/StatusChangeControl.tsx \
        front-end/src/app/tickets/[id]/page.tsx
git commit -m "feat(ticketing): admin status-change control on detail page"
```

---

# Phase 3 — Admin tickets panel

## Task 3.1 — `AdminTicketController.list` with search + filter + pagination

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AdminTicketController.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/TicketSpecifications.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/repository/TicketSpecificationsTest.java`

- [ ] **Step 1: Write `TicketSpecifications.java`**

```java
package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class TicketSpecifications {
    private TicketSpecifications() {}

    public static Specification<Ticket> matches(
            String q, List<TicketStatus> statuses, TicketType type,
            List<TicketPriority> priorities, LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                ps.add(cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("description")), like)
                ));
            }
            if (statuses != null && !statuses.isEmpty()) {
                ps.add(root.get("status").in(statuses));
            }
            if (type != null) ps.add(cb.equal(root.get("type"), type));
            if (priorities != null && !priorities.isEmpty()) {
                ps.add(root.get("priority").in(priorities));
            }
            if (from != null) ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) ps.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            return cb.and(ps.toArray(Predicate[]::new));
        };
    }
}
```

- [ ] **Step 2: Add the GET handler to `AdminTicketController`**

```java
@GetMapping
public Page<TicketSummaryResponse> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) List<TicketStatus> status,
        @RequestParam(required = false) TicketType type,
        @RequestParam(required = false) List<TicketPriority> priority,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime to,
        @RequestParam(defaultValue = "updatedAt,desc") String sort) {

    String[] sp = sort.split(",");
    Sort.Direction dir = sp.length > 1 && sp[1].equalsIgnoreCase("asc")
        ? Sort.Direction.ASC : Sort.Direction.DESC;
    PageRequest pr = PageRequest.of(page, Math.min(size, 100), dir, sp[0]);

    return tickets.findAll(
        TicketSpecifications.matches(q, status, type, priority, from, to), pr)
        .map(TicketSummaryResponse::from);
}
```

(Inject `TicketRepository tickets` into `AdminTicketController` if not already.)

- [ ] **Step 3: Write a small unit test for the spec builder**

(`@DataJpaTest` style, persists a few tickets, runs the spec, asserts the right ones come back.)

- [ ] **Step 4: Run tests**

Run: `cd back-end && mvn test -Dtest=TicketSpecificationsTest`

- [ ] **Step 5: Smoke test in Swagger**

`./dev.sh`. As SUPER_ADMIN, hit `GET /api/admin/tickets?status=OPEN&type=BUG`. Confirm filtered results.

- [ ] **Step 6: Commit**

```bash
git add -A back-end/src/main/java/gov/nist/oscal/tools/api/repository/TicketSpecifications.java \
           back-end/src/main/java/gov/nist/oscal/tools/api/controller/AdminTicketController.java \
           back-end/src/test/java/gov/nist/oscal/tools/api/repository/TicketSpecificationsTest.java
git commit -m "feat(ticketing): admin list endpoint with search, filter, pagination"
```

---

## Task 3.2 — `/admin/tickets` page

**Files:**
- Create: `front-end/src/app/admin/tickets/page.tsx`
- Modify: `front-end/src/lib/api/tickets.ts` (add `listAdminTickets`)

- [ ] **Step 1: Add `listAdminTickets` to the API client**

```ts
export interface AdminListParams {
  page?: number; size?: number;
  q?: string;
  status?: TicketStatus[];
  type?: TicketType;
  priority?: TicketPriority[];
  from?: string; to?: string;
}

export async function listAdminTickets(p: AdminListParams = {})
  : Promise<{ content: TicketSummaryResponse[]; totalElements: number; totalPages: number }> {
  const qs = new URLSearchParams();
  qs.set('page', String(p.page ?? 0));
  qs.set('size', String(p.size ?? 25));
  if (p.q) qs.set('q', p.q);
  if (p.type) qs.set('type', p.type);
  (p.status ?? []).forEach(s => qs.append('status', s));
  (p.priority ?? []).forEach(pr => qs.append('priority', pr));
  if (p.from) qs.set('from', p.from);
  if (p.to) qs.set('to', p.to);
  const res = await fetch(`${ADMIN_BASE}?${qs}`, { headers: authHeaders() });
  if (!res.ok) throw new Error(`Admin list failed: ${res.status}`);
  return res.json();
}
```

- [ ] **Step 2: Write `/admin/tickets` page**

Mirror the `/admin/users/page.tsx` patterns for the role gate, search input, filter dropdowns, and table layout. Skeleton:

```tsx
'use client';
import Link from 'next/link';
import { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { listAdminTickets } from '@/lib/api/tickets';
import type { TicketSummaryResponse, TicketStatus, TicketType, TicketPriority } from '@/types/ticket';
import { TicketStatusBadge } from '@/components/tickets/TicketStatusBadge';
import { TicketTypeBadge } from '@/components/tickets/TicketTypeBadge';
import { TicketPriorityBadge } from '@/components/tickets/TicketPriorityBadge';

const ALL_STATUSES: TicketStatus[] = ['OPEN','IN_PROGRESS','RESOLVED','CLOSED','WONT_FIX','DUPLICATE'];
const ALL_PRIORITIES: TicketPriority[] = ['LOW','MEDIUM','HIGH','CRITICAL'];

export default function AdminTicketsPage() {
  const router = useRouter();
  const params = useSearchParams();
  const { user } = useAuth();

  // Role gate
  useEffect(() => {
    if (user && user.globalRole !== 'SUPER_ADMIN') router.replace('/');
  }, [user, router]);

  const [items, setItems] = useState<TicketSummaryResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [q, setQ] = useState(params.get('q') ?? '');
  const [type, setType] = useState<TicketType | ''>((params.get('type') as TicketType) || '');
  const [statuses, setStatuses] = useState<TicketStatus[]>(params.getAll('status') as TicketStatus[]);
  const [priorities, setPriorities] = useState<TicketPriority[]>(params.getAll('priority') as TicketPriority[]);
  const [page, setPage] = useState(Number(params.get('page') ?? 0));

  useEffect(() => {
    setLoading(true);
    listAdminTickets({
      page, size: 25, q: q || undefined,
      type: type || undefined,
      status: statuses.length ? statuses : undefined,
      priority: priorities.length ? priorities : undefined,
    }).then(r => { setItems(r.content); setTotal(r.totalElements); setLoading(false); });
  }, [q, type, statuses, priorities, page]);

  function toggle<T>(arr: T[], v: T): T[] {
    return arr.includes(v) ? arr.filter(x => x !== v) : [...arr, v];
  }

  return (
    <div className="mx-auto max-w-6xl p-6">
      <h1 className="text-2xl font-semibold mb-6">Admin Tickets</h1>

      <div className="mb-4 flex flex-wrap gap-3 items-center">
        <input
          className="rounded border px-3 py-2 text-sm flex-1 min-w-[240px]"
          placeholder="Search title or description…"
          value={q} onChange={e => { setPage(0); setQ(e.target.value); }} />
        <select className="rounded border px-2 py-2 text-sm"
                value={type} onChange={e => { setPage(0); setType(e.target.value as TicketType | ''); }}>
          <option value="">All types</option>
          <option value="BUG">Bug</option>
          <option value="FEATURE">Feature</option>
        </select>
        <details className="relative">
          <summary className="rounded border px-3 py-2 text-sm cursor-pointer">
            Status ({statuses.length || 'any'})
          </summary>
          <div className="absolute z-10 mt-1 rounded border bg-popover p-2 shadow">
            {ALL_STATUSES.map(s => (
              <label key={s} className="flex items-center gap-2 py-1 text-sm">
                <input type="checkbox" checked={statuses.includes(s)}
                       onChange={() => { setPage(0); setStatuses(toggle(statuses, s)); }} />
                {s}
              </label>
            ))}
          </div>
        </details>
        <details className="relative">
          <summary className="rounded border px-3 py-2 text-sm cursor-pointer">
            Priority ({priorities.length || 'any'})
          </summary>
          <div className="absolute z-10 mt-1 rounded border bg-popover p-2 shadow">
            {ALL_PRIORITIES.map(p => (
              <label key={p} className="flex items-center gap-2 py-1 text-sm">
                <input type="checkbox" checked={priorities.includes(p)}
                       onChange={() => { setPage(0); setPriorities(toggle(priorities, p)); }} />
                {p}
              </label>
            ))}
          </div>
        </details>
      </div>

      {loading ? <div>Loading…</div> : (
        <>
          <table className="w-full text-sm border">
            <thead className="bg-muted">
              <tr>
                <th className="text-left px-3 py-2">ID</th>
                <th className="text-left px-3 py-2">Type</th>
                <th className="text-left px-3 py-2">Title</th>
                <th className="text-left px-3 py-2">Status</th>
                <th className="text-left px-3 py-2">Priority</th>
                <th className="text-left px-3 py-2">Reporter</th>
                <th className="text-left px-3 py-2">Updated</th>
              </tr>
            </thead>
            <tbody>
              {items.map(t => (
                <tr key={t.id} className="border-t hover:bg-accent">
                  <td className="px-3 py-2 tabular-nums text-muted-foreground">TKT-{t.id}</td>
                  <td className="px-3 py-2"><TicketTypeBadge type={t.type} /></td>
                  <td className="px-3 py-2">
                    <Link href={`/tickets/${t.id}`} className="text-blue-600 hover:underline">{t.title}</Link>
                  </td>
                  <td className="px-3 py-2"><TicketStatusBadge status={t.status} /></td>
                  <td className="px-3 py-2"><TicketPriorityBadge priority={t.priority} /></td>
                  <td className="px-3 py-2">{t.reporterUsername}</td>
                  <td className="px-3 py-2 text-muted-foreground">{new Date(t.updatedAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="mt-3 flex items-center justify-between text-sm">
            <span>{total} total</span>
            <div className="flex gap-2">
              <button disabled={page === 0} onClick={() => setPage(page - 1)}
                      className="rounded border px-3 py-1 disabled:opacity-50">Prev</button>
              <span className="px-2 py-1">Page {page + 1}</span>
              <button disabled={(page + 1) * 25 >= total} onClick={() => setPage(page + 1)}
                      className="rounded border px-3 py-1 disabled:opacity-50">Next</button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
```

(Fill in the toolbar and table rendering by mirroring `/admin/users/page.tsx`. Each row's title cell wraps in `<Link href={`/tickets/${t.id}`}>`.)

- [ ] **Step 3: Smoke test as admin**

Open `/admin/tickets`. Confirm the list of all tickets appears. Type in the search box; confirm filtering works. Toggle status filters; confirm filtering works. Click a row → ticket detail page opens with the admin's status-change control (from Task 2.7).

- [ ] **Step 4: Commit**

```bash
git add front-end/src/app/admin/tickets/page.tsx front-end/src/lib/api/tickets.ts
git commit -m "feat(ticketing): /admin/tickets page with search, filter, pagination"
```

---

# Phase 4 — Analytics + auto-close job

## Task 4.1 — `AdminTicketController.analytics`

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AdminTicketController.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/dto/TicketAnalyticsResponse.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/TicketRepository.java` (analytics queries)
- Test: append to `TicketRepositoryTest`

- [ ] **Step 1: Write `TicketAnalyticsResponse`**

```java
package gov.nist.oscal.tools.api.dto;

import gov.nist.oscal.tools.api.entity.TicketPriority;
import gov.nist.oscal.tools.api.entity.TicketStatus;
import gov.nist.oscal.tools.api.entity.TicketType;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record TicketAnalyticsResponse(
    Map<TicketStatus, Long> statusCounts,
    Map<TicketType, Long> typeSplit,
    List<WeekBucket> openedPerWeek,
    List<WeekBucket> resolvedPerWeek,
    List<StaleTicket> staleTickets) {

    public record WeekBucket(LocalDate week, long count) {}
    public record StaleTicket(Long id, TicketType type, String title,
                              TicketPriority priority, java.time.LocalDateTime createdAt,
                              long ageDays) {}
}
```

- [ ] **Step 2: Add native queries to `TicketRepository`**

```java
@Query(value = "SELECT status, COUNT(*) FROM tickets GROUP BY status", nativeQuery = true)
List<Object[]> countByStatus();

@Query(value = "SELECT type, COUNT(*) FROM tickets GROUP BY type", nativeQuery = true)
List<Object[]> countByType();

@Query(value =
    "SELECT date_trunc('week', created_at)::date AS w, COUNT(*) " +
    "FROM tickets WHERE created_at > now() - interval '12 weeks' " +
    "GROUP BY 1 ORDER BY 1", nativeQuery = true)
List<Object[]> openedPerWeek();

@Query(value =
    "SELECT date_trunc('week', resolved_at)::date AS w, COUNT(*) " +
    "FROM tickets WHERE resolved_at IS NOT NULL AND resolved_at > now() - interval '12 weeks' " +
    "GROUP BY 1 ORDER BY 1", nativeQuery = true)
List<Object[]> resolvedPerWeek();

@Query(value =
    "SELECT * FROM tickets WHERE status IN ('OPEN','IN_PROGRESS') " +
    "AND created_at < now() - interval '30 days' " +
    "ORDER BY created_at ASC LIMIT 20", nativeQuery = true)
List<Ticket> staleTickets();
```

- [ ] **Step 3: Add the `/analytics` handler to `AdminTicketController`**

```java
@GetMapping("/analytics")
public TicketAnalyticsResponse analytics() {
    Map<TicketStatus, Long> statusCounts = new EnumMap<>(TicketStatus.class);
    for (TicketStatus s : TicketStatus.values()) statusCounts.put(s, 0L);
    for (Object[] row : tickets.countByStatus()) {
        statusCounts.put(TicketStatus.valueOf((String) row[0]), ((Number) row[1]).longValue());
    }
    Map<TicketType, Long> typeSplit = new EnumMap<>(TicketType.class);
    for (TicketType t : TicketType.values()) typeSplit.put(t, 0L);
    for (Object[] row : tickets.countByType()) {
        typeSplit.put(TicketType.valueOf((String) row[0]), ((Number) row[1]).longValue());
    }
    List<TicketAnalyticsResponse.WeekBucket> opened = tickets.openedPerWeek().stream()
        .map(r -> new TicketAnalyticsResponse.WeekBucket(
            ((java.sql.Date) r[0]).toLocalDate(), ((Number) r[1]).longValue())).toList();
    List<TicketAnalyticsResponse.WeekBucket> resolved = tickets.resolvedPerWeek().stream()
        .map(r -> new TicketAnalyticsResponse.WeekBucket(
            ((java.sql.Date) r[0]).toLocalDate(), ((Number) r[1]).longValue())).toList();
    List<TicketAnalyticsResponse.StaleTicket> stale = tickets.staleTickets().stream()
        .map(t -> new TicketAnalyticsResponse.StaleTicket(
            t.getId(), t.getType(), t.getTitle(), t.getPriority(),
            t.getCreatedAt(),
            java.time.Duration.between(t.getCreatedAt(), java.time.LocalDateTime.now()).toDays()))
        .toList();

    return new TicketAnalyticsResponse(statusCounts, typeSplit, opened, resolved, stale);
}
```

- [ ] **Step 4: Smoke test in Swagger**

`./dev.sh`. As SUPER_ADMIN, `GET /api/admin/tickets/analytics`. Confirm a JSON object with the five keys comes back, with counts that match the tickets you've created during testing.

- [ ] **Step 5: Commit**

```bash
git add -A back-end/src/main/java/gov/nist/oscal/tools/api/dto/TicketAnalyticsResponse.java \
           back-end/src/main/java/gov/nist/oscal/tools/api/repository/TicketRepository.java \
           back-end/src/main/java/gov/nist/oscal/tools/api/controller/AdminTicketController.java
git commit -m "feat(ticketing): admin analytics endpoint with 5 metrics"
```

---

## Task 4.2 — Analytics panel on `/admin/tickets`

**Files:**
- Modify: `front-end/src/lib/api/tickets.ts`
- Create: `front-end/src/components/tickets/AnalyticsPanel.tsx`
- Modify: `front-end/src/app/admin/tickets/page.tsx`

- [ ] **Step 1: Add `getAnalytics` to API client**

```ts
export interface TicketAnalytics {
  statusCounts: Record<TicketStatus, number>;
  typeSplit: Record<TicketType, number>;
  openedPerWeek: { week: string; count: number }[];
  resolvedPerWeek: { week: string; count: number }[];
  staleTickets: {
    id: number; type: TicketType; title: string;
    priority: TicketPriority; createdAt: string; ageDays: number;
  }[];
}

export async function getTicketAnalytics(): Promise<TicketAnalytics> {
  const res = await fetch(`${ADMIN_BASE}/analytics`, { headers: authHeaders() });
  if (!res.ok) throw new Error(`Analytics failed: ${res.status}`);
  return res.json();
}
```

- [ ] **Step 2: Inspect existing admin pages for charting library**

Run: `grep -rn "recharts\|chart.js\|d3" front-end/src/app/admin/`. If `recharts` is already a dependency, use it. If not, render the trend chart as a simple inline SVG (60 lines). Do NOT add a new dep.

- [ ] **Step 3: Write `AnalyticsPanel.tsx`**

Compose the five views:
- 6 stat cards for status counts
- 2 cards for type split
- A combined opened/resolved-per-week chart (SVG line or recharts `<LineChart>` if available)
- A small table of stale tickets

(Layout: a 3-column grid for stat cards on the top row; chart spans full width below; stale-tickets table at the bottom.)

- [ ] **Step 4: Render the panel above the table on `/admin/tickets`**

In `front-end/src/app/admin/tickets/page.tsx`, after the role check and before the toolbar:

```tsx
import { AnalyticsPanel } from '@/components/tickets/AnalyticsPanel';
// ...
<AnalyticsPanel />
```

`AnalyticsPanel` fetches its own data on mount.

- [ ] **Step 5: Smoke test**

Open `/admin/tickets` as admin. Confirm cards show real counts; chart renders; stale table is empty if you have no tickets >30 days old (expected).

- [ ] **Step 6: Commit**

```bash
git add front-end/src/lib/api/tickets.ts \
        front-end/src/components/tickets/AnalyticsPanel.tsx \
        front-end/src/app/admin/tickets/page.tsx
git commit -m "feat(ticketing): admin analytics panel with 5 metrics"
```

---

## Task 4.3 — `TicketAutoCloseJob` (scheduled task)

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/job/TicketAutoCloseJob.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/job/TicketAutoCloseJobTest.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/OscalCliApiApplication.java` (add `@EnableScheduling` if not present)

- [ ] **Step 1: Verify scheduling is enabled**

Run: `grep -r "@EnableScheduling" back-end/src/main/java/gov/nist/oscal/tools/api/`. If found, skip step 2. Otherwise add `@EnableScheduling` to `OscalCliApiApplication.java` next to `@SpringBootApplication`.

- [ ] **Step 2: Write failing test**

```java
package gov.nist.oscal.tools.api.job;

import gov.nist.oscal.tools.api.entity.*;
import gov.nist.oscal.tools.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TicketAutoCloseJobTest {
    private TicketRepository tickets;
    private TicketCommentRepository comments;
    private TicketAutoCloseJob job;

    @BeforeEach
    void setUp() {
        tickets = mock(TicketRepository.class);
        comments = mock(TicketCommentRepository.class);
        job = new TicketAutoCloseJob(tickets, comments);
    }

    @Test
    void closesResolvedTicketsOlderThan7Days_writesSystemComment_noEmail() {
        Ticket t = new Ticket();
        t.setId(1L);
        t.setStatus(TicketStatus.RESOLVED);
        t.setResolvedAt(LocalDateTime.now().minusDays(8));
        when(tickets.findResolvedBefore(eq(TicketStatus.RESOLVED), any()))
            .thenReturn(List.of(t));

        job.run();

        assertThat(t.getStatus()).isEqualTo(TicketStatus.CLOSED);
        verify(tickets).save(t);
        verify(comments).save(any(TicketComment.class));
    }
}
```

- [ ] **Step 3: Run, expect failure**

Run: `cd back-end && mvn test -Dtest=TicketAutoCloseJobTest`

- [ ] **Step 4: Implement**

```java
package gov.nist.oscal.tools.api.job;

import gov.nist.oscal.tools.api.entity.*;
import gov.nist.oscal.tools.api.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class TicketAutoCloseJob {
    private static final Logger log = LoggerFactory.getLogger(TicketAutoCloseJob.class);
    private static final int IDLE_DAYS = 7;

    private final TicketRepository tickets;
    private final TicketCommentRepository comments;

    public TicketAutoCloseJob(TicketRepository tickets, TicketCommentRepository comments) {
        this.tickets = tickets;
        this.comments = comments;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void run() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(IDLE_DAYS);
        var stale = tickets.findResolvedBefore(TicketStatus.RESOLVED, cutoff);
        for (Ticket t : stale) {
            TicketStatus old = t.getStatus();
            t.setStatus(TicketStatus.CLOSED);
            t.setUpdatedAt(LocalDateTime.now());
            tickets.save(t);
            // Author of the system comment is null here; the comment factory needs
            // a non-null User. Resolve by either:
            //   (a) selecting the ticket's reporter as the placeholder author, or
            //   (b) adding an overload TicketComment.systemAutoClose(Ticket, from)
            //       that allows null author and renders "Auto-closed by system".
            // (b) is cleaner; pick (b). Adjust the comment table author_id to be
            // nullable in a follow-up migration if you go that route — OR re-use
            // the reporter as the author since reporters can technically "close"
            // their own tickets in this product. Pick reporter to avoid a schema
            // change: less DDL, no behavior cost.
            comments.save(TicketComment.statusChange(t, t.getReporter(), old, TicketStatus.CLOSED));
        }
        if (!stale.isEmpty()) log.info("Auto-closed {} resolved tickets", stale.size());
    }
}
```

(Per the inline note: choose to attribute the auto-close system comment to the ticket reporter to avoid schema changes. Update the test factory accordingly.)

- [ ] **Step 5: Run test**

Run: `cd back-end && mvn test -Dtest=TicketAutoCloseJobTest`

- [ ] **Step 6: Smoke test by running manually**

In Swagger UI, expose a temporary debug endpoint, OR run from a `@PostConstruct` once, OR connect via `psql` to backdate a `resolved_at`:

```bash
docker exec oscal-postgres-dev psql -U oscal_user -d oscal_dev -c \
  "UPDATE tickets SET status='RESOLVED', resolved_at=now() - interval '8 days' WHERE id=1;"
```

Then either wait until 03:00 (impractical) OR temporarily change the cron to `0 * * * * *` (every minute), restart, watch logs, then revert.

- [ ] **Step 7: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/job/TicketAutoCloseJob.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/job/TicketAutoCloseJobTest.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/OscalCliApiApplication.java
git commit -m "feat(ticketing): daily auto-close job for resolved tickets idle 7 days"
```

---

## Task 4.4 — End-to-end verification

**Files:** none — this is a manual verification pass.

- [ ] **Step 1: Boot fresh**

Run: `./stop.sh && ./dev.sh`

- [ ] **Step 2: Run the entire backend test suite**

Run: `cd back-end && mvn test`
Expected: all green.

- [ ] **Step 3: Run the entire frontend test suite**

Run: `cd front-end && npm test -- --watchAll=false`
Expected: all green.

- [ ] **Step 4: Walk the user-facing flow**

As a regular user:
1. Log in.
2. Click avatar → Open Ticket. Submit a Bug with all fields filled and one PNG attachment.
3. Verify you land on `/tickets/{id}` and see your description, metadata, and attachment.
4. Verify thowerton@regscale.com received the new-bug email.
5. Click avatar → My Tickets. Verify the ticket appears.

As SUPER_ADMIN:
6. Visit `/admin/tickets`. Verify the analytics panel shows status counts including your new ticket. Verify the search box filters by title.
7. Click the ticket row. Verify the status-change control is visible.
8. Move OPEN → IN_PROGRESS. Verify the system comment appears and you receive an email as the reporter.
9. Move IN_PROGRESS → RESOLVED with a note. Verify reporter receives an email containing the note.

Back as the reporter:
10. Reply to the RESOLVED ticket. Verify it flips back to OPEN, an additional system comment appears, and the admin receives a "Reopened" email.

- [ ] **Step 5: Final commit (if any tweaks)**

```bash
git status
# If there are leftover changes from manual smoke testing (e.g. cron back to nightly),
# commit them with a clear message.
```

---

# Spec coverage check

Each spec section maps to tasks above:
- Three tables, indexes, constraints → Task 1.1
- Status lifecycle + reopen → Tasks 1.2, 1.11
- Auto-close 7-day rule → Task 4.3
- API endpoints (user + admin) → Tasks 1.14, 1.15, 3.1, 4.1
- Authorization rule (reporter or admin) → Task 1.10 enforced in `getTicket`; controllers delegate
- Frontend pages (4) + avatar menu update → Tasks 2.3 through 2.7, 3.2, 4.2
- Email design (5 templates + matrix) → Task 1.8 (templates), 1.9–1.12 (call sites)
- Analytics queries (5) → Tasks 4.1, 4.2
- Implementation slices (4) → Phases 1–4 in this plan
- Out-of-scope items (org admin tier, markdown, voting, etc.) → not implemented; nothing to verify

# Final notes for the executor

- This plan assumes Hibernate 6.x for the `JsonBinaryType`. If the actual repo Hibernate version differs, look up the right hypersistence-utils artifact (e.g., `hypersistence-utils-hibernate-62` for 6.2). The spec doesn't depend on which version — just use whichever the project already uses.
- The "admin recipient" email refinement in Task 1.11 changes the `EmailService` signature from spec. That change is intentional and supersedes the spec.
- The auto-close job in Task 4.3 attributes the system comment to the ticket reporter to avoid an `author_id NULL` schema change. If a future iteration wants a "system" author, add a real system user row instead of relaxing the FK.
- Keep commits small. The plan structure already pushes one commit per task; resist bundling.
