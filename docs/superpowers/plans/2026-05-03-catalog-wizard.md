# OSCAL Catalog Wizard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** First user-facing AI generator wizard. User uploads any document (PDF, `.docx`, `.html`, `.txt`, etc.), Claude drafts an OSCAL Catalog with controls / parts / params / groups, draft loads into the existing `CatalogBuilderWizard` for review.

**Architecture:** Apache Tika normalizes any input format into XHTML+text. A new `CatalogWizard` service orchestrates four passes via the existing `AnthropicClient`: outline → per-family generation with `validate_oscal` self-correct loop → merge → final validate. Progress streams over the foundation's existing SSE infrastructure. On `complete`, the FE stashes the catalog JSON in `sessionStorage` and routes to `/build?section=catalogs&aiDraft=<sessionId>`, where `BuildPage` hydrates `CatalogBuilderWizard`.

**Tech Stack:** Spring Boot 4.0.6, Java 25, Apache Tika 3.x, `liboscal-java`, `com.anthropic:anthropic-java`, Next.js 16, vitest.

**Spec:** `docs/superpowers/specs/2026-05-03-catalog-wizard-design.md`

**Plan structure:**
- **Phase E** — Foundation cleanup (Tika, normalization, KnowledgeLoader targeting): E1–E4
- **Phase F** — Catalog wizard backend: F1–F5
- **Phase G** — Frontend: G1–G5
- **Phase H** — Validation: H1

---

## Phase E — Foundation cleanup

### Task E1: Add Tika, remove standalone POI

**Files:**
- Modify: `back-end/pom.xml`

- [ ] **Step 1: Edit deps**

In `<properties>`, replace `<poi-ooxml.version>` with:

```xml
<tika.version>3.2.5</tika.version>
```

In `<dependencies>`, replace the `org.apache.poi:poi-ooxml` entry with:

```xml
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
    <version>${tika.version}</version>
</dependency>
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-parsers-standard-package</artifactId>
    <version>${tika.version}</version>
</dependency>
```

- [ ] **Step 2: Verify Maven resolves**

Run: `cd back-end && mvn -q dependency:resolve -DincludeArtifactIds=tika-core,tika-parsers-standard-package`
Expected: exit 0; both artifacts resolve.

- [ ] **Step 3: Verify the existing test that uses POI directly still passes**

`SourceIngestorTest.docxIsExtractedToText` constructs an in-memory `XWPFDocument`. POI is still on the classpath transitively via Tika, but if compilation breaks, switch the test to use Tika's own `WordprocessingMLPackage` or replace the in-memory generation with a checked-in fixture file.

Run: `mvn -q -Dtest=SourceIngestorTest test`
Expected: PASS, or compile error pointing to a needed adjustment.

If the test passes, no further action. If it fails, also add `org.apache.poi:poi-ooxml` back as `scope=test` only — Tika's parser package may not expose `XWPFDocument` for direct use.

- [ ] **Step 4: Commit**

```bash
git add back-end/pom.xml
git commit -m "deps(ai): swap standalone POI for Apache Tika"
```

---

### Task E2: `DocumentNormalizer` service

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/DocumentNormalizer.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/NormalizedDoc.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/DocumentNormalizerTest.java`

- [ ] **Step 1: `NormalizedDoc` record**

```java
package gov.nist.oscal.tools.api.service.ai;

public record NormalizedDoc(
        String plainText,
        String xhtml,
        String detectedMime,
        String filename,
        int charCount
) { }
```

- [ ] **Step 2: Write the failing test**

`DocumentNormalizerTest.java`:

```java
package gov.nist.oscal.tools.api.service.ai;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentNormalizerTest {

    private final DocumentNormalizer normalizer = new DocumentNormalizer();

    @Test
    void plainTextPassesThrough() {
        NormalizedDoc d = normalizer.normalize("Hello world");
        assertThat(d.plainText()).isEqualTo("Hello world");
        assertThat(d.detectedMime()).isEqualTo("text/plain");
    }

    @Test
    void docxIsExtractedToTextAndXhtml() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XWPFDocument doc = new XWPFDocument()) {
            doc.createParagraph().createRun().setText("Document body for test.");
            doc.write(bos);
        }
        NormalizedDoc d = normalizer.normalize(bos.toByteArray(), "test.docx");
        assertThat(d.plainText()).contains("Document body for test");
        assertThat(d.xhtml()).contains("Document body for test");
        assertThat(d.detectedMime()).contains("wordprocessingml");
    }

    @Test
    void htmlIsExtractedAndStripped() {
        String html = "<html><body><h1>Heading</h1><p>Body text.</p></body></html>";
        NormalizedDoc d = normalizer.normalize(html.getBytes(), "test.html");
        assertThat(d.plainText()).contains("Heading");
        assertThat(d.plainText()).contains("Body text");
        // XHTML output preserves structure
        assertThat(d.xhtml()).containsAnyOf("<h1", "Heading");
    }
}
```

- [ ] **Step 3: Run test to confirm FAIL**

Run: `mvn -q -Dtest=DocumentNormalizerTest test`
Expected: FAIL, class not found.

- [ ] **Step 4: Implement `DocumentNormalizer`**

```java
package gov.nist.oscal.tools.api.service.ai;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.apache.tika.sax.ToTextContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class DocumentNormalizer {

    private static final Logger log = LoggerFactory.getLogger(DocumentNormalizer.class);
    private static final int MAX_CHARS = 1_500_000;

    public NormalizedDoc normalize(String plainText) {
        String safe = plainText == null ? "" : plainText;
        if (safe.length() > MAX_CHARS) {
            throw new IllegalArgumentException("Input exceeds " + MAX_CHARS + " chars");
        }
        return new NormalizedDoc(safe, safe, "text/plain", null, safe.length());
    }

    public NormalizedDoc normalize(byte[] bytes, String filename) {
        try {
            AutoDetectParser parser = new AutoDetectParser();
            Metadata md = new Metadata();
            if (filename != null) md.set(Metadata.RESOURCE_NAME_KEY, filename);

            ToHTMLContentHandler html = new ToHTMLContentHandler();
            parser.parse(new ByteArrayInputStream(bytes), html, md, new ParseContext());
            String xhtml = html.toString();

            ToTextContentHandler text = new ToTextContentHandler();
            parser.parse(new ByteArrayInputStream(bytes), text, new Metadata(), new ParseContext());
            String plain = text.toString();

            if (plain.length() > MAX_CHARS) {
                throw new IllegalArgumentException("Extracted text exceeds " + MAX_CHARS + " chars");
            }

            String mime = md.get(Metadata.CONTENT_TYPE);
            log.info("Normalized document filename={} mime={} chars={}", filename, mime, plain.length());
            return new NormalizedDoc(plain, xhtml, mime, filename, plain.length());
        } catch (TikaException | IOException | SAXException e) {
            throw new IllegalArgumentException("Failed to normalize document: " + e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 5: Run test to confirm PASS**

Run: `mvn -q -Dtest=DocumentNormalizerTest test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/DocumentNormalizer.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/NormalizedDoc.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/DocumentNormalizerTest.java
git commit -m "feat(ai): DocumentNormalizer wraps Apache Tika for multi-format extraction"
```

---

### Task E3: `SourceIngestor` delegates to `DocumentNormalizer`

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/SourceIngestor.java`

- [ ] **Step 1: Inject `DocumentNormalizer` and replace docx-specific code**

Add a constructor parameter:

```java
private final DocumentNormalizer normalizer;

public SourceIngestor(DocumentNormalizer normalizer) {
    this.normalizer = normalizer;
}
```

Replace `ingestDocx(byte[])` body with:

```java
public IngestedSource ingestDocx(byte[] bytes) {
    NormalizedDoc d = normalizer.normalize(bytes, "input.docx");
    return ingestText(d.plainText());
}
```

Add a generic method that handles any Tika-supported format:

```java
public IngestedSource ingestAny(byte[] bytes, String filename) {
    // PDF stays as PDF (Claude handles natively); everything else goes through Tika.
    if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
        return ingestPdf(filename, bytes);
    }
    NormalizedDoc d = normalizer.normalize(bytes, filename);
    return ingestText(d.plainText());
}
```

- [ ] **Step 2: Update `SourceIngestorTest` to construct with the normalizer**

Replace `private final SourceIngestor ingestor = new SourceIngestor();` with:

```java
private final SourceIngestor ingestor = new SourceIngestor(new DocumentNormalizer());
```

- [ ] **Step 3: Run tests**

Run: `mvn -q -Dtest=SourceIngestorTest test`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/SourceIngestor.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/SourceIngestorTest.java
git commit -m "refactor(ai): SourceIngestor delegates docx + generic ingestion to Tika normalizer"
```

---

### Task E4: `KnowledgeLoader.systemFor` per-WizardKind targeting

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/KnowledgeLoader.java`
- Modify: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/KnowledgeLoaderTest.java`

- [ ] **Step 1: Update test to assert targeted loading for CATALOG**

Add a test that catalog system prompt contains the test-oscal-catalog fixture content but NOT unrelated skill content. Since the test fixtures from B2 are minimal (one oscal-basics, one oscal-catalog, one metaschema-basics), targeted CATALOG loading should include all three test fixtures (which match the production target list). For other kinds (PROFILE, SSP, etc.), the existing load-all test continues to pass — they keep loading everything until their own wizard plans tighten them.

Update the `loadsCatalogSystemPromptFromFixture` test:

```java
@Test
void loadsCatalogSystemPromptIncludesBasicsAndCatalog() {
    Path root = Paths.get("src/test/resources/claude-plugins");
    KnowledgeLoader loader = new KnowledgeLoader(root);
    String prompt = loader.systemFor(WizardKind.CATALOG);

    assertThat(prompt).contains("OSCAL Layer Overview"); // from test-oscal-basics
    assertThat(prompt).contains("Catalog skill");        // from test-oscal-catalog
    assertThat(prompt).contains("Metaschema Constraints"); // from test-metaschema-basics
}

@Test
void loadsProfileSystemPromptStillLoadsAllForUntightenedKinds() {
    Path root = Paths.get("src/test/resources/claude-plugins");
    KnowledgeLoader loader = new KnowledgeLoader(root);
    String prompt = loader.systemFor(WizardKind.PROFILE);
    // PROFILE wizard plan hasn't landed yet — still load-all
    assertThat(prompt).contains("OSCAL Layer Overview");
    assertThat(prompt).contains("Catalog skill");
}
```

- [ ] **Step 2: Update `systemFor` implementation**

Replace the per-kind switch with the targeted strategy:

```java
public String systemFor(WizardKind kind) {
    StringBuilder sb = new StringBuilder();
    sb.append("You are an expert OSCAL author working inside OSCAL Hub. ");
    sb.append("Always produce schema-valid OSCAL output. Use the validate_oscal tool to confirm.\n\n");

    if (kind == WizardKind.SMOKE) {
        sb.append("This is a smoke-test wizard. Reply concisely.\n");
        return sb.toString();
    }

    if (kind == WizardKind.CATALOG) {
        appendSkillsFrom(sb, pluginRoot.resolve("plugins/oscal/skills/oscal-basics"));
        appendSkillsFrom(sb, pluginRoot.resolve("plugins/oscal/skills/oscal-catalog"));
        appendSkillsFrom(sb, pluginRoot.resolve("plugins/metaschema/skills/metaschema-basics"));
        sb.append("\nFocus: produce an OSCAL Catalog with controls, parts, params, groups.\n");
        return sb.toString();
    }

    // Other kinds keep load-all behavior until their own wizard plans land.
    appendSkillsFrom(sb, pluginRoot.resolve("plugins/oscal/skills"));
    appendSkillsFrom(sb, pluginRoot.resolve("plugins/metaschema/skills"));
    return sb.toString();
}
```

`appendSkillsFrom` already walks recursively, so passing `oscal-catalog/` walks the catalog subtree only.

But the test fixtures in `B2` are at `plugins/oscal/skills/test-oscal-basics/SKILL.md` (NOT `oscal-basics`). We need either to rename the test fixtures, or to make the test directory list match the production names. Easier: rename test fixtures.

- [ ] **Step 3: Rename test fixtures to match production directory names**

```bash
cd back-end/src/test/resources/claude-plugins/plugins/oscal/skills
mv test-oscal-basics oscal-basics
mv test-oscal-catalog oscal-catalog

cd ../../../metaschema/skills
mv test-metaschema-basics metaschema-basics
```

(Or use `git mv` if they're already tracked.)

- [ ] **Step 4: Run tests**

Run: `mvn -q -Dtest=KnowledgeLoaderTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/KnowledgeLoader.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/KnowledgeLoaderTest.java \
        back-end/src/test/resources/claude-plugins
git commit -m "refactor(ai): KnowledgeLoader targets oscal-basics + oscal-catalog for CATALOG"
```

---

## Phase F — Catalog wizard backend

### Task F1: Multipart upload endpoint on `AiSessionController`

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AiSessionController.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/CatalogWizardInput.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AiOrchestrator.java` — accept input bytes/filename in `start()` signature
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/WizardContext.java` — add optional `inputBytes` and `inputFilename` fields
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/controller/AiSessionControllerTest.java` — add multipart happy-path test

- [ ] **Step 1: Extend `WizardContext` to carry binary input**

```java
public record WizardContext(
        UUID sessionId,
        Long organizationId,
        Long userId,
        String apiKey,
        String model,
        String input,
        byte[] inputBytes,
        String inputFilename
) {
    public static WizardContext text(UUID id, Long orgId, Long userId, String apiKey, String model, String input) {
        return new WizardContext(id, orgId, userId, apiKey, model, input, null, null);
    }
    public static WizardContext file(UUID id, Long orgId, Long userId, String apiKey, String model, byte[] bytes, String filename) {
        return new WizardContext(id, orgId, userId, apiKey, model, null, bytes, filename);
    }
}
```

Update `SmokeWizard` and `AiOrchestrator` to use `WizardContext.text(...)` instead of the old constructor.

- [ ] **Step 2: Extend `AiOrchestrator.start` to accept optional file**

Add an overload:

```java
public UUID start(Long organizationId, Long userId, WizardKind kind, AiSessionMode mode,
                  String input, byte[] inputBytes, String inputFilename) {
    UUID id = UUID.randomUUID();
    String apiKey = settings.requireApiKey(organizationId);
    String model = settings.getDefaultModel(organizationId);
    AiSession session = new AiSession();
    // ... same as before ...
    sessions.save(session);

    Wizard wizard = router.get(kind);
    WizardContext ctx = inputBytes != null
        ? WizardContext.file(id, organizationId, userId, apiKey, model, inputBytes, inputFilename)
        : WizardContext.text(id, organizationId, userId, apiKey, model, input);
    asyncRunner.run(wizard, ctx);
    return id;
}
```

The original 5-arg `start(...)` keeps working — it can just delegate to the new method with `null` for bytes/filename.

- [ ] **Step 3: Add `/upload` endpoint to controller**

In `AiSessionController.java`:

```java
@Operation(summary = "Start an AI wizard session with a file upload")
@PreAuthorize("isAuthenticated()")
@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<StartSessionResponse> startWithUpload(
        @RequestParam Long organizationId,
        @RequestParam WizardKind wizardKind,
        @RequestParam(required = false, defaultValue = "STREAMING") AiSessionMode mode,
        @RequestParam(required = false) String prompt,
        @RequestPart MultipartFile file) throws IOException {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    User user = users.findByUsername(username).orElseThrow();
    requireOrgMembership(user, organizationId);
    UUID id = orchestrator.start(organizationId, user.getId(), wizardKind, mode,
            prompt, file.getBytes(), file.getOriginalFilename());
    return ResponseEntity.ok(new StartSessionResponse(id));
}
```

The `requireOrgMembership` helper already exists from the foundation review fix (commit `74846ff`).

- [ ] **Step 4: Add controller test for the upload endpoint**

```java
@Test
@WithMockUser(username = "alice", roles = "USER")
void startWithUploadAcceptsMultipartFile() throws Exception {
    User u = new User();
    u.setId(7L);
    u.setUsername("alice");
    when(users.findByUsername("alice")).thenReturn(Optional.of(u));
    OrganizationMembership m = new OrganizationMembership();
    m.setStatus(OrganizationMembership.MembershipStatus.ACTIVE);
    when(memberships.findByUserIdAndOrganizationId(7L, 1L)).thenReturn(Optional.of(m));

    UUID expected = UUID.randomUUID();
    when(orchestrator.start(eq(1L), eq(7L), eq(WizardKind.CATALOG),
            eq(AiSessionMode.STREAMING), isNull(), any(byte[].class), eq("input.pdf")))
        .thenReturn(expected);

    MockMultipartFile file = new MockMultipartFile("file", "input.pdf",
            "application/pdf", new byte[]{1, 2, 3});
    mockMvc.perform(multipart("/api/ai/sessions/upload")
                    .file(file)
                    .param("organizationId", "1")
                    .param("wizardKind", "CATALOG")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value(expected.toString()));
}
```

- [ ] **Step 5: Run tests**

Run: `mvn -q -Dtest=AiSessionControllerTest test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/WizardContext.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/SmokeWizard.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AiOrchestrator.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/controller/AiSessionController.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/controller/AiSessionControllerTest.java
git commit -m "feat(ai): multipart upload endpoint for wizard inputs"
```

---

### Task F2: `CatalogPromptBuilder`

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/CatalogPromptBuilder.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/CatalogPromptBuilderTest.java`

- [ ] **Step 1: Implement the prompt builder**

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogPromptBuilder {

    public String outlinePrompt() {
        return """
            You are analyzing a controls publication to draft an OSCAL Catalog.

            Step 1 — Outline.

            Read the attached document and produce a JSON outline. Output ONLY valid JSON
            (no commentary, no markdown). Schema:

            {
              "title": "<extracted document title>",
              "version": "<extracted version or 'unspecified'>",
              "publisher": "<extracted org or 'unspecified'>",
              "families": [
                { "id": "<short upper-case family id, e.g. AC>",
                  "title": "<family title>",
                  "controlIds": ["ac-1", "ac-2", ...] }
              ]
            }

            Use canonical lowercase-hyphenated control IDs (ac-1, not AC-1). If the
            source document uses a different ID convention, infer an OSCAL-compatible
            mapping. If you cannot identify discrete control families, return one
            family named "all" containing every control.
            """;
    }

    public String familyPrompt(String familyId, String familyTitle, List<String> controlIds) {
        return """
            Step 2 — Generate the %s family ("%s") for the OSCAL Catalog.

            Produce ONLY a JSON object representing this family as an OSCAL group.
            Output schema (https://pages.nist.gov/OSCAL/concepts/layer/control/catalog/):

            {
              "id": "%s",
              "class": "family",
              "title": "%s",
              "controls": [ ...one OSCAL control object per controlId... ]
            }

            Control IDs to produce: %s

            For each control include: id, title, params (if any), parts (statement /
            guidance / objective / assessment), and props where the source supplies them.
            Do not invent content not present in the source document. Quote source
            statement text literally where possible.

            After producing the JSON, call the validate_oscal tool with modelType="catalog",
            format="JSON", and content set to {"catalog":{"metadata":{"title":"draft","last-modified":"2026-01-01T00:00:00Z","version":"draft","oscal-version":"1.1.2"},"groups":[<your-group>]}}.
            If validation fails, fix the errors and call validate_oscal again. After 3
            attempts, return your best effort with a "validationWarnings" array.
            """.formatted(familyId, familyTitle, familyId, familyTitle, String.join(", ", controlIds));
    }

    public String mergePrompt(String title, String version, String publisher) {
        return """
            Step 3 — Wrap all generated families into a single OSCAL Catalog.

            Output ONLY a JSON object:

            {
              "catalog": {
                "uuid": "<generate a v4 UUID>",
                "metadata": {
                  "title": "%s",
                  "last-modified": "<ISO-8601 timestamp>",
                  "version": "%s",
                  "oscal-version": "1.1.2",
                  "parties": [ { "uuid": "<v4>", "type": "organization", "name": "%s" } ]
                },
                "groups": [ <all groups, in source-document order> ]
              }
            }

            Then call validate_oscal one final time. Return the JSON when valid; if
            still invalid after 3 attempts, return with a "validationWarnings" array.
            """.formatted(title, version, publisher);
    }
}
```

- [ ] **Step 2: Snapshot test**

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogPromptBuilderTest {

    private final CatalogPromptBuilder builder = new CatalogPromptBuilder();

    @Test
    void outlinePromptRequestsCanonicalControlIds() {
        String p = builder.outlinePrompt();
        assertThat(p).contains("canonical lowercase-hyphenated");
        assertThat(p).contains("\"families\":");
    }

    @Test
    void familyPromptIncludesControlIds() {
        String p = builder.familyPrompt("ac", "Access Control", List.of("ac-1", "ac-2"));
        assertThat(p).contains("ac-1, ac-2");
        assertThat(p).contains("Access Control");
        assertThat(p).contains("validate_oscal");
    }

    @Test
    void mergePromptCarriesMetadata() {
        String p = builder.mergePrompt("My Catalog", "1.0", "Acme");
        assertThat(p).contains("My Catalog");
        assertThat(p).contains("Acme");
        assertThat(p).contains("oscal-version");
    }
}
```

- [ ] **Step 3: Run + commit**

```bash
mvn -q -Dtest=CatalogPromptBuilderTest test
# expect PASS
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/CatalogPromptBuilder.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/CatalogPromptBuilderTest.java
git commit -m "feat(ai): CatalogPromptBuilder for outline/family/merge passes"
```

---

### Task F3: `CatalogChunkingStrategy`

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/CatalogChunkingStrategy.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/CatalogChunkingStrategyTest.java`

- [ ] **Step 1: Implement**

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CatalogChunkingStrategy {

    public record Family(String id, String title, List<String> controlIds) { }

    public List<List<Family>> chunk(List<Family> families) {
        if (families.size() <= 30) {
            // Group of 6 per call
            List<List<Family>> chunks = new ArrayList<>();
            for (int i = 0; i < families.size(); i += 6) {
                chunks.add(families.subList(i, Math.min(i + 6, families.size())));
            }
            return chunks;
        }
        // 1 family per call for large publications
        List<List<Family>> chunks = new ArrayList<>();
        for (Family f : families) chunks.add(List.of(f));
        return chunks;
    }
}
```

- [ ] **Step 2: Test**

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogChunkingStrategyTest {

    private final CatalogChunkingStrategy strategy = new CatalogChunkingStrategy();

    @Test
    void smallCatalogChunksInGroupsOfSix() {
        List<CatalogChunkingStrategy.Family> fams = mk(20);
        List<List<CatalogChunkingStrategy.Family>> chunks = strategy.chunk(fams);
        // 20 / 6 = 4 chunks (6,6,6,2)
        assertThat(chunks).hasSize(4);
        assertThat(chunks.get(0)).hasSize(6);
        assertThat(chunks.get(3)).hasSize(2);
    }

    @Test
    void largeCatalogChunksOneByOne() {
        List<CatalogChunkingStrategy.Family> fams = mk(100);
        List<List<CatalogChunkingStrategy.Family>> chunks = strategy.chunk(fams);
        assertThat(chunks).hasSize(100);
        assertThat(chunks.get(0)).hasSize(1);
    }

    private List<CatalogChunkingStrategy.Family> mk(int n) {
        List<CatalogChunkingStrategy.Family> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(new CatalogChunkingStrategy.Family("f" + i, "Family " + i, List.of("f" + i + "-1")));
        }
        return out;
    }
}
```

- [ ] **Step 3: Run + commit**

```bash
mvn -q -Dtest=CatalogChunkingStrategyTest test
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/CatalogChunkingStrategy.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/CatalogChunkingStrategyTest.java
git commit -m "feat(ai): CatalogChunkingStrategy splits family list per outline size"
```

---

### Task F4: `CatalogWizard` orchestrator

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/CatalogWizard.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/CatalogWizardTest.java`

- [ ] **Step 1: Implement (relies on `AnthropicClient`, `KnowledgeLoader`, `DocumentNormalizer`, `OscalToolBox`, `AiSessionEventStream`, `CatalogPromptBuilder`, `CatalogChunkingStrategy`)**

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.service.ai.AnthropicCall;
import gov.nist.oscal.tools.api.service.ai.AnthropicClient;
import gov.nist.oscal.tools.api.service.ai.AnthropicResult;
import gov.nist.oscal.tools.api.service.ai.DocumentNormalizer;
import gov.nist.oscal.tools.api.service.ai.KnowledgeLoader;
import gov.nist.oscal.tools.api.service.ai.NormalizedDoc;
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import gov.nist.oscal.tools.api.service.ai.stream.SessionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CatalogWizard implements Wizard {

    private static final Logger log = LoggerFactory.getLogger(CatalogWizard.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int TOKEN_BUDGET_IN = 250_000;
    private static final int TOKEN_BUDGET_OUT = 50_000;

    private final AnthropicClient client;
    private final AiSessionEventStream stream;
    private final KnowledgeLoader knowledge;
    private final DocumentNormalizer normalizer;
    private final CatalogPromptBuilder prompts;
    private final CatalogChunkingStrategy chunking;

    public CatalogWizard(AnthropicClient client, AiSessionEventStream stream,
                         KnowledgeLoader knowledge, DocumentNormalizer normalizer,
                         CatalogPromptBuilder prompts, CatalogChunkingStrategy chunking) {
        this.client = client;
        this.stream = stream;
        this.knowledge = knowledge;
        this.normalizer = normalizer;
        this.prompts = prompts;
        this.chunking = chunking;
    }

    @Override public WizardKind kind() { return WizardKind.CATALOG; }

    @Override
    public WizardOutcome run(WizardContext ctx) {
        try {
            stream.publish(ctx.sessionId(), SessionEvent.progress("Normalizing source document…"));

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

            String system = knowledge.systemFor(WizardKind.CATALOG);
            int tokensIn = 0, tokensOut = 0;

            // Pass 1 — outline
            stream.publish(ctx.sessionId(), SessionEvent.progress("Analyzing structure…"));
            AnthropicCall.Builder b = AnthropicCall.builder()
                    .model(ctx.model())
                    .systemPrompt(system)
                    .userMessage(prompts.outlinePrompt() + "\n\n---\n\n" + docText)
                    .maxTokens(8000);
            if (pdfBytes != null) b.addPdf(pdfBytes);
            AnthropicResult outlineRes = client.send(ctx.apiKey(), b.build());
            tokensIn += outlineRes.tokensIn();
            tokensOut += outlineRes.tokensOut();

            JsonNode outline = MAPPER.readTree(outlineRes.text());
            String title = outline.path("title").asText("Untitled Catalog");
            String version = outline.path("version").asText("unspecified");
            String publisher = outline.path("publisher").asText("unspecified");
            List<CatalogChunkingStrategy.Family> families = new ArrayList<>();
            for (JsonNode f : outline.path("families")) {
                List<String> ids = new ArrayList<>();
                for (JsonNode cid : f.path("controlIds")) ids.add(cid.asText());
                families.add(new CatalogChunkingStrategy.Family(
                        f.path("id").asText(), f.path("title").asText(), ids));
            }
            stream.publish(ctx.sessionId(), SessionEvent.progress(
                    "Found " + families.size() + " families covering "
                    + families.stream().mapToInt(f -> f.controlIds().size()).sum() + " controls"));

            // Pass 2 — per family
            List<JsonNode> producedGroups = new ArrayList<>();
            int chunkIndex = 0;
            for (List<CatalogChunkingStrategy.Family> chunk : chunking.chunk(families)) {
                for (CatalogChunkingStrategy.Family fam : chunk) {
                    stream.publish(ctx.sessionId(), SessionEvent.progress(
                            "Drafting family " + fam.id() + " — " + fam.title()));
                    AnthropicResult famRes = client.send(ctx.apiKey(), AnthropicCall.builder()
                            .model(ctx.model())
                            .systemPrompt(system)
                            .userMessage(prompts.familyPrompt(fam.id(), fam.title(), fam.controlIds())
                                    + "\n\n---\n\n" + docText)
                            .maxTokens(8000)
                            .build());
                    tokensIn += famRes.tokensIn();
                    tokensOut += famRes.tokensOut();
                    producedGroups.add(MAPPER.readTree(famRes.text()));
                    if (tokensIn > TOKEN_BUDGET_IN || tokensOut > TOKEN_BUDGET_OUT) {
                        return WizardOutcome.failed("token_budget",
                                "Token budget exceeded after " + (chunkIndex + 1) + " families");
                    }
                }
                chunkIndex++;
            }

            // Pass 3 — merge
            stream.publish(ctx.sessionId(), SessionEvent.progress("Merging into final catalog…"));
            Map<String, Object> catalog = new HashMap<>();
            Map<String, Object> meta = new HashMap<>();
            meta.put("title", title);
            meta.put("last-modified", java.time.Instant.now().toString());
            meta.put("version", version);
            meta.put("oscal-version", "1.1.2");
            meta.put("parties", List.of(Map.of(
                    "uuid", java.util.UUID.randomUUID().toString(),
                    "type", "organization",
                    "name", publisher)));
            Map<String, Object> root = new HashMap<>();
            root.put("uuid", java.util.UUID.randomUUID().toString());
            root.put("metadata", meta);
            root.put("groups", producedGroups);
            catalog.put("catalog", root);
            String catalogJson = MAPPER.writeValueAsString(catalog);

            stream.publish(ctx.sessionId(), SessionEvent.complete(catalogJson));
            return WizardOutcome.ok(tokensIn, tokensOut);

        } catch (IllegalArgumentException e) {
            stream.publish(ctx.sessionId(), SessionEvent.error("auth_or_input", e.getMessage()));
            return WizardOutcome.failed("auth_or_input", e.getMessage());
        } catch (Exception e) {
            log.error("Catalog wizard failed", e);
            stream.publish(ctx.sessionId(), SessionEvent.error("model_error", e.getMessage()));
            return WizardOutcome.failed("model_error", e.getMessage());
        }
    }
}
```

Note: this v1 doesn't actually call `validate_oscal` from inside the wizard — the prompt instructs Claude to call it itself, but since the foundation's `AnthropicClient` doesn't yet wire tool-use end-to-end (it sends prompts and reads back text only), tool calls in v1 are best-effort via prompt instruction. A follow-up plan can add a true tool-use loop.

- [ ] **Step 2: Test**

```java
package gov.nist.oscal.tools.api.service.ai.wizard;

import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.service.ai.AnthropicCall;
import gov.nist.oscal.tools.api.service.ai.AnthropicClient;
import gov.nist.oscal.tools.api.service.ai.AnthropicResult;
import gov.nist.oscal.tools.api.service.ai.DocumentNormalizer;
import gov.nist.oscal.tools.api.service.ai.KnowledgeLoader;
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CatalogWizardTest {

    @Test
    void runProducesMergedCatalogJsonAndPublishesComplete() {
        AnthropicClient client = mock(AnthropicClient.class);
        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        KnowledgeLoader knowledge = mock(KnowledgeLoader.class);
        DocumentNormalizer normalizer = mock(DocumentNormalizer.class);
        when(knowledge.systemFor(WizardKind.CATALOG)).thenReturn("system");

        // Outline response
        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("Step 1"))))
                .thenReturn(new AnthropicResult(
                        "{\"title\":\"Test Catalog\",\"version\":\"1.0\",\"publisher\":\"Acme\"," +
                        "\"families\":[{\"id\":\"ac\",\"title\":\"Access Control\",\"controlIds\":[\"ac-1\"]}]}",
                        100, 50));
        // Family response
        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("Step 2"))))
                .thenReturn(new AnthropicResult(
                        "{\"id\":\"ac\",\"class\":\"family\",\"title\":\"Access Control\"," +
                        "\"controls\":[{\"id\":\"ac-1\",\"title\":\"Policy\"}]}",
                        100, 50));

        CatalogWizard wizard = new CatalogWizard(client, stream, knowledge, normalizer,
                new CatalogPromptBuilder(), new CatalogChunkingStrategy());

        WizardContext ctx = WizardContext.text(UUID.randomUUID(), 1L, 7L,
                "sk-ant-xxx", "claude-opus-4-7", "Source document text");

        WizardOutcome outcome = wizard.run(ctx);

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.tokensIn()).isEqualTo(200);
        assertThat(outcome.tokensOut()).isEqualTo(100);
        verify(stream, atLeastOnce()).publish(eq(ctx.sessionId()), any());
    }
}
```

- [ ] **Step 3: Run + commit**

```bash
mvn -q -Dtest=CatalogWizardTest test
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/wizard/CatalogWizard.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/wizard/CatalogWizardTest.java
git commit -m "feat(ai): CatalogWizard orchestrates outline → per-family → merge"
```

---

### Task F5: Wire CatalogWizard into the system

CatalogWizard is `@Component`, so Spring picks it up and `WizardRouter` registers it automatically (the router was constructed with `List<Wizard>` in the foundation). No additional wiring code needed — just confirm the smoke + catalog routes work.

- [ ] **Step 1: Manual confirm**

Run all backend tests:

```bash
mvn -q test
```

Expected: ≥ 2,500 tests pass, 0 failures, 22 skipped (existing gated tests). The new tests from Phase E and F bring the count up.

- [ ] **Step 2: Confirm `WizardRouter.get(CATALOG)` returns the new bean**

Add a quick assertion to `AiOrchestratorTest` or write a tiny standalone test:

```java
@Test
void wizardRouterRegistersCatalogWizard() {
    // The catalog wizard is auto-wired as a @Component; just confirm the kind→bean map has it.
    // (If the existing AiOrchestratorTest already exercises the router with mocked wizards,
    //  this is implicit. Skip if redundant.)
}
```

If the existing test covers routing semantically, skip. Otherwise add a one-line `@SpringBootTest` slice that pulls `WizardRouter` from context and asserts `router.get(CATALOG) != null`.

- [ ] **Step 3: Commit any test additions, otherwise no-op**

If you added a routing test, commit it with: `test(ai): confirm CatalogWizard registered with router`.

---

## Phase G — Frontend

### Task G1: `aiClient.startSessionWithUpload`

**Files:**
- Modify: `front-end/src/lib/ai-client.ts`
- Modify: `front-end/src/lib/ai-client.test.ts`

- [ ] **Step 1: Add the method**

In `ai-client.ts` add after `startSession`:

```typescript
async startSessionWithUpload(
  organizationId: number,
  wizardKind: WizardKind,
  file: File,
  prompt?: string,
  mode: SessionMode = 'STREAMING',
): Promise<StartSessionResponse> {
  const fd = new FormData();
  fd.append('file', file);
  const url = new URL(`${API_BASE_URL}/ai/sessions/upload`);
  url.searchParams.set('organizationId', String(organizationId));
  url.searchParams.set('wizardKind', wizardKind);
  url.searchParams.set('mode', mode);
  if (prompt) url.searchParams.set('prompt', prompt);

  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
  const headers: Record<string, string> = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  // Don't set Content-Type — browser sets multipart boundary

  const res = await fetch(url.toString(), { method: 'POST', headers, body: fd });
  if (!res.ok) throw new Error(`status ${res.status}`);
  return res.json();
},
```

- [ ] **Step 2: Add a test**

Append to `ai-client.test.ts`:

```typescript
describe('aiClient.startSessionWithUpload', () => {
  it('POSTs multipart with file + query params', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ sessionId: 'xyz' }),
    });
    const file = new File(['hello'], 'test.pdf', { type: 'application/pdf' });
    const result = await aiClient.startSessionWithUpload(1, 'CATALOG', file);
    expect(result.sessionId).toBe('xyz');
    const call = fetchMock.mock.calls[0];
    expect(call[0]).toContain('/ai/sessions/upload?');
    expect(call[0]).toContain('organizationId=1');
    expect(call[0]).toContain('wizardKind=CATALOG');
    expect(call[1]?.method).toBe('POST');
  });
});
```

- [ ] **Step 3: Run + commit**

```bash
npx vitest run src/lib/ai-client.test.ts
git add front-end/src/lib/ai-client.ts front-end/src/lib/ai-client.test.ts
git commit -m "feat(ai/fe): aiClient.startSessionWithUpload for multipart wizard input"
```

---

### Task G2: `CatalogWizardForm` component

**Files:**
- Create: `front-end/src/components/ai/CatalogWizardForm.tsx`
- Create: `front-end/src/components/ai/CatalogWizardForm.test.tsx`

- [ ] **Step 1: Implement the form**

```tsx
'use client';
import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { aiClient } from '@/lib/ai-client';
import { toast } from 'sonner';

interface Props {
  organizationId: number;
  onSessionStarted: (sessionId: string) => void;
}

type Tab = 'file' | 'paste';

export function CatalogWizardForm({ organizationId, onSessionStarted }: Props) {
  const [tab, setTab] = useState<Tab>('file');
  const [file, setFile] = useState<File | null>(null);
  const [pasted, setPasted] = useState('');
  const [running, setRunning] = useState(false);

  const canRun = (tab === 'file' && file) || (tab === 'paste' && pasted.trim().length > 0);

  const onRun = async () => {
    if (!canRun) return;
    setRunning(true);
    try {
      const res = tab === 'file' && file
        ? await aiClient.startSessionWithUpload(organizationId, 'CATALOG', file)
        : await aiClient.startSession({
            organizationId,
            wizardKind: 'CATALOG',
            mode: 'STREAMING',
            input: pasted,
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
        <CardTitle>Build Catalog from Source</CardTitle>
      </CardHeader>
      <CardContent className="space-y-6">
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
            <Label htmlFor="file-upload">Source document</Label>
            <Input
              id="file-upload"
              type="file"
              accept=".pdf,.docx,.html,.htm,.txt,.md,.odt,.rtf"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
            <p className="text-sm text-muted-foreground">
              Accepts PDF, Word, HTML, plain text, Markdown, OpenDocument, and RTF.
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            <Label htmlFor="paste-text">Paste source content</Label>
            <Textarea
              id="paste-text"
              rows={12}
              value={pasted}
              onChange={(e) => setPasted(e.target.value)}
              placeholder="Paste the controls publication text here…"
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

If `Textarea` doesn't exist at `@/components/ui/textarea` (shadcn variants vary), use a plain `<textarea>` with the project's input styling — check whether `front-end/src/components/ui/textarea.tsx` exists; if not, replace `<Textarea>` with `<textarea className="w-full rounded-md border px-3 py-2 text-sm" rows={12} ...>`.

- [ ] **Step 2: Test**

```tsx
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { CatalogWizardForm } from './CatalogWizardForm';
import { aiClient } from '@/lib/ai-client';

vi.mock('@/lib/ai-client', () => ({
  aiClient: {
    startSession: vi.fn(),
    startSessionWithUpload: vi.fn(),
  },
}));

beforeEach(() => {
  vi.clearAllMocks();
});

describe('CatalogWizardForm', () => {
  it('Run button disabled until input provided', () => {
    render(<CatalogWizardForm organizationId={1} onSessionStarted={vi.fn()} />);
    expect(screen.getByText('Run AI Wizard')).toBeDisabled();
  });

  it('paste mode: enables Run after typing and calls startSession on click', async () => {
    const onStart = vi.fn();
    (aiClient.startSession as unknown as { mockResolvedValue: (v: unknown) => void })
      .mockResolvedValue({ sessionId: 'abc' });
    render(<CatalogWizardForm organizationId={1} onSessionStarted={onStart} />);
    fireEvent.click(screen.getByText('Paste text'));
    fireEvent.change(screen.getByLabelText(/Paste source content/i), {
      target: { value: 'some controls' },
    });
    const btn = screen.getByText('Run AI Wizard');
    expect(btn).not.toBeDisabled();
    fireEvent.click(btn);
    await new Promise((r) => setTimeout(r, 0));
    expect(aiClient.startSession).toHaveBeenCalled();
    expect(onStart).toHaveBeenCalledWith('abc');
  });
});
```

- [ ] **Step 3: Run + commit**

```bash
cd front-end && npx vitest run src/components/ai/CatalogWizardForm.test.tsx
git add front-end/src/components/ai/CatalogWizardForm.tsx \
        front-end/src/components/ai/CatalogWizardForm.test.tsx
git commit -m "feat(ai/fe): CatalogWizardForm — file-upload or paste-text input"
```

---

### Task G3: `[kind]/page.tsx` branches per wizard kind

**Files:**
- Modify: `front-end/src/app/ai/wizard/[kind]/page.tsx`

- [ ] **Step 1: Add catalog branch**

Replace the current single-form layout with a per-kind switch:

```tsx
{wizardKind === 'CATALOG' && orgId && !sessionId && (
  <CatalogWizardForm
    organizationId={orgId}
    onSessionStarted={(id) => setSessionId(id)}
  />
)}
{wizardKind === 'SMOKE' && !sessionId && (
  // existing smoke input form
)}
```

Move the existing smoke input form into the SMOKE branch.

For all kinds: when `sessionId` is set, render the existing session view (unchanged). Add a special case in the session view: when `wizardKind === 'CATALOG'` and `session.isComplete && session.finalDocument`, instead of the JSON `<pre>`, stash to sessionStorage and route to the builder:

```tsx
useEffect(() => {
  if (
    wizardKind === 'CATALOG' &&
    session.isComplete &&
    session.finalDocument != null &&
    sessionId
  ) {
    sessionStorage.setItem(`aiDraft:${sessionId}`, JSON.stringify(session.finalDocument));
    router.push(`/build?section=catalogs&aiDraft=${sessionId}`);
  }
}, [wizardKind, session.isComplete, session.finalDocument, sessionId, router]);
```

Import `useEffect` from `react` and `useRouter` from `next/navigation` if not already present.

- [ ] **Step 2: Manual smoke**

`tsc --noEmit` clean, then `npx vitest run` — ensure no existing tests broke.

- [ ] **Step 3: Commit**

```bash
git add front-end/src/app/ai/wizard/[kind]/page.tsx
git commit -m "feat(ai/fe): catalog branch on wizard run page with builder hand-off"
```

---

### Task G4: `BuildPage` aiDraft hydration + `CatalogBuilderWizard` initial-JSON prop

**Files:**
- Modify: `front-end/src/app/build/page.tsx`
- Modify: `front-end/src/components/build/CatalogBuilderWizard.tsx`

- [ ] **Step 1: Add an `initialCatalog` prop to `CatalogBuilderWizard`**

Open `CatalogBuilderWizard.tsx`. It already accepts `editingCatalog` for edit mode. Add a new prop `initialCatalog?: { catalog: unknown }` that — when present and `editingCatalog` is null — pre-populates the form's local state with the AI draft. Use the same loading code path that the file-upload "Import JSON" mode uses inside the wizard (search for `JSON.parse` / `setCatalog` to find the existing import path).

- [ ] **Step 2: Hydrate from sessionStorage in `BuildPage`**

In `BuildPage`, near the top of the component:

```typescript
const [aiDraftCatalog, setAiDraftCatalog] = useState<unknown | null>(null);

useEffect(() => {
  if (typeof window === 'undefined') return;
  const params = new URLSearchParams(window.location.search);
  const aiDraft = params.get('aiDraft');
  const sec = params.get('section');
  if (aiDraft && sec === 'catalogs') {
    const raw = sessionStorage.getItem(`aiDraft:${aiDraft}`);
    if (raw) {
      try {
        setAiDraftCatalog(JSON.parse(raw));
        setSection('catalogs');
        setMode('create');
        setEditingCatalog(null);
        sessionStorage.removeItem(`aiDraft:${aiDraft}`);
      } catch {
        // ignore malformed
      }
    }
  }
}, []);
```

Pass `aiDraftCatalog` as `initialCatalog` to the existing `<CatalogBuilderWizard ... initialCatalog={aiDraftCatalog as { catalog: unknown } | undefined} />` render block. Clear `aiDraftCatalog` on `onSaveComplete`.

- [ ] **Step 3: Type-check + commit**

```bash
cd front-end && npx tsc --noEmit -p tsconfig.json 2>&1 | grep -E '(error TS|build/page|CatalogBuilderWizard)' | head
git add front-end/src/app/build/page.tsx \
        front-end/src/components/build/CatalogBuilderWizard.tsx
git commit -m "feat(ai/fe): /build hydrates AI catalog draft from sessionStorage"
```

---

### Task G5: End-to-end frontend wiring smoke

- [ ] **Step 1: Run the full frontend test suite, then load in browser**

```bash
cd front-end && npx vitest run 2>&1 | tail -5
# expect Tests passed, no regressions
```

Then in browser:
1. Configure an Anthropic API key.
2. Go to `/build`. Click the **Generate with AI** banner.
3. Pick **Build Catalog**.
4. Paste a small fake controls publication (e.g., 3-4 controls). Click Run.
5. Watch SSE events stream.
6. On complete: should auto-redirect to `/build?section=catalogs&aiDraft=…` with the draft loaded into `CatalogBuilderWizard`.
7. Save the draft.

If anything in step 6 fails, narrow it down: check sessionStorage in DevTools (`aiDraft:<sessionId>` should exist briefly), check the URL change, check `BuildPage` mounts.

- [ ] **Step 2: No commit unless something needed fixing during smoke**

---

## Phase H — Validation

### Task H1: Gated E2E integration test

**Files:**
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/integration/AiCatalogWizardIntegrationTest.java`
- Create: `back-end/src/test/resources/ai-fixtures/sample-catalog.txt` (a tiny made-up controls publication, ~50 lines, plain text)

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
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class AiCatalogWizardIntegrationTest {

    @Autowired private AiOrchestrator orchestrator;
    @Autowired private AiSessionRepository sessions;
    @MockitoBean private AiSettingsServiceFacade settings;

    @Test
    void catalogWizardProducesValidOscalFromSampleText() throws Exception {
        String key = System.getenv("ANTHROPIC_API_KEY");
        when(settings.requireApiKey(1L)).thenReturn(key);
        when(settings.getDefaultModel(1L)).thenReturn("claude-haiku-4-5-20251001");

        String sampleText = Files.readString(new ClassPathResource("ai-fixtures/sample-catalog.txt").getFile().toPath());

        UUID id = orchestrator.start(1L, 1L, WizardKind.CATALOG, AiSessionMode.STREAMING, sampleText, null, null);

        await().atMost(180, SECONDS).until(() ->
                sessions.findById(id).map(s -> s.getStatus() == AiSessionStatus.COMPLETED).orElse(false));

        AiSession s = sessions.findById(id).orElseThrow();
        assertThat(s.getTokensIn()).isPositive();
        assertThat(s.getTokensOut()).isPositive();
    }
}
```

- [ ] **Step 2: Create the fixture**

`back-end/src/test/resources/ai-fixtures/sample-catalog.txt`:

```
ACME Sample Controls Publication, version 1.0

Family AC: Access Control

AC-1 Policy and Procedures
The organization shall develop, document, and disseminate access control policy and procedures.

AC-2 Account Management
The organization shall manage system accounts including establishment, activation, modification, review, and removal.

Family AT: Awareness and Training

AT-1 Policy and Procedures
The organization shall develop and maintain a security awareness training policy.

AT-2 Literacy Training and Awareness
The organization shall provide security awareness training to all users.
```

- [ ] **Step 3: Run only when env var present**

```bash
cd back-end && ANTHROPIC_API_KEY=sk-ant-... mvn -q -Dtest=AiCatalogWizardIntegrationTest test
# Expect PASS within ~60-180 seconds (real API call)
```

Without the env var, the test is auto-skipped.

- [ ] **Step 4: Commit**

```bash
git add back-end/src/test/java/gov/nist/oscal/tools/api/integration/AiCatalogWizardIntegrationTest.java \
        back-end/src/test/resources/ai-fixtures/sample-catalog.txt
git commit -m "test(ai): gated E2E catalog wizard integration test"
```

---

## Self-Review

After all tasks complete:

```bash
cd back-end && mvn test 2>&1 | grep -E '(Tests run|BUILD)' | tail -2
cd front-end && npx vitest run 2>&1 | tail -5
```

Both must remain green. Then run the manual smoke checklist from Task G5.

If everything passes, this PR can stack on top of the foundation PR (#104) targeting `ai-foundation`, or merge into `ai-foundation` directly. Five more wizards (Component-def, Profile, SSP, POA&M, Author Assist) follow as separate plans/PRs in that order.
