# AI-Powered Custom Validation Rule Generator — Design

**Date**: 2026-05-04
**Status**: Approved (brainstorm), pending implementation plan
**Author**: Travis Howerton (with Claude)

## Problem

OSCAL Hub already supports user-authored custom validation rules
(`custom_validation_rules` table, full CRUD at `/rules/custom`), but creating one
today is a manual, expert-only task: the user must know JSONPath, XPath, or
similar, and must hand-author the rule expression and metadata. Worse, the
rules currently sit as metadata only — they're stored and listed but **not
applied during document validation** because `ValidationService` only runs the
native NIST `liboscal-java` constraints. So the feature offers neither a usable
authoring experience nor real enforcement.

We want any user to be able to describe a rule in plain English, have AI
generate the formal rule, prove it works against synthetic test cases, and have
it actually enforced when OSCAL documents are validated going forward.

## Goals

1. Natural-language → enforceable rule, end to end, in a single wizard.
2. Resilient generation: clarification turns when the description is ambiguous,
   automatic revision when the generated rule misfires, transparent surfacing
   when the AI can't reach a working rule.
3. Real enforcement: AI-generated rules are applied during normal document
   validation, with violations indistinguishable from native NIST violations
   except for a `[custom: <rule-id>]` tag.
4. OSCAL-native semantics: rules express what Metaschema already models
   (cardinality, cross-field, allowed-values, id-reference, etc.), not a
   weaker subset.

## Non-goals (v1)

- Batch generation ("generate the FedRAMP Moderate ruleset").
- Sharing rules between users or promoting private → org-wide.
- Saved drafts / resume mid-flow.
- AI-assisted editing of an existing saved rule (workaround: delete and
  regenerate).
- Persisted test fixtures stored with the rule.
- Curated browseable rule library.

## Approach

### High-level

The AI generates **Metaschema external constraint XML** (Metapath), which is
loaded into `OscalBindingContext` so violations come back through the same
`ValidationError` channel as built-in NIST rules. This avoids building a
parallel evaluator: `liboscal-java` already includes the Metapath engine, and
Metaschema constraints natively cover the breadth of rule types in our existing
schema (cardinality, cross-field, allowed-values, id-reference, etc.).

Considered and rejected:

- **JSONPath canonical** — simpler engine, Claude is more reliable at JSONPath,
  but expressivity is too limited (cross-field assertions and conditional
  cardinality become awkward or impossible). We'd outgrow it.
- **Hybrid evaluator** (Metapath + JSONPath + regex) — maximum flexibility but
  three engines, three error formats, three sets of failure modes. Resilience
  suffers because there are more places things can go wrong.

### Components

**Frontend (Next.js)**

- New page `/rules/custom/ai-generate`. Linked from a prominent
  "Generate with AI" button on `/rules/custom`.
- Single page, three zones:
  - Left: chat history (clarification turns).
  - Right top: generated rule preview (name, description, severity, Metapath
    in a code view).
  - Right bottom: synthetic test matrix (pass/fail per case).
- Save button is gated on every test row being green.
- Stack: shadcn/ui + Radix + Tailwind v4 (matches the rest of the app);
  React Query for API calls; Sonner for toasts.

**Backend (Spring Boot)**

- New controller `AiRuleGenController` exposing session-scoped endpoints:
  - `POST /api/rules/ai-generate/session` — start a session for a chosen
    OSCAL model type; returns session id.
  - `POST /api/rules/ai-generate/session/{id}/turn` — send a user turn
    (description or clarification answer); returns the AI's next move
    (clarifying question OR generated rule + test matrix).
  - `POST /api/rules/ai-generate/session/{id}/edit` — user manually edited
    the Metapath; re-runs the test matrix only.
  - `POST /api/rules/ai-generate/session/{id}/save` — persist as a
    `CustomValidationRule`.
- New service `MetapathConstraintService`:
  - Parses a constraint XML string into an `IConstraintSet`.
  - Caches parsed constraint sets per (user, model type), invalidated on
    rule create/update/delete/toggle.
  - `evaluateFragment(fragment, constraint)` — runs a single constraint
    against a single OSCAL fragment, returning violations from that
    constraint only.
  - `loadEnabledConstraints(modelType, userId)` — reads enabled custom
    rules for the doc's model type and the validating user, returns
    constraint sets ready to register.
- `ValidationService` modification: before deserializing the user's doc,
  call `MetapathConstraintService.loadEnabledConstraints(...)` and register
  the returned constraint sets with `OscalBindingContext`. Existing
  deserializer-driven constraint validation now also applies custom rules.
- Reuses existing `AnthropicClient` and `OrgAiSettings`.

### Wizard flow

1. **Pick model type** — dropdown (catalog / profile / ssp / ap / ar / poam /
   component-definition). Set up front because it scopes the system prompt;
   inferring from NL alone is too ambiguous to be reliable.
2. **Describe rule** — large textarea with starter examples ("All controls
   in an SSP must have an implementation status", "Profile imports must
   reference an existing catalog UUID").
3. **Clarification turns (0..N)** — Claude either asks a clarifying question
   or moves on to generation. Chat-style assistant + user bubbles.
4. **Generate + test** — Claude produces a structured envelope: rule
   metadata (name, description, severity), Metaschema constraint XML, and
   4–6 synthetic test cases (mix of should-pass and should-fail OSCAL
   fragments). Backend evaluates each case against the constraint and
   renders a green/red matrix.
5. **Auto-iterate (silent up to 3 tries)** — if any row's actual ≠
   expected, the failures are fed back to Claude with a request to revise.
   User only sees iterations beyond 3.
6. **User review** — accept and save, edit description and re-generate,
   hand-tweak the Metapath (which re-runs the matrix automatically — we
   never save an unverified rule), or abandon.
7. **Save** — persists to `custom_validation_rules` with `rule_type =
   'metapath'`, the constraint XML in `rule_expression`, and the original
   NL prompt + Claude model name in the new metadata columns.

### AI prompting

Single Claude conversation per session. Anthropic tool-use enforces
structured output — each turn the model must produce exactly one tool call:

- `ask_clarifying_question(question)` — needs more info before it can
  generate.
- `generate_rule(name, description, severity, metaschemaConstraintXml,
  testCases[])` — has enough info; here's the full proposal.
- `revise_rule(...)` — same shape as `generate_rule`, used when iterating
  off a dirty matrix.

System prompt content (prompt-cached per OSCAL model type to amortize
token cost across all sessions):

- Compact primer on Metaschema constraints + Metapath syntax with curated
  examples (`<expect>`, `<allowed-values>`, `<has-cardinality>`,
  `<index-has-key>`, `<matches>`).
- A condensed schema summary for the chosen OSCAL model — key entities,
  paths, cardinalities. Pre-built once per model from the OSCAL JSON
  schemas and shipped as a static asset (full schema is too big).
- Output contract describing each tool call.
- Anti-patterns: "don't generate Metapath that requires features not in
  liboscal-java 6.0.0", "test fragments must be minimal valid OSCAL stubs,
  not full documents".

Default model: `claude-opus-4-7` (matches existing org AI default).
Fallback model configurable via existing `OrgAiSettings` if cost matters.

### Synthetic test bench

Claude generates 4–6 test cases per `generate_rule` / `revise_rule`:

- 2–3 should-pass fragments — minimal valid OSCAL stubs where the rule is
  satisfied.
- 2–3 should-fail fragments — minimal stubs where the rule is violated,
  varying *how* it's violated where possible (missing field, wrong value,
  wrong cardinality).
- Format: JSON. Claude is instructed to keep fragments minimal — only the
  scaffolding required to be a parseable doc of the chosen model.

`MetapathConstraintService.evaluateFragment(fragment, constraint)` returns
the violations from this constraint only. A fragment **passes** if no
violations, **fails** if any.

UI: matrix table with `# / Description / Expected / Actual / Status`
columns. Save button disabled until all rows green.

### Resilience

| Failure                                      | Response                                                                                            | Budget       |
|----------------------------------------------|-----------------------------------------------------------------------------------------------------|--------------|
| Anthropic API transient error                | exponential backoff retry                                                                           | 2 retries    |
| Claude returns malformed tool-call args      | re-ask with the validation error fed back                                                           | 2 retries    |
| Generated Metapath fails to parse            | feed parser error to Claude, ask for fix                                                            | 3 retries    |
| Test matrix has any red row                  | feed failing rows + actual-vs-expected to Claude, ask for revision                                  | 3 iterations |
| All retries exhausted                        | surface to user: last attempt + plain-English explanation + "clarify" / "edit manually" / "abandon" | —            |
| No org Anthropic key configured              | wizard entry shows "Configure AI in org settings →" with deep-link                                  | —            |
| User edits Metapath manually                 | matrix re-runs; save stays disabled until clean                                                     | —            |
| User abandons mid-flow                       | session is in-memory only; no draft persistence in v1                                               | —            |

Cross-cutting: every Claude call is logged (request, response, latency)
keyed by session id for postmortem debugging. Tokens used per session
are summed and shown in the wizard footer ("This generation used ~12k
tokens") since billing is on the org's API key.

### Data model

Single migration `V1.<next>__ai_rule_metadata.sql`. Look at the highest
existing migration version under `back-end/src/main/resources/db/migration/`
when implementing and increment from there.

```sql
ALTER TABLE custom_validation_rules
    ADD COLUMN IF NOT EXISTS ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS generation_prompt TEXT,
    ADD COLUMN IF NOT EXISTS generation_model VARCHAR(64);
```

- `ai_generated` — flags rules created via the wizard. Subtle badge in the
  rule list.
- `generation_prompt` — the user's original natural-language description.
  Traceability + future "regenerate from this prompt".
- `generation_model` — Claude model that produced it (e.g.,
  `claude-opus-4-7`). Future model upgrades won't silently change behavior
  of existing rules.

Mirror these on the JPA `CustomValidationRule` entity. For the
`NOT NULL DEFAULT FALSE` column, add
`@org.hibernate.annotations.ColumnDefault("false")` so Hibernate's schema
validator sees a match (per the project's Flyway-as-authority policy).

The existing `rule_type` column is a `VARCHAR(50)` and accepts `metapath`
without schema change. Existing rule types remain valid for manual entry.

`rule_expression` is currently `VARCHAR(2000)`. Typical Metaschema
constraints are well under 500 chars, so 2000 is fine for v1. If it
becomes a constraint we'll widen to `TEXT` in a follow-up migration —
not pre-emptively, to keep migrations focused.

### Visibility

Rules remain private to the creating user (matches current schema's
`user_id` foreign key). Org-wide sharing is deferred to v2 to avoid
bikeshedding permissions in this scope.

`MetapathConstraintService.loadEnabledConstraints(modelType, userId)`
filters by user id to honor this.

## Testing strategy

- **Backend unit tests** — `MetapathConstraintService`: parsing valid
  constraint XML, rejecting invalid, evaluating a fragment with hits,
  evaluating a fragment with no hits, cache invalidation on rule
  CRUD events.
- **Backend integration tests** — `AiRuleGenController`: full session
  lifecycle (start → describe → clarify → generate → save). Mock
  `AnthropicClient` to return deterministic envelopes; assert the test
  matrix is computed correctly. Add a real-API integration test gated
  on `ANTHROPIC_API_KEY` (matches the existing pattern in
  `AiCatalogWizardIntegrationTest`).
- **Backend integration test for enforcement** — create an
  AI-generated rule via API, upload an OSCAL doc that should fail it,
  assert the violation appears in the validation result with the
  `[custom: <rule-id>]` tag.
- **Frontend unit tests** — wizard state machine: clarification turn
  bubbles render in order, save button gating responds to matrix
  state, manual edit triggers re-validation.
- **E2E (Playwright)** — happy path: log in → /rules/custom → click
  "Generate with AI" → describe rule → wait for green matrix → save
  → see new rule in list with "AI" badge → upload bad OSCAL doc →
  see custom violation. Mock Claude responses at the network layer
  for determinism.

## Open questions

None blocking. The implementation plan should answer:

- Exact `liboscal-java` API for registering external constraint sets
  with `OscalBindingContext` (likely `IConstraintLoader` + a load step
  before deserialization, but verify against 6.0.0 source).
- Schema-summary asset format and where it lives in the repo
  (`back-end/src/main/resources/oscal-schema-summaries/<model>.txt`?).
- Whether the wizard's session state lives server-side
  (in-memory `Cache<SessionId, SessionState>` with 30-min TTL) or
  is round-tripped through the client. Server-side keeps the
  conversation transcript out of localStorage but adds a stateful
  surface; client-side is simpler but exposes prompt history. v1
  recommendation: server-side with TTL.

## Rollout

Single feature flag (`feature.ai-rule-gen`) gating the wizard entry
point and the AI-gen API endpoints. Enforcement of saved rules is
not flagged — once a rule is saved it's a regular custom rule and
gets enforced by default. Disable the flag rolls back the authoring
surface without disabling already-saved rules.
