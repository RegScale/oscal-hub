# OSCAL Hub AI Features — Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Land the shared infrastructure that all OSCAL AI wizards (Catalog, Profile, Component-def, SSP, POA&M, Builder Author Assist) will run on, plus a "smoke" wizard kind that proves the pipeline works end-to-end. After this plan lands, the per-doc-type wizards become small slices on top.

**Architecture:** New `back-end/.../api/ai/` module wraps the Anthropic Java SDK. Per-org API keys are encrypted via the existing `EncryptionService`. The `oscal` and `metaschema` plugins from `metaschema-framework/claude-plugins` are vendored as a git submodule — their markdown skill files are loaded at startup as system-prompt fragments. Six in-process tools wrap `liboscal-java` (validate, convert, resolve-profile, lookup-control, fetch-catalog, read-current-doc-section). Generation runs are orchestrated as `AiSession` rows with progress streamed to the frontend over SSE. Frontend gets a settings page (org admin), a feature gate, and a wizard picker shell.

**Tech Stack:** Spring Boot 4.0.6, Java 25, JPA/Hibernate, PostgreSQL, Flyway, `liboscal-java` 6.0.0, `com.anthropic:anthropic-java` 2.27.0, Apache POI (new dep, for `.docx`), Spring MVC `SseEmitter`, Next.js 16, React 19, shadcn/ui, vitest, Playwright.

**Spec:** `docs/superpowers/specs/2026-05-03-oscal-ai-features-design.md`

**Plan structure:** Sequenced in four phases. Each task is one focused unit ending in a commit.

- **Phase A — Persistence & key storage** (Tasks A1–A5)
- **Phase B — Services: Anthropic, knowledge, tools, ingestion** (Tasks B1–B6)
- **Phase C — Orchestrator + SSE + smoke wizard** (Tasks C1–C4)
- **Phase D — Frontend** (Tasks D1–D5)

---

## Phase A — Persistence & key storage

### Task A1: Add Anthropic Java SDK and Apache POI dependencies

**Files:**
- Modify: `back-end/pom.xml`

- [ ] **Step 1: Read current `<dependencies>` block to confirm position**

Run: `grep -n 'liboscal-java' back-end/pom.xml`
Expected: locates the existing `liboscal-java` dependency entry.

- [ ] **Step 2: Add the two new dependencies just below `liboscal-java`**

In `back-end/pom.xml`, add these dependencies inside `<dependencies>`:

```xml
<dependency>
    <groupId>com.anthropic</groupId>
    <artifactId>anthropic-java</artifactId>
    <version>2.27.0</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.4.0</version>
</dependency>
```

- [ ] **Step 3: Verify Maven resolves**

Run: `cd back-end && mvn -q dependency:resolve -DincludeArtifactIds=anthropic-java,poi-ooxml`
Expected: exits 0; both artifacts listed.

- [ ] **Step 4: Commit**

```bash
git add back-end/pom.xml
git commit -m "deps(ai): add anthropic-java SDK and apache POI"
```

---

### Task A2: Add `claude-plugins` git submodule

**Files:**
- Create: `.gitmodules`
- Create: `back-end/src/main/resources/claude-plugins/` (submodule mount point)

- [ ] **Step 1: Add the submodule pinned to the latest commit**

Run from repo root:

```bash
git submodule add https://github.com/metaschema-framework/claude-plugins.git back-end/src/main/resources/claude-plugins
git submodule update --init --recursive
```

Expected: creates `.gitmodules`, fetches the repo, populates the directory.

- [ ] **Step 2: Verify the expected skill files are present**

Run: `ls back-end/src/main/resources/claude-plugins/plugins/oscal/skills/ 2>/dev/null && ls back-end/src/main/resources/claude-plugins/plugins/metaschema/skills/ 2>/dev/null`
Expected: both directories list `.md` skill files.

If the upstream repo's directory layout differs, adjust the `KnowledgeLoader` paths in Task B2 accordingly. Document the actual layout in a comment on the constant `PLUGIN_ROOT` in `KnowledgeLoader`.

- [ ] **Step 3: Commit**

```bash
git add .gitmodules back-end/src/main/resources/claude-plugins
git commit -m "deps(ai): vendor claude-plugins repo as git submodule"
```

---

### Task A3: Flyway migration — `org_ai_settings` and `ai_sessions` tables

**Files:**
- Create: `back-end/src/main/resources/db/migration/V1.22__ai_features.sql`

- [ ] **Step 1: Write the migration**

```sql
-- V1.22: AI features — per-org API key storage and session tracking

CREATE TABLE org_ai_settings (
    id                          BIGSERIAL PRIMARY KEY,
    organization_id             BIGINT NOT NULL UNIQUE REFERENCES organizations(id) ON DELETE CASCADE,
    anthropic_key_encrypted     TEXT,
    anthropic_key_fingerprint   VARCHAR(32),
    default_model               VARCHAR(64) NOT NULL DEFAULT 'claude-opus-4-7',
    enabled                     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMP NOT NULL,
    updated_at                  TIMESTAMP
);

CREATE INDEX idx_org_ai_settings_org ON org_ai_settings(organization_id);

CREATE TABLE ai_sessions (
    id                  UUID PRIMARY KEY,
    organization_id     BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    wizard_kind         VARCHAR(32) NOT NULL,
    mode                VARCHAR(16) NOT NULL,
    model               VARCHAR(64) NOT NULL,
    input_summary       TEXT,
    status              VARCHAR(20) NOT NULL,
    tokens_in           INTEGER NOT NULL DEFAULT 0,
    tokens_out          INTEGER NOT NULL DEFAULT 0,
    error_code          VARCHAR(64),
    error_message       TEXT,
    started_at          TIMESTAMP NOT NULL,
    ended_at            TIMESTAMP
);

CREATE INDEX idx_ai_sessions_org_time ON ai_sessions(organization_id, started_at DESC);
CREATE INDEX idx_ai_sessions_user_time ON ai_sessions(user_id, started_at DESC);
```

- [ ] **Step 2: Run the test profile boot to confirm Flyway parses the migration**

Run: `cd back-end && mvn -q -Dtest='HealthControllerTest' test`
Expected: exits 0. (H2 test profile disables Flyway, but Spring Boot still resolves the SQL file; this just verifies the file is reachable on the classpath.) If you have a Flyway-enabled test, prefer that.

- [ ] **Step 3: Commit**

```bash
git add back-end/src/main/resources/db/migration/V1.22__ai_features.sql
git commit -m "feat(ai): add org_ai_settings and ai_sessions tables"
```

---

### Task A4: Entities + repositories for `OrgAiSettings` and `AiSession`

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/OrgAiSettings.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/AiSession.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/WizardKind.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/AiSessionStatus.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/AiSessionMode.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/OrgAiSettingsRepository.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/AiSessionRepository.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/entity/AiEntitiesTest.java`

- [ ] **Step 1: Write the failing repository test**

```java
package gov.nist.oscal.tools.api.entity;

import gov.nist.oscal.tools.api.repository.AiSessionRepository;
import gov.nist.oscal.tools.api.repository.OrgAiSettingsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AiEntitiesTest {

    @Autowired private OrgAiSettingsRepository settingsRepo;
    @Autowired private AiSessionRepository sessionRepo;

    @Test
    void persistsOrgAiSettings() {
        OrgAiSettings s = new OrgAiSettings();
        s.setOrganizationId(42L);
        s.setAnthropicKeyEncrypted("ciphertext");
        s.setAnthropicKeyFingerprint("abcd...1234");
        s.setDefaultModel("claude-opus-4-7");
        s.setEnabled(true);
        OrgAiSettings saved = settingsRepo.save(s);

        Optional<OrgAiSettings> loaded = settingsRepo.findByOrganizationId(42L);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getId()).isEqualTo(saved.getId());
        assertThat(loaded.get().getDefaultModel()).isEqualTo("claude-opus-4-7");
    }

    @Test
    void persistsAiSession() {
        AiSession s = new AiSession();
        s.setId(UUID.randomUUID());
        s.setOrganizationId(42L);
        s.setUserId(7L);
        s.setWizardKind(WizardKind.SMOKE);
        s.setMode(AiSessionMode.STREAMING);
        s.setModel("claude-opus-4-7");
        s.setStatus(AiSessionStatus.RUNNING);
        s.setStartedAt(LocalDateTime.now());
        AiSession saved = sessionRepo.save(s);

        assertThat(sessionRepo.findById(saved.getId())).isPresent();
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run: `cd back-end && mvn -q -Dtest=AiEntitiesTest test`
Expected: FAIL with compile errors (entities/repos not yet written).

- [ ] **Step 3: Write the enums**

`back-end/src/main/java/gov/nist/oscal/tools/api/entity/WizardKind.java`:

```java
package gov.nist.oscal.tools.api.entity;

public enum WizardKind {
    SMOKE,
    CATALOG,
    PROFILE,
    COMPONENT_DEF,
    SSP,
    POAM,
    BUILDER_ASSIST
}
```

`back-end/src/main/java/gov/nist/oscal/tools/api/entity/AiSessionStatus.java`:

```java
package gov.nist.oscal.tools.api.entity;

public enum AiSessionStatus {
    RUNNING,
    AWAITING_INPUT,
    COMPLETED,
    CANCELLED,
    FAILED
}
```

`back-end/src/main/java/gov/nist/oscal/tools/api/entity/AiSessionMode.java`:

```java
package gov.nist.oscal.tools.api.entity;

public enum AiSessionMode {
    STREAMING,
    THOROUGH
}
```

- [ ] **Step 4: Write `OrgAiSettings` entity**

`back-end/src/main/java/gov/nist/oscal/tools/api/entity/OrgAiSettings.java`:

```java
package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "org_ai_settings")
public class OrgAiSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false, unique = true)
    private Long organizationId;

    @Column(name = "anthropic_key_encrypted", columnDefinition = "TEXT")
    private String anthropicKeyEncrypted;

    @Column(name = "anthropic_key_fingerprint", length = 32)
    private String anthropicKeyFingerprint;

    @Column(name = "default_model", nullable = false, length = 64)
    private String defaultModel = "claude-opus-4-7";

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public OrgAiSettings() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public String getAnthropicKeyEncrypted() { return anthropicKeyEncrypted; }
    public void setAnthropicKeyEncrypted(String anthropicKeyEncrypted) { this.anthropicKeyEncrypted = anthropicKeyEncrypted; }
    public String getAnthropicKeyFingerprint() { return anthropicKeyFingerprint; }
    public void setAnthropicKeyFingerprint(String anthropicKeyFingerprint) { this.anthropicKeyFingerprint = anthropicKeyFingerprint; }
    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 5: Write `AiSession` entity**

`back-end/src/main/java/gov/nist/oscal/tools/api/entity/AiSession.java`:

```java
package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_sessions")
public class AiSession {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "wizard_kind", nullable = false, length = 32)
    private WizardKind wizardKind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AiSessionMode mode;

    @Column(nullable = false, length = 64)
    private String model;

    @Column(name = "input_summary", columnDefinition = "TEXT")
    private String inputSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiSessionStatus status;

    @Column(name = "tokens_in", nullable = false)
    private int tokensIn = 0;

    @Column(name = "tokens_out", nullable = false)
    private int tokensOut = 0;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public WizardKind getWizardKind() { return wizardKind; }
    public void setWizardKind(WizardKind wizardKind) { this.wizardKind = wizardKind; }
    public AiSessionMode getMode() { return mode; }
    public void setMode(AiSessionMode mode) { this.mode = mode; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getInputSummary() { return inputSummary; }
    public void setInputSummary(String inputSummary) { this.inputSummary = inputSummary; }
    public AiSessionStatus getStatus() { return status; }
    public void setStatus(AiSessionStatus status) { this.status = status; }
    public int getTokensIn() { return tokensIn; }
    public void setTokensIn(int tokensIn) { this.tokensIn = tokensIn; }
    public int getTokensOut() { return tokensOut; }
    public void setTokensOut(int tokensOut) { this.tokensOut = tokensOut; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
}
```

- [ ] **Step 6: Write the repositories**

`back-end/src/main/java/gov/nist/oscal/tools/api/repository/OrgAiSettingsRepository.java`:

```java
package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.OrgAiSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrgAiSettingsRepository extends JpaRepository<OrgAiSettings, Long> {
    Optional<OrgAiSettings> findByOrganizationId(Long organizationId);
}
```

`back-end/src/main/java/gov/nist/oscal/tools/api/repository/AiSessionRepository.java`:

```java
package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.AiSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiSessionRepository extends JpaRepository<AiSession, UUID> {
}
```

- [ ] **Step 7: Run tests**

Run: `cd back-end && mvn -q -Dtest=AiEntitiesTest test`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/entity/OrgAiSettings.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/entity/AiSession.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/entity/WizardKind.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/entity/AiSessionStatus.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/entity/AiSessionMode.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/repository/OrgAiSettingsRepository.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/repository/AiSessionRepository.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/entity/AiEntitiesTest.java
git commit -m "feat(ai): entities + repositories for AI settings and sessions"
```

---

### Task A5: `AiSettingsService` and `AiSettingsController`

The service stores/rotates the org's Anthropic key (encrypted via `EncryptionService`), exposes a fingerprint for UI display, and tells the orchestrator whether AI is enabled for an org.

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AiSettingsService.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AiSettingsController.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/AiSettingsResponse.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/UpdateAiSettingsRequest.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/AiSettingsServiceTest.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/controller/AiSettingsControllerTest.java`

- [ ] **Step 1: Write the service test**

```java
package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.OrgAiSettings;
import gov.nist.oscal.tools.api.repository.OrgAiSettingsRepository;
import gov.nist.oscal.tools.api.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiSettingsServiceTest {

    private OrgAiSettingsRepository repo;
    private EncryptionService encryption;
    private AiSettingsService service;

    @BeforeEach
    void setUp() {
        repo = mock(OrgAiSettingsRepository.class);
        encryption = mock(EncryptionService.class);
        service = new AiSettingsService(repo, encryption);
    }

    @Test
    void setKeyCreatesNewSettingsWhenNoneExist() {
        when(repo.findByOrganizationId(1L)).thenReturn(Optional.empty());
        when(encryption.encrypt("sk-ant-1234567890abcdef")).thenReturn("encrypted");
        when(repo.save(any(OrgAiSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        AiSettingsResponse resp = service.setApiKey(1L, "sk-ant-1234567890abcdef", "claude-opus-4-7");

        ArgumentCaptor<OrgAiSettings> captor = ArgumentCaptor.forClass(OrgAiSettings.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getAnthropicKeyEncrypted()).isEqualTo("encrypted");
        assertThat(captor.getValue().isEnabled()).isTrue();
        assertThat(resp.fingerprint()).endsWith("cdef");
    }

    @Test
    void enabledReturnsFalseWhenNoSettings() {
        when(repo.findByOrganizationId(2L)).thenReturn(Optional.empty());
        assertThat(service.isEnabledFor(2L)).isFalse();
    }

    @Test
    void enabledReturnsTrueWhenSettingsHaveKey() {
        OrgAiSettings s = new OrgAiSettings();
        s.setOrganizationId(3L);
        s.setAnthropicKeyEncrypted("ciphertext");
        s.setEnabled(true);
        when(repo.findByOrganizationId(3L)).thenReturn(Optional.of(s));
        assertThat(service.isEnabledFor(3L)).isTrue();
    }

    @Test
    void getDecryptedKeyDecryptsViaEncryptionService() {
        OrgAiSettings s = new OrgAiSettings();
        s.setOrganizationId(4L);
        s.setAnthropicKeyEncrypted("ciphertext");
        s.setEnabled(true);
        when(repo.findByOrganizationId(4L)).thenReturn(Optional.of(s));
        when(encryption.decrypt("ciphertext")).thenReturn("sk-ant-real");

        assertThat(service.getDecryptedKey(4L)).isEqualTo("sk-ant-real");
    }

    @Test
    void disableClearsKey() {
        OrgAiSettings s = new OrgAiSettings();
        s.setOrganizationId(5L);
        s.setAnthropicKeyEncrypted("ciphertext");
        s.setEnabled(true);
        when(repo.findByOrganizationId(5L)).thenReturn(Optional.of(s));

        service.disable(5L);

        ArgumentCaptor<OrgAiSettings> captor = ArgumentCaptor.forClass(OrgAiSettings.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getAnthropicKeyEncrypted()).isNull();
        assertThat(captor.getValue().isEnabled()).isFalse();
    }
}
```

- [ ] **Step 2: Write the DTOs**

`back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/AiSettingsResponse.java`:

```java
package gov.nist.oscal.tools.api.model.ai;

public record AiSettingsResponse(
        boolean enabled,
        String fingerprint,
        String defaultModel
) { }
```

`back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/UpdateAiSettingsRequest.java`:

```java
package gov.nist.oscal.tools.api.model.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateAiSettingsRequest {
    @NotBlank
    @Size(min = 20, max = 256)
    private String apiKey;

    @Size(max = 64)
    private String defaultModel;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
}
```

- [ ] **Step 3: Write the service**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AiSettingsService.java`:

```java
package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.OrgAiSettings;
import gov.nist.oscal.tools.api.model.ai.AiSettingsResponse;
import gov.nist.oscal.tools.api.repository.OrgAiSettingsRepository;
import gov.nist.oscal.tools.api.service.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AiSettingsService {

    private static final Logger log = LoggerFactory.getLogger(AiSettingsService.class);

    private final OrgAiSettingsRepository repo;
    private final EncryptionService encryption;

    public AiSettingsService(OrgAiSettingsRepository repo, EncryptionService encryption) {
        this.repo = repo;
        this.encryption = encryption;
    }

    @Transactional
    public AiSettingsResponse setApiKey(Long organizationId, String apiKey, String defaultModel) {
        OrgAiSettings s = repo.findByOrganizationId(organizationId).orElseGet(() -> {
            OrgAiSettings n = new OrgAiSettings();
            n.setOrganizationId(organizationId);
            return n;
        });
        s.setAnthropicKeyEncrypted(encryption.encrypt(apiKey));
        s.setAnthropicKeyFingerprint(fingerprint(apiKey));
        if (defaultModel != null && !defaultModel.isBlank()) {
            s.setDefaultModel(defaultModel);
        }
        s.setEnabled(true);
        repo.save(s);
        log.info("AI settings updated for org {}", organizationId);
        return toResponse(s);
    }

    @Transactional(readOnly = true)
    public AiSettingsResponse getSettings(Long organizationId) {
        Optional<OrgAiSettings> s = repo.findByOrganizationId(organizationId);
        if (s.isEmpty()) {
            return new AiSettingsResponse(false, null, "claude-opus-4-7");
        }
        return toResponse(s.get());
    }

    @Transactional(readOnly = true)
    public boolean isEnabledFor(Long organizationId) {
        return repo.findByOrganizationId(organizationId)
                .map(s -> s.isEnabled() && s.getAnthropicKeyEncrypted() != null)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public String getDecryptedKey(Long organizationId) {
        OrgAiSettings s = repo.findByOrganizationId(organizationId)
                .filter(OrgAiSettings::isEnabled)
                .orElseThrow(() -> new IllegalStateException("AI not enabled for org " + organizationId));
        return encryption.decrypt(s.getAnthropicKeyEncrypted());
    }

    @Transactional(readOnly = true)
    public String getDefaultModel(Long organizationId) {
        return repo.findByOrganizationId(organizationId)
                .map(OrgAiSettings::getDefaultModel)
                .orElse("claude-opus-4-7");
    }

    @Transactional
    public void disable(Long organizationId) {
        repo.findByOrganizationId(organizationId).ifPresent(s -> {
            s.setEnabled(false);
            s.setAnthropicKeyEncrypted(null);
            s.setAnthropicKeyFingerprint(null);
            repo.save(s);
            log.info("AI settings disabled for org {}", organizationId);
        });
    }

    private AiSettingsResponse toResponse(OrgAiSettings s) {
        return new AiSettingsResponse(s.isEnabled() && s.getAnthropicKeyEncrypted() != null,
                s.getAnthropicKeyFingerprint(),
                s.getDefaultModel());
    }

    private String fingerprint(String apiKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(apiKey.getBytes());
            String hex = HexFormat.of().formatHex(digest).substring(0, 8);
            String last4 = apiKey.substring(Math.max(0, apiKey.length() - 4));
            return hex + "..." + last4;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
```

- [ ] **Step 4: Run service test**

Run: `cd back-end && mvn -q -Dtest=AiSettingsServiceTest test`
Expected: PASS.

- [ ] **Step 5: Write the controller test**

```java
package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.model.ai.AiSettingsResponse;
import gov.nist.oscal.tools.api.model.ai.UpdateAiSettingsRequest;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.ai.AiSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiSettingsController.class)
class AiSettingsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private AiSettingsService service;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ORG_ADMIN")
    void putSettingsCallsSetApiKey() throws Exception {
        when(service.setApiKey(eq(1L), eq("sk-ant-key12345678901234"), eq("claude-opus-4-7")))
                .thenReturn(new AiSettingsResponse(true, "abcd...1234", "claude-opus-4-7"));

        UpdateAiSettingsRequest req = new UpdateAiSettingsRequest();
        req.setApiKey("sk-ant-key12345678901234");
        req.setDefaultModel("claude-opus-4-7");

        mockMvc.perform(put("/api/ai/settings").param("organizationId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.fingerprint").value("abcd...1234"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void putSettingsForbiddenForNonAdmin() throws Exception {
        UpdateAiSettingsRequest req = new UpdateAiSettingsRequest();
        req.setApiKey("sk-ant-key12345678901234");

        mockMvc.perform(put("/api/ai/settings").param("organizationId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ORG_ADMIN")
    void getSettingsReturnsCurrent() throws Exception {
        when(service.getSettings(1L)).thenReturn(new AiSettingsResponse(false, null, "claude-opus-4-7"));
        mockMvc.perform(get("/api/ai/settings").param("organizationId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void getStatusIsPublicAndReturnsEnabledFlag() throws Exception {
        when(service.isEnabledFor(1L)).thenReturn(true);
        mockMvc.perform(get("/api/ai/settings/status").param("organizationId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }
}
```

- [ ] **Step 6: Write the controller**

`back-end/src/main/java/gov/nist/oscal/tools/api/controller/AiSettingsController.java`:

```java
package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.model.ai.AiSettingsResponse;
import gov.nist.oscal.tools.api.model.ai.UpdateAiSettingsRequest;
import gov.nist.oscal.tools.api.service.ai.AiSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/settings")
@Tag(name = "AI Settings", description = "Per-organization AI configuration")
public class AiSettingsController {

    private final AiSettingsService service;

    public AiSettingsController(AiSettingsService service) {
        this.service = service;
    }

    @Operation(summary = "Get AI settings for an organization")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<AiSettingsResponse> get(@RequestParam Long organizationId) {
        return ResponseEntity.ok(service.getSettings(organizationId));
    }

    @Operation(summary = "Set or rotate the Anthropic API key for an organization")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @PutMapping
    public ResponseEntity<AiSettingsResponse> put(@RequestParam Long organizationId,
                                                  @Valid @RequestBody UpdateAiSettingsRequest req) {
        return ResponseEntity.ok(service.setApiKey(organizationId, req.getApiKey(), req.getDefaultModel()));
    }

    @Operation(summary = "Disable AI for an organization (clears stored key)")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping
    public ResponseEntity<Void> disable(@RequestParam Long organizationId) {
        service.disable(organizationId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Capability probe — is AI enabled for this org?")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status(@RequestParam Long organizationId) {
        return ResponseEntity.ok(Map.of("enabled", service.isEnabledFor(organizationId)));
    }
}
```

- [ ] **Step 7: Update `SecurityConfig` to allow `/api/ai/settings/status` unauthenticated**

Read `back-end/src/main/java/gov/nist/oscal/tools/api/config/SecurityConfig.java`. Find the public-endpoints whitelist (the `requestMatchers(...).permitAll()` chain). Add `"/api/ai/settings/status"` to it. Then keep `"/api/ai/**"` authenticated for everything else.

- [ ] **Step 8: Run controller test**

Run: `cd back-end && mvn -q -Dtest=AiSettingsControllerTest test`
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AiSettingsService.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/controller/AiSettingsController.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/AiSettingsResponse.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/UpdateAiSettingsRequest.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/config/SecurityConfig.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/AiSettingsServiceTest.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/controller/AiSettingsControllerTest.java
git commit -m "feat(ai): per-org Anthropic API key storage + settings API"
```

---

## Phase B — Services: Anthropic, knowledge, tools, ingestion

### Task B1: `AnthropicClient` wrapper

This wraps the SDK and accepts a per-call API key. Real Claude calls happen here; everything above this is testable without network.

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AnthropicClient.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AnthropicCall.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AnthropicResult.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/AnthropicClientTest.java`

- [ ] **Step 1: Write the test**

```java
package gov.nist.oscal.tools.api.service.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnthropicClientTest {

    @Test
    void rejectsBlankApiKey() {
        AnthropicClient client = new AnthropicClient();
        AnthropicCall call = AnthropicCall.builder()
                .model("claude-opus-4-7")
                .systemPrompt("hi")
                .userMessage("ping")
                .build();

        assertThatThrownBy(() -> client.send("", call))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API key");
    }

    @Test
    void buildsCallParams() {
        AnthropicCall call = AnthropicCall.builder()
                .model("claude-opus-4-7")
                .systemPrompt("system")
                .userMessage("hello")
                .maxTokens(256)
                .build();
        assertThat(call.model()).isEqualTo("claude-opus-4-7");
        assertThat(call.systemPrompt()).isEqualTo("system");
        assertThat(call.userMessage()).isEqualTo("hello");
        assertThat(call.maxTokens()).isEqualTo(256);
    }
}
```

The real Anthropic call is exercised in the smoke wizard integration test (Task C4) using a recorded fixture or a developer key in a `@DisabledIfEnvironmentVariable`-gated test. We do NOT call Anthropic in unit tests.

- [ ] **Step 2: Write `AnthropicCall`**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AnthropicCall.java`:

```java
package gov.nist.oscal.tools.api.service.ai;

import java.util.ArrayList;
import java.util.List;

public record AnthropicCall(
        String model,
        String systemPrompt,
        String userMessage,
        int maxTokens,
        List<byte[]> pdfDocuments,
        List<String> textDocuments
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String model = "claude-opus-4-7";
        private String systemPrompt = "";
        private String userMessage = "";
        private int maxTokens = 4096;
        private final List<byte[]> pdfs = new ArrayList<>();
        private final List<String> texts = new ArrayList<>();

        public Builder model(String m) { this.model = m; return this; }
        public Builder systemPrompt(String s) { this.systemPrompt = s; return this; }
        public Builder userMessage(String u) { this.userMessage = u; return this; }
        public Builder maxTokens(int m) { this.maxTokens = m; return this; }
        public Builder addPdf(byte[] bytes) { pdfs.add(bytes); return this; }
        public Builder addText(String text) { texts.add(text); return this; }

        public AnthropicCall build() {
            return new AnthropicCall(model, systemPrompt, userMessage, maxTokens, pdfs, texts);
        }
    }
}
```

- [ ] **Step 3: Write `AnthropicResult`**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AnthropicResult.java`:

```java
package gov.nist.oscal.tools.api.service.ai;

public record AnthropicResult(
        String text,
        int tokensIn,
        int tokensOut
) { }
```

- [ ] **Step 4: Write `AnthropicClient`**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AnthropicClient.java`:

```java
package gov.nist.oscal.tools.api.service.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.TextBlockParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service("anthropicClientService")
public class AnthropicClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicClient.class);

    public AnthropicResult send(String apiKey, AnthropicCall call) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Missing Anthropic API key");
        }
        com.anthropic.client.AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();

        List<ContentBlockParam> blocks = new ArrayList<>();
        for (String text : call.textDocuments()) {
            blocks.add(ContentBlockParam.ofText(TextBlockParam.builder().text(text).build()));
        }
        for (byte[] pdf : call.pdfDocuments()) {
            String base64 = Base64.getEncoder().encodeToString(pdf);
            blocks.add(ContentBlockParam.ofDocument(
                    com.anthropic.models.messages.DocumentBlockParam.builder()
                            .source(com.anthropic.models.messages.Base64PdfSource.builder()
                                    .data(base64)
                                    .mediaType(com.anthropic.models.messages.Base64PdfSource.MediaType.APPLICATION_PDF)
                                    .build())
                            .build()));
        }
        blocks.add(ContentBlockParam.ofText(TextBlockParam.builder().text(call.userMessage()).build()));

        MessageCreateParams params = MessageCreateParams.builder()
                .model(call.model())
                .maxTokens(call.maxTokens())
                .system(call.systemPrompt())
                .addMessage(MessageParam.builder()
                        .role(MessageParam.Role.USER)
                        .contentOfBlockParams(blocks)
                        .build())
                .build();

        Message message = client.messages().create(params);
        String text = message.content().stream()
                .filter(c -> c.text().isPresent())
                .map(c -> c.text().get().text())
                .reduce("", (a, b) -> a + b);

        long tokensIn = message.usage().inputTokens();
        long tokensOut = message.usage().outputTokens();
        log.info("Anthropic call complete: model={} in={} out={}", call.model(), tokensIn, tokensOut);
        return new AnthropicResult(text, (int) tokensIn, (int) tokensOut);
    }
}
```

If the SDK's exact builder names differ from the snippet above (the SDK evolves), look at `com.anthropic.models.messages.MessageCreateParams` and adjust — the responsibility of this method is unchanged: take an API key + a call, return text + token usage. Update the test if signatures change.

- [ ] **Step 5: Run test**

Run: `cd back-end && mvn -q -Dtest=AnthropicClientTest test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AnthropicClient.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AnthropicCall.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AnthropicResult.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/AnthropicClientTest.java
git commit -m "feat(ai): AnthropicClient wrapper for SDK calls"
```

---

### Task B2: `KnowledgeLoader` — read claude-plugins skills as system prompts

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/KnowledgeLoader.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/KnowledgeLoaderTest.java`
- Test fixture: `back-end/src/test/resources/claude-plugins/plugins/oscal/skills/test-skill.md`
- Test fixture: `back-end/src/test/resources/claude-plugins/plugins/metaschema/skills/test-meta.md`

- [ ] **Step 1: Create test fixture skill files**

`back-end/src/test/resources/claude-plugins/plugins/oscal/skills/test-skill.md`:

```markdown
# OSCAL Layer Overview

This is a fixture used in unit tests. Catalog, Profile, Component-definition, SSP, SAP, SAR, POA&M.
```

`back-end/src/test/resources/claude-plugins/plugins/metaschema/skills/test-meta.md`:

```markdown
# Metaschema Constraints

This is a fixture used in unit tests. allowed-values, expect, matches, has-cardinality.
```

- [ ] **Step 2: Write the failing test**

```java
package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.WizardKind;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeLoaderTest {

    @Test
    void loadsCatalogSystemPromptFromFixture() {
        Path root = Paths.get("src/test/resources/claude-plugins");
        KnowledgeLoader loader = new KnowledgeLoader(root);
        String prompt = loader.systemFor(WizardKind.CATALOG);

        assertThat(prompt).contains("OSCAL Layer Overview");
        assertThat(prompt).contains("Metaschema Constraints");
    }

    @Test
    void smokeWizardSystemPromptIsTerse() {
        Path root = Paths.get("src/test/resources/claude-plugins");
        KnowledgeLoader loader = new KnowledgeLoader(root);
        String prompt = loader.systemFor(WizardKind.SMOKE);

        assertThat(prompt).contains("smoke");
        assertThat(prompt.length()).isLessThan(2000);
    }
}
```

- [ ] **Step 3: Run test (expect FAIL)**

Run: `cd back-end && mvn -q -Dtest=KnowledgeLoaderTest test`
Expected: FAIL — KnowledgeLoader doesn't exist yet.

- [ ] **Step 4: Implement `KnowledgeLoader`**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/KnowledgeLoader.java`:

```java
package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.WizardKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@Service
public class KnowledgeLoader {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeLoader.class);

    private final Path pluginRoot;

    public KnowledgeLoader(@Value("${ai.plugins.root:back-end/src/main/resources/claude-plugins}") String root) {
        this(Paths.get(root));
    }

    public KnowledgeLoader(Path pluginRoot) {
        this.pluginRoot = pluginRoot;
    }

    public String systemFor(WizardKind kind) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert OSCAL author working inside OSCAL Hub. ");
        sb.append("Always produce schema-valid OSCAL output. Use the validate_oscal tool to confirm.\n\n");

        if (kind == WizardKind.SMOKE) {
            sb.append("This is a smoke-test wizard. Reply concisely.\n");
            return sb.toString();
        }

        // Append all OSCAL plugin skills
        appendSkillsFrom(sb, pluginRoot.resolve("plugins/oscal/skills"));
        // Append all Metaschema plugin skills
        appendSkillsFrom(sb, pluginRoot.resolve("plugins/metaschema/skills"));

        switch (kind) {
            case CATALOG -> sb.append("\nFocus: produce an OSCAL Catalog with controls, parts, params, groups.\n");
            case PROFILE -> sb.append("\nFocus: produce an OSCAL Profile with imports, includes, and modifications.\n");
            case COMPONENT_DEF -> sb.append("\nFocus: produce an OSCAL Component-definition mapping product features to controls.\n");
            case SSP -> sb.append("\nFocus: produce SSP per-control implementation narratives grounded in the system description.\n");
            case POAM -> sb.append("\nFocus: produce OSCAL POA&M items with risk ratings, milestones, and control mappings.\n");
            case BUILDER_ASSIST -> sb.append("\nFocus: act as an inline assistant for the document the user is editing.\n");
            default -> { }
        }

        return sb.toString();
    }

    private void appendSkillsFrom(StringBuilder sb, Path dir) {
        if (!Files.isDirectory(dir)) {
            log.warn("Knowledge directory not found: {}", dir);
            return;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> files = stream.filter(p -> p.toString().endsWith(".md")).sorted().toList();
            for (Path file : files) {
                sb.append(Files.readString(file)).append("\n\n");
            }
        } catch (IOException e) {
            log.warn("Failed to read skills from {}: {}", dir, e.toString());
        }
    }
}
```

- [ ] **Step 5: Run test**

Run: `cd back-end && mvn -q -Dtest=KnowledgeLoaderTest test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/KnowledgeLoader.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/KnowledgeLoaderTest.java \
        back-end/src/test/resources/claude-plugins
git commit -m "feat(ai): KnowledgeLoader composes per-wizard system prompts from plugin skills"
```

---

### Task B3: `OscalToolBox` — define the 6 in-process tools (skeletons + tool definitions)

This task lands the tool *registry* and *interface*. The actual `liboscal-java` calls are filled in in Task B4.

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/Tool.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/ToolCall.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/ToolResult.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/OscalToolBox.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/tools/OscalToolBoxTest.java`

- [ ] **Step 1: Write the test**

```java
package gov.nist.oscal.tools.api.service.ai.tools;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OscalToolBoxTest {

    @Test
    void registersExpectedTools() {
        Tool stub = new Tool() {
            public String name() { return "stub"; }
            public String description() { return "stub"; }
            public String inputSchemaJson() { return "{}"; }
            public ToolResult invoke(ToolCall call) { return ToolResult.ok("ok"); }
        };

        OscalToolBox box = new OscalToolBox(List.of(stub, stub));
        assertThat(box.tools()).hasSize(2);
    }

    @Test
    void invokesByName() {
        Tool t = new Tool() {
            public String name() { return "echo"; }
            public String description() { return "echo"; }
            public String inputSchemaJson() { return "{\"type\":\"object\"}"; }
            public ToolResult invoke(ToolCall call) { return ToolResult.ok("echoed: " + call.argsJson()); }
        };
        OscalToolBox box = new OscalToolBox(List.of(t));
        ToolResult r = box.invoke(new ToolCall("echo", "{\"hi\":1}"));
        assertThat(r.ok()).isTrue();
        assertThat(r.summary()).contains("echoed");
    }

    @Test
    void unknownToolReturnsError() {
        OscalToolBox box = new OscalToolBox(List.of());
        ToolResult r = box.invoke(new ToolCall("nope", "{}"));
        assertThat(r.ok()).isFalse();
    }
}
```

- [ ] **Step 2: Implement the interfaces and registry**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/Tool.java`:

```java
package gov.nist.oscal.tools.api.service.ai.tools;

public interface Tool {
    String name();
    String description();
    String inputSchemaJson();
    ToolResult invoke(ToolCall call);
}
```

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/ToolCall.java`:

```java
package gov.nist.oscal.tools.api.service.ai.tools;

public record ToolCall(String name, String argsJson) { }
```

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/ToolResult.java`:

```java
package gov.nist.oscal.tools.api.service.ai.tools;

public record ToolResult(boolean ok, String summary, String contentJson) {
    public static ToolResult ok(String summary) {
        return new ToolResult(true, summary, "{}");
    }
    public static ToolResult ok(String summary, String contentJson) {
        return new ToolResult(true, summary, contentJson);
    }
    public static ToolResult error(String summary) {
        return new ToolResult(false, summary, "{}");
    }
}
```

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/OscalToolBox.java`:

```java
package gov.nist.oscal.tools.api.service.ai.tools;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OscalToolBox {

    private final Map<String, Tool> byName;

    public OscalToolBox(List<Tool> tools) {
        this.byName = tools.stream().collect(Collectors.toMap(Tool::name, t -> t));
    }

    public List<Tool> tools() {
        return List.copyOf(byName.values());
    }

    public ToolResult invoke(ToolCall call) {
        Tool t = byName.get(call.name());
        if (t == null) return ToolResult.error("Unknown tool: " + call.name());
        try {
            return t.invoke(call);
        } catch (Exception e) {
            return ToolResult.error("Tool error: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 3: Run test**

Run: `cd back-end && mvn -q -Dtest=OscalToolBoxTest test`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/Tool.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/ToolCall.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/ToolResult.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/OscalToolBox.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/tools/OscalToolBoxTest.java
git commit -m "feat(ai): tool registry and Tool/ToolCall/ToolResult contract"
```

---

### Task B4: Implement the 6 tools (one Spring bean each, wraps `liboscal-java`)

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/ValidateOscalTool.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/ConvertFormatTool.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/ResolveProfileTool.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/LookupControlTool.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/FetchCatalogTool.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/ReadCurrentDocSectionTool.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/tools/ValidateOscalToolTest.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/tools/ConvertFormatToolTest.java`

- [ ] **Step 1: Write `ValidateOscalTool` test**

```java
package gov.nist.oscal.tools.api.service.ai.tools;

import gov.nist.oscal.tools.api.service.ValidationService;
import gov.nist.oscal.tools.api.model.ValidationRequest;
import gov.nist.oscal.tools.api.model.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ValidateOscalToolTest {

    @Test
    void invokesValidationServiceAndReportsOk() {
        ValidationService svc = mock(ValidationService.class);
        ValidationResult ok = new ValidationResult();
        ok.setValid(true);
        when(svc.validate(any(ValidationRequest.class), eq("ai"))).thenReturn(ok);

        ValidateOscalTool tool = new ValidateOscalTool(svc);
        ToolResult r = tool.invoke(new ToolCall("validate_oscal",
                "{\"content\":\"{}\",\"format\":\"JSON\",\"modelType\":\"catalog\"}"));

        assertThat(r.ok()).isTrue();
        assertThat(r.summary()).containsIgnoringCase("valid");
    }

    @Test
    void reportsErrorsFromValidation() {
        ValidationService svc = mock(ValidationService.class);
        ValidationResult bad = new ValidationResult();
        bad.setValid(false);
        bad.setErrors(List.of("missing metadata.title"));
        when(svc.validate(any(ValidationRequest.class), anyString())).thenReturn(bad);

        ValidateOscalTool tool = new ValidateOscalTool(svc);
        ToolResult r = tool.invoke(new ToolCall("validate_oscal",
                "{\"content\":\"{}\",\"format\":\"JSON\",\"modelType\":\"catalog\"}"));

        assertThat(r.ok()).isFalse();
        assertThat(r.summary()).contains("missing metadata.title");
    }
}
```

The `ValidationRequest` / `ValidationResult` classes already exist in this codebase under `gov.nist.oscal.tools.api.model`. If their fields differ from what's used here, adjust the request construction inside the tool — the contract remains: take a `validate_oscal` tool call → run existing validation → return ok/error.

- [ ] **Step 2: Implement `ValidateOscalTool`**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/ValidateOscalTool.java`:

```java
package gov.nist.oscal.tools.api.service.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.model.ValidationRequest;
import gov.nist.oscal.tools.api.model.ValidationResult;
import gov.nist.oscal.tools.api.service.ValidationService;
import org.springframework.stereotype.Component;

@Component
public class ValidateOscalTool implements Tool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ValidationService validationService;

    public ValidateOscalTool(ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override public String name() { return "validate_oscal"; }
    @Override public String description() {
        return "Validate OSCAL content against the schema and constraints. "
                + "Returns valid=true or a list of error messages.";
    }
    @Override public String inputSchemaJson() {
        return "{\"type\":\"object\","
                + "\"properties\":{"
                + "\"content\":{\"type\":\"string\"},"
                + "\"format\":{\"type\":\"string\",\"enum\":[\"JSON\",\"XML\",\"YAML\"]},"
                + "\"modelType\":{\"type\":\"string\"}"
                + "},\"required\":[\"content\",\"format\",\"modelType\"]}";
    }

    @Override
    public ToolResult invoke(ToolCall call) {
        try {
            JsonNode args = MAPPER.readTree(call.argsJson());
            ValidationRequest req = new ValidationRequest();
            req.setContent(args.get("content").asText());
            req.setFormat(args.get("format").asText());
            req.setModelType(args.get("modelType").asText());

            ValidationResult result = validationService.validate(req, "ai");
            if (result.isValid()) return ToolResult.ok("valid");
            String errors = result.getErrors() == null ? "" : String.join("; ", result.getErrors());
            return ToolResult.error("invalid: " + errors);
        } catch (Exception e) {
            return ToolResult.error("validate_oscal error: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 3: Run `ValidateOscalToolTest`**

Run: `cd back-end && mvn -q -Dtest=ValidateOscalToolTest test`
Expected: PASS. If `ValidationService.validate(request, username)` signature differs, adjust both the test and tool to match the existing service.

- [ ] **Step 4: Implement `ConvertFormatTool`**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/ConvertFormatTool.java`:

```java
package gov.nist.oscal.tools.api.service.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.model.ConversionRequest;
import gov.nist.oscal.tools.api.model.ConversionResult;
import gov.nist.oscal.tools.api.service.ConversionService;
import org.springframework.stereotype.Component;

@Component
public class ConvertFormatTool implements Tool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ConversionService conversionService;

    public ConvertFormatTool(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override public String name() { return "convert_format"; }
    @Override public String description() { return "Convert OSCAL content between JSON, XML, and YAML."; }
    @Override public String inputSchemaJson() {
        return "{\"type\":\"object\","
                + "\"properties\":{"
                + "\"content\":{\"type\":\"string\"},"
                + "\"from\":{\"type\":\"string\",\"enum\":[\"JSON\",\"XML\",\"YAML\"]},"
                + "\"to\":{\"type\":\"string\",\"enum\":[\"JSON\",\"XML\",\"YAML\"]},"
                + "\"modelType\":{\"type\":\"string\"}"
                + "},\"required\":[\"content\",\"from\",\"to\",\"modelType\"]}";
    }

    @Override
    public ToolResult invoke(ToolCall call) {
        try {
            JsonNode args = MAPPER.readTree(call.argsJson());
            ConversionRequest req = new ConversionRequest();
            req.setContent(args.get("content").asText());
            req.setSourceFormat(args.get("from").asText());
            req.setTargetFormat(args.get("to").asText());
            req.setModelType(args.get("modelType").asText());

            ConversionResult result = conversionService.convert(req, "ai");
            return ToolResult.ok("converted", "{\"content\":" + MAPPER.writeValueAsString(result.getConvertedContent()) + "}");
        } catch (Exception e) {
            return ToolResult.error("convert_format error: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 5: Write `ConvertFormatTool` test**

`back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/tools/ConvertFormatToolTest.java`:

```java
package gov.nist.oscal.tools.api.service.ai.tools;

import gov.nist.oscal.tools.api.model.ConversionRequest;
import gov.nist.oscal.tools.api.model.ConversionResult;
import gov.nist.oscal.tools.api.service.ConversionService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConvertFormatToolTest {

    @Test
    void invokesConversionServiceAndReturnsContent() {
        ConversionService svc = mock(ConversionService.class);
        ConversionResult ok = new ConversionResult();
        ok.setConvertedContent("<catalog/>");
        when(svc.convert(any(ConversionRequest.class), anyString())).thenReturn(ok);

        ConvertFormatTool tool = new ConvertFormatTool(svc);
        ToolResult r = tool.invoke(new ToolCall("convert_format",
                "{\"content\":\"{}\",\"from\":\"JSON\",\"to\":\"XML\",\"modelType\":\"catalog\"}"));

        assertThat(r.ok()).isTrue();
        assertThat(r.contentJson()).contains("<catalog/>");
    }

    @Test
    void wrapsServiceExceptionsAsToolError() {
        ConversionService svc = mock(ConversionService.class);
        when(svc.convert(any(ConversionRequest.class), anyString()))
                .thenThrow(new RuntimeException("boom"));

        ConvertFormatTool tool = new ConvertFormatTool(svc);
        ToolResult r = tool.invoke(new ToolCall("convert_format",
                "{\"content\":\"{}\",\"from\":\"JSON\",\"to\":\"XML\",\"modelType\":\"catalog\"}"));

        assertThat(r.ok()).isFalse();
        assertThat(r.summary()).contains("boom");
    }
}
```

If the existing `ConversionService.convert(request, username)` signature differs (return type, parameter names), align both this test and `ConvertFormatTool` to the existing method — the contract of this tool is unchanged.

- [ ] **Step 6: Implement `ResolveProfileTool`**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/ResolveProfileTool.java`:

```java
package gov.nist.oscal.tools.api.service.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.model.ProfileResolutionRequest;
import gov.nist.oscal.tools.api.model.ProfileResolutionResult;
import gov.nist.oscal.tools.api.service.ProfileResolutionService;
import org.springframework.stereotype.Component;

@Component
public class ResolveProfileTool implements Tool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ProfileResolutionService resolver;

    public ResolveProfileTool(ProfileResolutionService resolver) {
        this.resolver = resolver;
    }

    @Override public String name() { return "resolve_profile"; }
    @Override public String description() { return "Resolve an OSCAL Profile against its imported catalogs and return the resolved catalog."; }
    @Override public String inputSchemaJson() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"profileContent\":{\"type\":\"string\"},"
                + "\"format\":{\"type\":\"string\",\"enum\":[\"JSON\",\"XML\",\"YAML\"]}},"
                + "\"required\":[\"profileContent\",\"format\"]}";
    }

    @Override
    public ToolResult invoke(ToolCall call) {
        try {
            JsonNode args = MAPPER.readTree(call.argsJson());
            ProfileResolutionRequest req = new ProfileResolutionRequest();
            req.setProfileContent(args.get("profileContent").asText());
            req.setFormat(args.get("format").asText());
            ProfileResolutionResult result = resolver.resolveProfile(req, "ai");
            if (result.isSuccess()) {
                return ToolResult.ok("resolved", "{\"resolvedCatalog\":" + MAPPER.writeValueAsString(result.getResolvedCatalog()) + "}");
            }
            return ToolResult.error("resolve_profile failed: " + result.getMessage());
        } catch (Exception e) {
            return ToolResult.error("resolve_profile error: " + e.getMessage());
        }
    }
}
```

If the existing `ProfileResolutionService` / `ProfileResolutionRequest` field names differ from what's referenced above, the engineer should align them — the responsibility of this tool is unchanged.

- [ ] **Step 7: Implement `LookupControlTool` (stub-quality scaffold)**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/LookupControlTool.java`:

```java
package gov.nist.oscal.tools.api.service.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class LookupControlTool implements Tool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override public String name() { return "lookup_control"; }
    @Override public String description() {
        return "Look up a control by ID (e.g., 'ac-1') in a referenced catalog. Returns the control's statement, parts, and parameters.";
    }
    @Override public String inputSchemaJson() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"controlId\":{\"type\":\"string\"},"
                + "\"catalogRef\":{\"type\":\"string\",\"description\":\"e.g., 'NIST_SP-800-53_rev5'\"}},"
                + "\"required\":[\"controlId\",\"catalogRef\"]}";
    }

    @Override
    public ToolResult invoke(ToolCall call) {
        try {
            JsonNode args = MAPPER.readTree(call.argsJson());
            String controlId = args.get("controlId").asText();
            String catalogRef = args.get("catalogRef").asText();
            // TODO(post-foundation): bridge to the catalog cache / fetch_catalog. For foundation,
            // return a not-found marker so wizards can still call this without crashing.
            return ToolResult.error("lookup_control: catalog '" + catalogRef + "' not loaded; control " + controlId + " unavailable");
        } catch (Exception e) {
            return ToolResult.error("lookup_control error: " + e.getMessage());
        }
    }
}
```

This intentionally returns "not loaded" — the catalog cache lands in a follow-up plan (the Catalog wizard plan adds NIST 800-53r5 + FedRAMP baselines). For foundation, the tool exists, is registered, and fails gracefully.

- [ ] **Step 8: Implement `FetchCatalogTool` (same stub strategy)**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/FetchCatalogTool.java`:

```java
package gov.nist.oscal.tools.api.service.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class FetchCatalogTool implements Tool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override public String name() { return "fetch_catalog"; }
    @Override public String description() {
        return "Fetch a referenced OSCAL catalog (e.g., NIST_SP-800-53_rev5) for grounded reasoning. "
                + "Foundation release: returns a not-loaded marker; populated by per-wizard plans.";
    }
    @Override public String inputSchemaJson() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"ref\":{\"type\":\"string\"}},"
                + "\"required\":[\"ref\"]}";
    }

    @Override
    public ToolResult invoke(ToolCall call) {
        try {
            JsonNode args = MAPPER.readTree(call.argsJson());
            String ref = args.get("ref").asText();
            return ToolResult.error("fetch_catalog: '" + ref + "' not loaded in foundation release");
        } catch (Exception e) {
            return ToolResult.error("fetch_catalog error: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 9: Implement `ReadCurrentDocSectionTool`**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/ReadCurrentDocSectionTool.java`:

```java
package gov.nist.oscal.tools.api.service.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ReadCurrentDocSectionTool implements Tool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override public String name() { return "read_current_document_section"; }
    @Override public String description() {
        return "Builder Author Assist only: read a JSONPath section of the document the user is editing.";
    }
    @Override public String inputSchemaJson() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"jsonPath\":{\"type\":\"string\"}},"
                + "\"required\":[\"jsonPath\"]}";
    }

    @Override
    public ToolResult invoke(ToolCall call) {
        try {
            JsonNode args = MAPPER.readTree(call.argsJson());
            String path = args.get("jsonPath").asText();
            // The Author Assist plan wires this to the current builder doc via a request-scoped bean.
            // For foundation, return empty so wizards can register the tool without a wired source.
            return ToolResult.ok("no document context attached", "{\"path\":\"" + path + "\",\"value\":null}");
        } catch (Exception e) {
            return ToolResult.error("read_current_document_section error: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 10: Run all tool tests**

Run: `cd back-end && mvn -q -Dtest='*Tool*Test' test`
Expected: PASS for `ValidateOscalToolTest`, `ConvertFormatToolTest`, `OscalToolBoxTest`. The four stub tools have no unit tests (they're trivial constants for foundation); they get tests in the wizard plans that actually use them.

- [ ] **Step 11: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/tools/ \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/tools/
git commit -m "feat(ai): six in-process tools wrapping liboscal-java services"
```

---

### Task B5: `SourceIngestor` — normalize PDF / URL / .docx / paste / OSCAL inputs

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/SourceIngestor.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/IngestedSource.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/SourceIngestorTest.java`
- Test fixture: `back-end/src/test/resources/ai-fixtures/sample.docx`
- Test fixture: `back-end/src/test/resources/ai-fixtures/sample.pdf`

- [ ] **Step 1: Add a tiny .docx fixture (1-2 lines of body text)**

Either commit a hand-made fixture, or generate one in the test using POI's `XWPFDocument`. Generating in-test is simpler and avoids binary churn:

- [ ] **Step 2: Write the test (uses POI to generate a docx in memory and pass it to the ingestor)**

```java
package gov.nist.oscal.tools.api.service.ai;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class SourceIngestorTest {

    private final SourceIngestor ingestor = new SourceIngestor();

    @Test
    void plainTextPassesThrough() {
        IngestedSource s = ingestor.ingestText("Hello world");
        assertThat(s.text()).isEqualTo("Hello world");
        assertThat(s.kind()).isEqualTo(IngestedSource.Kind.TEXT);
    }

    @Test
    void docxIsExtractedToText() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XWPFDocument doc = new XWPFDocument()) {
            doc.createParagraph().createRun().setText("Document body for test.");
            doc.write(bos);
        }
        IngestedSource s = ingestor.ingestDocx(bos.toByteArray());
        assertThat(s.text()).contains("Document body for test");
        assertThat(s.kind()).isEqualTo(IngestedSource.Kind.TEXT);
    }

    @Test
    void pdfIsKeptAsBytes() {
        IngestedSource s = ingestor.ingestPdf("name.pdf", new byte[]{1, 2, 3});
        assertThat(s.kind()).isEqualTo(IngestedSource.Kind.PDF);
        assertThat(s.pdfBytes()).hasSize(3);
    }

    @Test
    void rejectsOversizedPdf() {
        byte[] big = new byte[60 * 1024 * 1024]; // 60 MB
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ingestor.ingestPdf("big.pdf", big))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds");
    }
}
```

URL fetching is exercised by an integration test inside the smoke wizard (Task C4) that hits a known stable URL, gated by an env var so CI doesn't depend on the network.

- [ ] **Step 3: Implement `IngestedSource`**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/IngestedSource.java`:

```java
package gov.nist.oscal.tools.api.service.ai;

public record IngestedSource(
        Kind kind,
        String filename,
        String text,
        byte[] pdfBytes,
        long sizeBytes
) {
    public enum Kind { TEXT, PDF }
}
```

- [ ] **Step 4: Implement `SourceIngestor`**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/SourceIngestor.java`:

```java
package gov.nist.oscal.tools.api.service.ai;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.net.URI;

@Service
public class SourceIngestor {

    private static final Logger log = LoggerFactory.getLogger(SourceIngestor.class);
    private static final long MAX_PDF_BYTES = 32L * 1024 * 1024;
    private static final long MAX_TEXT_CHARS = 1_500_000;

    private final RestTemplate restTemplate = new RestTemplate();

    public IngestedSource ingestText(String text) {
        if (text == null) text = "";
        if (text.length() > MAX_TEXT_CHARS) {
            throw new IllegalArgumentException("Text input exceeds " + MAX_TEXT_CHARS + " chars");
        }
        return new IngestedSource(IngestedSource.Kind.TEXT, null, text, null, text.length());
    }

    public IngestedSource ingestPdf(String filename, byte[] bytes) {
        if (bytes == null) throw new IllegalArgumentException("Empty PDF");
        if (bytes.length > MAX_PDF_BYTES) {
            throw new IllegalArgumentException("PDF size " + bytes.length + " exceeds " + MAX_PDF_BYTES);
        }
        return new IngestedSource(IngestedSource.Kind.PDF, filename, null, bytes, bytes.length);
    }

    public IngestedSource ingestDocx(byte[] bytes) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            String text = extractor.getText();
            return ingestText(text);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read .docx: " + e.getMessage(), e);
        }
    }

    public IngestedSource ingestUrl(String url) {
        try {
            String body = restTemplate.getForObject(URI.create(url), String.class);
            // Best-effort HTML→text strip. For richer extraction, a follow-up plan can swap in jsoup.
            String text = body == null ? "" : body.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
            return ingestText(text);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to fetch URL " + url + ": " + e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 5: Run test**

Run: `cd back-end && mvn -q -Dtest=SourceIngestorTest test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/SourceIngestor.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/IngestedSource.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/SourceIngestorTest.java
git commit -m "feat(ai): SourceIngestor for PDF, .docx, URL, and plain text inputs"
```

---

### Task B6: SSE event types and emitter pool

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/stream/SessionEvent.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/stream/AiSessionEventStream.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/stream/AiSessionEventStreamTest.java`

- [ ] **Step 1: Write the test**

```java
package gov.nist.oscal.tools.api.service.ai.stream;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AiSessionEventStreamTest {

    @Test
    void emitsEventsToSubscriber() throws Exception {
        AiSessionEventStream stream = new AiSessionEventStream();
        UUID id = UUID.randomUUID();
        SseEmitter emitter = stream.subscribe(id);
        AtomicInteger count = new AtomicInteger();
        emitter.onCompletion(count::incrementAndGet);

        stream.publish(id, SessionEvent.progress("starting"));
        stream.publish(id, SessionEvent.complete("{\"hello\":\"world\"}"));
        stream.close(id);

        assertThat(count.get()).isEqualTo(1);
    }

    @Test
    void publishWithoutSubscriberDoesNotThrow() {
        AiSessionEventStream stream = new AiSessionEventStream();
        stream.publish(UUID.randomUUID(), SessionEvent.progress("nobody home"));
    }
}
```

- [ ] **Step 2: Implement `SessionEvent`**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/stream/SessionEvent.java`:

```java
package gov.nist.oscal.tools.api.service.ai.stream;

public record SessionEvent(Type type, String dataJson) {
    public enum Type {
        SESSION_STARTED,
        PROGRESS,
        TOOL_CALL,
        TOOL_RESULT,
        AWAITING_INPUT,
        CHUNK,
        PARTIAL_DOCUMENT,
        COMPLETE,
        ERROR
    }

    public static SessionEvent progress(String message) {
        return new SessionEvent(Type.PROGRESS, "{\"message\":\"" + escape(message) + "\"}");
    }
    public static SessionEvent complete(String documentJson) {
        return new SessionEvent(Type.COMPLETE, "{\"document\":" + documentJson + "}");
    }
    public static SessionEvent error(String code, String message) {
        return new SessionEvent(Type.ERROR,
                "{\"code\":\"" + escape(code) + "\",\"message\":\"" + escape(message) + "\"}");
    }
    public static SessionEvent chunk(String text) {
        return new SessionEvent(Type.CHUNK, "{\"text\":\"" + escape(text) + "\"}");
    }
    public static SessionEvent toolCall(String tool, String argsSummary) {
        return new SessionEvent(Type.TOOL_CALL,
                "{\"tool\":\"" + escape(tool) + "\",\"args\":\"" + escape(argsSummary) + "\"}");
    }
    public static SessionEvent toolResult(String tool, boolean ok, String summary) {
        return new SessionEvent(Type.TOOL_RESULT,
                "{\"tool\":\"" + escape(tool) + "\",\"ok\":" + ok + ",\"summary\":\"" + escape(summary) + "\"}");
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
```

- [ ] **Step 3: Implement `AiSessionEventStream`**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/stream/AiSessionEventStream.java`:

```java
package gov.nist.oscal.tools.api.service.ai.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiSessionEventStream {

    private static final Logger log = LoggerFactory.getLogger(AiSessionEventStream.class);
    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000; // 30 min

    private final ConcurrentHashMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID sessionId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitter.onCompletion(() -> emitters.remove(sessionId));
        emitter.onTimeout(() -> emitters.remove(sessionId));
        emitter.onError(e -> emitters.remove(sessionId));
        emitters.put(sessionId, emitter);
        return emitter;
    }

    public void publish(UUID sessionId, SessionEvent event) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                    .name(event.type().name().toLowerCase())
                    .data(event.dataJson()));
        } catch (IOException e) {
            log.warn("SSE send failed for session {}: {}", sessionId, e.toString());
            emitters.remove(sessionId);
        }
    }

    public void close(UUID sessionId) {
        SseEmitter emitter = emitters.remove(sessionId);
        if (emitter != null) emitter.complete();
    }
}
```

- [ ] **Step 4: Run test**

Run: `cd back-end && mvn -q -Dtest=AiSessionEventStreamTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/stream/ \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/stream/
git commit -m "feat(ai): SSE event types and per-session emitter pool"
```

---

## Phase C — Orchestrator + smoke wizard + controller

### Task C1: Wizard interface and `SmokeWizard`

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/Wizard.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/WizardContext.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/SmokeWizard.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/SmokeWizardTest.java`

- [ ] **Step 1: Write the test**

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.service.ai.AnthropicCall;
import gov.nist.oscal.tools.api.service.ai.AnthropicClient;
import gov.nist.oscal.tools.api.service.ai.AnthropicResult;
import gov.nist.oscal.tools.api.service.ai.KnowledgeLoader;
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import gov.nist.oscal.tools.api.service.ai.stream.SessionEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SmokeWizardTest {

    @Test
    void runsHelloWorldAndPublishesProgressAndComplete() {
        AnthropicClient client = mock(AnthropicClient.class);
        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        KnowledgeLoader loader = mock(KnowledgeLoader.class);
        when(loader.systemFor(WizardKind.SMOKE)).thenReturn("system");
        when(client.send(eq("sk-ant-test-key-1234567890"), any(AnthropicCall.class)))
                .thenReturn(new AnthropicResult("hello back", 5, 3));

        SmokeWizard wizard = new SmokeWizard(client, stream, loader);
        WizardContext ctx = new WizardContext(UUID.randomUUID(), 1L, 7L,
                "sk-ant-test-key-1234567890", "claude-opus-4-7", "ping me");

        WizardOutcome outcome = wizard.run(ctx);

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.tokensIn()).isEqualTo(5);
        assertThat(outcome.tokensOut()).isEqualTo(3);
        verify(stream).publish(eq(ctx.sessionId()), any(SessionEvent.class));
    }
}
```

- [ ] **Step 2: Implement `WizardContext` and `WizardOutcome`**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/WizardContext.java`:

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

import java.util.UUID;

public record WizardContext(
        UUID sessionId,
        Long organizationId,
        Long userId,
        String apiKey,
        String model,
        String input
) { }
```

In the same package, `WizardOutcome.java`:

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

public record WizardOutcome(boolean success, int tokensIn, int tokensOut, String errorCode, String errorMessage) {
    public static WizardOutcome ok(int in, int out) { return new WizardOutcome(true, in, out, null, null); }
    public static WizardOutcome failed(String code, String msg) { return new WizardOutcome(false, 0, 0, code, msg); }
}
```

- [ ] **Step 3: Implement `Wizard` interface**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/Wizard.java`:

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

import gov.nist.oscal.tools.api.entity.WizardKind;

public interface Wizard {
    WizardKind kind();
    WizardOutcome run(WizardContext ctx);
}
```

- [ ] **Step 4: Implement `SmokeWizard`**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/SmokeWizard.java`:

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.service.ai.AnthropicCall;
import gov.nist.oscal.tools.api.service.ai.AnthropicClient;
import gov.nist.oscal.tools.api.service.ai.AnthropicResult;
import gov.nist.oscal.tools.api.service.ai.KnowledgeLoader;
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import gov.nist.oscal.tools.api.service.ai.stream.SessionEvent;
import org.springframework.stereotype.Component;

@Component
public class SmokeWizard implements Wizard {

    private final AnthropicClient client;
    private final AiSessionEventStream stream;
    private final KnowledgeLoader knowledge;

    public SmokeWizard(AnthropicClient client, AiSessionEventStream stream, KnowledgeLoader knowledge) {
        this.client = client;
        this.stream = stream;
        this.knowledge = knowledge;
    }

    @Override public WizardKind kind() { return WizardKind.SMOKE; }

    @Override
    public WizardOutcome run(WizardContext ctx) {
        stream.publish(ctx.sessionId(), SessionEvent.progress("Calling Claude…"));
        try {
            AnthropicCall call = AnthropicCall.builder()
                    .model(ctx.model())
                    .systemPrompt(knowledge.systemFor(WizardKind.SMOKE))
                    .userMessage(ctx.input() == null ? "Say hello back." : ctx.input())
                    .maxTokens(256)
                    .build();
            AnthropicResult result = client.send(ctx.apiKey(), call);
            stream.publish(ctx.sessionId(), SessionEvent.chunk(result.text()));
            stream.publish(ctx.sessionId(), SessionEvent.complete(
                    "{\"reply\":" + JsonStrings.quote(result.text()) + "}"));
            return WizardOutcome.ok(result.tokensIn(), result.tokensOut());
        } catch (IllegalArgumentException e) {
            stream.publish(ctx.sessionId(), SessionEvent.error("auth_failed", e.getMessage()));
            return WizardOutcome.failed("auth_failed", e.getMessage());
        } catch (Exception e) {
            stream.publish(ctx.sessionId(), SessionEvent.error("model_error", e.getMessage()));
            return WizardOutcome.failed("model_error", e.getMessage());
        }
    }
}
```

In the same package, `JsonStrings.java`:

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

final class JsonStrings {
    private JsonStrings() { }
    static String quote(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
```

- [ ] **Step 5: Run test**

Run: `cd back-end && mvn -q -Dtest=SmokeWizardTest test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/ \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/
git commit -m "feat(ai): Wizard contract + SmokeWizard end-to-end skeleton"
```

---

### Task C2: `WizardRouter` and `AiOrchestrator`

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/WizardRouter.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AiOrchestrator.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/AiOrchestratorTest.java`

- [ ] **Step 1: Write the test**

```java
package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.AiSession;
import gov.nist.oscal.tools.api.entity.AiSessionMode;
import gov.nist.oscal.tools.api.entity.AiSessionStatus;
import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.repository.AiSessionRepository;
import gov.nist.oscal.tools.api.service.ai.wizard.SmokeWizard;
import gov.nist.oscal.tools.api.service.ai.wizard.WizardContext;
import gov.nist.oscal.tools.api.service.ai.wizard.WizardOutcome;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class AiOrchestratorTest {

    @Test
    void persistsSessionAndRunsWizard() {
        SmokeWizard smoke = mock(SmokeWizard.class);
        when(smoke.kind()).thenReturn(WizardKind.SMOKE);
        when(smoke.run(any())).thenReturn(WizardOutcome.ok(7, 4));

        AiSessionRepository sessions = mock(AiSessionRepository.class);
        when(sessions.save(any(AiSession.class))).thenAnswer(inv -> inv.getArgument(0));

        AiSettingsServiceFacade settings = mock(AiSettingsServiceFacade.class);
        when(settings.requireApiKey(1L)).thenReturn("sk-ant-test-1234567890");
        when(settings.getDefaultModel(1L)).thenReturn("claude-opus-4-7");

        WizardRouter router = new WizardRouter(List.of(smoke));
        AiOrchestrator orch = new AiOrchestrator(sessions, settings, router);

        UUID id = orch.start(1L, 7L, WizardKind.SMOKE, AiSessionMode.STREAMING, "ping");

        ArgumentCaptor<AiSession> captor = ArgumentCaptor.forClass(AiSession.class);
        verify(sessions, atLeast(2)).save(captor.capture());
        AiSession last = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(last.getStatus()).isEqualTo(AiSessionStatus.COMPLETED);
        assertThat(last.getTokensIn()).isEqualTo(7);
        assertThat(last.getTokensOut()).isEqualTo(4);
        assertThat(id).isNotNull();
    }
}
```

- [ ] **Step 2: Add a thin facade interface (so the orchestrator's tests don't need the full settings stack)**

Add `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AiSettingsServiceFacade.java`:

```java
package gov.nist.oscal.tools.api.service.ai;

public interface AiSettingsServiceFacade {
    String requireApiKey(Long organizationId);
    String getDefaultModel(Long organizationId);
}
```

Make `AiSettingsService` (Phase A) implement this — add `implements AiSettingsServiceFacade` to its declaration and add `@Override` `requireApiKey` returning `getDecryptedKey(organizationId)`. (`getDefaultModel` already exists.)

- [ ] **Step 3: Implement `WizardRouter`**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/WizardRouter.java`:

```java
package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.service.ai.wizard.Wizard;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WizardRouter {
    private final Map<WizardKind, Wizard> byKind;

    public WizardRouter(List<Wizard> wizards) {
        this.byKind = wizards.stream().collect(Collectors.toMap(Wizard::kind, w -> w));
    }

    public Wizard get(WizardKind kind) {
        Wizard w = byKind.get(kind);
        if (w == null) throw new IllegalArgumentException("No wizard registered for " + kind);
        return w;
    }
}
```

- [ ] **Step 4: Implement `AiOrchestrator`**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AiOrchestrator.java`:

```java
package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.AiSession;
import gov.nist.oscal.tools.api.entity.AiSessionMode;
import gov.nist.oscal.tools.api.entity.AiSessionStatus;
import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.repository.AiSessionRepository;
import gov.nist.oscal.tools.api.service.ai.wizard.Wizard;
import gov.nist.oscal.tools.api.service.ai.wizard.WizardContext;
import gov.nist.oscal.tools.api.service.ai.wizard.WizardOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AiOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AiOrchestrator.class);

    private final AiSessionRepository sessions;
    private final AiSettingsServiceFacade settings;
    private final WizardRouter router;

    public AiOrchestrator(AiSessionRepository sessions, AiSettingsServiceFacade settings, WizardRouter router) {
        this.sessions = sessions;
        this.settings = settings;
        this.router = router;
    }

    public UUID start(Long organizationId, Long userId, WizardKind kind, AiSessionMode mode, String input) {
        UUID id = UUID.randomUUID();
        String apiKey = settings.requireApiKey(organizationId);
        String model = settings.getDefaultModel(organizationId);

        AiSession session = new AiSession();
        session.setId(id);
        session.setOrganizationId(organizationId);
        session.setUserId(userId);
        session.setWizardKind(kind);
        session.setMode(mode);
        session.setModel(model);
        session.setStatus(AiSessionStatus.RUNNING);
        session.setStartedAt(LocalDateTime.now());
        sessions.save(session);

        Wizard wizard = router.get(kind);
        runAsync(wizard, new WizardContext(id, organizationId, userId, apiKey, model, input));
        return id;
    }

    @Async
    public void runAsync(Wizard wizard, WizardContext ctx) {
        AiSession session = sessions.findById(ctx.sessionId()).orElseThrow();
        try {
            WizardOutcome outcome = wizard.run(ctx);
            session.setTokensIn(outcome.tokensIn());
            session.setTokensOut(outcome.tokensOut());
            session.setEndedAt(LocalDateTime.now());
            session.setStatus(outcome.success() ? AiSessionStatus.COMPLETED : AiSessionStatus.FAILED);
            session.setErrorCode(outcome.errorCode());
            session.setErrorMessage(outcome.errorMessage());
        } catch (Exception e) {
            log.error("Wizard run failed", e);
            session.setStatus(AiSessionStatus.FAILED);
            session.setErrorCode("orchestrator_error");
            session.setErrorMessage(e.getMessage());
            session.setEndedAt(LocalDateTime.now());
        }
        sessions.save(session);
    }
}
```

- [ ] **Step 5: Enable `@Async` if not already**

Confirm there is `@EnableAsync` somewhere on a `@Configuration` class. If not, add it to `back-end/src/main/java/gov/nist/oscal/tools/api/OscalCliApiApplication.java` (annotate the class with `@EnableAsync`).

- [ ] **Step 6: Run test**

Run: `cd back-end && mvn -q -Dtest=AiOrchestratorTest test`
Expected: PASS. (The test bypasses `@Async` because we call `start()` which then calls `runAsync` synchronously when `@Async` proxies aren't applied in the unit test. If the test fails because `runAsync` is invoked through Spring's proxy in some Spring slice, refactor `start()` to call a private synchronous helper and put `@Async` on a public-facing wrapper.)

- [ ] **Step 7: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/WizardRouter.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AiOrchestrator.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AiSettingsServiceFacade.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AiSettingsService.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/AiOrchestratorTest.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/OscalCliApiApplication.java
git commit -m "feat(ai): WizardRouter + AiOrchestrator with async session lifecycle"
```

---

### Task C3: `AiSessionController` — start, stream, cancel

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AiSessionController.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/StartSessionRequest.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/StartSessionResponse.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/controller/AiSessionControllerTest.java`

- [ ] **Step 1: Write the controller test**

```java
package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.AiSessionMode;
import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.model.ai.StartSessionRequest;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.ai.AiOrchestrator;
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiSessionController.class)
class AiSessionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private AiOrchestrator orchestrator;
    @MockitoBean private AiSessionEventStream stream;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void startSessionReturnsSessionId() throws Exception {
        UUID id = UUID.randomUUID();
        when(orchestrator.start(eq(1L), anyLong(), eq(WizardKind.SMOKE), eq(AiSessionMode.STREAMING), eq("ping")))
                .thenReturn(id);

        StartSessionRequest req = new StartSessionRequest();
        req.setOrganizationId(1L);
        req.setWizardKind(WizardKind.SMOKE);
        req.setMode(AiSessionMode.STREAMING);
        req.setInput("ping");

        mockMvc.perform(post("/api/ai/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(id.toString()));
    }
}
```

The user-id resolution from the authenticated principal is left for the engineer to wire to the existing `UserService` pattern used elsewhere in the codebase (see `AuthService` for the conventional lookup-by-username flow). The test stubs `anyLong()` for that reason.

- [ ] **Step 2: Implement DTOs**

`back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/StartSessionRequest.java`:

```java
package gov.nist.oscal.tools.api.model.ai;

import gov.nist.oscal.tools.api.entity.AiSessionMode;
import gov.nist.oscal.tools.api.entity.WizardKind;
import jakarta.validation.constraints.NotNull;

public class StartSessionRequest {
    @NotNull private Long organizationId;
    @NotNull private WizardKind wizardKind;
    @NotNull private AiSessionMode mode = AiSessionMode.STREAMING;
    private String input;

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public WizardKind getWizardKind() { return wizardKind; }
    public void setWizardKind(WizardKind wizardKind) { this.wizardKind = wizardKind; }
    public AiSessionMode getMode() { return mode; }
    public void setMode(AiSessionMode mode) { this.mode = mode; }
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
}
```

`back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/StartSessionResponse.java`:

```java
package gov.nist.oscal.tools.api.model.ai;

import java.util.UUID;

public record StartSessionResponse(UUID sessionId) { }
```

- [ ] **Step 3: Implement the controller**

`back-end/src/main/java/gov/nist/oscal/tools/api/controller/AiSessionController.java`:

```java
package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.ai.StartSessionRequest;
import gov.nist.oscal.tools.api.model.ai.StartSessionResponse;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.ai.AiOrchestrator;
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai/sessions")
@Tag(name = "AI Sessions", description = "Run an AI wizard and stream progress")
public class AiSessionController {

    private final AiOrchestrator orchestrator;
    private final AiSessionEventStream stream;
    private final UserRepository users;

    public AiSessionController(AiOrchestrator orchestrator, AiSessionEventStream stream, UserRepository users) {
        this.orchestrator = orchestrator;
        this.stream = stream;
        this.users = users;
    }

    @Operation(summary = "Start an AI wizard session")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<StartSessionResponse> start(@Valid @RequestBody StartSessionRequest req) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = users.findByUsername(username).orElseThrow();
        UUID id = orchestrator.start(req.getOrganizationId(), user.getId(),
                req.getWizardKind(), req.getMode(), req.getInput());
        return ResponseEntity.ok(new StartSessionResponse(id));
    }

    @Operation(summary = "Subscribe to a session's progress stream")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamFor(@PathVariable UUID id) {
        return stream.subscribe(id);
    }

    @Operation(summary = "Cancel a running session")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        stream.close(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Run test**

Run: `cd back-end && mvn -q -Dtest=AiSessionControllerTest test`
Expected: PASS. The `users.findByUsername` call will need a `@MockitoBean UserRepository` added to the test — add it and stub `findByUsername` to return a populated `User` so the mocked id flows through.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/controller/AiSessionController.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/StartSessionRequest.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/StartSessionResponse.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/controller/AiSessionControllerTest.java
git commit -m "feat(ai): AiSessionController — start, stream (SSE), cancel"
```

---

### Task C4: End-to-end smoke wizard integration test

**Files:**
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/integration/AiSmokeWizardIntegrationTest.java`

This test is `@DisabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = "^$")` so CI skips it; running locally with the env var set drives a real call. It validates: settings → key load → SDK call → SSE relay → DB persistence.

- [ ] **Step 1: Write the test**

```java
package gov.nist.oscal.tools.api.integration;

import gov.nist.oscal.tools.api.entity.AiSession;
import gov.nist.oscal.tools.api.entity.AiSessionMode;
import gov.nist.oscal.tools.api.entity.AiSessionStatus;
import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.repository.AiSessionRepository;
import gov.nist.oscal.tools.api.service.ai.AiOrchestrator;
import gov.nist.oscal.tools.api.service.ai.AiSettingsServiceFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = "^$|^null$")
class AiSmokeWizardIntegrationTest {

    @Autowired private AiOrchestrator orchestrator;
    @Autowired private AiSessionRepository sessions;
    @MockitoBean private AiSettingsServiceFacade settings;

    @Test
    void smokeWizardCallsAnthropicAndPersistsCompletion() {
        String key = System.getenv("ANTHROPIC_API_KEY");
        when(settings.requireApiKey(1L)).thenReturn(key);
        when(settings.getDefaultModel(1L)).thenReturn("claude-haiku-4-5-20251001");

        UUID id = orchestrator.start(1L, 1L, WizardKind.SMOKE, AiSessionMode.STREAMING, "Reply with the single word OK.");

        await().atMost(60, SECONDS).until(() ->
                sessions.findById(id).map(s -> s.getStatus() == AiSessionStatus.COMPLETED).orElse(false));
        AiSession s = sessions.findById(id).orElseThrow();
        assertThat(s.getTokensIn()).isPositive();
        assertThat(s.getTokensOut()).isPositive();
    }
}
```

Add a test dep on `org.awaitility:awaitility` if not already present (most Spring Boot test setups include it via parent — verify with `mvn dependency:tree | grep awaitility`).

- [ ] **Step 2: Run with env var to verify**

Run (only when you have a developer key handy):

```bash
cd back-end && ANTHROPIC_API_KEY=sk-ant-... mvn -q -Dtest=AiSmokeWizardIntegrationTest test
```

Expected: PASS. Without the env var, the test is auto-skipped.

- [ ] **Step 3: Commit**

```bash
git add back-end/src/test/java/gov/nist/oscal/tools/api/integration/AiSmokeWizardIntegrationTest.java
git commit -m "test(ai): end-to-end smoke wizard integration test (gated on ANTHROPIC_API_KEY)"
```

---

## Phase D — Frontend

### Task D1: API client — settings + sessions

**Files:**
- Modify: `front-end/src/lib/api-client.ts`
- Create: `front-end/src/lib/ai-client.ts`
- Test: `front-end/src/lib/ai-client.test.ts`

- [ ] **Step 1: Write the test**

```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { aiClient } from './ai-client';

const fetchMock = vi.fn();
beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal('fetch', fetchMock);
  vi.stubGlobal('localStorage', {
    getItem: () => 'fake-token',
    setItem: vi.fn(),
    removeItem: vi.fn(),
  });
});

describe('aiClient.getSettingsStatus', () => {
  it('GETs /api/ai/settings/status with org id', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ enabled: true }),
    });
    const result = await aiClient.getSettingsStatus(7);
    expect(result.enabled).toBe(true);
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/ai/settings/status?organizationId=7'),
      expect.objectContaining({ method: 'GET' }),
    );
  });
});

describe('aiClient.startSession', () => {
  it('POSTs to /api/ai/sessions', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ sessionId: 'abc-123' }),
    });
    const result = await aiClient.startSession({
      organizationId: 1,
      wizardKind: 'SMOKE',
      mode: 'STREAMING',
      input: 'ping',
    });
    expect(result.sessionId).toBe('abc-123');
  });
});
```

- [ ] **Step 2: Implement `ai-client.ts`**

`front-end/src/lib/ai-client.ts`:

```typescript
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8090/api';

function authHeaders(): Record<string, string> {
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
  const h: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) h.Authorization = `Bearer ${token}`;
  return h;
}

export type WizardKind =
  | 'SMOKE'
  | 'CATALOG'
  | 'PROFILE'
  | 'COMPONENT_DEF'
  | 'SSP'
  | 'POAM'
  | 'BUILDER_ASSIST';

export type SessionMode = 'STREAMING' | 'THOROUGH';

export interface StartSessionRequest {
  organizationId: number;
  wizardKind: WizardKind;
  mode: SessionMode;
  input?: string;
}

export interface StartSessionResponse {
  sessionId: string;
}

export interface AiSettingsResponse {
  enabled: boolean;
  fingerprint: string | null;
  defaultModel: string;
}

export interface UpdateAiSettingsRequest {
  apiKey: string;
  defaultModel?: string;
}

export const aiClient = {
  async getSettingsStatus(organizationId: number): Promise<{ enabled: boolean }> {
    const res = await fetch(`${API_BASE_URL}/ai/settings/status?organizationId=${organizationId}`, {
      method: 'GET',
      headers: authHeaders(),
    });
    if (!res.ok) throw new Error(`status ${res.status}`);
    return res.json();
  },

  async getSettings(organizationId: number): Promise<AiSettingsResponse> {
    const res = await fetch(`${API_BASE_URL}/ai/settings?organizationId=${organizationId}`, {
      method: 'GET',
      headers: authHeaders(),
    });
    if (!res.ok) throw new Error(`status ${res.status}`);
    return res.json();
  },

  async putSettings(organizationId: number, req: UpdateAiSettingsRequest): Promise<AiSettingsResponse> {
    const res = await fetch(`${API_BASE_URL}/ai/settings?organizationId=${organizationId}`, {
      method: 'PUT',
      headers: authHeaders(),
      body: JSON.stringify(req),
    });
    if (!res.ok) throw new Error(`status ${res.status}`);
    return res.json();
  },

  async disable(organizationId: number): Promise<void> {
    const res = await fetch(`${API_BASE_URL}/ai/settings?organizationId=${organizationId}`, {
      method: 'DELETE',
      headers: authHeaders(),
    });
    if (!res.ok) throw new Error(`status ${res.status}`);
  },

  async startSession(req: StartSessionRequest): Promise<StartSessionResponse> {
    const res = await fetch(`${API_BASE_URL}/ai/sessions`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify(req),
    });
    if (!res.ok) throw new Error(`status ${res.status}`);
    return res.json();
  },

  async cancelSession(sessionId: string): Promise<void> {
    const res = await fetch(`${API_BASE_URL}/ai/sessions/${sessionId}`, {
      method: 'DELETE',
      headers: authHeaders(),
    });
    if (!res.ok) throw new Error(`status ${res.status}`);
  },
};
```

- [ ] **Step 3: Run test**

Run: `cd front-end && npx vitest run src/lib/ai-client.test.ts`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add front-end/src/lib/ai-client.ts front-end/src/lib/ai-client.test.ts
git commit -m "feat(ai/fe): API client for settings, sessions, cancellation"
```

---

### Task D2: `useAiSession` hook — SSE consumption

**Files:**
- Create: `front-end/src/hooks/useAiSession.ts`
- Test: `front-end/src/hooks/useAiSession.test.tsx`

- [ ] **Step 1: Implement the hook**

`front-end/src/hooks/useAiSession.ts`:

```typescript
'use client';
import { useCallback, useEffect, useRef, useState } from 'react';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8090/api';

export type AiEventType =
  | 'session_started'
  | 'progress'
  | 'tool_call'
  | 'tool_result'
  | 'awaiting_input'
  | 'chunk'
  | 'partial_document'
  | 'complete'
  | 'error';

export interface AiEvent {
  type: AiEventType;
  data: Record<string, unknown>;
}

export interface UseAiSessionState {
  events: AiEvent[];
  isComplete: boolean;
  error: string | null;
  finalDocument: unknown | null;
  cancel: () => void;
}

export function useAiSession(sessionId: string | null): UseAiSessionState {
  const [events, setEvents] = useState<AiEvent[]>([]);
  const [isComplete, setIsComplete] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [finalDocument, setFinalDocument] = useState<unknown | null>(null);
  const sourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    if (!sessionId) return;
    const url = `${API_BASE_URL}/ai/sessions/${sessionId}/stream`;
    const source = new EventSource(url, { withCredentials: false });
    sourceRef.current = source;

    const handle = (type: AiEventType) => (e: MessageEvent) => {
      try {
        const data = JSON.parse(e.data);
        setEvents((prev) => [...prev, { type, data }]);
        if (type === 'complete') {
          setFinalDocument((data as { document?: unknown }).document ?? null);
          setIsComplete(true);
          source.close();
        }
        if (type === 'error') {
          setError((data as { message?: string }).message ?? 'Unknown error');
          setIsComplete(true);
          source.close();
        }
      } catch (err) {
        console.error('Failed to parse SSE event', err);
      }
    };

    const types: AiEventType[] = [
      'session_started', 'progress', 'tool_call', 'tool_result',
      'awaiting_input', 'chunk', 'partial_document', 'complete', 'error',
    ];
    types.forEach((t) => source.addEventListener(t, handle(t)));

    source.onerror = () => {
      setError('Stream interrupted');
      source.close();
    };

    return () => {
      source.close();
    };
  }, [sessionId]);

  const cancel = useCallback(() => {
    sourceRef.current?.close();
    if (sessionId) {
      fetch(`${API_BASE_URL}/ai/sessions/${sessionId}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
      }).catch(() => {});
    }
  }, [sessionId]);

  return { events, isComplete, error, finalDocument, cancel };
}
```

- [ ] **Step 2: Test the hook (basic state shape)**

`front-end/src/hooks/useAiSession.test.tsx`:

```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useAiSession } from './useAiSession';

class MockEventSource {
  static instances: MockEventSource[] = [];
  url: string;
  listeners: Record<string, ((e: MessageEvent) => void)[]> = {};
  closed = false;
  onerror: ((e: Event) => void) | null = null;
  constructor(url: string) {
    this.url = url;
    MockEventSource.instances.push(this);
  }
  addEventListener(type: string, fn: (e: MessageEvent) => void) {
    (this.listeners[type] ||= []).push(fn);
  }
  emit(type: string, data: unknown) {
    (this.listeners[type] || []).forEach((fn) =>
      fn(new MessageEvent(type, { data: JSON.stringify(data) })),
    );
  }
  close() { this.closed = true; }
}

beforeEach(() => {
  MockEventSource.instances = [];
  vi.stubGlobal('EventSource', MockEventSource as unknown as typeof EventSource);
});

describe('useAiSession', () => {
  it('starts empty and accumulates events', () => {
    const { result } = renderHook(() => useAiSession('abc'));
    expect(result.current.events).toEqual([]);
    act(() => MockEventSource.instances[0].emit('progress', { message: 'hi' }));
    expect(result.current.events).toHaveLength(1);
  });

  it('marks complete on complete event', () => {
    const { result } = renderHook(() => useAiSession('abc'));
    act(() => MockEventSource.instances[0].emit('complete', { document: { ok: true } }));
    expect(result.current.isComplete).toBe(true);
    expect(result.current.finalDocument).toEqual({ ok: true });
  });
});
```

- [ ] **Step 3: Run test**

Run: `cd front-end && npx vitest run src/hooks/useAiSession.test.tsx`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add front-end/src/hooks/useAiSession.ts front-end/src/hooks/useAiSession.test.tsx
git commit -m "feat(ai/fe): useAiSession hook for SSE event consumption"
```

---

### Task D3: `AiFeatureGate` and capability hook

**Files:**
- Create: `front-end/src/hooks/useAiEnabled.ts`
- Create: `front-end/src/components/ai/AiFeatureGate.tsx`
- Test: `front-end/src/components/ai/AiFeatureGate.test.tsx`

- [ ] **Step 1: Write the hook**

`front-end/src/hooks/useAiEnabled.ts`:

```typescript
'use client';
import { useEffect, useState } from 'react';
import { aiClient } from '@/lib/ai-client';

export function useAiEnabled(organizationId: number | null): {
  enabled: boolean;
  loading: boolean;
} {
  const [enabled, setEnabled] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!organizationId) {
      setLoading(false);
      return;
    }
    let active = true;
    aiClient
      .getSettingsStatus(organizationId)
      .then((res) => {
        if (active) {
          setEnabled(res.enabled);
          setLoading(false);
        }
      })
      .catch(() => {
        if (active) {
          setEnabled(false);
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [organizationId]);

  return { enabled, loading };
}
```

- [ ] **Step 2: Write the gate component**

`front-end/src/components/ai/AiFeatureGate.tsx`:

```typescript
'use client';
import { ReactNode } from 'react';
import { useAiEnabled } from '@/hooks/useAiEnabled';

interface AiFeatureGateProps {
  organizationId: number | null;
  fallback?: ReactNode;
  children: ReactNode;
}

export function AiFeatureGate({ organizationId, fallback = null, children }: AiFeatureGateProps) {
  const { enabled, loading } = useAiEnabled(organizationId);
  if (loading) return null;
  if (!enabled) return <>{fallback}</>;
  return <>{children}</>;
}
```

- [ ] **Step 3: Test**

`front-end/src/components/ai/AiFeatureGate.test.tsx`:

```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { AiFeatureGate } from './AiFeatureGate';
import { aiClient } from '@/lib/ai-client';

vi.mock('@/lib/ai-client', () => ({
  aiClient: { getSettingsStatus: vi.fn() },
}));

describe('AiFeatureGate', () => {
  it('renders children when enabled', async () => {
    (aiClient.getSettingsStatus as unknown as { mockResolvedValue: (v: unknown) => void })
      .mockResolvedValue({ enabled: true });
    render(<AiFeatureGate organizationId={1}><div>secret</div></AiFeatureGate>);
    await waitFor(() => expect(screen.getByText('secret')).toBeInTheDocument());
  });

  it('renders fallback when disabled', async () => {
    (aiClient.getSettingsStatus as unknown as { mockResolvedValue: (v: unknown) => void })
      .mockResolvedValue({ enabled: false });
    render(
      <AiFeatureGate organizationId={1} fallback={<div>nope</div>}>
        <div>secret</div>
      </AiFeatureGate>,
    );
    await waitFor(() => expect(screen.getByText('nope')).toBeInTheDocument());
  });
});
```

- [ ] **Step 4: Run test**

Run: `cd front-end && npx vitest run src/components/ai/AiFeatureGate.test.tsx`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add front-end/src/hooks/useAiEnabled.ts \
        front-end/src/components/ai/AiFeatureGate.tsx \
        front-end/src/components/ai/AiFeatureGate.test.tsx
git commit -m "feat(ai/fe): capability hook + AiFeatureGate component"
```

---

### Task D4: AI Settings page (org admin)

**Files:**
- Create: `front-end/src/app/org-admin/ai-settings/page.tsx`

- [ ] **Step 1: Implement the page**

`front-end/src/app/org-admin/ai-settings/page.tsx`:

```typescript
'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { toast } from 'sonner';
import { aiClient, type AiSettingsResponse } from '@/lib/ai-client';

export default function AiSettingsPage() {
  const router = useRouter();
  const [orgId, setOrgId] = useState<number | null>(null);
  const [settings, setSettings] = useState<AiSettingsResponse | null>(null);
  const [apiKey, setApiKey] = useState('');
  const [model, setModel] = useState('claude-opus-4-7');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const stored = typeof window !== 'undefined' ? localStorage.getItem('user') : null;
    if (!stored) {
      router.push('/');
      return;
    }
    const userData = JSON.parse(stored) as { orgRole?: string; globalRole?: string; organizationId?: number };
    const isAdmin = userData.orgRole === 'ORG_ADMIN' || userData.globalRole === 'SUPER_ADMIN';
    if (!isAdmin || !userData.organizationId) {
      router.push('/');
      return;
    }
    setOrgId(userData.organizationId);
    aiClient.getSettings(userData.organizationId).then((s) => {
      setSettings(s);
      setModel(s.defaultModel);
      setLoading(false);
    });
  }, [router]);

  const onSave = async () => {
    if (!orgId || !apiKey) return;
    try {
      const next = await aiClient.putSettings(orgId, { apiKey, defaultModel: model });
      setSettings(next);
      setApiKey('');
      toast.success('AI features enabled. Key fingerprint saved.');
    } catch (err) {
      toast.error('Failed to save: ' + (err instanceof Error ? err.message : 'unknown'));
    }
  };

  const onDisable = async () => {
    if (!orgId) return;
    if (!confirm('Disable AI features and clear the stored key?')) return;
    try {
      await aiClient.disable(orgId);
      const refreshed = await aiClient.getSettings(orgId);
      setSettings(refreshed);
      toast.success('AI disabled and key cleared.');
    } catch (err) {
      toast.error('Failed to disable: ' + (err instanceof Error ? err.message : 'unknown'));
    }
  };

  if (loading) return <div className="p-8">Loading…</div>;

  return (
    <div className="container mx-auto py-8 max-w-2xl">
      <Card>
        <CardHeader>
          <CardTitle>AI Features</CardTitle>
          <CardDescription>
            Configure your organization's Anthropic API key to enable AI-assisted OSCAL authoring.
            Your key is encrypted at rest and never returned to the browser.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="rounded-md bg-muted p-4 text-sm">
            <strong>Status:</strong>{' '}
            {settings?.enabled ? `Enabled — fingerprint ${settings.fingerprint}` : 'Disabled'}
          </div>

          <div className="space-y-2">
            <Label htmlFor="api-key">Anthropic API Key</Label>
            <Input
              id="api-key"
              type="password"
              autoComplete="off"
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              placeholder="sk-ant-…"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="model">Default Model</Label>
            <Input id="model" value={model} onChange={(e) => setModel(e.target.value)} />
          </div>

          <div className="flex gap-2">
            <Button onClick={onSave} disabled={!apiKey}>Save</Button>
            {settings?.enabled && (
              <Button variant="destructive" onClick={onDisable}>Disable</Button>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
```

- [ ] **Step 2: Manually verify (no test for the page itself; the underlying `aiClient` is tested)**

After deploying locally: navigate to `http://localhost:3010/org-admin/ai-settings`, paste an API key, save, observe the fingerprint, then click Disable. Verify with `curl -H "Authorization: Bearer <jwt>" http://localhost:8090/api/ai/settings?organizationId=<id>` that the saved/disabled state matches.

- [ ] **Step 3: Commit**

```bash
git add front-end/src/app/org-admin/ai-settings/page.tsx
git commit -m "feat(ai/fe): org-admin AI settings page (key, model, enable/disable)"
```

---

### Task D5: AI Wizard picker shell + smoke wizard run page

**Files:**
- Create: `front-end/src/app/ai/wizard/page.tsx`
- Create: `front-end/src/app/ai/wizard/[kind]/page.tsx`

- [ ] **Step 1: Implement the wizard kind picker**

`front-end/src/app/ai/wizard/page.tsx`:

```typescript
'use client';
import Link from 'next/link';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { AiFeatureGate } from '@/components/ai/AiFeatureGate';
import { useEffect, useState } from 'react';

interface WizardOption {
  kind: string;
  title: string;
  description: string;
  available: boolean;
}

const OPTIONS: WizardOption[] = [
  { kind: 'SMOKE', title: 'Smoke Test', description: 'Diagnostic round-trip — confirms AI is reachable.', available: true },
  { kind: 'CATALOG', title: 'Build Catalog from PDF', description: 'Coming next plan.', available: false },
  { kind: 'COMPONENT_DEF', title: 'Build Component-definition', description: 'Coming next plan.', available: false },
  { kind: 'PROFILE', title: 'Build Profile', description: 'Coming later plan.', available: false },
  { kind: 'SSP', title: 'Draft SSP', description: 'Coming later plan.', available: false },
  { kind: 'POAM', title: 'Draft POA&M', description: 'Coming later plan.', available: false },
];

export default function WizardPickerPage() {
  const [orgId, setOrgId] = useState<number | null>(null);
  useEffect(() => {
    const stored = typeof window !== 'undefined' ? localStorage.getItem('user') : null;
    if (stored) setOrgId((JSON.parse(stored) as { organizationId?: number }).organizationId ?? null);
  }, []);

  return (
    <AiFeatureGate
      organizationId={orgId}
      fallback={<div className="p-8 text-muted-foreground">AI features are disabled. Ask your org admin to add an Anthropic API key.</div>}
    >
      <div className="container mx-auto py-8">
        <h1 className="text-2xl font-semibold mb-6">AI Wizard</h1>
        <div className="grid gap-4 md:grid-cols-2">
          {OPTIONS.map((opt) => (
            <Card key={opt.kind} className={!opt.available ? 'opacity-60' : ''}>
              <CardHeader>
                <CardTitle>{opt.title}</CardTitle>
                <CardDescription>{opt.description}</CardDescription>
              </CardHeader>
              <CardContent>
                {opt.available ? (
                  <Link href={`/ai/wizard/${opt.kind.toLowerCase()}`} className="text-primary hover:underline">
                    Start →
                  </Link>
                ) : (
                  <span className="text-sm text-muted-foreground">Not yet available</span>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </AiFeatureGate>
  );
}
```

- [ ] **Step 2: Implement the per-kind run page (smoke only for foundation)**

`front-end/src/app/ai/wizard/[kind]/page.tsx`:

```typescript
'use client';
import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { aiClient, type WizardKind } from '@/lib/ai-client';
import { useAiSession } from '@/hooks/useAiSession';
import { toast } from 'sonner';

export default function WizardRunPage() {
  const params = useParams<{ kind: string }>();
  const wizardKind = (params.kind?.toUpperCase() ?? 'SMOKE') as WizardKind;

  const [orgId, setOrgId] = useState<number | null>(null);
  const [input, setInput] = useState('Reply with the single word OK.');
  const [sessionId, setSessionId] = useState<string | null>(null);
  const session = useAiSession(sessionId);

  useEffect(() => {
    const stored = typeof window !== 'undefined' ? localStorage.getItem('user') : null;
    if (stored) setOrgId((JSON.parse(stored) as { organizationId?: number }).organizationId ?? null);
  }, []);

  const start = async () => {
    if (!orgId) return;
    try {
      const res = await aiClient.startSession({
        organizationId: orgId,
        wizardKind,
        mode: 'STREAMING',
        input,
      });
      setSessionId(res.sessionId);
    } catch (err) {
      toast.error('Failed to start: ' + (err instanceof Error ? err.message : 'unknown'));
    }
  };

  return (
    <div className="container mx-auto py-8 max-w-3xl space-y-4">
      <h1 className="text-2xl font-semibold">{wizardKind} Wizard</h1>

      {!sessionId && (
        <Card>
          <CardHeader>
            <CardTitle>Input</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <Input value={input} onChange={(e) => setInput(e.target.value)} />
            <Button onClick={start} disabled={!orgId}>Run</Button>
          </CardContent>
        </Card>
      )}

      {sessionId && (
        <Card>
          <CardHeader>
            <CardTitle>Session {sessionId}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <ul className="text-sm font-mono space-y-1 max-h-96 overflow-auto">
              {session.events.map((e, i) => (
                <li key={i}>
                  <strong>[{e.type}]</strong> {JSON.stringify(e.data)}
                </li>
              ))}
            </ul>
            {session.error && <div className="text-destructive">Error: {session.error}</div>}
            {session.isComplete && session.finalDocument != null && (
              <pre className="text-xs bg-muted p-3 rounded max-h-64 overflow-auto">
                {JSON.stringify(session.finalDocument, null, 2)}
              </pre>
            )}
            {!session.isComplete && (
              <Button variant="outline" onClick={session.cancel}>Cancel</Button>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
```

- [ ] **Step 3: Manually smoke-test end-to-end**

1. Set the API key on the AI Settings page.
2. Navigate to `/ai/wizard`. Confirm Smoke Test is the only enabled card.
3. Click Start, change the input if desired, click Run.
4. Confirm: events stream in, a chunk arrives with the model's reply, and the final document JSON renders.

- [ ] **Step 4: Commit**

```bash
git add front-end/src/app/ai/wizard/page.tsx front-end/src/app/ai/wizard/[kind]/page.tsx
git commit -m "feat(ai/fe): wizard picker shell + smoke wizard run page"
```

---

## Self-Review

After completing all tasks, run the full backend test suite and verify nothing else broke:

```bash
cd back-end && mvn -q test
```

Then the front-end:

```bash
cd front-end && npx vitest run
```

If both pass and the smoke wizard works end-to-end manually, the foundation is complete. Per-doc-type wizards (Catalog, Component-def, Profile, SSP, POA&M) and the Builder Author Assist drawer are tracked as their own plans, each adding only the wizard-specific code on top of this foundation.
