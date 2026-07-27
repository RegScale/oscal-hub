# Guided Tours & Onboarding Plan

**Date:** 2026-07-27
**Status:** Phase 1 implemented (engine + Get Started tour) — see `docs/ONBOARDING-TOURS-PHASE1-PLAN.md`. Phases 2–4 pending.
**Author:** Claude (with Travis Howerton)

## Problem

OSCAL Hub is a free tool; adoption depends on ease of use. New users land on a
dashboard with 11+ feature tiles and no guidance on where to start. OSCAL
itself has a steep learning curve, so even experienced security practitioners
need orientation. We want an optional "Get Started" tour that explains how the
app works, plus advanced tours for the deeper workflows (validation,
authorizations, build/AI), all discoverable but never forced.

## Goals

1. A new user can go from first login to understanding the app's layout in
   under two minutes, without reading documentation.
2. Tours are **opt-in**: offered once, dismissible forever, always re-launchable.
3. Each tour ends by pointing at the next step (an advanced tour or the
   relevant guide page), so tours and the existing MDX guide reinforce each
   other rather than compete.
4. Tours meet the project's accessibility bar: keyboard operable, screen-reader
   friendly, and axe-clean under the zero-exclusion policy in
   `e2e/accessibility.spec.ts`.

## Non-goals (for now)

- Tour analytics / funnel tracking (Phase 4 option).
- Cross-device persistence of tour state via the backend (Phase 4 option).
- Tours for the super-admin console.
- Seeding demo data so tours can show populated screens (tours must work
  against empty accounts).

## Current state (from codebase exploration)

- **No tour library installed**; this is greenfield.
- **Stack:** Next.js 16 App Router, React 19, Tailwind v4 + shadcn/Radix,
  TanStack Query, dark-only theme via oklch tokens in `globals.css`.
- **No stable anchors:** nav items, dashboard tiles, and feature controls have
  no `data-testid`/`id` scheme — anchor attributes must be added.
- **Strong existing help layer to build on:**
  - Full MDX guide at `/guide` with per-feature pages.
  - `src/lib/help-targets.ts` (`HELP_TARGETS`) maps every feature to its guide
    page — ideal for "Learn more" links inside tour steps.
  - `src/components/HelpButton.tsx` puts a `?` on feature pages.
  - `EmptyState` component with CTAs on the dashboard's no-org branches.
- **User model has no preferences field** (`src/types/auth.ts`); the only
  client-side preference precedent is `guide.toc.expanded` in localStorage
  (`src/components/guide/DocSidebar.tsx`).
- **Role gating matters:** super admins never see the dashboard (redirected to
  `/admin`); users without org access see an onboarding empty state instead of
  tiles. Nav links `Browse`/`Documentation`/`Actions` are hidden below the
  `sm` breakpoint.

## Approaches considered

### Option A — Custom lightweight tour engine on Radix (RECOMMENDED)

Build a small (~400–600 line) tour engine using primitives already in the
codebase: a Radix-based popover/dialog for step content, an SVG-mask spotlight
overlay, and a React context for orchestration.

- **Pros:** Full control over ARIA/focus (critical for the zero-exclusion axe
  policy); step content is real React (can use `next/link`, `HELP_TARGETS`,
  lucide icons); styles use the existing oklch tokens so it looks native;
  no new dependency; cross-page tours need custom orchestration anyway, so a
  library only saves the rendering layer.
- **Cons:** We own positioning/scroll/resize edge cases (mitigated: Radix
  handles anchoring, collision, and focus; the spotlight is the only truly
  custom rendering).

### Option B — driver.js as the render engine

[driver.js](https://driverjs.com) (MIT, ~5 kB, vanilla JS, actively
maintained) renders highlight + popover; we still write the orchestration
layer (cross-page steps, persistence, launcher UI).

- **Pros:** Battle-tested highlighting/positioning; tiny; framework-agnostic
  so React 19 is a non-issue.
- **Cons:** Popover content is DOM/HTML strings, not React — "Learn more"
  links and shadcn-styled buttons get awkward; ships its own CSS to re-theme;
  its default DOM/ARIA output isn't guaranteed axe-clean under our policy, and
  fixing that means fighting the library.

### Option C — Onborda / NextStep (Next.js-native tour libs)

- **Pros:** App Router aware, React 19 compatible, polished animations.
- **Cons:** Pulls in framer-motion (heavy); young projects with small
  communities; less control over a11y output; still requires our own
  persistence + launcher layer.

**Rejected outright:** react-joyride (no React 19 support), react-shepherd
(same), intro.js (AGPL/commercial-license friction).

**Decision: Option A (confirmed by Travis, 2026-07-27).** The deciding factors are the strict
accessibility policy (we need to own the DOM) and the fact that step content
should be first-class React so tours can deep-link into the guide. If the
spotlight/positioning work balloons during implementation, driver.js (Option
B) is the fallback — the tour definitions and orchestration layer are designed
to be render-engine agnostic, so switching costs little.

## Design

### 1. Tour engine (`src/components/tour/`, `src/lib/tours/`)

```
src/lib/tours/
  types.ts            # TourStep, TourDefinition, TourState
  registry.ts         # all registered tours
  get-started.tsx     # step definitions per tour (one file each)
  validate.tsx
  share-library.tsx
  authorizations.tsx
  storage.ts          # localStorage read/write, versioning
src/components/tour/
  TourProvider.tsx    # context + reducer: activeTour, stepIndex, next/back/skip
  TourSpotlight.tsx   # fixed overlay w/ SVG mask cutout around target
  TourPopover.tsx     # step card: title, body, progress dots, Back/Next/Skip,
                      #   optional "Learn more" -> /guide/<HELP_TARGETS[slug]>
  TourWelcomeDialog.tsx  # first-run "Take a 2-minute tour?" prompt
  TourMenu.tsx        # list of available tours with completed checkmarks
```

**Step/tour contract:**

```ts
interface TourStep {
  id: string;
  target?: string;          // matches [data-tour="…"]; undefined = centered modal step
  route?: string;           // router.push before showing (cross-page tours)
  title: string;
  body: React.ReactNode;
  helpSlug?: HelpSlug;      // renders "Learn more" link into the guide
  placement?: 'top' | 'bottom' | 'left' | 'right';
  onBeforeStep?: () => void | Promise<void>;  // e.g. open the Actions popover
  skipIfMissing?: boolean;  // default true: absent target => skip step, don't break
}

interface TourDefinition {
  id: string;               // 'get-started', 'validate', …
  version: number;          // bump to re-offer after major UI changes
  title: string;
  description: string;
  startRoute: string;
  eligible: (user: User | null) => boolean;  // role/org gating
  minViewport?: 'sm';       // steps target sm:-hidden nav? require >= sm
  steps: TourStep[];
}
```

**Behavioral rules:**

- `TourProvider` mounts once in the root layout (inside `AuthProvider`).
- Targets resolve by `document.querySelector('[data-tour="…"]')` with a short
  retry window (targets may mount async). Missing target after retry ⇒ skip
  the step (log in dev), never crash or trap the user.
- Cross-page steps: `route` triggers `router.push`, engine waits for the
  target to appear before rendering the step.
- Route changes *not* initiated by the tour (user clicks away) end the tour
  gracefully and record it as dismissed-at-step.
- Steps that need transient UI open (e.g. the Actions popover) use
  `onBeforeStep`; the popover component exposes a controlled-open hook for
  this. If that proves fragile, fall back to anchoring the *trigger* button
  and describing the menu instead of opening it.
- Viewport below the tour's `minViewport` ⇒ launcher shows the tour as
  unavailable on small screens ("best experienced on a larger screen") rather
  than running a broken tour. MVP: `get-started` requires `sm`.

### 2. Anchor attributes (`data-tour`)

Add `data-tour` attributes to stable UI landmarks. Initial set:

| Attribute | Element | File |
|---|---|---|
| `nav-logo` | OSCAL Hub logo link | `src/components/Navigation.tsx` |
| `nav-browse` | Browse link | `Navigation.tsx` |
| `nav-docs` | Documentation link | `Navigation.tsx` |
| `nav-actions` | Actions popover trigger | `Navigation.tsx` |
| `nav-org-switcher` | OrganizationSwitcher | `organization-switcher.tsx` |
| `nav-avatar` | UserAvatarMenu trigger | `UserAvatarMenu.tsx` |
| `dashboard-tiles` | feature tile grid | `src/app/page.tsx` |
| `dashboard-getting-started` | Getting Started card | `src/app/page.tsx` |
| `help-button` | per-page `?` help button | `HelpButton.tsx` |
| `library-upload` | upload control on Library page | `src/app/library/page.tsx` |
| `library-visibility-filter` | visibility filter bar | `src/app/library/page.tsx` |
| `library-item-actions` | visibility action menu on a card | `library/VisibilityActionMenu.tsx` |
| `validate-*`, `authz-*`, `build-*` | per-tour targets | added in Phases 2–3 |

Convention: kebab-case, `area-element` naming, documented in the tour README.
These double as stable e2e selectors — a side benefit for Playwright tests.

### 3. Tour catalog

**Basic — "Get Started" (~8 steps, ~2 min, on `/`):**

1. *(modal)* Welcome to OSCAL Hub — what it is, what OSCAL is in one sentence,
   "takes about 2 minutes, Esc to exit anytime".
2. `dashboard-tiles` — your toolbox: validate, convert, build, visualize…
3. `nav-actions` — the same tools from any page via the Actions menu.
4. `nav-browse` — the public catalog of example OSCAL documents (great first
   stop to grab a sample file).
5. `nav-docs` — the full documentation guide.
6. `nav-org-switcher` — you work inside an organization; switch here.
7. `nav-avatar` — profile, support tickets, org admin.
8. *(modal)* Finish — "Where next?": buttons to launch the **Validate** tour,
   open the guide's getting-started page, or browse the catalog.

**Advanced — "Validate a document" (on `/validate`, Phase 2):**
upload/paste area → format auto-detection → validation rules (link to
`/rules`) → run validation → reading results (errors vs warnings) → history
(`/history` records every run, re-runnable). Steps that only exist
post-validation (results panel) either anchor to the static container or use a
sample-document CTA; decided during implementation against the real DOM.

**Advanced — "Share content to the Library" (on `/library`, Phase 2):**
How community content flows from your account to the public catalog:

1. *(modal)* The Library is where your OSCAL documents live — and how you
   share them with your org or the world.
2. `library-upload` — getting content in: upload directly here, or use
   **Save to Library** from any Build wizard.
3. `library-visibility-filter` — every item has one of three visibility
   levels: **Private** (default — only you), **Organization** (your whole
   org), **Public** (everyone, including unauthenticated visitors).
4. `library-item-actions` — promote an item here: Private → Organization →
   Public. Each change confirms what the new audience will see. (Empty
   library: this step anchors the empty-state upload CTA and describes the
   menu instead — `skipIfMissing` handles the card-menu target.)
5. `nav-browse` — Public items appear in the **Browse** catalog at
   `/catalog`, searchable by anyone on the internet; downloads still require
   sign-in.
6. *(modal)* Finish — "Learn more" links to the guide's `library/visibility`
   and `library/public-catalog` pages (both already in `HELP_TARGETS`).

**Advanced — "Authorizations" (on `/authorizations`, Phase 3):**
Authorizations vs Templates tabs → what a template is (markdown +
`{{variables}}`) → New Authorization wizard overview (select SSP → template →
variables → review) → the authorization detail page's tabs (Overview,
Documents, Continuous Monitoring). Multi-page; empty accounts tour the
empty-state CTAs. May split into "Authorizations basics" + a contextual detail
-page tour if a single tour runs long.

**Advanced — "Build & AI" (on `/build`, Phase 3):**
model-type cards → visual builder concept → AI wizard (`/ai/wizard`) →
generated-document handoff to Validate/Library.

Catalog is extensible: adding a tour = one definition file + anchors +
registry entry.

### 4. Entry points & triggering

1. **First-run prompt (the "option" for a Get Started tour):** on the
   dashboard, when the user is authenticated, has org access, and
   `welcomePromptSeen` is unset ⇒ show `TourWelcomeDialog`:
   *"New here? Take a 2-minute tour."* → **Start tour** / **Maybe later** /
   **Don't ask again**. "Maybe later" re-prompts next session (max 3 times,
   then converts to "Don't ask again"); any choice never blocks the app.
   Never auto-starts a tour uninvited.
2. **Avatar menu:** "Guided tours" item in `UserAvatarMenu` opens `TourMenu`
   (all tours, completed checkmarks, replay).
3. **Dashboard "Getting Started" card:** add a "Take the tour" action.
4. **HelpButton upgrade (Phase 2):** on pages that have a tour, the `?`
   becomes a two-item popover: "Read the guide" + "Tour this page". Pages
   without a tour keep today's direct-link behavior.
5. **Guide cross-links (Phase 2+):** relevant guide pages get a "Prefer a
   guided tour?" launch button (small MDX component).

### 5. Persistence

Phase 1 uses localStorage (follows the `guide.toc.expanded` precedent), keyed
per user so shared machines don't cross-contaminate:

```
key:   oscal-hub.tours.v1.<userId>
value: {
  welcomePrompt: { seen: boolean, deferrals: number },
  tours: { [tourId]: { completedVersion?: number, dismissedAtStep?: number } }
}
```

- Completed/dismissed tours never re-prompt; replay is always available from
  the launcher.
- Bumping a tour's `version` makes the launcher badge it as "Updated" (no
  re-prompting).
- **Phase 4 option:** persist to a backend `user_preferences` JSON column
  (new entity + Flyway migration per the schema policy) with localStorage as
  cache, giving cross-device state. Deliberately out of MVP.

### 6. Accessibility requirements (hard requirements, not polish)

- Step popover is a labeled `role="dialog"` with focus trapped while the tour
  is active; focus moves into it on each step, `aria-labelledby`/
  `aria-describedby` set.
- Keyboard: `Enter`/`→` next, `←` back, `Esc` exits (records dismissal).
- Step changes announced via `aria-live="polite"` (e.g. "Step 3 of 8: …").
- Spotlight overlay is `aria-hidden` decoration only; it must not remove page
  content from the accessibility tree, and it must not intercept AT focus.
- `prefers-reduced-motion`: no animated spotlight transitions; instant moves;
  `scrollIntoView` uses `behavior: 'auto'`.
- All colors from existing theme tokens; text on overlay meets contrast.
- `e2e/accessibility.spec.ts` gains a scan **with a tour open mid-step** —
  the zero-exclusion policy applies to tour UI.

### 7. Testing

- **Vitest:** engine reducer (next/back/skip/complete), storage versioning +
  per-user keying, eligibility gating, missing-target skip, welcome-prompt
  deferral logic.
- **Playwright:** complete the Get Started tour end-to-end; Esc-dismiss
  records state and never re-prompts; replay from avatar menu; keyboard-only
  completion; axe scan mid-tour; tour ends gracefully when user navigates
  away.

### 8. Phased rollout

| Phase | Scope | Est. size |
|---|---|---|
| **1 — MVP** | Tour engine, anchors, Get Started tour, welcome prompt, avatar-menu launcher, localStorage persistence, unit + e2e + a11y tests | The bulk of the work; one PR |
| **2** | Validate tour, Share-to-Library tour, HelpButton "Tour this page" upgrade, guide cross-links | Small PR |
| **3** | Authorizations tour, Build & AI tour | Small-medium PR |
| **4 (optional)** | Backend preference sync, tour analytics, onboarding checklist widget, admin-console tour | Only if usage justifies |

Front-end only; no backend or schema changes until Phase 4.

## Open questions for review

1. ~~Engine choice~~ — **Resolved:** custom Radix-based engine (Option A),
   confirmed 2026-07-27.
2. **First-run behavior:** prompt-with-dialog as designed, or auto-start the
   Get Started tour for brand-new accounts? (Plan recommends prompt — forced
   tours annoy expert users.)
3. **Advanced tour priority:** Phase 2 = Validation + Share-to-Library,
   Phase 3 = Authorizations + Build/AI — right order? Authorizations is the
   richest workflow but also the one requiring the most existing data.
4. **Backend persistence:** comfortable deferring cross-device tour state to
   Phase 4?
