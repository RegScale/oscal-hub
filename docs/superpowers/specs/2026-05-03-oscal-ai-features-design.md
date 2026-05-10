# OSCAL Hub AI Features — Design Spec

**Date:** 2026-05-03
**Author:** Travis Howerton
**Status:** Draft — pending user review

## Problem

OSCAL authoring is labor-intensive. A System Security Plan can take weeks to draft; a custom controls catalog requires careful translation from a source publication; component-definitions require mapping product features to NIST controls statement-by-statement. OSCAL Hub today provides a form-based builder that ensures schema correctness but does nothing to reduce the underlying authoring effort.

We want to add AI assistance that drafts OSCAL documents from natural inputs (PDFs, URLs, product docs, pen test reports) and supports authors mid-edit, while reusing the OSCAL/Metaschema knowledge surface published by the `metaschema-framework/claude-plugins` project.

## Goals

1. A guided AI Wizard inside OSCAL Hub that turns natural inputs into draft OSCAL documents — initial scope covers Catalog, Profile, Component-definition, SSP, and POA&M.
2. An in-builder author-assist sidebar that helps users rephrase, expand, fill, and explain while editing any document type.
3. A shared AI service layer used by all wizards and the author assist, so prompts, tools, telemetry, and key handling live in one place.
4. Direct reuse of the `metaschema-framework/claude-plugins` repo's content (knowledge skills + agent prompts), so OSCAL Hub's web AI stays aligned with the Claude Code plugin ecosystem.
5. Bring-your-own-key (BYOK) — each org configures its own Anthropic API key; OSCAL Hub never bills for tokens.

## Non-Goals

- Embedding the plugin's MCP servers as runtime subprocesses. We use the same operations in-process via `liboscal-java`.
- Provenance metadata on AI-generated fields. AI output is treated as a normal draft once it lands in the builder.
- Quotas, per-org cost dashboards, or central billing. BYOK pushes those concerns to Anthropic and the org's own billing.
- New OSCAL document types. Scope is limited to types the existing builder already supports.

## Decisions

| Decision | Choice |
|---|---|
| Surface | Web app, server-side Claude API calls. |
| Feature set | Wizards for Catalog, Profile, Component-def, SSP, POA&M, plus cross-cutting in-builder author assist — all on a shared service layer. |
| Plugin reuse | Vendor `metaschema-framework/claude-plugins` as a git submodule. Use its **knowledge skills** as system-prompt fragments and its **agent prompts** as per-wizard system prompts. Do not run its MCP servers. |
| Tools available to Claude | In-process Spring beans wrapping `liboscal-java`: `validate_oscal`, `convert_format`, `resolve_profile`, `lookup_control`, `fetch_catalog`, `read_current_document_section`. |
| Interaction model | Streaming progress (SSE) by default; opt-in "Thorough" mode adds checkpoints where AI can ask the user clarifying questions mid-run. |
| Output destination | Generated draft loads directly into the existing builder editor for the corresponding doc type. No provenance metadata. |
| API key handling | BYOK per organization. Keys encrypted at rest. AI features are **hidden** in the UI when no key is configured for the org. |
| Source inputs accepted | PDF (Claude native PDF documents), URL (server-side fetch + clean), `.docx` (Apache POI text extraction), plain text / paste, existing OSCAL JSON / XML / YAML. |

## High-Level Flow

### Wizard path (Catalog from PDF, illustrative)

1. Org admin enters Anthropic API key in **Org Admin → AI Settings**. Key is encrypted via the existing KMS-backed envelope. AI Wizard becomes visible to org members.
2. User opens **AI Wizard** → picks **Build Catalog** → uploads a controls publication PDF (or pastes a URL) → optionally toggles **Thorough mode**.
3. FE `POST /api/ai/sessions` with wizard kind, inputs, mode. Backend creates an `AiSession` row, opens an SSE channel, returns `sessionId`.
4. `CatalogWizard` runs:
   - Build system prompt = `KnowledgeLoader.systemFor("catalog")` + the `oscal` plugin's catalog agent prompt + style instructions.
   - Claude call 1: extract outline (control IDs, families, sections) from the PDF.
   - SSE event `outline_complete` → UI shows "Found 312 controls in 18 families."
   - For each family chunk: Claude generates that family's OSCAL JSON with `validate_oscal` available as a tool. Claude self-checks; on schema errors it auto-fixes (max 3 attempts per chunk). SSE events `family_progress` stream live.
   - In Thorough mode, before chunked generation Claude may emit `awaiting_input` events to ask clarifying questions; the UI prompts the user; answers feed back into the prompt.
   - Merge family outputs → final catalog → final `validate_oscal` pass.
5. SSE event `complete` carries the final OSCAL JSON. FE opens the existing Catalog editor pre-populated with the draft. User reviews, edits, saves through normal builder flow.
6. `AiSession` marked complete; token usage recorded for the org's own telemetry (BYOK billing is between the org and Anthropic).

### Wizard inputs by document type

| Wizard | Source inputs | OSCAL inputs | Output |
|---|---|---|---|
| Catalog | PDF / URL / `.docx` / paste | — | Catalog JSON |
| Profile | (optional) tailoring description | catalog ref + baseline name | Profile JSON |
| Component-definition | product PDFs / URLs / `.docx` | target catalog ref | Component-definition JSON |
| SSP | system description PDF / paste | profile ref + selected components | SSP JSON (per-control narratives) |
| POA&M | findings PDF / scanner CSV | SSP / system ref | POA&M JSON |
| In-builder Author Assist | inline selection / chat message | current document context | inline edit suggestion / chat reply |

### Builder Author Assist path

1. User editing any doc type clicks the **AI Assist** drawer in the builder.
2. Same `AiOrchestrator` is invoked with `wizardKind = builder_assist`. Tools include `read_current_document_section` so Claude can ground replies in the document under edit.
3. User can request: rewrite this part, expand this remark, suggest props, explain this control, free-form chat.
4. Streamed reply appears in the drawer; user clicks **Apply** to splice the suggestion into the form, or copies text manually.

### Org without an API key

- AI Settings shows the empty state with a one-time setup CTA.
- No `/api/ai/*` endpoints respond with content for that org's users (returns `404` with `feature_disabled` reason).
- The AI Wizard nav item and the in-builder AI Assist drawer button are hidden by feature-detection on session bootstrap.

## Backend Design

### New module: `back-end/.../api/ai/`

```
ai/
├── controller/
│   ├── AiSessionController.java        # POST /api/ai/sessions, GET /api/ai/sessions/{id}/stream (SSE)
│   ├── AiSettingsController.java       # GET/PUT /api/ai/settings (org admin only)
│   └── AiAssistController.java         # POST /api/ai/assist (builder sidecar)
├── service/
│   ├── AiOrchestrator.java             # entry point; loads org key; opens session; dispatches to wizard
│   ├── WizardRouter.java
│   ├── wizard/
│   │   ├── CatalogWizard.java
│   │   ├── ProfileWizard.java
│   │   ├── ComponentDefWizard.java
│   │   ├── SspWizard.java
│   │   ├── PoamWizard.java
│   │   └── BuilderAssistService.java
│   ├── AnthropicClient.java            # wraps Anthropic Java SDK
│   ├── KnowledgeLoader.java            # reads markdown skills/prompts from claude-plugins submodule
│   ├── SourceIngestor.java             # PDF / URL / docx / paste / OSCAL normalization
│   └── tools/
│       ├── OscalToolBox.java           # registers tool definitions with Claude
│       ├── ValidateOscalTool.java
│       ├── ConvertFormatTool.java
│       ├── ResolveProfileTool.java
│       ├── LookupControlTool.java
│       ├── FetchCatalogTool.java
│       └── ReadCurrentDocSectionTool.java
├── model/
│   ├── AiSession.java                  # @Entity
│   ├── OrgAiSettings.java              # @Entity
│   ├── WizardKind.java                 # enum
│   └── dto/...                         # request/response/event DTOs
└── stream/
    └── AiSessionEventStream.java       # SSE emitter pool keyed by sessionId
```

### New entities

**`OrgAiSettings`** (one row per organization)

| Field | Type | Notes |
|---|---|---|
| `id` | Long | PK |
| `organization_id` | Long | FK → organizations, unique |
| `anthropic_key_encrypted` | bytea | KMS-envelope-encrypted |
| `anthropic_key_fingerprint` | String | last-4 + sha256 prefix for UI display |
| `default_model` | String | e.g., `claude-opus-4-7`; UI-selectable from a curated list |
| `enabled` | boolean | default `true` once a key is set |
| `created_at` / `updated_at` | timestamp | |

**`AiSession`**

| Field | Type | Notes |
|---|---|---|
| `id` | UUID | PK |
| `organization_id` / `user_id` | Long | FKs |
| `wizard_kind` | enum | catalog, profile, component_def, ssp, poam, builder_assist |
| `mode` | enum | streaming, thorough |
| `model` | String | resolved at run time |
| `input_summary` | jsonb | source filenames, sizes, refs (NOT the raw content) |
| `status` | enum | running, awaiting_input, completed, cancelled, failed |
| `tokens_in` / `tokens_out` | int | |
| `error_code` / `error_message` | String nullable | |
| `started_at` / `ended_at` | timestamp | |

Indexes: `(organization_id, started_at DESC)`, `(user_id, started_at DESC)`.

### Anthropic key encryption

Reuse the existing OSCAL Hub KMS-envelope pattern (the same one used for storing other org-level secrets). Key plaintext is never returned to the FE; the settings UI shows the fingerprint and a "Replace key" action.

### Knowledge submodule

```
back-end/src/main/resources/claude-plugins/   # git submodule pinned to a known commit
├── plugins/oscal/skills/...                  # markdown knowledge files
├── plugins/oscal/agents/...                  # markdown agent prompts
├── plugins/metaschema/skills/...
└── plugins/metaschema/agents/...
```

`KnowledgeLoader` reads relevant skill files at startup, caches them in memory, and exposes `systemFor(WizardKind)` returning a composed system-prompt string. A scheduled GitHub Actions job opens a PR to bump the submodule monthly so upstream improvements flow in via review.

### Tools (registered with Claude via Anthropic tool-use)

| Tool | Implementation |
|---|---|
| `validate_oscal(content, format)` | `liboscal-java` schema + constraint validation. Returns errors with line/column. |
| `convert_format(content, from, to)` | XML ↔ JSON ↔ YAML via `OscalBindingContext`. |
| `resolve_profile(profile_json, catalog_refs)` | `ProfileResolver` with `DynamicContext`. |
| `lookup_control(control_id, catalog_ref)` | Fetches a control's definition (statement, parts, params) for grounded reasoning. |
| `fetch_catalog(ref)` | Loads a referenced catalog (e.g., NIST 800-53r5) from the org's library or a known-public bundle shipped with the app. |
| `read_current_document_section(path)` | Author-assist only. Reads from the document the user is editing in the builder. |

Tools are plain Spring beans behind a `Tool` interface so they're trivially unit-testable. Each tool returns structured JSON that Claude can consume, plus a tool-result block streamed to the FE so the UI can show "Validating against OSCAL Catalog schema…" progress lines.

### Streaming protocol (SSE)

`GET /api/ai/sessions/{id}/stream` emits typed events:

| Event | Payload |
|---|---|
| `session_started` | `{ sessionId, wizardKind, model }` |
| `progress` | `{ stage, message, percent? }` |
| `tool_call` | `{ tool, args_summary }` |
| `tool_result` | `{ tool, ok, summary }` |
| `awaiting_input` | `{ question, options? }` (Thorough mode) |
| `chunk` | `{ text }` (assistant text deltas, for Author Assist) |
| `partial_document` | `{ jsonPatch }` (incremental builder doc updates) |
| `complete` | `{ document, validationSummary }` |
| `error` | `{ code, message, retriable }` |

User answers in Thorough mode are submitted via `POST /api/ai/sessions/{id}/answer` and unblock the run.

### Error handling

| Failure | Behavior |
|---|---|
| No API key configured | Endpoints return `404 feature_disabled`; UI hides AI surfaces. |
| Invalid / revoked key | `error` event with `auth_failed`; UI deep-links to AI Settings. |
| Anthropic rate limit | `error` event with `rate_limited`; UI suggests retry; session marked failed (no auto-retry to avoid runaway cost on the org's account). |
| Tool-loop validation errors after 3 self-correct attempts | Run completes with a `partial` flag; UI loads draft and highlights remaining issues. |
| Source ingestion failure (PDF parse, oversize, URL fetch) | Fail before any Claude call; no tokens spent. |
| Stream interruption / client disconnect | Session continues server-side until finishable; client can reconnect via `GET /api/ai/sessions/{id}/stream` (resumes from last cursor) or call `DELETE` to cancel. |
| User cancel | Session marked `cancelled`; partial output discarded unless user explicitly opts to keep it. |

### Security

- All AI endpoints require JWT auth with org scope (existing `JwtAuthenticationFilter`).
- API keys never leave the backend; UI shows fingerprint only.
- Org admin role required for `AiSettingsController`.
- Audit log entries: `ai_session.created`, `ai_session.completed`, `ai_session.failed`, `ai_settings.key_set`, `ai_settings.key_rotated`, `ai_settings.disabled`.
- Rate limit: existing per-user rate limiter applied to `/api/ai/*` (lower threshold than non-AI endpoints).
- Source documents are streamed to Anthropic over TLS using the org's key. OSCAL Hub does not retain source PDFs/docx beyond the lifetime of the request unless the user explicitly attaches them to a saved document.

## Frontend Design

### New routes / surfaces

```
front-end/src/app/
├── ai/
│   ├── wizard/
│   │   ├── page.tsx              # wizard kind selection (Catalog / Profile / ...)
│   │   └── [kind]/page.tsx       # per-kind input form + run + progress UI
│   └── sessions/[id]/page.tsx    # observe an in-flight or recent session
└── org-admin/ai-settings/
    └── page.tsx                  # API key set/rotate, model selection, enable toggle
```

### New components

```
front-end/src/components/ai/
├── WizardKindPicker.tsx
├── inputs/
│   ├── PdfUploadField.tsx
│   ├── UrlField.tsx
│   ├── CatalogRefPicker.tsx
│   └── ComponentRefPicker.tsx
├── progress/
│   ├── SseSessionView.tsx        # SSE consumer; renders progress lines + tool calls
│   ├── AwaitingInputPrompt.tsx   # Thorough-mode question UI
│   └── PartialDocPreview.tsx
├── AssistDrawer.tsx              # in-builder sidecar
└── AiFeatureGate.tsx             # hides AI UI when org has no key
```

### Builder integration

- Wizard `complete` event → FE navigates to the appropriate builder route (e.g., `/build/catalog/new`) with the draft document hydrated into local state via the existing builder's "load from JSON" path.
- `AssistDrawer` mounts inside `front-end/src/components/build/oscal/` editors and reads/writes via the editors' existing form context.
- `AiFeatureGate` wraps every AI entry point and reads `useAiFeatureEnabled()`, which calls `GET /api/ai/settings/status` once on session bootstrap.

### Tests

- Unit: each input component, `SseSessionView` event handling, `AssistDrawer` apply/discard, `AiFeatureGate` show/hide.
- Integration: wizard happy-path with a mocked SSE stream → asserts builder hydrates correctly.

## Testing Strategy

- **Unit (backend):** every wizard's prompt construction (snapshot tests against the composed system prompts), `OscalToolBox` tools (assert each delegates correctly to `liboscal-java`), `SourceIngestor` per format (sample fixtures), `KnowledgeLoader` submodule load.
- **Integration (backend):** stub `AnthropicClient` with replayable fixtures of recorded sessions; wire real `liboscal-java`. Assert each wizard produces schema-valid OSCAL on a fixed sample input.
- **Frontend:** component tests for wizard inputs, SSE consumer, builder hydration, feature gate.
- **E2E (Playwright):** one happy-path per wizard against recorded Anthropic fixtures; one failure path (no API key configured → wizard surfaces hidden); one Thorough-mode path with a clarifying question.
- **Manual smoke:** real Anthropic call (developer key) against a known PDF for Catalog wizard; visually confirm builder loads the draft and the document validates.

## Migration / Rollout

1. Land backend skeleton (entities, controller scaffolding, encryption wiring, submodule add) behind a `feature.ai.enabled` global flag, default off in prod.
2. Land `OscalToolBox` with full unit tests against `liboscal-java`.
3. Land `KnowledgeLoader` and snapshot tests of composed system prompts.
4. Implement wizards in this order: **Catalog → Component-def → Profile → SSP → POA&M**, each end-to-end (backend + FE + tests) before the next.
5. Implement Builder Author Assist after Catalog and Component-def are stable.
6. Beta test with one internal org (BYOK with a developer Anthropic key) before flipping the global flag.
7. Production enablement: flip global flag; orgs opt in by setting their key.

## Open Questions

- Catalog cache: which baseline catalogs do we pre-bundle for `fetch_catalog` (NIST 800-53r5, 800-171r3, FedRAMP baselines, ISO 27001 mappings)? Initial answer: ship NIST 800-53r5 + the three FedRAMP baselines; everything else loads from the org's existing library.
- Output guardrails for SSP narratives (style/tone enforcement) — keep generic in v1, expose org-level style prompt overrides in a follow-up.
- Whether to surface AI-suggested control mappings as inline annotations inside Author Assist, vs. only on explicit user request — defer to user testing during the Component-def wizard build.
