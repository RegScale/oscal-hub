# Build SSP with AI — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an AI wizard that turns a dropped source document (PDF / Word / HTML / etc.) plus an optional profile pick into a schema-valid OSCAL System Security Plan, hand it off to the existing OSCAL document editor for validate/save/publish.

**Architecture:** Backend `SspWizard` mirrors `ComponentDefWizard`'s outline → per-chunk → Java-assemble pipeline. Each `implemented-requirement` carries an `ai-confidence` prop (high/medium/low). Optional profile picker fetches profile JSON from the org library or a URL; control IDs come from the profile when picked, or from the outline pass when skipped. Frontend reuses the existing `OscalDocumentWizard` for review/save/publish; new `initialDocument` prop seeds it from the AI draft. SSP step adds a low-confidence review panel.

**Tech Stack:** Spring Boot 3.5 / Java 17, Anthropic SDK, Next.js 15 / React 18 / TypeScript, Jest + React Testing Library, JUnit 5 + AssertJ + Mockito.

**Reference spec:** `docs/superpowers/specs/2026-05-08-build-ssp-with-ai-design.md`

**Conventions used in this plan:**
- Each task ends with a commit. Branch is `ai-foundation` (current).
- The repo's CLAUDE.md says *Claude does not run builds*. After every code change, the user runs `./stop.sh && ./dev.sh` and a hard refresh in the browser. The plan still tells you which tests to run mentally / which suites would cover each change so you can confirm at review time.
- Use `mvn -pl back-end -Dtest=<TestName> test` mentally for backend tests; `npm --prefix front-end test -- --runTestsByPath <path>` for frontend. **The AI executing this plan does NOT run these commands** — leave that to the user. The "Run test" steps below are written so a human reviewer can replay them; for the AI, the equivalent step is "verify the test file compiles and the assertions match the implementation."

---

## File Structure

### Backend — new files

| Path | Responsibility |
|------|----------------|
| `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/SspWizard.java` | Spring `@Component` implementing `Wizard`. Orchestrates ingest → outline → optional profile-resolve → per-chunk → assemble. |
| `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/SspPromptBuilder.java` | Two prompts: `outlinePrompt()` and `controlsPrompt(systemTitle, controlIds)`. |
| `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/SspChunkingStrategy.java` | Chunks a control-id list into groups for per-chunk LLM calls. |
| `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/ProfileControlIdExtractor.java` | Reads a profile JSON string, returns `Optional<List<String>>` of control IDs from `imports[].include-controls[].with-ids`. Returns `Optional.empty()` for `include-all` (catalog resolution required). |
| `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/ProfileSourceLoader.java` | Loads profile content given a `profileHref`. Recognizes `library:<itemId>` (delegates to `LibraryService.getCurrentVersionContent`) and `http(s)://` URLs (delegates to `RestTemplate`/`HttpClient`). |
| `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/SspPromptBuilderTest.java` | Snapshots both prompts. |
| `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/SspChunkingStrategyTest.java` | Boundary cases. |
| `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/SspWizardTest.java` | Integration with stub Anthropic client. |
| `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/ProfileControlIdExtractorTest.java` | Profile JSON parsing tests. |

### Backend — modified files

| Path | Change |
|------|--------|
| `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/WizardContext.java` | Add nullable `profileHref` field; update factories. |
| `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AiOrchestrator.java` | Plumb `profileHref` through `start(...)`. |
| `back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/StartSessionRequest.java` | Add nullable `profileHref` field. |
| `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AiSessionController.java` | Forward `profileHref` from JSON body and `startWithUpload` form param. |
| `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/KnowledgeLoader.java` | Add explicit `SSP` branch with targeted skill loading. |

### Frontend — new files

| Path | Responsibility |
|------|----------------|
| `front-end/src/components/ai/SspWizardForm.tsx` | Profile picker (library / URL / skip) + file/paste source-doc tabs. |
| `front-end/src/components/ai/SspWizardForm.test.tsx` | Three profile-mode states + payload assertion. |

### Frontend — modified files

| Path | Change |
|------|--------|
| `front-end/src/app/ai/wizard/page.tsx` | Remove the PROFILE option; mark SSP as `available: true` with a real description. |
| `front-end/src/app/ai/wizard/[kind]/page.tsx` | Mount `SspWizardForm` for `wizardKind === 'SSP'`; add hand-off `useEffect` that stashes finalDocument and routes to `/build?section=ssp&aiDraft=…`. |
| `front-end/src/lib/ai-client.ts` | Add optional `profileHref` to `StartSessionRequest` and `startSessionWithUpload`. |
| `front-end/src/lib/api/library.ts` | Add `listByOscalType(type)` helper if not present. |
| `front-end/src/components/build/OscalDocumentWizard.tsx` | New `initialDocument` prop; SSP-only low-confidence review panel on Step 3. |
| `front-end/src/components/build/OscalDocumentWizard.test.tsx` | New tests for `initialDocument` seeding and the low-confidence panel. |
| `front-end/src/app/build/page.tsx` | Hydrate `?aiDraft=…&section=ssp` into a new `aiDraftSsp` state, pass into `OscalDocumentWizard`. |

---

## Task 1: Add `profileHref` to `WizardContext`

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/WizardContext.java`
- Modify (call sites): `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AiOrchestrator.java`
- Modify (test compile): `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/AsyncWizardRunnerTest.java`, `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/ComponentDefWizardTest.java`, `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/CatalogWizardTest.java`, `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/SmokeWizardTest.java`

- [ ] **Step 1: Add `profileHref` field to `WizardContext` record**

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

import java.util.UUID;

public record WizardContext(
        UUID sessionId,
        Long organizationId,
        Long userId,
        String apiKey,
        String model,
        String input,
        byte[] inputBytes,
        String inputFilename,
        String profileHref
) {
    public static WizardContext text(UUID id, Long orgId, Long userId, String apiKey, String model, String input) {
        return new WizardContext(id, orgId, userId, apiKey, model, input, null, null, null);
    }

    public static WizardContext text(UUID id, Long orgId, Long userId, String apiKey, String model, String input, String profileHref) {
        return new WizardContext(id, orgId, userId, apiKey, model, input, null, null, profileHref);
    }

    public static WizardContext file(UUID id, Long orgId, Long userId, String apiKey, String model, byte[] bytes, String filename) {
        return new WizardContext(id, orgId, userId, apiKey, model, null, bytes, filename, null);
    }

    public static WizardContext file(UUID id, Long orgId, Long userId, String apiKey, String model, byte[] bytes, String filename, String profileHref) {
        return new WizardContext(id, orgId, userId, apiKey, model, null, bytes, filename, profileHref);
    }
}
```

The two-arg `text` and `file` factories are kept so existing wizards (and tests) compile unchanged. New three-arg overloads are used by `AiOrchestrator` and `SspWizardTest`.

- [ ] **Step 2: Verify existing call sites still compile**

Open `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AiOrchestrator.java` and confirm lines 53–55 still reference the two-arg factories. They should compile unchanged.

Open the four test files listed above and confirm they only call `WizardContext.text(...)` with the original six args. They should compile unchanged.

- [ ] **Step 3: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/WizardContext.java
git commit -m "feat(ai): add profileHref field to WizardContext

Adds nullable profileHref carrying the user's optional profile choice
into wizard execution. Existing factories are preserved unchanged so
catalog/component-def wizards and their tests compile without edits.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Add `profileHref` to `StartSessionRequest` and controller

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/StartSessionRequest.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AiSessionController.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AiOrchestrator.java`

- [ ] **Step 1: Add `profileHref` to `StartSessionRequest`**

Append the new field, getter, and setter to `StartSessionRequest`:

```java
private String profileHref;

public String getProfileHref() { return profileHref; }
public void setProfileHref(String profileHref) { this.profileHref = profileHref; }
```

No bean validation annotation (it's optional).

- [ ] **Step 2: Add overloads to `AiOrchestrator.start`**

Replace the body of `AiOrchestrator.java` so both `start` overloads accept an optional `profileHref` and forward it through. Preserve the existing two public overloads' signatures by adding two new overloads alongside them:

```java
public UUID start(Long organizationId, Long userId, WizardKind kind, AiSessionMode mode, String input) {
    return start(organizationId, userId, kind, mode, input, null, null, null);
}

public UUID start(Long organizationId, Long userId, WizardKind kind, AiSessionMode mode,
                  String input, byte[] inputBytes, String inputFilename) {
    return start(organizationId, userId, kind, mode, input, inputBytes, inputFilename, null);
}

public UUID start(Long organizationId, Long userId, WizardKind kind, AiSessionMode mode,
                  String input, byte[] inputBytes, String inputFilename, String profileHref) {
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
    WizardContext ctx = inputBytes != null
            ? WizardContext.file(id, organizationId, userId, apiKey, model, inputBytes, inputFilename, profileHref)
            : WizardContext.text(id, organizationId, userId, apiKey, model, input, profileHref);
    asyncRunner.run(wizard, ctx);
    return id;
}
```

- [ ] **Step 3: Forward `profileHref` from `AiSessionController`**

Replace the `start` method body with:

```java
@PostMapping
public ResponseEntity<StartSessionResponse> start(@Valid @RequestBody StartSessionRequest req) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    User user = users.findByUsername(username).orElseThrow();
    requireOrgMembership(user, req.getOrganizationId());
    UUID id = orchestrator.start(req.getOrganizationId(), user.getId(),
            req.getWizardKind(), req.getMode(), req.getInput(),
            null, null, req.getProfileHref());
    return ResponseEntity.ok(new StartSessionResponse(id));
}
```

Replace the `startWithUpload` method signature and body:

```java
@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<StartSessionResponse> startWithUpload(
        @RequestParam Long organizationId,
        @RequestParam WizardKind wizardKind,
        @RequestParam(required = false, defaultValue = "STREAMING") AiSessionMode mode,
        @RequestParam(required = false) String prompt,
        @RequestParam(required = false) String profileHref,
        @RequestPart MultipartFile file) throws IOException {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    User user = users.findByUsername(username).orElseThrow();
    requireOrgMembership(user, organizationId);
    UUID id = orchestrator.start(organizationId, user.getId(), wizardKind, mode,
            prompt, file.getBytes(), file.getOriginalFilename(), profileHref);
    return ResponseEntity.ok(new StartSessionResponse(id));
}
```

- [ ] **Step 4: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/StartSessionRequest.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/controller/AiSessionController.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AiOrchestrator.java
git commit -m "feat(ai): plumb optional profileHref through start-session API

Accepts profileHref on both the JSON start endpoint and the multipart
upload endpoint. AiOrchestrator forwards it into WizardContext.
Catalog and component-def wizards are unaffected (they ignore the
field).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: `SspChunkingStrategy` + test

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/SspChunkingStrategy.java`
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/SspChunkingStrategyTest.java`

- [ ] **Step 1: Write the failing test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/SspChunkingStrategyTest.java`:

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class SspChunkingStrategyTest {

    private final SspChunkingStrategy strategy = new SspChunkingStrategy();

    @Test
    void emptyListProducesNoChunks() {
        assertThat(strategy.chunk(List.of())).isEmpty();
    }

    @Test
    void singleControlProducesOneChunkOfOne() {
        List<List<String>> chunks = strategy.chunk(List.of("ac-1"));
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).containsExactly("ac-1");
    }

    @Test
    void smallBaselineUsesSmallChunkSize() {
        List<String> ids = IntStream.range(0, 25).mapToObj(i -> "ac-" + i).toList();
        List<List<String>> chunks = strategy.chunk(ids);
        // 25 controls / 10 per chunk = 3 chunks (10, 10, 5)
        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).hasSize(10);
        assertThat(chunks.get(1)).hasSize(10);
        assertThat(chunks.get(2)).hasSize(5);
    }

    @Test
    void boundaryAt50UsesSmallChunkSize() {
        List<String> ids = IntStream.range(0, 50).mapToObj(i -> "ac-" + i).toList();
        List<List<String>> chunks = strategy.chunk(ids);
        // 50 / 10 = 5 chunks of 10
        assertThat(chunks).hasSize(5);
        assertThat(chunks.get(0)).hasSize(10);
    }

    @Test
    void largeBaselineUsesLargeChunkSize() {
        List<String> ids = IntStream.range(0, 100).mapToObj(i -> "ac-" + i).toList();
        List<List<String>> chunks = strategy.chunk(ids);
        // 100 controls / 20 per chunk = 5 chunks
        assertThat(chunks).hasSize(5);
        assertThat(chunks.get(0)).hasSize(20);
    }

    @Test
    void fedrampModerateApproximation() {
        List<String> ids = IntStream.range(0, 325).mapToObj(i -> "c-" + i).toList();
        List<List<String>> chunks = strategy.chunk(ids);
        // 325 / 20 = 17 chunks (16 of 20, 1 of 5)
        assertThat(chunks).hasSize(17);
        assertThat(chunks.get(16)).hasSize(5);
    }
}
```

- [ ] **Step 2: Run test (mentally) — should fail to compile, class doesn't exist**

The test imports `SspChunkingStrategy`, which doesn't exist yet → compile error.

- [ ] **Step 3: Implement `SspChunkingStrategy`**

Create `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/SspChunkingStrategy.java`:

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Chunks SSP control IDs into per-LLM-call sub-lists.
 *
 * <ul>
 *   <li>≤ 50 controls → 10 per chunk (small custom baselines)</li>
 *   <li>&gt; 50 controls → 20 per chunk (FedRAMP-class baselines).
 *       SSP narrative output is leaner than catalog group output, so
 *       we can fit more controls per chunk than CatalogChunkingStrategy.</li>
 * </ul>
 */
@Component
public class SspChunkingStrategy {

    private static final int SMALL_THRESHOLD = 50;
    private static final int SMALL_CHUNK_SIZE = 10;
    private static final int LARGE_CHUNK_SIZE = 20;

    public List<List<String>> chunk(List<String> controlIds) {
        if (controlIds.isEmpty()) return List.of();
        int size = controlIds.size() <= SMALL_THRESHOLD ? SMALL_CHUNK_SIZE : LARGE_CHUNK_SIZE;
        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < controlIds.size(); i += size) {
            chunks.add(controlIds.subList(i, Math.min(i + size, controlIds.size())));
        }
        return chunks;
    }
}
```

- [ ] **Step 4: Run test — should pass**

All six tests in `SspChunkingStrategyTest` pass (the user runs `mvn -pl back-end -Dtest=SspChunkingStrategyTest test`).

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/SspChunkingStrategy.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/SspChunkingStrategyTest.java
git commit -m "feat(ai): SspChunkingStrategy splits control-id lists into LLM-sized chunks

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: `SspPromptBuilder` + test

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/SspPromptBuilder.java`
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/SspPromptBuilderTest.java`

- [ ] **Step 1: Write the failing test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/SspPromptBuilderTest.java`:

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SspPromptBuilderTest {

    private final SspPromptBuilder builder = new SspPromptBuilder();

    @Test
    void outlinePromptDemandsSingleRawJsonObject() {
        String prompt = builder.outlinePrompt();
        assertThat(prompt).contains("Respond with a SINGLE raw JSON object");
        assertThat(prompt).contains("First character must be `{`");
        assertThat(prompt).contains("\"systemName\"");
        assertThat(prompt).contains("\"sensitivityLevel\"");
        assertThat(prompt).contains("\"informationTypes\"");
        assertThat(prompt).contains("\"components\"");
        assertThat(prompt).contains("\"users\"");
        assertThat(prompt).contains("\"authorizationBoundary\"");
        assertThat(prompt).contains("\"controlIds\"");
    }

    @Test
    void controlsPromptInterpolatesTitleAndIds() {
        String prompt = builder.controlsPrompt("Acme Trust Center", List.of("ac-1", "ac-2", "ac-3"));
        assertThat(prompt).contains("SINGLE raw JSON array");
        assertThat(prompt).contains("Acme Trust Center");
        assertThat(prompt).contains("ac-1, ac-2, ac-3");
        assertThat(prompt).contains("ai-confidence");
        assertThat(prompt).contains("https://oscal-hub.io/ns");
        assertThat(prompt).contains("\"high\"");
        assertThat(prompt).contains("\"medium\"");
        assertThat(prompt).contains("\"low\"");
        // Confidence rubric must be present
        assertThat(prompt).contains("directly addresses");
        assertThat(prompt).contains("topic generally");
        assertThat(prompt).contains("no direct evidence");
        // Stub instruction for low-confidence entries
        assertThat(prompt).contains("Source document does not address this control. To be completed.");
    }

    @Test
    void controlsPromptAlwaysEmitsOnePerControl() {
        String prompt = builder.controlsPrompt("X", List.of("ac-1"));
        assertThat(prompt).contains("Always emit one entry per requested control");
    }
}
```

- [ ] **Step 2: Run test — should fail (class doesn't exist)**

- [ ] **Step 3: Implement `SspPromptBuilder`**

Create `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/SspPromptBuilder.java`:

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SspPromptBuilder {

    public String outlinePrompt() {
        return """
            CRITICAL: Respond with a SINGLE raw JSON object only. No prose, no
            preamble, no ```json fences, no commentary. First character must be
            `{`, last character must be `}`.

            You are analyzing a source document to draft an OSCAL System
            Security Plan (SSP). The input may be any of:

            * An architecture or design document (PDF, Word, HTML)
            * A system description / discovery document
            * An existing draft SSP from another tool (Word, PDF)
            * Plain narrative text describing a system

            Extract everything you can identify about the system. Schema:

            {
              "title": "<SSP title — usually 'System Security Plan for <system>'>",
              "version": "<document version, default '1.0' if unspecified>",
              "publisher": "<organization that owns the system, or 'unspecified'>",
              "systemName": "<canonical system name>",
              "systemDescription": "<one paragraph: what the system does>",
              "systemId": "<external system identifier; generate a stable string if unspecified>",
              "sensitivityLevel": "low | moderate | high",
              "informationTypes": [{
                "uuid": "<v4 UUID>",
                "title": "<information type title>",
                "description": "<short description>",
                "categorizations": [{
                  "system": "https://doi.org/10.6028/NIST.SP.800-60v2r1",
                  "information-type-ids": ["<NIST 800-60 ID, e.g. C.2.4.1>"]
                }]
              }],
              "components": [{
                "uuid": "<v4 UUID>",
                "type": "software | service | policy | process | physical | system | this-system",
                "title": "<component name>",
                "description": "<what this component does and how it fits in the system>"
              }],
              "users": [{
                "uuid": "<v4 UUID>",
                "title": "<user role title, e.g. 'System Administrator'>",
                "role-ids": ["<role-id, e.g. 'admin'>"]
              }],
              "authorizationBoundary": "<paragraph describing the authorization boundary>",
              "controlIds": ["ac-1", "ac-2", "..."]
            }

            For `sensitivityLevel`, infer from FIPS-199 / 800-60 context if
            present, otherwise default to "moderate".

            For `controlIds`, list every NIST SP 800-53 control ID the source
            document mentions or addresses. Use canonical lowercase-hyphenated
            IDs (ac-1, not AC-1). This list is a fallback; if the user has
            picked a baseline profile separately, the platform will override
            it with the resolved control list. If the source document doesn't
            mention specific controls, return an empty array — do not invent.

            If a field is genuinely unknown, return an empty string for
            scalar fields or an empty array for list fields. Do not invent.
            """;
    }

    public String controlsPrompt(String systemTitle, List<String> controlIds) {
        return """
            CRITICAL: Respond with a SINGLE raw JSON array only. No prose. No
            preamble like "Here is the JSON" or "I'll draft...". No ```json
            fences. No commentary after the JSON. The first character of your
            reply MUST be `[` and the last MUST be `]`.

            Generate OSCAL implemented-requirement entries for the system "%s".

            Output schema — a JSON array of objects:

            [
              {
                "uuid": "<v4 UUID>",
                "control-id": "<control-id>",
                "description": "<implementation narrative grounded in source>",
                "props": [
                  { "name": "ai-confidence", "ns": "https://oscal-hub.io/ns", "value": "high" }
                ]
              },
              ...
            ]

            Control IDs to produce: %s

            Always emit one entry per requested control. Score each entry's
            ai-confidence:

            * "high"   — the source document directly addresses this control
                        with implementation specifics.
            * "medium" — the source document addresses the topic generally but
                        does not fully describe the implementation.
            * "low"    — the source document has no direct evidence; you had
                        to extrapolate or no relevant content exists.

            For "low"-confidence entries, set the description to exactly:
            "Source document does not address this control. To be completed."

            For "high" and "medium" entries, ground the description in
            specific configuration settings, statements, or recommendations
            from the source document. Do not invent content not present in
            the source document.
            """.formatted(systemTitle, String.join(", ", controlIds));
    }
}
```

- [ ] **Step 4: Run test — should pass**

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/SspPromptBuilder.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/SspPromptBuilderTest.java
git commit -m "feat(ai): SspPromptBuilder with outline + per-chunk control prompts

Outline pass extracts system characteristics, system-implementation
skeleton, and a fallback controlIds list. Per-chunk pass emits one
implemented-requirement per control with an ai-confidence prop scored
high/medium/low.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: `ProfileControlIdExtractor` + test

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/ProfileControlIdExtractor.java`
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/ProfileControlIdExtractorTest.java`

- [ ] **Step 1: Write the failing test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/ProfileControlIdExtractorTest.java`:

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileControlIdExtractorTest {

    private final ProfileControlIdExtractor extractor = new ProfileControlIdExtractor();

    @Test
    void includeControlsWithIdsExtractsControlIds() {
        String json = """
            {
              "profile": {
                "uuid": "00000000-0000-0000-0000-000000000001",
                "imports": [{
                  "href": "catalog.json",
                  "include-controls": [{
                    "with-ids": ["ac-1", "ac-2", "au-3"]
                  }]
                }]
              }
            }
            """;
        Optional<List<String>> result = extractor.extract(json);
        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly("ac-1", "ac-2", "au-3");
    }

    @Test
    void multipleImportsAreUnioned() {
        String json = """
            {
              "profile": {
                "imports": [
                  { "include-controls": [{ "with-ids": ["ac-1"] }] },
                  { "include-controls": [{ "with-ids": ["au-1", "au-2"] }] }
                ]
              }
            }
            """;
        Optional<List<String>> result = extractor.extract(json);
        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly("ac-1", "au-1", "au-2");
    }

    @Test
    void duplicateControlIdsAreDeduplicatedPreservingOrder() {
        String json = """
            {
              "profile": {
                "imports": [{
                  "include-controls": [
                    { "with-ids": ["ac-1", "ac-2"] },
                    { "with-ids": ["ac-2", "ac-3"] }
                  ]
                }]
              }
            }
            """;
        Optional<List<String>> result = extractor.extract(json);
        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly("ac-1", "ac-2", "ac-3");
    }

    @Test
    void includeAllReturnsEmptyOptional() {
        String json = """
            {
              "profile": {
                "imports": [{ "include-all": {} }]
              }
            }
            """;
        Optional<List<String>> result = extractor.extract(json);
        assertThat(result).isEmpty();
    }

    @Test
    void mixedIncludeAllAndWithIdsReturnsEmptyOptional() {
        String json = """
            {
              "profile": {
                "imports": [
                  { "include-controls": [{ "with-ids": ["ac-1"] }] },
                  { "include-all": {} }
                ]
              }
            }
            """;
        Optional<List<String>> result = extractor.extract(json);
        assertThat(result).isEmpty();
    }

    @Test
    void malformedJsonReturnsEmptyOptional() {
        Optional<List<String>> result = extractor.extract("not json");
        assertThat(result).isEmpty();
    }

    @Test
    void profileWithoutImportsReturnsEmptyOptional() {
        String json = "{\"profile\":{\"uuid\":\"x\"}}";
        Optional<List<String>> result = extractor.extract(json);
        assertThat(result).isEmpty();
    }
}
```

- [ ] **Step 2: Run test — fails (class doesn't exist)**

- [ ] **Step 3: Implement `ProfileControlIdExtractor`**

Create `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/ProfileControlIdExtractor.java`:

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Extracts the union of controls explicitly listed under
 * {@code imports[].include-controls[].with-ids} in an OSCAL profile.
 *
 * <p>Returns {@link Optional#empty()} when:
 * <ul>
 *   <li>any import uses {@code include-all} (catalog resolution required)</li>
 *   <li>the profile JSON cannot be parsed</li>
 *   <li>no profile imports are present</li>
 * </ul>
 */
@Component
public class ProfileControlIdExtractor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Optional<List<String>> extract(String profileJson) {
        if (profileJson == null || profileJson.isBlank()) return Optional.empty();
        try {
            JsonNode root = MAPPER.readTree(profileJson);
            JsonNode profile = root.path("profile");
            JsonNode imports = profile.path("imports");
            if (!imports.isArray() || imports.isEmpty()) return Optional.empty();

            LinkedHashSet<String> ids = new LinkedHashSet<>();
            for (JsonNode imp : imports) {
                if (imp.has("include-all")) {
                    return Optional.empty();
                }
                JsonNode includeControls = imp.path("include-controls");
                if (!includeControls.isArray()) continue;
                for (JsonNode inc : includeControls) {
                    JsonNode withIds = inc.path("with-ids");
                    if (!withIds.isArray()) continue;
                    for (JsonNode id : withIds) {
                        if (id.isTextual()) ids.add(id.asText());
                    }
                }
            }
            if (ids.isEmpty()) return Optional.empty();
            return Optional.of(new ArrayList<>(ids));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
```

- [ ] **Step 4: Run test — passes**

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/ProfileControlIdExtractor.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/ProfileControlIdExtractorTest.java
git commit -m "feat(ai): ProfileControlIdExtractor for SSP wizard profile resolution

Extracts the union of imports[].include-controls[].with-ids from a
profile JSON. Returns empty for include-all (catalog resolution
required) or malformed input. Used by SspWizard when the user picks a
profile.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: `ProfileSourceLoader`

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/ProfileSourceLoader.java`
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/ProfileSourceLoaderTest.java`

This component takes a `profileHref` (which is one of `library:<itemId>`, `http://…`, `https://…`) and returns the profile JSON content as a string. It does **not** parse the profile — that's `ProfileControlIdExtractor`'s job.

- [ ] **Step 1: Confirm `LibraryService` content method signature**

`LibraryService` lives at `back-end/src/main/java/gov/nist/oscal/tools/api/service/LibraryService.java` (note: package is `…api.service`, NOT `…api.service.library`). The relevant method is:

```java
public String getCurrentVersionContent(String itemId, User caller)
```

at line 261 of that file. The test below mocks this method.

- [ ] **Step 2: Write the failing test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/ProfileSourceLoaderTest.java`:

```java
package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.service.LibraryService;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfileSourceLoaderTest {

    @Test
    void libraryHrefDelegatesToLibraryService() {
        LibraryService library = mock(LibraryService.class);
        RestTemplate rest = mock(RestTemplate.class);
        User caller = new User();

        when(library.getCurrentVersionContent(eq("abc-123"), eq(caller))).thenReturn("{\"profile\":{}}");
        ProfileSourceLoader loader = new ProfileSourceLoader(library, rest);

        String content = loader.load("library:abc-123", caller);
        assertThat(content).isEqualTo("{\"profile\":{}}");
    }

    @Test
    void httpHrefDelegatesToRestTemplate() {
        LibraryService library = mock(LibraryService.class);
        RestTemplate rest = mock(RestTemplate.class);
        when(rest.getForObject(eq("https://example.test/profile.json"), eq(String.class)))
                .thenReturn("{\"profile\":{\"uuid\":\"x\"}}");

        ProfileSourceLoader loader = new ProfileSourceLoader(library, rest);
        String content = loader.load("https://example.test/profile.json", new User());
        assertThat(content).contains("\"profile\"");
    }

    @Test
    void invalidSchemeThrowsIllegalArgument() {
        ProfileSourceLoader loader = new ProfileSourceLoader(mock(LibraryService.class), mock(RestTemplate.class));
        assertThatLoading(() -> loader.load("ftp://x", new User()));
    }

    @Test
    void httpFailureWraps() {
        LibraryService library = mock(LibraryService.class);
        RestTemplate rest = mock(RestTemplate.class);
        when(rest.getForObject(eq("https://bad.test/p.json"), eq(String.class)))
                .thenThrow(new RestClientException("boom"));

        ProfileSourceLoader loader = new ProfileSourceLoader(library, rest);
        assertThatLoading(() -> loader.load("https://bad.test/p.json", new User()));
    }

    private void assertThatLoading(Runnable r) {
        try {
            r.run();
            assertThat(true).as("should have thrown").isFalse();
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }
}
```

- [ ] **Step 3: Run test — fails (class doesn't exist)**

- [ ] **Step 4: Implement `ProfileSourceLoader`**

Create `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/ProfileSourceLoader.java`:

```java
package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.service.LibraryService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Loads OSCAL profile content given a {@code profileHref} chosen by the
 * SSP wizard caller.
 *
 * <p>Recognized schemes:
 * <ul>
 *   <li>{@code library:<itemId>} — fetches via {@link LibraryService}</li>
 *   <li>{@code http://...} or {@code https://...} — fetches via {@link RestTemplate}</li>
 * </ul>
 *
 * <p>Throws {@link IllegalArgumentException} on unsupported schemes or HTTP failures
 * — the wizard catches this and falls back to outline-derived control IDs.
 */
@Service
public class ProfileSourceLoader {

    private static final String LIBRARY_PREFIX = "library:";

    private final LibraryService library;
    private final RestTemplate rest;

    public ProfileSourceLoader(LibraryService library, RestTemplate rest) {
        this.library = library;
        this.rest = rest;
    }

    public String load(String href, User caller) {
        if (href == null || href.isBlank()) {
            throw new IllegalArgumentException("profileHref is empty");
        }
        if (href.startsWith(LIBRARY_PREFIX)) {
            String itemId = href.substring(LIBRARY_PREFIX.length());
            try {
                return library.getCurrentVersionContent(itemId, caller);
            } catch (Exception e) {
                throw new IllegalArgumentException("Library profile not readable: " + itemId, e);
            }
        }
        if (href.startsWith("http://") || href.startsWith("https://")) {
            try {
                String body = rest.getForObject(href, String.class);
                if (body == null) throw new IllegalArgumentException("Empty response from " + href);
                return body;
            } catch (RestClientException e) {
                throw new IllegalArgumentException("Failed to fetch " + href + ": " + e.getMessage(), e);
            }
        }
        throw new IllegalArgumentException("Unsupported profileHref scheme: " + href);
    }
}
```

If `RestTemplate` is not yet a Spring bean in this project, add a `@Bean RestTemplate` method to an existing `@Configuration` class (search for `@Configuration` under `back-end/src/main/java/.../config/` — most projects have one). For this plan, assume the bean already exists; if compilation fails on injection, add it to `EnvironmentConfig.java`:

```java
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

- [ ] **Step 5: Run test — passes**

- [ ] **Step 6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/ProfileSourceLoader.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/ProfileSourceLoaderTest.java
git commit -m "feat(ai): ProfileSourceLoader for library:/http(s) profile hrefs

Resolves a profileHref to its JSON content. Library items go through
LibraryService respecting visibility; URLs go through RestTemplate.
Throws IllegalArgumentException on unsupported schemes or fetch
failures, which SspWizard converts into a 'fallback to outline'
warning.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: `SspWizard` happy-path test (skip-profile branch)

**Files:**
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/SspWizardTest.java` (Step 1 only — partial, will grow in Tasks 8 and 9)

We split this large component across three TDD passes: skip-profile happy path, with-profile override, profile-resolution failure fallback. Each task adds tests, then implementation grows to satisfy them.

- [ ] **Step 1: Write the failing happy-path test (skip-profile)**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/SspWizardTest.java`:

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.ai.AnthropicCall;
import gov.nist.oscal.tools.api.service.ai.AnthropicClient;
import gov.nist.oscal.tools.api.service.ai.AnthropicResult;
import gov.nist.oscal.tools.api.service.ai.DocumentNormalizer;
import gov.nist.oscal.tools.api.service.ai.KnowledgeLoader;
import gov.nist.oscal.tools.api.service.ai.ProfileSourceLoader;
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import gov.nist.oscal.tools.api.service.ai.stream.SessionEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SspWizardTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Pulls the COMPLETE event's wrapped SSP doc — SessionEvent.complete wraps as {"document": ...}. */
    private JsonNode extractCompletedDocument(java.util.List<SessionEvent> events) throws Exception {
        SessionEvent complete = events.stream()
                .filter(e -> e.type() == SessionEvent.Type.COMPLETE)
                .findFirst().orElseThrow();
        return MAPPER.readTree(complete.dataJson()).path("document");
    }

    private boolean hasProgressContaining(java.util.List<SessionEvent> events, String needle) {
        return events.stream()
                .filter(e -> e.type() == SessionEvent.Type.PROGRESS)
                .anyMatch(e -> e.dataJson().contains(needle));
    }

    private static final String OUTLINE_JSON = """
        {
          "title": "Acme Trust Center SSP",
          "version": "1.0",
          "publisher": "Acme",
          "systemName": "Acme Trust Center",
          "systemDescription": "Customer trust portal.",
          "systemId": "acme-trust",
          "sensitivityLevel": "moderate",
          "informationTypes": [{
            "uuid": "00000000-0000-0000-0000-000000000010",
            "title": "Customer Data",
            "description": "PII.",
            "categorizations": []
          }],
          "components": [{
            "uuid": "00000000-0000-0000-0000-000000000020",
            "type": "this-system",
            "title": "Trust Center",
            "description": "Web app."
          }],
          "users": [{
            "uuid": "00000000-0000-0000-0000-000000000030",
            "title": "Admin",
            "role-ids": ["admin"]
          }],
          "authorizationBoundary": "Cloud Run + Cloud SQL.",
          "controlIds": ["ac-1", "ac-2"]
        }
        """;

    private static final String CHUNK_JSON = """
        [
          {"uuid":"00000000-0000-0000-0000-000000000100","control-id":"ac-1","description":"D1","props":[{"name":"ai-confidence","ns":"https://oscal-hub.io/ns","value":"high"}]},
          {"uuid":"00000000-0000-0000-0000-000000000101","control-id":"ac-2","description":"D2","props":[{"name":"ai-confidence","ns":"https://oscal-hub.io/ns","value":"medium"}]}
        ]
        """;

    @Test
    void skipProfileHappyPathProducesSchemaShapedSsp() throws Exception {
        AnthropicClient client = mock(AnthropicClient.class);
        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        KnowledgeLoader knowledge = mock(KnowledgeLoader.class);
        DocumentNormalizer normalizer = mock(DocumentNormalizer.class);
        ProfileSourceLoader profileLoader = mock(ProfileSourceLoader.class);
        ProfileControlIdExtractor extractor = mock(ProfileControlIdExtractor.class);
        UserRepository users = mock(UserRepository.class);
        when(knowledge.systemFor(WizardKind.SSP)).thenReturn("system");

        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("System Security Plan")), any()))
                .thenReturn(new AnthropicResult(OUTLINE_JSON, 100, 50));
        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("implemented-requirement entries")), any()))
                .thenReturn(new AnthropicResult(CHUNK_JSON, 50, 30));

        SspWizard wizard = new SspWizard(client, stream, knowledge, normalizer,
                new SspPromptBuilder(), new SspChunkingStrategy(),
                profileLoader, extractor, users);

        WizardContext ctx = WizardContext.text(UUID.randomUUID(), 1L, 7L,
                "sk-ant-xxx", "claude-opus-4-7", "Source doc text", null);

        WizardOutcome outcome = wizard.run(ctx);

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.tokensIn()).isEqualTo(150);
        assertThat(outcome.tokensOut()).isEqualTo(80);

        ArgumentCaptor<SessionEvent> ev = ArgumentCaptor.forClass(SessionEvent.class);
        verify(stream, atLeastOnce()).publish(eq(ctx.sessionId()), ev.capture());
        JsonNode doc = extractCompletedDocument(ev.getAllValues());
        JsonNode ssp = doc.path("system-security-plan");
        assertThat(ssp.path("metadata").path("title").asText()).isEqualTo("Acme Trust Center SSP");
        assertThat(ssp.path("import-profile").path("href").asText()).isEmpty();
        assertThat(ssp.path("system-characteristics").path("system-name").asText()).isEqualTo("Acme Trust Center");
        assertThat(ssp.path("system-characteristics").path("security-sensitivity-level").asText()).isEqualTo("moderate");
        assertThat(ssp.path("system-implementation").path("components").size()).isEqualTo(1);
        JsonNode reqs = ssp.path("control-implementation").path("implemented-requirements");
        assertThat(reqs.size()).isEqualTo(2);
        assertThat(reqs.get(0).path("control-id").asText()).isEqualTo("ac-1");
        assertThat(reqs.get(0).path("props").get(0).path("name").asText()).isEqualTo("ai-confidence");
    }
}
```

- [ ] **Step 2: Run test — fails (SspWizard doesn't exist)**

- [ ] **Step 3: Skip implementation for now — Task 8 builds the wizard.**

(No commit; this test will pass at the end of Task 8.)

---

## Task 8: `SspWizard` implementation (skip-profile branch)

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/SspWizard.java`

- [ ] **Step 1: Implement `SspWizard`**

Create `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/SspWizard.java`:

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.ai.AnthropicCall;
import gov.nist.oscal.tools.api.service.ai.AnthropicClient;
import gov.nist.oscal.tools.api.service.ai.AnthropicResult;
import gov.nist.oscal.tools.api.service.ai.DocumentNormalizer;
import gov.nist.oscal.tools.api.service.ai.KnowledgeLoader;
import gov.nist.oscal.tools.api.service.ai.NormalizedDoc;
import gov.nist.oscal.tools.api.service.ai.ProfileSourceLoader;
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import gov.nist.oscal.tools.api.service.ai.stream.SessionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class SspWizard implements Wizard {

    private static final Logger log = LoggerFactory.getLogger(SspWizard.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int TOKEN_BUDGET_IN = 5_000_000;
    private static final int TOKEN_BUDGET_OUT = 200_000;

    private final AnthropicClient client;
    private final AiSessionEventStream stream;
    private final KnowledgeLoader knowledge;
    private final DocumentNormalizer normalizer;
    private final SspPromptBuilder prompts;
    private final SspChunkingStrategy chunking;
    private final ProfileSourceLoader profileLoader;
    private final ProfileControlIdExtractor extractor;
    private final UserRepository users;

    public SspWizard(AnthropicClient client, AiSessionEventStream stream,
                     KnowledgeLoader knowledge, DocumentNormalizer normalizer,
                     SspPromptBuilder prompts, SspChunkingStrategy chunking,
                     ProfileSourceLoader profileLoader,
                     ProfileControlIdExtractor extractor,
                     UserRepository users) {
        this.client = client;
        this.stream = stream;
        this.knowledge = knowledge;
        this.normalizer = normalizer;
        this.prompts = prompts;
        this.chunking = chunking;
        this.profileLoader = profileLoader;
        this.extractor = extractor;
        this.users = users;
    }

    @Override
    public WizardKind kind() { return WizardKind.SSP; }

    @Override
    public WizardOutcome run(WizardContext ctx) {
        try {
            stream.publish(ctx.sessionId(), SessionEvent.progress("Reading source document…"));

            String docText;
            byte[] pdfBytes = null;
            String filename = ctx.inputFilename() == null ? "input" : ctx.inputFilename();
            if (ctx.inputBytes() != null && filename.toLowerCase().endsWith(".pdf")) {
                pdfBytes = ctx.inputBytes();
                docText = "(PDF attached as document block)";
            } else if (ctx.inputBytes() != null) {
                NormalizedDoc d = normalizer.normalize(ctx.inputBytes(), filename);
                docText = d.plainText();
            } else {
                docText = ctx.input() == null ? "" : ctx.input();
            }

            String system = knowledge.systemFor(WizardKind.SSP);
            int tokensIn = 0, tokensOut = 0;

            // Pass 1 — outline
            stream.publish(ctx.sessionId(), SessionEvent.progress("Extracting system characteristics…"));
            AnthropicCall.Builder b = AnthropicCall.builder()
                    .model(ctx.model())
                    .systemPrompt(system)
                    .userMessage(prompts.outlinePrompt() + "\n\n---\n\n" + docText)
                    .maxTokens(8000);
            if (pdfBytes != null) b.addPdf(pdfBytes);
            AnthropicResult outlineRes = client.send(ctx.apiKey(), b.build(),
                    msg -> stream.publish(ctx.sessionId(), SessionEvent.progress(msg)));
            tokensIn += outlineRes.tokensIn();
            tokensOut += outlineRes.tokensOut();

            JsonNode outline = MAPPER.readTree(extractJson(outlineRes.text()));
            String title = outline.path("title").asText("System Security Plan");
            String version = outline.path("version").asText("1.0");
            String publisher = outline.path("publisher").asText("unspecified");
            String systemName = outline.path("systemName").asText("Untitled System");
            String systemDescription = outline.path("systemDescription").asText("");
            String systemId = outline.path("systemId").asText("system-" + UUID.randomUUID());
            String sensitivityLevel = outline.path("sensitivityLevel").asText("moderate");
            String authorizationBoundary = outline.path("authorizationBoundary").asText("");

            List<JsonNode> informationTypes = new ArrayList<>();
            for (JsonNode t : outline.path("informationTypes")) informationTypes.add(t);
            List<JsonNode> components = new ArrayList<>();
            for (JsonNode c : outline.path("components")) components.add(c);
            List<JsonNode> ussers = new ArrayList<>();
            for (JsonNode u : outline.path("users")) ussers.add(u);

            List<String> controlIds = new ArrayList<>();
            for (JsonNode cid : outline.path("controlIds")) controlIds.add(cid.asText());

            // Pass 1.5 — optional profile resolution (skip-profile branch leaves controlIds as-is)
            String profileHref = ctx.profileHref();
            if (profileHref != null && !profileHref.isBlank()) {
                controlIds = resolveProfileControlIds(ctx, profileHref, controlIds);
            }

            stream.publish(ctx.sessionId(), SessionEvent.progress(
                    "Outline complete — " + systemName + ", " + controlIds.size() + " controls"));

            // Pass 2 — per-chunk narratives
            List<JsonNode> producedRequirements = new ArrayList<>();
            List<List<String>> chunks = chunking.chunk(controlIds);
            int chunkIndex = 0;
            for (List<String> chunk : chunks) {
                chunkIndex++;
                stream.publish(ctx.sessionId(), SessionEvent.progress(
                        "Drafting implementation narratives (" + chunkIndex + " of " + chunks.size() + ")…"));
                AnthropicResult chunkRes = client.send(ctx.apiKey(), AnthropicCall.builder()
                        .model(ctx.model())
                        .systemPrompt(system)
                        .userMessage(prompts.controlsPrompt(systemName, chunk) + "\n\n---\n\n" + docText)
                        .maxTokens(8000)
                        .build(),
                        msg -> stream.publish(ctx.sessionId(), SessionEvent.progress(msg)));
                tokensIn += chunkRes.tokensIn();
                tokensOut += chunkRes.tokensOut();
                JsonNode arr = MAPPER.readTree(extractJsonArray(chunkRes.text()));
                if (arr.isArray()) {
                    for (JsonNode req : arr) producedRequirements.add(req);
                }
                if (tokensIn > TOKEN_BUDGET_IN || tokensOut > TOKEN_BUDGET_OUT) {
                    String msg = "Token budget exceeded after chunk " + chunkIndex
                            + " (in=" + tokensIn + ", out=" + tokensOut + ")";
                    stream.publish(ctx.sessionId(), SessionEvent.error("token_budget", msg));
                    return WizardOutcome.failed("token_budget", msg);
                }
            }

            // Pass 3 — assemble
            stream.publish(ctx.sessionId(), SessionEvent.progress(
                    "Assembling OSCAL System Security Plan…"));

            Map<String, Object> meta = new HashMap<>();
            meta.put("title", title);
            meta.put("last-modified", java.time.Instant.now().toString());
            meta.put("version", version);
            meta.put("oscal-version", "1.1.2");
            meta.put("parties", List.of(Map.of(
                    "uuid", UUID.randomUUID().toString(),
                    "type", "organization",
                    "name", publisher)));

            Map<String, Object> sysChar = new HashMap<>();
            sysChar.put("system-name", systemName);
            sysChar.put("description", systemDescription);
            sysChar.put("system-ids", List.of(Map.of("id", systemId)));
            sysChar.put("security-sensitivity-level", sensitivityLevel);
            sysChar.put("system-information", Map.of("information-types", informationTypes));
            sysChar.put("security-impact-level", Map.of(
                    "security-objective-confidentiality", sensitivityLevel,
                    "security-objective-integrity", sensitivityLevel,
                    "security-objective-availability", sensitivityLevel));
            sysChar.put("status", Map.of("state", "operational"));
            sysChar.put("authorization-boundary", Map.of("description", authorizationBoundary));

            Map<String, Object> sysImpl = new HashMap<>();
            sysImpl.put("users", ussers);
            sysImpl.put("components", components);

            Map<String, Object> ctrlImpl = new HashMap<>();
            ctrlImpl.put("uuid", UUID.randomUUID().toString());
            ctrlImpl.put("description", "Control implementations drafted from source document.");
            ctrlImpl.put("implemented-requirements", producedRequirements);

            Map<String, Object> root = new HashMap<>();
            root.put("uuid", UUID.randomUUID().toString());
            root.put("metadata", meta);
            root.put("import-profile", Map.of("href", profileHref == null ? "" : profileHref));
            root.put("system-characteristics", sysChar);
            root.put("system-implementation", sysImpl);
            root.put("control-implementation", ctrlImpl);

            Map<String, Object> doc = new HashMap<>();
            doc.put("system-security-plan", root);

            String resultJson = MAPPER.writeValueAsString(doc);
            stream.publish(ctx.sessionId(), SessionEvent.complete(resultJson));
            return WizardOutcome.ok(tokensIn, tokensOut);

        } catch (IllegalArgumentException e) {
            stream.publish(ctx.sessionId(), SessionEvent.error("auth_or_input", e.getMessage()));
            return WizardOutcome.failed("auth_or_input", e.getMessage());
        } catch (Exception e) {
            log.error("SSP wizard failed", e);
            stream.publish(ctx.sessionId(), SessionEvent.error("model_error", e.getMessage()));
            return WizardOutcome.failed("model_error", e.getMessage());
        }
    }

    private List<String> resolveProfileControlIds(WizardContext ctx, String profileHref, List<String> outlineFallback) {
        stream.publish(ctx.sessionId(), SessionEvent.progress("Resolving profile…"));
        try {
            User caller = users.findById(ctx.userId()).orElseThrow(
                    () -> new IllegalStateException("User " + ctx.userId() + " not found"));
            String profileJson = profileLoader.load(profileHref, caller);
            Optional<List<String>> ids = extractor.extract(profileJson);
            if (ids.isPresent()) {
                stream.publish(ctx.sessionId(), SessionEvent.progress(
                        "Profile resolved — " + ids.get().size() + " controls"));
                return ids.get();
            }
            stream.publish(ctx.sessionId(), SessionEvent.progress(
                    "Profile uses include-all (catalog resolution not yet supported) — falling back to controls inferred from source document."));
            return outlineFallback;
        } catch (Exception e) {
            log.warn("Profile resolution failed for {}: {}", profileHref, e.getMessage());
            stream.publish(ctx.sessionId(), SessionEvent.progress(
                    "Profile resolution failed: " + e.getMessage()
                    + " — falling back to controls inferred from source document."));
            return outlineFallback;
        }
    }

    private static String extractJson(String text) {
        if (text == null) return "{}";
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) t = t.substring(firstNl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
            t = t.trim();
        }
        int firstBrace = t.indexOf('{');
        int lastBrace = t.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            t = t.substring(firstBrace, lastBrace + 1);
        }
        return t.trim();
    }

    private static String extractJsonArray(String text) {
        if (text == null) return "[]";
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) t = t.substring(firstNl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
            t = t.trim();
        }
        int firstBracket = t.indexOf('[');
        int lastBracket = t.lastIndexOf(']');
        if (firstBracket >= 0 && lastBracket > firstBracket) {
            t = t.substring(firstBracket, lastBracket + 1);
        }
        return t.trim();
    }
}
```

- [ ] **Step 2: Run `SspWizardTest` — should pass**

- [ ] **Step 3: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/SspWizard.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/SspWizardTest.java
git commit -m "feat(ai): SspWizard with outline + chunked narratives + Java assemble

Mirrors ComponentDefWizard's three-pass shape. New Pass 1.5 resolves
the optional profile via ProfileSourceLoader + ProfileControlIdExtractor
and overrides the outline's controlIds. Falls back gracefully when
resolution fails or the profile uses include-all.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 9: `SspWizard` profile-resolution tests

**Files:**
- Modify: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/SspWizardTest.java`

- [ ] **Step 1: Add the with-profile override test**

Append to `SspWizardTest`:

```java
    @Test
    void withProfileOverridesOutlineControlIds() throws Exception {
        AnthropicClient client = mock(AnthropicClient.class);
        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        KnowledgeLoader knowledge = mock(KnowledgeLoader.class);
        DocumentNormalizer normalizer = mock(DocumentNormalizer.class);
        ProfileSourceLoader profileLoader = mock(ProfileSourceLoader.class);
        ProfileControlIdExtractor extractor = mock(ProfileControlIdExtractor.class);
        UserRepository users = mock(UserRepository.class);
        when(knowledge.systemFor(WizardKind.SSP)).thenReturn("system");

        User caller = new User();
        when(users.findById(7L)).thenReturn(Optional.of(caller));
        when(profileLoader.load(eq("library:profile-x"), eq(caller))).thenReturn("{\"profile\":{}}");
        when(extractor.extract("{\"profile\":{}}")).thenReturn(Optional.of(List.of("au-1", "au-2", "au-3")));

        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("System Security Plan")), any()))
                .thenReturn(new AnthropicResult(OUTLINE_JSON, 100, 50));
        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("implemented-requirement entries")), any()))
                .thenReturn(new AnthropicResult("""
                    [
                      {"uuid":"u1","control-id":"au-1","description":"D","props":[{"name":"ai-confidence","ns":"https://oscal-hub.io/ns","value":"high"}]},
                      {"uuid":"u2","control-id":"au-2","description":"D","props":[{"name":"ai-confidence","ns":"https://oscal-hub.io/ns","value":"high"}]},
                      {"uuid":"u3","control-id":"au-3","description":"D","props":[{"name":"ai-confidence","ns":"https://oscal-hub.io/ns","value":"high"}]}
                    ]
                    """, 50, 30));

        SspWizard wizard = new SspWizard(client, stream, knowledge, normalizer,
                new SspPromptBuilder(), new SspChunkingStrategy(),
                profileLoader, extractor, users);

        WizardContext ctx = WizardContext.text(UUID.randomUUID(), 1L, 7L,
                "sk-ant-xxx", "claude-opus-4-7", "Source", "library:profile-x");

        WizardOutcome outcome = wizard.run(ctx);
        assertThat(outcome.success()).isTrue();

        ArgumentCaptor<SessionEvent> ev = ArgumentCaptor.forClass(SessionEvent.class);
        verify(stream, atLeastOnce()).publish(eq(ctx.sessionId()), ev.capture());
        JsonNode doc = extractCompletedDocument(ev.getAllValues());
        JsonNode reqs = doc.path("system-security-plan")
                .path("control-implementation")
                .path("implemented-requirements");
        assertThat(reqs.size()).isEqualTo(3);
        assertThat(reqs.get(0).path("control-id").asText()).isEqualTo("au-1");
        // import-profile.href reflects the user's choice
        assertThat(doc.path("system-security-plan").path("import-profile").path("href").asText())
                .isEqualTo("library:profile-x");
    }

    @Test
    void profileResolutionFailureFallsBackToOutlineControls() throws Exception {
        AnthropicClient client = mock(AnthropicClient.class);
        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        KnowledgeLoader knowledge = mock(KnowledgeLoader.class);
        DocumentNormalizer normalizer = mock(DocumentNormalizer.class);
        ProfileSourceLoader profileLoader = mock(ProfileSourceLoader.class);
        ProfileControlIdExtractor extractor = mock(ProfileControlIdExtractor.class);
        UserRepository users = mock(UserRepository.class);
        when(knowledge.systemFor(WizardKind.SSP)).thenReturn("system");

        User caller = new User();
        when(users.findById(7L)).thenReturn(Optional.of(caller));
        when(profileLoader.load(eq("https://bad.test/p.json"), eq(caller)))
                .thenThrow(new IllegalArgumentException("404 Not Found"));

        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("System Security Plan")), any()))
                .thenReturn(new AnthropicResult(OUTLINE_JSON, 100, 50));
        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("implemented-requirement entries")), any()))
                .thenReturn(new AnthropicResult(CHUNK_JSON, 50, 30));

        SspWizard wizard = new SspWizard(client, stream, knowledge, normalizer,
                new SspPromptBuilder(), new SspChunkingStrategy(),
                profileLoader, extractor, users);

        WizardContext ctx = WizardContext.text(UUID.randomUUID(), 1L, 7L,
                "sk-ant-xxx", "claude-opus-4-7", "Source", "https://bad.test/p.json");

        WizardOutcome outcome = wizard.run(ctx);
        assertThat(outcome.success()).isTrue();

        ArgumentCaptor<SessionEvent> ev = ArgumentCaptor.forClass(SessionEvent.class);
        verify(stream, atLeastOnce()).publish(eq(ctx.sessionId()), ev.capture());

        assertThat(hasProgressContaining(ev.getAllValues(), "Profile resolution failed")).isTrue();
        assertThat(hasProgressContaining(ev.getAllValues(), "falling back")).isTrue();

        // Assembled doc uses the outline's control IDs (ac-1, ac-2)
        JsonNode doc = extractCompletedDocument(ev.getAllValues());
        JsonNode reqs = doc.path("system-security-plan")
                .path("control-implementation")
                .path("implemented-requirements");
        assertThat(reqs.size()).isEqualTo(2);
    }

    @Test
    void includeAllProfileFallsBackToOutlineControls() throws Exception {
        AnthropicClient client = mock(AnthropicClient.class);
        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        KnowledgeLoader knowledge = mock(KnowledgeLoader.class);
        DocumentNormalizer normalizer = mock(DocumentNormalizer.class);
        ProfileSourceLoader profileLoader = mock(ProfileSourceLoader.class);
        ProfileControlIdExtractor extractor = mock(ProfileControlIdExtractor.class);
        UserRepository users = mock(UserRepository.class);
        when(knowledge.systemFor(WizardKind.SSP)).thenReturn("system");

        User caller = new User();
        when(users.findById(7L)).thenReturn(Optional.of(caller));
        when(profileLoader.load(eq("library:include-all-profile"), eq(caller)))
                .thenReturn("{\"profile\":{\"imports\":[{\"include-all\":{}}]}}");
        when(extractor.extract("{\"profile\":{\"imports\":[{\"include-all\":{}}]}}"))
                .thenReturn(Optional.empty());

        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("System Security Plan")), any()))
                .thenReturn(new AnthropicResult(OUTLINE_JSON, 100, 50));
        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("implemented-requirement entries")), any()))
                .thenReturn(new AnthropicResult(CHUNK_JSON, 50, 30));

        SspWizard wizard = new SspWizard(client, stream, knowledge, normalizer,
                new SspPromptBuilder(), new SspChunkingStrategy(),
                profileLoader, extractor, users);

        WizardContext ctx = WizardContext.text(UUID.randomUUID(), 1L, 7L,
                "sk-ant-xxx", "claude-opus-4-7", "Source", "library:include-all-profile");

        WizardOutcome outcome = wizard.run(ctx);
        assertThat(outcome.success()).isTrue();

        ArgumentCaptor<SessionEvent> ev = ArgumentCaptor.forClass(SessionEvent.class);
        verify(stream, atLeastOnce()).publish(eq(ctx.sessionId()), ev.capture());
        assertThat(hasProgressContaining(ev.getAllValues(), "include-all")).isTrue();

        JsonNode doc = extractCompletedDocument(ev.getAllValues());
        JsonNode reqs = doc.path("system-security-plan")
                .path("control-implementation")
                .path("implemented-requirements");
        assertThat(reqs.size()).isEqualTo(2);
    }
```

- [ ] **Step 2: Run all `SspWizardTest` tests — should all pass**

- [ ] **Step 3: Commit**

```bash
git add back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/SspWizardTest.java
git commit -m "test(ai): SspWizard profile-resolution branches

Covers (1) library: profile override with explicit with-ids,
(2) HTTP fetch failure → fallback to outline IDs with warning,
(3) include-all profile → fallback to outline IDs with warning.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 10: `KnowledgeLoader` SSP branch

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/KnowledgeLoader.java`

- [ ] **Step 1: Add explicit `SSP` branch**

Replace lines 100–106 (the `if (kind == WizardKind.COMPONENT_DEF)` block) with the same block followed immediately by an SSP block:

```java
        if (kind == WizardKind.COMPONENT_DEF) {
            appendSkillsFrom(sb, pluginRoot.resolve("plugins/oscal/skills/oscal-basics"));
            appendSkillsFrom(sb, pluginRoot.resolve("plugins/oscal/skills/oscal-component-definition"));
            appendSkillsFrom(sb, pluginRoot.resolve("plugins/metaschema/skills/metaschema-basics"));
            sb.append("\nFocus: produce an OSCAL Component-definition mapping product features to controls.\n");
            return sb.toString();
        }

        if (kind == WizardKind.SSP) {
            appendSkillsFrom(sb, pluginRoot.resolve("plugins/oscal/skills/oscal-basics"));
            appendSkillsFrom(sb, pluginRoot.resolve("plugins/oscal/skills/oscal-ssp"));
            appendSkillsFrom(sb, pluginRoot.resolve("plugins/metaschema/skills/metaschema-basics"));
            sb.append("\nFocus: produce SSP per-control implementation narratives grounded in the system description.\n");
            return sb.toString();
        }
```

The fallback `switch` later in the method still has an `SSP` case for the load-all path — that's now dead for `SSP` since the explicit branch returns early. Leave it; it doesn't hurt and matches the COMPONENT_DEF style.

- [ ] **Step 2: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/KnowledgeLoader.java
git commit -m "feat(ai): targeted skill loading for SSP wizard

Mirrors the CATALOG / COMPONENT_DEF pattern: load only oscal-basics,
oscal-ssp, and metaschema-basics rather than every skill in the
submodule. Cuts system-prompt size and keeps the SSP focus tight.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 11: Frontend `ai-client` profileHref support

**Files:**
- Modify: `front-end/src/lib/ai-client.ts`

- [ ] **Step 1: Add `profileHref` to `StartSessionRequest`**

In `front-end/src/lib/ai-client.ts`, replace the `StartSessionRequest` interface (lines 43–48) with:

```typescript
export interface StartSessionRequest {
  organizationId: number;
  wizardKind: WizardKind;
  mode: SessionMode;
  input?: string;
  profileHref?: string | null;
}
```

The body of `startSession` already serializes the whole request via `JSON.stringify(req)`, so no change needed there.

- [ ] **Step 2: Add `profileHref` param to `startSessionWithUpload`**

Replace the `startSessionWithUpload` method (lines 140–162) with:

```typescript
  async startSessionWithUpload(
    organizationId: number,
    wizardKind: WizardKind,
    file: File,
    options?: { prompt?: string; mode?: SessionMode; profileHref?: string | null },
  ): Promise<StartSessionResponse> {
    const fd = new FormData();
    fd.append('file', file);
    const url = new URL(`${API_BASE_URL}/ai/sessions/upload`);
    url.searchParams.set('organizationId', String(organizationId));
    url.searchParams.set('wizardKind', wizardKind);
    url.searchParams.set('mode', options?.mode ?? 'STREAMING');
    if (options?.prompt) url.searchParams.set('prompt', options.prompt);
    if (options?.profileHref) url.searchParams.set('profileHref', options.profileHref);

    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
    const headers: Record<string, string> = {};
    if (token) headers.Authorization = `Bearer ${token}`;
    // Don't set Content-Type — browser sets multipart boundary

    const res = await aiFetch(url.toString(), { method: 'POST', headers, body: fd });
    return res.json();
  },
```

This is a **breaking signature change** for `startSessionWithUpload`: previous callers used positional `prompt` and `mode` args. Update existing call sites:

- `front-end/src/components/ai/CatalogWizardForm.tsx:31`
- `front-end/src/components/ai/ComponentDefWizardForm.tsx:31`

Both pass only the file with no prompt/mode, so the change is:

`aiClient.startSessionWithUpload(organizationId, 'CATALOG', file)` — unchanged.
`aiClient.startSessionWithUpload(organizationId, 'COMPONENT_DEF', file)` — unchanged.

Verify by grepping:

```bash
grep -rn "startSessionWithUpload" front-end/src/
```

Confirm every call uses positional `(organizationId, wizardKind, file)` only. Each call should still work with the new options-object signature.

- [ ] **Step 3: Commit**

```bash
git add front-end/src/lib/ai-client.ts
git commit -m "feat(ai): allow profileHref on start-session and upload-session

Adds optional profileHref to the JSON start request and the multipart
upload form. startSessionWithUpload now takes its optional flags via
an options object so future params don't keep extending the positional
signature.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 12: Library list helper for the picker

**Files:**
- Modify: `front-end/src/lib/api/library.ts`

- [ ] **Step 1: Inspect existing helpers**

Open `front-end/src/lib/api/library.ts`. If a function that lists items by oscal type (calling `GET /api/library/type/{oscalType}`) already exists, use it in Task 13 and skip this task. Otherwise, add it.

- [ ] **Step 2: Add `listByOscalType` helper**

Append to `front-end/src/lib/api/library.ts`:

```typescript
export interface LibraryItemSummary {
  itemId: string;
  title: string;
  description?: string;
  version?: string;
  oscalType: string;
  visibility: string;
  updatedAt: string;
}

export interface LibraryListResponse {
  content: LibraryItemSummary[];
  totalElements: number;
}

export const libraryListApi = {
  async listByOscalType(oscalType: string, page = 0, size = 50): Promise<LibraryItemSummary[]> {
    const url = `${API_BASE_URL}/library/type/${encodeURIComponent(oscalType)}?page=${page}&size=${size}&sortBy=updatedAt&sortDir=desc`;
    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers.Authorization = `Bearer ${token}`;
    const res = await fetch(url, { method: 'GET', headers });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const json = (await res.json()) as LibraryListResponse;
    return json.content ?? [];
  },
};
```

If the file already imports `API_BASE_URL`, reuse it; otherwise import from the same module the rest of the file does.

- [ ] **Step 3: Commit**

```bash
git add front-end/src/lib/api/library.ts
git commit -m "feat(library): listByOscalType helper for SSP wizard profile picker

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 13: `SspWizardForm` component

**Files:**
- Create: `front-end/src/components/ai/SspWizardForm.tsx`

- [ ] **Step 1: Implement `SspWizardForm`**

Create `front-end/src/components/ai/SspWizardForm.tsx`:

```typescript
'use client';
import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { aiClient } from '@/lib/ai-client';
import { libraryListApi, type LibraryItemSummary } from '@/lib/api/library';
import { toast } from 'sonner';

interface Props {
  organizationId: number;
  onSessionStarted: (sessionId: string) => void;
}

type SourceTab = 'file' | 'paste';
type ProfileMode = 'library' | 'url' | 'skip';

export function SspWizardForm({ organizationId, onSessionStarted }: Props) {
  const [profileMode, setProfileMode] = useState<ProfileMode>('library');
  const [profileLibraryId, setProfileLibraryId] = useState<string>('');
  const [profileUrl, setProfileUrl] = useState('');
  const [libraryProfiles, setLibraryProfiles] = useState<LibraryItemSummary[]>([]);
  const [loadingProfiles, setLoadingProfiles] = useState(false);

  const [tab, setTab] = useState<SourceTab>('file');
  const [file, setFile] = useState<File | null>(null);
  const [pasted, setPasted] = useState('');
  const [running, setRunning] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setLoadingProfiles(true);
    libraryListApi
      .listByOscalType('profile')
      .then((items) => {
        if (!cancelled) setLibraryProfiles(items);
      })
      .catch(() => {
        if (!cancelled) setLibraryProfiles([]);
      })
      .finally(() => {
        if (!cancelled) setLoadingProfiles(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const profileHref =
    profileMode === 'library' && profileLibraryId
      ? `library:${profileLibraryId}`
      : profileMode === 'url' && profileUrl.trim().length > 0
      ? profileUrl.trim()
      : null;

  const profileValid =
    profileMode === 'skip' ||
    (profileMode === 'library' && profileLibraryId !== '') ||
    (profileMode === 'url' && profileUrl.trim().length > 0);

  const sourceValid =
    (tab === 'file' && file !== null) || (tab === 'paste' && pasted.trim().length > 0);

  const canRun = profileValid && sourceValid;

  const onRun = async () => {
    if (!canRun) return;
    setRunning(true);
    try {
      const res =
        tab === 'file' && file
          ? await aiClient.startSessionWithUpload(organizationId, 'SSP', file, { profileHref })
          : await aiClient.startSession({
              organizationId,
              wizardKind: 'SSP',
              mode: 'STREAMING',
              input: pasted,
              profileHref,
            });
      onSessionStarted(res.sessionId);
    } catch (err) {
      toast.error('Failed to start: ' + (err instanceof Error ? err.message : 'unknown'));
      setRunning(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Build SSP from Source</CardTitle>
      </CardHeader>
      <CardContent className="space-y-6">
        <p className="text-sm text-muted-foreground">
          Upload an architecture document, system description, or existing draft SSP. AI will
          extract system characteristics and draft an OSCAL System Security Plan you can review
          and save.
        </p>

        {/* Profile picker */}
        <div className="space-y-3 border rounded-md p-4">
          <Label className="font-medium">Control baseline</Label>
          <div className="space-y-2">
            <label className="flex items-center gap-2 text-sm">
              <input
                type="radio"
                name="profile-mode"
                value="library"
                checked={profileMode === 'library'}
                onChange={() => setProfileMode('library')}
              />
              Pick a profile from your library
            </label>
            {profileMode === 'library' && (
              <select
                aria-label="Profile from library"
                value={profileLibraryId}
                onChange={(e) => setProfileLibraryId(e.target.value)}
                className="w-full rounded border px-3 py-2 text-sm"
                disabled={loadingProfiles}
              >
                <option value="">{loadingProfiles ? 'Loading…' : 'Select a profile'}</option>
                {libraryProfiles.map((p) => (
                  <option key={p.itemId} value={p.itemId}>
                    {p.title}
                    {p.version ? ` (v${p.version})` : ''}
                  </option>
                ))}
              </select>
            )}

            <label className="flex items-center gap-2 text-sm">
              <input
                type="radio"
                name="profile-mode"
                value="url"
                checked={profileMode === 'url'}
                onChange={() => setProfileMode('url')}
              />
              Paste a profile URL
            </label>
            {profileMode === 'url' && (
              <Input
                aria-label="Profile URL"
                value={profileUrl}
                onChange={(e) => setProfileUrl(e.target.value)}
                placeholder="https://example.com/fedramp-moderate-profile.json"
              />
            )}

            <label className="flex items-center gap-2 text-sm">
              <input
                type="radio"
                name="profile-mode"
                value="skip"
                checked={profileMode === 'skip'}
                onChange={() => setProfileMode('skip')}
              />
              Skip — let AI infer controls from the source document
            </label>
          </div>
        </div>

        {/* Source doc */}
        <div className="flex gap-2 border-b">
          <button
            onClick={() => setTab('file')}
            className={`px-4 py-2 text-sm font-medium ${tab === 'file' ? 'border-b-2 border-primary text-foreground' : 'text-muted-foreground'}`}
          >
            Upload file
          </button>
          <button
            onClick={() => setTab('paste')}
            className={`px-4 py-2 text-sm font-medium ${tab === 'paste' ? 'border-b-2 border-primary text-foreground' : 'text-muted-foreground'}`}
          >
            Paste text
          </button>
        </div>

        {tab === 'file' ? (
          <div className="space-y-2">
            <Label htmlFor="ssp-file-upload">Source document</Label>
            <Input
              id="ssp-file-upload"
              type="file"
              accept=".pdf,.docx,.html,.htm,.txt,.md,.odt,.rtf"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
            <p className="text-sm text-muted-foreground">
              Architecture write-up, system description, security questionnaire, or existing
              draft SSP. PDF, Word, HTML, plain text, Markdown, OpenDocument, or RTF.
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            <Label htmlFor="ssp-paste-text">Paste source content</Label>
            <Textarea
              id="ssp-paste-text"
              rows={12}
              value={pasted}
              onChange={(e) => setPasted(e.target.value)}
              placeholder="Paste the system description or draft SSP text here…"
            />
          </div>
        )}

        <Button onClick={onRun} disabled={!canRun || running}>
          {running ? 'Starting…' : 'Run AI Wizard'}
        </Button>
      </CardContent>
    </Card>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add front-end/src/components/ai/SspWizardForm.tsx
git commit -m "feat(ai): SspWizardForm with profile picker + file/paste source tabs

Three profile-mode states (library / URL / skip) with the same file-vs-
paste source toggle as the catalog and component-def forms. Library
mode populates from /api/library/type/profile.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 14: `SspWizardForm` tests

**Files:**
- Create: `front-end/src/components/ai/SspWizardForm.test.tsx`

- [ ] **Step 1: Write the test**

Create `front-end/src/components/ai/SspWizardForm.test.tsx`:

```typescript
/**
 * @jest-environment jsdom
 */
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { SspWizardForm } from './SspWizardForm';
import { aiClient } from '@/lib/ai-client';
import { libraryListApi } from '@/lib/api/library';

jest.mock('@/lib/ai-client', () => ({
  aiClient: {
    startSession: jest.fn(),
    startSessionWithUpload: jest.fn(),
  },
}));

jest.mock('@/lib/api/library', () => ({
  libraryListApi: {
    listByOscalType: jest.fn(),
  },
}));

jest.mock('sonner', () => ({ toast: { error: jest.fn() } }));

describe('SspWizardForm', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (libraryListApi.listByOscalType as jest.Mock).mockResolvedValue([
      { itemId: 'p-1', title: 'FedRAMP Moderate', version: '5', oscalType: 'profile', visibility: 'PUBLIC', updatedAt: '2026-01-01' },
    ]);
  });

  test('library profile + paste text starts session with library:<id> profileHref', async () => {
    (aiClient.startSession as jest.Mock).mockResolvedValue({ sessionId: 's-1' });
    const onStarted = jest.fn();
    render(<SspWizardForm organizationId={42} onSessionStarted={onStarted} />);

    await waitFor(() => expect(screen.getByText('FedRAMP Moderate (v5)')).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText('Profile from library'), { target: { value: 'p-1' } });

    fireEvent.click(screen.getByRole('button', { name: /paste text/i }));
    fireEvent.change(screen.getByLabelText('Paste source content'), { target: { value: 'system description text' } });

    fireEvent.click(screen.getByRole('button', { name: /run ai wizard/i }));

    await waitFor(() => expect(aiClient.startSession).toHaveBeenCalled());
    expect(aiClient.startSession).toHaveBeenCalledWith(expect.objectContaining({
      organizationId: 42,
      wizardKind: 'SSP',
      input: 'system description text',
      profileHref: 'library:p-1',
    }));
    expect(onStarted).toHaveBeenCalledWith('s-1');
  });

  test('skip mode + file upload starts session with null profileHref', async () => {
    (aiClient.startSessionWithUpload as jest.Mock).mockResolvedValue({ sessionId: 's-2' });
    const onStarted = jest.fn();
    render(<SspWizardForm organizationId={42} onSessionStarted={onStarted} />);

    await waitFor(() => expect(screen.getByText('FedRAMP Moderate (v5)')).toBeInTheDocument());

    fireEvent.click(screen.getByLabelText(/Skip — let AI infer/i));

    const file = new File(['hello'], 'sys.pdf', { type: 'application/pdf' });
    fireEvent.change(screen.getByLabelText('Source document'), { target: { files: [file] } });

    fireEvent.click(screen.getByRole('button', { name: /run ai wizard/i }));

    await waitFor(() => expect(aiClient.startSessionWithUpload).toHaveBeenCalled());
    expect(aiClient.startSessionWithUpload).toHaveBeenCalledWith(
      42,
      'SSP',
      file,
      { profileHref: null },
    );
  });

  test('URL mode requires non-empty URL', async () => {
    render(<SspWizardForm organizationId={42} onSessionStarted={jest.fn()} />);
    await waitFor(() => expect(screen.getByText('FedRAMP Moderate (v5)')).toBeInTheDocument());

    // Switch profile to URL mode but leave it empty.
    fireEvent.click(screen.getByLabelText(/Paste a profile URL/i));

    // Fill the source-doc side so only the profile is gating Run.
    fireEvent.click(screen.getByRole('button', { name: /paste text/i }));
    fireEvent.change(screen.getByLabelText('Paste source content'), { target: { value: 'doc' } });

    // With empty URL, Run is disabled.
    expect(screen.getByRole('button', { name: /run ai wizard/i })).toBeDisabled();

    // Filling the URL re-enables Run.
    fireEvent.change(screen.getByLabelText('Profile URL'), { target: { value: 'https://example.com/p.json' } });
    expect(screen.getByRole('button', { name: /run ai wizard/i })).not.toBeDisabled();
  });
});
```

- [ ] **Step 2: Commit**

```bash
git add front-end/src/components/ai/SspWizardForm.test.tsx
git commit -m "test(ai): SspWizardForm covers library/url/skip profile modes

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 15: Wire `SspWizardForm` into the wizard run page

**Files:**
- Modify: `front-end/src/app/ai/wizard/[kind]/page.tsx`

- [ ] **Step 1: Import the new form**

Add to the imports block:

```typescript
import { SspWizardForm } from '@/components/ai/SspWizardForm';
```

- [ ] **Step 2: Add the SSP hand-off effect**

Below the existing `COMPONENT_DEF` `useEffect` (around line 53–63), add an SSP variant:

```typescript
  useEffect(() => {
    if (
      wizardKind === 'SSP' &&
      session.isComplete &&
      session.finalDocument != null &&
      sessionId
    ) {
      sessionStorage.setItem(`aiDraft:${sessionId}`, JSON.stringify(session.finalDocument));
      router.push(`/build?section=ssp&aiDraft=${sessionId}`);
    }
  }, [wizardKind, session.isComplete, session.finalDocument, sessionId, router]);
```

- [ ] **Step 3: Mount `SspWizardForm` for the SSP kind**

Below the existing `COMPONENT_DEF` form mount (around line 104–106), add:

```typescript
      {!sessionId && wizardKind === 'SSP' && orgId != null && (
        <SspWizardForm organizationId={orgId} onSessionStarted={setSessionId} />
      )}
```

- [ ] **Step 4: Suppress raw-JSON details panel for SSP**

Find the line that gates the `<details>` with the final-output JSON dump (currently around line 179):

```typescript
{session.isComplete && !session.error && session.finalDocument != null && wizardKind !== 'CATALOG' && wizardKind !== 'COMPONENT_DEF' && (
```

Replace with:

```typescript
{session.isComplete && !session.error && session.finalDocument != null && wizardKind !== 'CATALOG' && wizardKind !== 'COMPONENT_DEF' && wizardKind !== 'SSP' && (
```

- [ ] **Step 5: Commit**

```bash
git add front-end/src/app/ai/wizard/[kind]/page.tsx
git commit -m "feat(ai): mount SspWizardForm and route to /build on completion

When the SSP session completes, stash the doc in sessionStorage and
redirect to /build?section=ssp&aiDraft=<id>. Suppress the raw-JSON
details panel for SSP since the editor takes over.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 16: `OscalDocumentWizard.initialDocument` prop + test

**Files:**
- Modify: `front-end/src/components/build/OscalDocumentWizard.tsx`
- Modify: `front-end/src/components/build/OscalDocumentWizard.test.tsx`

- [ ] **Step 1: Add the new prop and seeding logic**

Add to the props interface (around line 52):

```typescript
interface OscalDocumentWizardProps {
  modelType: GenericOscalModelSlug;
  editingDocument?: OscalDocumentResponse | null;
  /**
   * AI-generated draft to seed a fresh wizard. Ignored when editingDocument
   * is set. Shape: the wrapped JSON body, e.g. `{ "system-security-plan": {...} }`.
   */
  initialDocument?: unknown;
  onSaveComplete?: () => void;
  onCancel?: () => void;
  userOrganizationId?: number | null;
}
```

Update the function signature (around line 156) to receive it:

```typescript
export function OscalDocumentWizard({
  modelType,
  editingDocument,
  initialDocument,
  onSaveComplete,
  onCancel,
  userOrganizationId,
}: OscalDocumentWizardProps) {
```

In the `useEffect` that reacts to `editingDocument` (around lines 196–233), add an `else-if` branch before the empty-state case:

```typescript
  useEffect(() => {
    let cancelled = false;
    async function load() {
      if (editingDocument) {
        setIsLoading(true);
        setError(null);
        setIsDraft(editingDocument.draft);
        setSavedDocId(editingDocument.id);
        try {
          const raw = await oscalDocumentApi.getContent(editingDocument.id);
          const next = parseLoadedDoc(modelType, raw);
          if (!cancelled) {
            setDoc(next);
            setBodyText(JSON.stringify(next.body, null, 2));
            setStep(1);
          }
        } catch (e) {
          if (!cancelled) setError(`Failed to load document: ${e instanceof Error ? e.message : 'unknown'}`);
        } finally {
          if (!cancelled) setIsLoading(false);
        }
        return;
      }

      if (initialDocument) {
        try {
          const next = parseLoadedDoc(modelType, JSON.stringify(initialDocument));
          setDoc(next);
          setBodyText(JSON.stringify(next.body, null, 2));
          setStep(1);
          setSuccess(false);
          setIsDraft(true);
          setSavedDocId(null);
        } catch (e) {
          setError(`Failed to seed document: ${e instanceof Error ? e.message : 'unknown'}`);
        }
        return;
      }

      const fresh = emptyParsedDoc(modelType);
      setDoc(fresh);
      setBodyText(JSON.stringify(fresh.body, null, 2));
      setStep(1);
      setSuccess(false);
      setIsDraft(true);
      setSavedDocId(null);
    }
    void load();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editingDocument, initialDocument, modelType]);
```

- [ ] **Step 2: Add the seeding test**

Append to `front-end/src/components/build/OscalDocumentWizard.test.tsx` (use the file's existing test scaffolding for imports):

```typescript
test('initialDocument seeds the wizard with metadata, import, and body', async () => {
  const draft = {
    'system-security-plan': {
      uuid: '00000000-0000-0000-0000-000000000ff0',
      metadata: {
        title: 'Acme Trust Center SSP',
        version: '1.0',
        'oscal-version': '1.1.2',
        'last-modified': '2026-05-08T00:00:00Z',
      },
      'import-profile': { href: 'library:p-1' },
      'system-characteristics': {
        'system-name': 'Acme Trust Center',
        description: 'Web app',
        'system-ids': [{ id: 'acme-trust' }],
        'security-sensitivity-level': 'moderate',
        'system-information': { 'information-types': [] },
        'security-impact-level': {
          'security-objective-confidentiality': 'moderate',
          'security-objective-integrity': 'moderate',
          'security-objective-availability': 'moderate',
        },
        status: { state: 'operational' },
        'authorization-boundary': { description: 'Cloud Run.' },
      },
      'system-implementation': { users: [], components: [] },
      'control-implementation': {
        description: 'Drafted from source',
        'implemented-requirements': [],
      },
    },
  };

  const { getByDisplayValue } = render(
    <OscalDocumentWizard
      modelType="system-security-plan"
      initialDocument={draft}
    />,
  );

  // Title input is populated from initialDocument.metadata.title
  await waitFor(() => expect(getByDisplayValue('Acme Trust Center SSP')).toBeInTheDocument());
});
```

- [ ] **Step 3: Commit**

```bash
git add front-end/src/components/build/OscalDocumentWizard.tsx \
        front-end/src/components/build/OscalDocumentWizard.test.tsx
git commit -m "feat(build): OscalDocumentWizard accepts initialDocument seed

Mirrors initialCatalog/initialComponent on the catalog/component
builders. Used by the AI SSP wizard hand-off so the editor opens
pre-populated with the AI draft.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 17: Hydrate `?aiDraft=…&section=ssp` on `/build`

**Files:**
- Modify: `front-end/src/app/build/page.tsx`

- [ ] **Step 1: Add SSP draft state**

Around line 82, alongside the existing `aiDraftCatalog` and `aiDraftComponent` state declarations, add:

```typescript
const [aiDraftSsp, setAiDraftSsp] = useState<unknown | null>(null);
```

- [ ] **Step 2: Extend the hydration `useEffect`**

In the existing hydration effect (around lines 91–130), append a new branch after the `components` branch:

```typescript
    } else if (aiDraft && sec === 'ssp') {
      const raw = sessionStorage.getItem(`aiDraft:${aiDraft}`);
      if (raw) {
        try {
          const parsed = JSON.parse(raw) as unknown;
          setAiDraftSsp(parsed);
          setSection('ssp');
          setMode('create');
          setEditingDoc(null);
          sessionStorage.removeItem(`aiDraft:${aiDraft}`);
          window.history.replaceState({}, '', '/build');
          aiDraftHydrated.current = true;
        } catch {
          // ignore malformed draft
        }
      }
    }
```

- [ ] **Step 3: Reset SSP draft in `onSaveComplete` and on cancel**

In `onSaveComplete` (around lines 164–173), append:

```typescript
setAiDraftSsp(null);
```

In the SSP rendering branch — the loop that renders generic OSCAL documents (around lines 377–408) — pass `initialDocument` only for SSP. Replace that loop verbatim with:

```tsx
{(['ssp', 'assessment-plan', 'assessment-results', 'poam'] as const).map((key) => {
  const cfg = GENERIC_SECTIONS[key];
  return (
    <TabsContent key={key} value={key} className="space-y-6">
      {mode === 'list' ? (
        <BuiltDocList
          docType={cfg.slug}
          reloadKey={reloadKey}
          onCreateNew={() => {
            setEditingDoc(null);
            setMode('create');
          }}
          onEdit={(doc) => {
            setEditingDoc(doc as OscalDocumentResponse);
            setMode('create');
          }}
        />
      ) : (
        <OscalDocumentWizard
          modelType={cfg.slug}
          editingDocument={editingDoc}
          initialDocument={key === 'ssp' ? aiDraftSsp : undefined}
          onSaveComplete={onSaveComplete}
          userOrganizationId={orgId}
          onCancel={() => {
            setMode('list');
            setEditingDoc(null);
            if (key === 'ssp') setAiDraftSsp(null);
          }}
        />
      )}
    </TabsContent>
  );
})}
```

The only differences from the existing block are: (a) the `initialDocument` prop on `OscalDocumentWizard`, gated to SSP; (b) the additional `setAiDraftSsp(null)` inside the `onCancel` handler when the active tab is SSP. The `BuiltDocList` block is unchanged.

- [ ] **Step 4: Commit**

```bash
git add front-end/src/app/build/page.tsx
git commit -m "feat(build): hydrate ?aiDraft=...&section=ssp into the SSP editor

Mirrors the existing catalog and component AI-draft hydration paths.
The SSP tab renders OscalDocumentWizard with initialDocument seeded
from sessionStorage so the user lands directly on Step 1 of the editor
with the AI output pre-populated.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 18: Low-confidence review panel on SSP editor Step 3

**Files:**
- Create: `front-end/src/components/build/oscal/AiConfidencePanel.tsx`
- Modify: `front-end/src/components/build/OscalDocumentWizard.tsx`
- Modify: `front-end/src/components/build/OscalDocumentWizard.test.tsx`

- [ ] **Step 1: Create the panel component**

Create `front-end/src/components/build/oscal/AiConfidencePanel.tsx`:

```typescript
'use client';
import { useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Sparkles } from 'lucide-react';

interface ImplementedRequirement {
  uuid?: string;
  'control-id'?: string;
  description?: string;
  props?: Array<{ name?: string; ns?: string; value?: string }>;
}

interface AiConfidenceCounts {
  high: number;
  medium: number;
  low: number;
  total: number;
}

export function readConfidenceCounts(body: unknown): AiConfidenceCounts {
  const empty: AiConfidenceCounts = { high: 0, medium: 0, low: 0, total: 0 };
  if (!body || typeof body !== 'object') return empty;
  const ctrlImpl = (body as Record<string, unknown>)['control-implementation'];
  if (!ctrlImpl || typeof ctrlImpl !== 'object') return empty;
  const reqs = (ctrlImpl as Record<string, unknown>)['implemented-requirements'];
  if (!Array.isArray(reqs)) return empty;
  const counts = { ...empty };
  for (const r of reqs as ImplementedRequirement[]) {
    const prop = (r.props ?? []).find((p) => p?.name === 'ai-confidence');
    if (!prop) continue;
    counts.total += 1;
    if (prop.value === 'high') counts.high += 1;
    else if (prop.value === 'medium') counts.medium += 1;
    else if (prop.value === 'low') counts.low += 1;
  }
  return counts;
}

interface Props {
  body: unknown;
  /**
   * Optional callback invoked with a control-id when the user wants to
   * locate that control's narrative in the JSON editor. Provided by
   * OscalDocumentWizard which knows about the Monaco instance.
   */
  onLocate?: (controlId: string) => void;
}

export function AiConfidencePanel({ body, onLocate }: Props) {
  const counts = useMemo(() => readConfidenceCounts(body), [body]);
  const [showLow, setShowLow] = useState(false);

  if (counts.total === 0) return null;

  const lowEntries: ImplementedRequirement[] = useMemo(() => {
    if (!body || typeof body !== 'object') return [];
    const ctrlImpl = (body as Record<string, unknown>)['control-implementation'];
    if (!ctrlImpl || typeof ctrlImpl !== 'object') return [];
    const reqs = (ctrlImpl as Record<string, unknown>)['implemented-requirements'];
    if (!Array.isArray(reqs)) return [];
    return (reqs as ImplementedRequirement[]).filter((r) => {
      const prop = (r.props ?? []).find((p) => p?.name === 'ai-confidence');
      return prop?.value === 'low';
    });
  }, [body]);

  return (
    <div className="rounded-md border bg-indigo-50/40 dark:bg-indigo-950/20 p-3 space-y-2">
      <div className="flex items-center gap-2">
        <Sparkles className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />
        <span className="text-sm font-medium">AI confidence</span>
        <Badge variant="outline">{counts.high} high</Badge>
        <Badge variant="outline">{counts.medium} medium</Badge>
        <Badge variant="outline" className="border-amber-500 text-amber-700 dark:text-amber-300">
          {counts.low} low
        </Badge>
        <span className="text-sm text-muted-foreground ml-1">/ {counts.total} controls drafted by AI</span>
        {counts.low > 0 && (
          <Button
            size="sm"
            variant="outline"
            className="ml-auto"
            onClick={() => setShowLow((v) => !v)}
          >
            {showLow ? 'Hide low confidence' : 'Review low confidence'}
          </Button>
        )}
      </div>

      {showLow && (
        <div className="space-y-2 max-h-72 overflow-auto">
          {lowEntries.map((r, i) => (
            <div key={r.uuid ?? i} className="rounded border bg-background p-2 text-sm">
              <div className="flex items-center gap-2 mb-1">
                <code className="text-xs font-mono">{r['control-id']}</code>
                {onLocate && (
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => onLocate(r['control-id'] ?? '')}
                  >
                    Find in editor
                  </Button>
                )}
              </div>
              <div className="text-xs text-muted-foreground line-clamp-3">{r.description}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: Render the panel inside `OscalDocumentWizard` Step 3**

In `OscalDocumentWizard.tsx`, add the import:

```typescript
import { AiConfidencePanel } from '@/components/build/oscal/AiConfidencePanel';
```

Find the JSX block that renders the body editor on Step 3. Just above the `<LazyMonacoEditor …>` (or whatever the Monaco wrapper is named), insert (gate to SSP only):

```tsx
{step === 3 && modelType === 'system-security-plan' && (
  <AiConfidencePanel body={doc.body} />
)}
```

The `onLocate` prop is omitted for v1 — the panel still renders the list and counts. Wiring `onLocate` to Monaco's `findMatches` is a v1.5 polish and is intentionally not in this plan.

- [ ] **Step 3: Add tests**

Append to `OscalDocumentWizard.test.tsx`:

```typescript
test('renders AI confidence panel on SSP step 3 when body has ai-confidence props', async () => {
  const draft = {
    'system-security-plan': {
      uuid: '00000000-0000-0000-0000-000000000abc',
      metadata: { title: 't', version: '1', 'oscal-version': '1.1.2', 'last-modified': 'now' },
      'import-profile': { href: 'library:p-1' },
      'system-characteristics': {
        'system-name': 't', description: '',
        'system-ids': [{ id: 's' }],
        'security-sensitivity-level': 'moderate',
        'system-information': { 'information-types': [] },
        'security-impact-level': {
          'security-objective-confidentiality': 'moderate',
          'security-objective-integrity': 'moderate',
          'security-objective-availability': 'moderate',
        },
        status: { state: 'operational' },
        'authorization-boundary': { description: '' },
      },
      'system-implementation': { users: [], components: [] },
      'control-implementation': {
        description: '',
        'implemented-requirements': [
          { uuid: 'u1', 'control-id': 'ac-1', description: 'D1', props: [{ name: 'ai-confidence', ns: 'https://oscal-hub.io/ns', value: 'high' }] },
          { uuid: 'u2', 'control-id': 'ac-2', description: 'D2', props: [{ name: 'ai-confidence', ns: 'https://oscal-hub.io/ns', value: 'low' }] },
        ],
      },
    },
  };

  render(<OscalDocumentWizard modelType="system-security-plan" initialDocument={draft} />);

  // Navigate to step 3 — depends on existing test helpers in the file.
  // The test simply confirms the counts render.
  // (Adjust the step navigation per the file's existing helpers.)
  await waitFor(() => expect(screen.getByText(/AI confidence/i)).toBeInTheDocument());
  expect(screen.getByText(/1 high/i)).toBeInTheDocument();
  expect(screen.getByText(/0 medium/i)).toBeInTheDocument();
  expect(screen.getByText(/1 low/i)).toBeInTheDocument();
});

test('AI confidence panel does not render for non-SSP models', () => {
  const draft = {
    'plan-of-action-and-milestones': {
      uuid: 'x',
      metadata: { title: 'p', version: '1', 'oscal-version': '1.1.2', 'last-modified': 'now' },
      observations: [], risks: [], findings: [], 'poam-items': [],
    },
  };
  render(<OscalDocumentWizard modelType="plan-of-action-and-milestones" initialDocument={draft} />);
  expect(screen.queryByText(/AI confidence/i)).not.toBeInTheDocument();
});
```

If the existing test file doesn't already import `screen` and `waitFor` from `@testing-library/react`, add them to the import. The existing tests in the file already render `OscalDocumentWizard`; reuse the same import patterns.

- [ ] **Step 4: Commit**

```bash
git add front-end/src/components/build/oscal/AiConfidencePanel.tsx \
        front-end/src/components/build/OscalDocumentWizard.tsx \
        front-end/src/components/build/OscalDocumentWizard.test.tsx
git commit -m "feat(build): AI confidence review panel on SSP editor step 3

Reads ai-confidence props from the SSP body, surfaces high/medium/low
counts, and offers a 'review low confidence' drawer listing each
low-confidence requirement. Only renders for SSPs that contain at
least one ai-confidence prop, so editing a hand-built SSP is unchanged.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 19: Update `/ai/wizard` index — remove Profile, enable SSP

**Files:**
- Modify: `front-end/src/app/ai/wizard/page.tsx`

- [ ] **Step 1: Replace `OPTIONS`**

Replace the `OPTIONS` array (lines 15–21) with:

```typescript
const OPTIONS: WizardOption[] = [
  { kind: 'CATALOG', title: 'Build Catalog from Source', description: 'Drop a PDF, Word doc, HTML, or paste text — AI drafts an OSCAL catalog you can review and save.', available: true },
  { kind: 'COMPONENT_DEF', title: 'Build Component-definition from STIG / CIS / Config Guide', description: 'Drop a STIG, CIS Benchmark, or vendor configuration guide — AI maps the recommended settings to NIST 800-53 controls and drafts an OSCAL component-definition.', available: true },
  { kind: 'SSP', title: 'Draft SSP from Source', description: 'Drop an architecture doc, system description, or existing draft SSP — AI extracts system characteristics and drafts an OSCAL System Security Plan you can review and save.', available: true },
  { kind: 'POAM', title: 'Draft POA&M', description: 'Coming soon.', available: false },
];
```

- [ ] **Step 2: Commit**

```bash
git add front-end/src/app/ai/wizard/page.tsx
git commit -m "feat(ai): enable SSP wizard, remove placeholder Build Profile card

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 20: End-to-end manual verification

This is a checklist for the user (and reviewer) to run through after the user has rebuilt and restarted servers.

- [ ] Restart backend + frontend (`./stop.sh && ./dev.sh`), hard refresh the browser, log in.
- [ ] Navigate to `/ai/wizard`. Confirm:
  - "Build Profile" card is gone.
  - "Draft SSP from Source" card is enabled.
- [ ] Click Start on Draft SSP. Confirm form renders with:
  - Profile picker showing three radio options.
  - Library dropdown populated (or empty if no profiles in library).
  - File / paste tabs for source doc.
- [ ] Test path A: pick a library profile + paste system-description text + Run.
  - Watch streaming progress: "Reading source document…", "Extracting system characteristics…", "Resolving profile…", "Profile resolved — N controls", "Drafting implementation narratives (1 of N)…"
  - On complete, should redirect to `/build?section=ssp` with the SSP editor open.
  - Editor metadata, import-profile.href, system-characteristics, control-implementation pre-populated.
- [ ] Step through the editor to Step 3 (Body). Confirm:
  - AI confidence panel renders above the JSON editor with high/medium/low counts.
  - "Review low confidence" button shows the drawer of low-confidence requirements.
- [ ] Step 5 (Review & Save). Confirm validation passes (or surfaces real issues), Save Final works, "Save to Library" flow works.
- [ ] Test path B: skip profile + drop a PDF + Run.
  - Confirm AI extracts a controlIds set from the doc and drafts narratives for them.
  - import-profile.href is empty in the editor; user can fill it before final save.
- [ ] Test path C: pick a URL profile that 404s + paste text + Run.
  - Confirm the stream shows "Profile resolution failed: ... — falling back to controls inferred from source document."
  - Wizard still completes successfully.

---

## Cross-task verification

After all tasks land, the user should run the full backend test suite (`mvn -pl back-end test`) and the full frontend test suite (`npm --prefix front-end test`) to confirm nothing else regressed. New tests added by this plan:

- Backend: `SspChunkingStrategyTest`, `SspPromptBuilderTest`, `ProfileControlIdExtractorTest`, `ProfileSourceLoaderTest`, `SspWizardTest` (3 cases).
- Frontend: `SspWizardForm.test.tsx` (3 cases), additions in `OscalDocumentWizard.test.tsx` (3 cases).
