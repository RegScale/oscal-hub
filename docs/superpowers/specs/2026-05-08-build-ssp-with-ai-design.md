# Build SSP with AI — Design

**Date:** 2026-05-08
**Status:** Approved (brainstorming) — pending implementation plan
**Owner:** Travis Howerton

## Goal

Add a new AI wizard at `/ai/wizard/ssp` that turns a dropped source document
(architecture write-up, system description, prior-format SSP, etc.) into a
schema-valid OSCAL System Security Plan. Mirror the existing CATALOG and
COMPONENT_DEF wizards: drop a file, watch streamed progress, land in the
existing OSCAL document editor for review, validate, and publish to the
library.

Also remove the placeholder "Build Profile" card from `/ai/wizard` — that
flow is not on this roadmap.

## Non-goals (v1)

- `by-components` mappings inside each `implemented-requirement` (which
  system component implements which part). Too sparse in source docs to
  infer reliably. User can add in the editor.
- Statement-level decomposition (`statements[]` inside each
  `implemented-requirement`).
- Parameter set-values from the resolved profile.
- Multi-document ingest (drop architecture + design + risk register at
  once). Single source doc only.

## User flow

1. User opens `/ai/wizard`. The Build Profile card is gone. The SSP card
   shows "Drop an architecture doc, system description, or existing draft
   SSP — AI extracts system characteristics and drafts an OSCAL
   system-security-plan you can review and save."
2. User clicks Start, lands on `/ai/wizard/ssp`.
3. Form prompts:
   - **Profile** — pick from library, paste a URL, or skip and let AI
     infer the control list.
   - **Source document** — file upload (PDF, Word, HTML, text, ODT, RTF,
     Markdown) or paste text.
4. User clicks **Run AI Wizard**. Streaming session begins, showing
   progress: "Reading source document…", "Extracting system
   characteristics…", "Resolving profile…" (if applicable), "Drafting
   implementation narratives (1 of N)…" through completion.
5. On complete, page redirects to
   `/build?section=ssp&aiDraft=<sessionId>`. The OSCAL document editor
   opens with the draft pre-loaded.
6. Editor shows the standard 5 steps (Metadata, Import, Body, Back-matter,
   Review & Save). On Step 3, a low-confidence review panel surfaces
   AI-confidence stats and a "Review low confidence" drawer.
7. User edits as needed, validates on Step 5, saves as draft or final, and
   optionally publishes to the library.

## Architecture

The pipeline reuses the catalog/component-def shape:

```
[ source doc ]                [ profile (optional) ]
       │                                │
       ▼                                ▼
DocumentNormalizer          ProfileResolutionService
       │                                │
       ▼                                │
  Outline pass (LLM) ◀──────── seed prompt
       │                                │
       ▼                                ▼
   controlIds (from doc) ←── overridden by ── controlIds (from profile)
       │
       ▼
SspChunkingStrategy.chunk(controlIds)
       │
       ▼
   ┌───────────────────────────┐
   │ For each chunk:           │  ← Per-chunk narrative pass (LLM)
   │   produce N implemented-  │     emits one implemented-requirement
   │   requirement entries     │     per control with ai-confidence prop
   │   with confidence prop    │
   └───────────────────────────┘
       │
       ▼
Java assemble pass (no LLM)
       │
       ▼
SessionEvent.complete(json) ──▶ /build?section=ssp&aiDraft=...
```

## Frontend changes

### `front-end/src/app/ai/wizard/page.tsx`

- Remove the `PROFILE` entry from `OPTIONS`.
- Set the `SSP` entry to `available: true`, update its description.

### `front-end/src/components/ai/SspWizardForm.tsx` (new)

Renders inside `WizardRunPage` when `wizardKind === 'SSP'`. Mirrors
`ComponentDefWizardForm` plus a profile picker.

State:
- `profileMode: 'library' | 'url' | 'skip'` (default `'library'`)
- `profileLibraryId: number | null`
- `profileUrl: string`
- `tab: 'file' | 'paste'`
- `file: File | null` / `pasted: string`
- `running: boolean`

Validation: source document is required. Profile choice is optional in
`'skip'` mode; required to be filled in `'library'` or `'url'` mode.

The component computes a final `profileHref`:
- `library` → URL of the library item's content (existing library API
  exposes a content URL we can pass through; if not, we resolve to a
  data-URL or pass the library item id and let the backend look it up.
  Decided in the implementation plan.)
- `url` → the user-supplied URL verbatim.
- `skip` → `null`.

`profileHref` is forwarded into `aiClient.startSession` /
`startSessionWithUpload`. Both helpers gain an optional `profileHref`
parameter; backend forwards it into `WizardContext`.

Library fetch uses the existing list-library-items API filtered to
profile model type (no new endpoint needed).

### `front-end/src/lib/ai-client.ts`

Add `profileHref?: string | null` to:
- `startSession(req)` request body
- `startSessionWithUpload(orgId, kind, file, profileHref?)` as a new
  multipart form param.

### `front-end/src/app/ai/wizard/[kind]/page.tsx`

Add the `SSP` mounting branch that renders `SspWizardForm`. Add a
`useEffect` that, when `wizardKind === 'SSP' && session.isComplete &&
session.finalDocument != null`, stashes the doc in
`sessionStorage` and routes to
`/build?section=ssp&aiDraft=<sessionId>`. Add `'SSP'` to the
"hide raw final-output details" guard so we don't double-render JSON
when handing off.

### `front-end/src/app/build/page.tsx`

Extend the `aiDraftHydrated` `useEffect` with an `else if (aiDraft && sec
=== 'ssp')` branch that sets a new `aiDraftSsp` state, switches to the
`'ssp'` section in `'create'` mode, and clears `editingDoc`.

Pass the new state into `OscalDocumentWizard` as `initialDocument`. Reset
it in `onSaveComplete` and on cancel, same as the catalog/component
flows.

### `front-end/src/components/build/OscalDocumentWizard.tsx`

- Add a new prop `initialDocument?: unknown` (typed as the model's
  wrapped JSON shape, e.g. `{ "system-security-plan": {...} }`).
- On a fresh (non-editing) mount when `initialDocument` is provided:
  parse via `parseLoadedDoc(modelType, JSON.stringify(initialDocument))`,
  populate state, set `step = 1`, `isDraft = true`. Reuses the existing
  `parseLoadedDoc` helper; no new parser logic.
- Existing `editingDocument` path is unchanged. If both are passed (which
  shouldn't happen), `editingDocument` wins.

Add a low-confidence review panel inside Step 3 (Body), gated to
`modelType === 'system-security-plan'`. Behavior:

- Scan `body['control-implementation']['implemented-requirements']` for
  `props[].name === 'ai-confidence'`.
- Compute counts: high / medium / low / total.
- Render only when `total > 0` (so editing a hand-built SSP doesn't show
  the panel).
- Stats line: `"X high / Y medium / Z low / N controls drafted by AI"`.
- "Review low confidence" button opens a side `Sheet` (existing
  `@/components/ui/sheet` if present, otherwise `Dialog`) listing each
  low-confidence requirement: control-id (large), description (truncated
  to 3 lines), "Find in editor" button. Clicking the button calls
  Monaco's `editor.getModel().findMatches(controlId)` and reveals the
  first match. v1: scroll-to-line works; if Monaco surface is awkward,
  fall back to copying the control-id to clipboard.

### Frontend tests

- Extend `OscalDocumentWizard.test.tsx` with:
  - `initialDocument` seeding populates metadata/import/body for SSP.
  - Low-confidence panel renders only when `ai-confidence` props exist.
  - Panel counts are correct against a fixture body.
- New `SspWizardForm.test.tsx` covering the three profile-mode states
  (library / URL / skip), required-field gating, and the resulting
  `aiClient.startSessionWithUpload` payload.

## Backend changes

### `back-end/src/main/java/.../service/ai/wizard/SspWizard.java` (new)

Spring `@Component` implementing `Wizard`. `kind() == WizardKind.SSP`.
Constructor injection: `AnthropicClient`, `AiSessionEventStream`,
`KnowledgeLoader`, `DocumentNormalizer`, `SspPromptBuilder`,
`SspChunkingStrategy`, `ProfileResolutionService`.

Token-budget caps reuse the same constants as `ComponentDefWizard`
(5,000,000 in / 200,000 out).

`run(WizardContext ctx)` implementation:

1. Stream `progress("Reading source document…")`. Same
   PDF-passthrough-vs-Tika branching as ComponentDefWizard.
2. Stream `progress("Extracting system characteristics…")`. Outline pass
   to Anthropic with `SspPromptBuilder.outlinePrompt()` + the doc text.
3. Parse outline JSON. Pull system metadata, system-characteristics,
   system-implementation, fallback `controlIds`.
4. If `ctx.profileHref()` is non-null:
   - Stream `progress("Resolving profile…")`.
   - Call `ProfileResolutionService.resolve(profileHref)`.
   - On success, replace `controlIds` with the resolved control list.
   - On failure, stream `progress("Profile resolution failed: <reason> —
     falling back to controls inferred from source document.")` and keep
     the outline-derived `controlIds`. Do not abort the run.
5. Stream `progress("Drafting implementation narratives (1 of N)…")` per
   chunk. For each chunk, call Anthropic with
   `SspPromptBuilder.controlsPrompt(systemTitle, chunk)` + the doc text.
   Parse the returned JSON array, accumulate `implemented-requirements`.
   Enforce token budget after each chunk; emit `error("token_budget",
   ...)` and abort if exceeded.
6. Stream `progress("Assembling OSCAL System Security Plan…")`. Build
   the SSP envelope deterministically in Java:
   - `metadata.title` = outline.title, `version`, `oscal-version`
     `"1.1.2"`, `last-modified` = now, `parties` from publisher.
   - `import-profile.href` = `ctx.profileHref()` or `""` (the editor
     enforces non-empty for final save; draft state is fine with `""`).
   - `system-characteristics` = mapped from outline (system-name,
     description, system-ids, sensitivity-level, system-information +
     information-types, security-impact-level (CIA from sensitivity),
     status `{state: "operational"}`, authorization-boundary).
   - `system-implementation` = `{ users: outline.users, components:
     outline.components }`.
   - `control-implementation` = `{ uuid: <v4>, description: "Control
     implementations drafted from source document.", source:
     ctx.profileHref() OR null,
     implemented-requirements: <merged> }`.
7. Stream `complete(json)`. Return `WizardOutcome.ok(tokensIn, tokensOut)`.

Exception handling matches ComponentDefWizard: `IllegalArgumentException
→ error("auth_or_input", ...)`, anything else → `error("model_error",
...)`. Logs every pass with `tokensIn / tokensOut / cumIn / cumOut /
elapsedMs`.

### `back-end/src/main/java/.../service/ai/wizard/SspPromptBuilder.java` (new)

Two prompts.

`outlinePrompt()` — instructs Claude to read any of:
- Architecture / design document (PDF, Word, HTML)
- System description / discovery document
- Existing draft SSP from another tool (Word, PDF)
- Plain narrative text

…and return a single JSON object:

```json
{
  "title": "...",
  "version": "...",
  "publisher": "...",
  "systemName": "...",
  "systemDescription": "...",
  "systemId": "<canonical or generated>",
  "sensitivityLevel": "low | moderate | high",
  "informationTypes": [{
    "uuid": "<v4>", "title": "...", "description": "...",
    "categorizations": [{ "system": "https://doi.org/10.6028/NIST.SP.800-60v2r1",
      "information-type-ids": ["..."] }]
  }],
  "components": [{
    "uuid": "<v4>", "type": "software | service | ...",
    "title": "...", "description": "...",
    "status": { "state": "operational" }
  }],
  "users": [{
    "uuid": "<v4>", "title": "...", "role-ids": ["..."]
  }],
  "authorizationBoundary": "<paragraph describing boundary>",
  "controlIds": ["ac-1", "ac-2", "..."]
}
```

The CRITICAL header matches existing wizard prompts (single raw JSON,
first char `{`, last `}`, no fences).

`controlsPrompt(systemTitle, controlIds)` — instructs Claude to produce
a JSON array of `implemented-requirement` objects, one per requested
control, **always emitting an entry even if the source doc is silent**:

```json
[{
  "uuid": "<v4>",
  "control-id": "ac-1",
  "description": "<narrative grounded in source>",
  "props": [{
    "name": "ai-confidence",
    "ns": "https://oscal-hub.io/ns",
    "value": "high | medium | low"
  }]
}]
```

Confidence rubric in the prompt:
- `high` — source doc directly addresses this control with specifics.
- `medium` — source doc addresses the topic generally but doesn't fully
  describe implementation.
- `low` — source doc has no direct evidence; description is a
  TBD-style placeholder.

Prompt explicitly forbids inventing content and tells Claude to write
*"Source document does not address this control. To be completed."* for
`low`-confidence entries.

### `back-end/src/main/java/.../service/ai/wizard/SspChunkingStrategy.java` (new)

Same shape as `ComponentDefChunkingStrategy`. Default chunk size 25
control IDs. `chunk(List<String> controlIds) -> List<List<String>>`.

### `back-end/src/main/java/.../service/ai/WizardContext.java`

Add a `profileHref` field (nullable). Existing wizards ignore it. Update
the builder.

### `back-end/src/main/java/.../service/ai/AiOrchestrator.java`

Plumb `profileHref` from `StartSessionRequest` and the upload endpoint
into `WizardContext.builder().profileHref(...)`.

### `back-end/src/main/java/.../model/ai/StartSessionRequest.java`

Add nullable `profileHref` field. Bean validation: optional.

### `back-end/src/main/java/.../controller/AiSessionController.java`

Both `start` and `startWithUpload`: forward the new param/body field.
`startWithUpload` adds a `@RequestParam(required = false) String
profileHref`.

### `back-end/src/main/java/.../service/ai/KnowledgeLoader.java`

Extend `systemFor` with an explicit `SSP` branch that loads:
- `plugins/oscal/skills/oscal-basics`
- `plugins/oscal/skills/oscal-ssp`
- `plugins/metaschema/skills/metaschema-basics`

Falls back to empty if the submodule isn't initialized — same behavior
as today for CATALOG/COMPONENT_DEF.

### Backend tests

- `SspPromptBuilderTest` — snapshot the two prompts so changes are
  visible in code review.
- `SspChunkingStrategyTest` — boundaries: 0, 1, 25, 26, 100 control IDs.
- `SspWizardTest` — integration test with a stub `AnthropicClient` that
  returns canned outline + chunk responses. Asserts:
  - profile-resolution path overrides `controlIds`.
  - skip path uses outline `controlIds`.
  - profile-resolution failure falls back to outline `controlIds` and
    streams the warning event.
  - assembled JSON is schema-valid via the existing OSCAL validation
    harness.
  - confidence props survive into the assembled doc.
  - token-budget abort emits the right error event.

## Data flow examples

### Happy path with profile

1. User picks FedRAMP Moderate (profile in their org library) and
   uploads a 50-page architecture PDF.
2. Outline pass returns `systemName: "Acme Trust Center"`, sensitivity
   `moderate`, 8 components, 5 information types, 12 users, plus 200
   inferred control IDs.
3. Profile resolution returns 325 control IDs (FedRAMP Moderate).
   Override applied; outline's 200 are discarded.
4. 13 chunk passes (325 / 25). Each emits ~25 implemented-requirement
   entries with confidence scores. Aggregate: 195 high, 80 medium, 50
   low.
5. Java assemble produces the SSP envelope.
6. User lands in editor; low-confidence panel shows
   `"195 high / 80 medium / 50 low / 325 controls drafted by AI"` and
   the "Review low confidence" button.

### Skip-profile path

1. User uploads a draft SSP from a prior tool, picks "Skip".
2. Outline pass returns system metadata + 287 control IDs the doc
   actually mentions.
3. No profile resolution.
4. 12 chunk passes. Confidence scores trend higher (source actually
   addresses controls).
5. Editor opens with `import-profile.href` empty. Step 5 final-save
   validation will require user to fill it before final save (draft save
   is allowed).

## Risks and mitigations

| Risk | Mitigation |
|------|------------|
| Source doc too sparse, most narratives `low` | Panel surfaces it; user knows where to dig. |
| Profile resolution slow on FedRAMP-class profiles | Stream "Resolving profile…" event; existing service caches resolved profiles. |
| Token cost unbounded for huge baselines | Existing 5M in / 200K out cap aborts pathological runs. 325 controls × 25/chunk × ~3K tokens output ≈ 40K out — well under cap. |
| Outline returns malformed JSON | Existing `extractJson` carve-out handles fences and prose preludes; same as ComponentDef. |
| Library item content URL not directly fetchable from backend | Backend resolves library item id → content URL during `start` request; or frontend resolves to a content URL before the call. Decision punted to implementation plan. |

## Out-of-scope items tracked for follow-up

- v1.5: per-component `by-components` mapping inside
  `implemented-requirement`.
- v1.5: parameter set-values pulled from resolved profile.
- v1.5: statement-level decomposition.
- v1.5: multi-document ingest.
- v2: AI-suggested responsible-roles per control based on system roles.
