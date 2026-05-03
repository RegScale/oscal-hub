# OSCAL Catalog Wizard — Design Spec

**Date:** 2026-05-03
**Author:** Travis Howerton
**Status:** Draft — pending user review
**Builds on:** `docs/superpowers/specs/2026-05-03-oscal-ai-features-design.md` and the foundation it produced.

## Problem

The foundation PR shipped the AI service layer and a smoke wizard, but produced no user-facing OSCAL output yet. Users can configure their Anthropic key and prove the pipeline works, but they cannot actually draft an OSCAL document from a natural input.

This spec covers the first concrete generator wizard: **Catalog**. A user uploads a controls publication in any common format (PDF, Word, HTML, etc.), the wizard normalizes it, Claude drafts an OSCAL Catalog with controls/parts/params/groups, and the draft loads into the existing `CatalogBuilderWizard` for human review.

## Goals

1. End-to-end wizard at `/ai/wizard/catalog`: input form → run page with streaming progress → draft loaded into the existing builder.
2. Multi-format input via Apache Tika — single normalized text path so the wizard handles whatever document the user uploads (PDF, `.docx`, `.html`, etc.).
3. Per-family generation with validate-and-correct tool loop, so output is schema-valid OSCAL JSON.
4. Targeted system prompt — load only `oscal-catalog` + `oscal-basics` skills (resolve foundation's "load all" inefficiency for this kind).
5. Hand-off to the existing `CatalogBuilderWizard` so the user keeps the form-based editor for review/edit/save.

## Non-Goals

- NIST 800-53 grounding (`fetch_catalog`-based control lookup). Defer to a v2 — v1 extracts whatever is in the source document as-is.
- OCR for scanned PDFs. Tika supports it via Tesseract but adding the OCR runtime is its own deployment story; v1 extracts text-layer-only.
- Other generator wizards (Profile, Component-def, SSP, POA&M, Author Assist). Each gets its own follow-up plan after this one lands.
- A "native PDF to Claude" fallback path (sending raw PDF bytes to Claude vision). Tika handles the common case; if scanned PDFs become a real pain point, that path can be added later — it's two extra branches in `DocumentNormalizer` and one extra `AnthropicCall` content-block kind.

## Decisions

| Decision | Choice |
|---|---|
| Input formats | Whatever Apache Tika can parse: PDF, `.docx`, `.html`, `.txt`, `.md`, `.odt`, `.rtf`, plus paste-as-text. |
| Normalization | Apache Tika `AutoDetectParser` → XHTML body → fed to Claude as a text content block. XHTML preserves heading/list structure that the prompt uses to identify families. |
| Foundation cleanup | Remove the standalone `poi-ooxml` dependency (Task A1) — Tika bundles it. Update `SourceIngestor.ingestDocx` to delegate to the new `DocumentNormalizer`. |
| Generation strategy | (a) Outline pass → list of family identifiers + section ranges. (b) Per-family pass → OSCAL JSON for that family with `validate_oscal` tool, up to 3 self-correct attempts. (c) Merge into a single catalog. (d) Final `validate_oscal`. |
| Knowledge loading | Replace `KnowledgeLoader.systemFor`'s load-all behavior with per-`WizardKind` targeting. For `CATALOG`: `oscal/skills/oscal-basics` + `oscal/skills/oscal-catalog` + `metaschema/skills/metaschema-basics`. Other kinds keep the broader behavior until their own wizard plans tighten them. |
| Streaming | Reuse foundation's SSE event types. `progress`, `tool_call`, `tool_result`, `partial_document`, `complete`, `error` events fire throughout the run. |
| Output destination | Browser receives final OSCAL Catalog JSON via SSE `complete` event → router pushes to `/build?section=catalogs&aiDraft=<sessionId>` with the JSON in `sessionStorage`; `CatalogBuilderWizard` reads and pre-populates as if loading an existing draft. |
| Per-family chunking limit | Default 6 control families per Claude call when the outline lists ≤ 30 families; switch to 1-family-per-call for larger publications. Prevents context blow-up while keeping latency reasonable. |
| Validation budget | Each per-family call may invoke `validate_oscal` up to 3 times before falling through with a partial-success marker. Final merged-catalog validation runs once; if it fails, the run completes with `partial=true` and the FE highlights remaining issues. |
| Token cost guardrail | A run that exceeds 250k input + 50k output tokens aborts with `error: token_budget`. Configurable per org in a follow-up; v1 ships fixed defaults. |

## High-Level Flow

1. User clicks the **Generate with AI** banner on `/build` (already shipped in `cddb383`) → lands on `/ai/wizard`.
2. Picks **Build Catalog** → routes to `/ai/wizard/catalog` (existing `[kind]` route).
3. New `CatalogWizardForm` component:
   - Drag-drop file uploader (any Tika-supported format) **or** paste text **or** URL.
   - Optional: catalog title override (defaults to extracted title).
   - **Run** button (disabled until input is provided).
4. FE `POST /api/ai/sessions` with `wizardKind: CATALOG`, file uploaded as multipart or URL/text in body.
5. Backend:
   - `SourceIngestor.ingest(input)` → `DocumentNormalizer.normalize` (Tika) → text+XHTML.
   - `CatalogWizard.run`:
     - System prompt = targeted `KnowledgeLoader.systemFor(CATALOG)` + catalog agent prompt.
     - Outline pass → SSE `progress` "Analyzing structure…" → emit `progress` "Found N families".
     - For each family chunk: SSE `progress` "Drafting AC family…" → tool calls (`validate_oscal`) → SSE `tool_call` / `tool_result` → emit `partial_document` patch.
     - Merge → final validate → SSE `complete` with full catalog JSON.
6. FE on `complete`: stash catalog JSON in `sessionStorage` keyed by sessionId; `router.push('/build?section=catalogs&aiDraft=<sessionId>')`.
7. `BuildPage` reads `aiDraft` query param, pulls JSON from `sessionStorage`, opens `CatalogBuilderWizard` in `create` mode pre-populated with the draft.
8. User reviews, edits, saves through the existing flow.

## Backend Design

### New / modified files

```
back-end/src/main/java/.../api/
├── service/ai/
│   ├── DocumentNormalizer.java                # NEW — Tika wrapper
│   ├── KnowledgeLoader.java                   # MODIFY — per-WizardKind targeting
│   ├── SourceIngestor.java                    # MODIFY — delegates docx/html to DocumentNormalizer
│   └── wizard/
│       ├── CatalogWizard.java                 # NEW — implements Wizard for CATALOG
│       ├── CatalogPromptBuilder.java          # NEW — composes per-pass prompts
│       └── CatalogChunkingStrategy.java       # NEW — outline → family chunks
├── controller/AiSessionController.java        # MODIFY — multipart upload support
└── model/ai/
    └── CatalogWizardInput.java                # NEW — DTO for catalog-specific inputs
```

### Dependencies

| Maven coords | Reason |
|---|---|
| `org.apache.tika:tika-core:3.2.5` | Multi-format extraction core |
| `org.apache.tika:tika-parsers-standard-package:3.2.5` | PDF, Word, Excel, HTML parsers |
| _(remove)_ `org.apache.poi:poi-ooxml` | Tika bundles it; no longer needed standalone |

### `DocumentNormalizer`

```java
@Service
public class DocumentNormalizer {
    private final AutoDetectParser parser = new AutoDetectParser();

    public NormalizedDoc normalize(byte[] bytes, String filename) throws TikaException {
        // returns { plainText, xhtml, detectedMime, originalFilename, charCount }
    }

    public NormalizedDoc normalize(String text) {
        // pass-through for paste/URL-fetched plain text
    }
}
```

XHTML output is the wizard-facing artifact; plain text is kept for fallback / smaller-context calls.

### `CatalogWizard`

Implements `Wizard.run(WizardContext)` and orchestrates the four passes (outline → per-family → merge → final-validate). Emits SSE events at each stage.

System prompt construction:

```java
String systemPrompt = knowledge.systemFor(WizardKind.CATALOG)
    + "\n\n"
    + agentPrompts.get("catalog-specialist");
```

The `agentPrompts` map is loaded from the vendored claude-plugins repo's `plugins/oscal/agents/` directory at startup.

### `KnowledgeLoader.systemFor` change

Replace the load-all path with a kind-targeted switch:

```java
public String systemFor(WizardKind kind) {
    return switch (kind) {
        case SMOKE -> SMOKE_PREFIX;
        case CATALOG -> compose(
            "plugins/oscal/skills/oscal-basics",
            "plugins/oscal/skills/oscal-catalog",
            "plugins/metaschema/skills/metaschema-basics"
        );
        case PROFILE, COMPONENT_DEF, SSP, POAM, BUILDER_ASSIST ->
            // Other wizards keep load-all until their own plans land.
            loadAll();
    };
}
```

### Multipart upload on `AiSessionController`

The current `start()` endpoint accepts JSON. Catalog wizard needs file upload. Add a parallel endpoint:

```java
@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<StartSessionResponse> startWithUpload(
    @RequestParam Long organizationId,
    @RequestParam WizardKind wizardKind,
    @RequestParam(required = false) AiSessionMode mode,
    @RequestParam(required = false) String prompt,
    @RequestPart MultipartFile file
) { ... }
```

Existing `start()` (JSON-only) stays as-is for the smoke wizard and any future text-only wizards.

## Frontend Design

### New / modified files

```
front-end/src/
├── components/ai/
│   └── CatalogWizardForm.tsx              # NEW — drag-drop + URL + paste form
├── app/ai/wizard/[kind]/
│   └── page.tsx                           # MODIFY — branch on kind, render CatalogWizardForm for catalog
├── app/build/
│   └── page.tsx                           # MODIFY — accept ?aiDraft=<sessionId>, hydrate CatalogBuilderWizard
├── components/build/
│   └── CatalogBuilderWizard.tsx           # MODIFY — accept initial JSON via prop
└── lib/
    └── ai-client.ts                       # MODIFY — add `startSessionWithUpload`
```

### `CatalogWizardForm`

- Reuses the existing `FileUploader` component but with `acceptedFormats` widened to Tika's set.
- Adds a "Paste text" textarea and a "Or fetch from URL" input as alternatives.
- "Run" button POSTs to `/api/ai/sessions/upload` (or `/api/ai/sessions` for paste/URL) and routes to the run page.

### `[kind]/page.tsx` changes

The existing run page handles `SMOKE`. For `CATALOG`, render `CatalogWizardForm` instead of the simple text input. On `complete`, do the sessionStorage stash + navigate.

### `BuildPage` AI-draft hydration

Add a `useEffect` that reads `?aiDraft=<sessionId>` from the URL, pulls the corresponding JSON from `sessionStorage`, and opens `CatalogBuilderWizard` in `create` mode pre-populated.

## Testing

- **Backend unit:** `DocumentNormalizer` (PDF + .docx + HTML fixtures), `CatalogPromptBuilder` (snapshot tests on composed prompts), `CatalogChunkingStrategy` (outline → chunks for 5, 30, 100 family cases), `CatalogWizard.run` against a stub `AnthropicClient` (asserts SSE event sequence).
- **Backend integration:** real `liboscal-java` validation in the loop; stub Anthropic returning a known catalog fragment; assert merged catalog passes schema validation.
- **Backend gated E2E:** new `AiCatalogWizardIntegrationTest` gated on `ANTHROPIC_API_KEY`. Uses a small fixture PDF (5-10 controls), runs the wizard end-to-end against real Claude, asserts schema-valid output. Skipped in CI.
- **Frontend:** `CatalogWizardForm` (file accept, URL accept, paste accept, Run button enable/disable), updated `[kind]/page.tsx` (catalog branch renders form vs smoke branch renders text input), `BuildPage` aiDraft hydration.

## Migration / Rollout

1. Land Tika dep + `DocumentNormalizer` + `SourceIngestor` refactor + `KnowledgeLoader` targeting (foundation cleanup that benefits future wizards too).
2. Land `CatalogWizard` + prompt builder + chunking strategy + multipart endpoint.
3. Land frontend wizard form + run-page branch + builder hydration.
4. Manually smoke-test against three real PDFs (NIST publication, custom corporate framework, regulatory bulletin) before the per-doc-type follow-ups start.
5. Land per-doc-type wizards in this order in subsequent PRs: **Component-definition → Profile → SSP → POA&M → Builder Author Assist**.

## Open Questions

- After v1 ships, evaluate whether per-family chunking limit needs tuning. Track in a follow-up.
- The `aiDraft` sessionStorage hand-off isn't multi-tab safe (two parallel runs collide on the same key). Acceptable for v1; revisit if real users hit it.
- Long-running runs (>2 minutes) may hit browser idle timeouts on the SSE connection. The foundation's `SseEmitter` has a 30-min server timeout; if a client disconnect orphans a run, the session row goes stale at `RUNNING` until manually cleaned. Status reaper job is a follow-up.
