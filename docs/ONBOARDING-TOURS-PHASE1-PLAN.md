# Guided Onboarding Tours — Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the custom tour engine and ship the opt-in "Get Started" tour: first-run welcome prompt, avatar-menu launcher, localStorage persistence, and unit + e2e + a11y coverage.

**Architecture:** A `TourProvider` React context mounted in the root layout orchestrates tours; it resolves `[data-tour]` anchor elements, renders a spotlight overlay (SVG mask) plus a focus-trapped step dialog via portal, and persists completion/dismissal per user in localStorage. Tour content is defined declaratively in `src/lib/tours/` and rendered by dumb components in `src/components/tour/`.

**Tech Stack:** Next.js 16 App Router, React 19, TypeScript, Tailwind v4 + shadcn/Radix (existing `Dialog`, `Button`), Vitest + Testing Library (happy-dom), Playwright + @axe-core/playwright.

**Spec:** `docs/ONBOARDING-TOURS-PLAN.md` (design doc, approved). This plan implements Phase 1 only.

## Global Constraints

- **No new npm dependencies.** The engine uses only what is already in `front-end/package.json`.
- All work is under `front-end/`; no backend or schema changes.
- Dark theme only: style exclusively with existing tokens (`bg-popover`, `text-muted-foreground`, `border-border`, `var(--ring)`, etc.). No hard-coded colors.
- Accessibility is a hard requirement: `role="dialog"` with focus trap, Esc exits, `aria-live="polite"` step announcements, `prefers-reduced-motion`-safe (no animations; `scrollIntoView` with `behavior: 'auto'`), axe-clean with **zero excluded rules** (see `e2e/accessibility.spec.ts`).
- Anchor attribute convention: `data-tour="<area>-<element>"`, kebab-case.
- localStorage key: `oscal-hub.tours.v1.<userId>`; sessionStorage key: `oscal-hub.tours.prompt-deferred`.
- Tours never auto-start; the welcome prompt only offers. Prompt: max 3 "Maybe later" deferrals, then treated as "don't ask again".
- Super admins and users without an organization are not eligible for the Get Started tour.
- The Get Started tour requires viewport ≥ 640 px (nav links are `hidden` below `sm`).
- Unit test commands run from `front-end/`: `npx vitest run <path>`. E2E: `npm run test:e2e -- <file>`.
- Commit after every task (branch: `feature/onboarding-tours`).

---

### Task 1: Tour types, storage module, and registry stub

**Files:**
- Create: `front-end/src/lib/tours/types.ts`
- Create: `front-end/src/lib/tours/storage.ts`
- Create: `front-end/src/lib/tours/registry.ts` (stub; filled in Task 4)
- Test: `front-end/src/lib/tours/__tests__/storage.test.ts`

**Interfaces:**
- Consumes: `User` from `@/types/auth`, `HelpSlug` from `@/lib/help-targets`.
- Produces (used by Tasks 3–7):
  - Types `TourStep`, `TourDefinition`, `TourRecord`, `TourStorageState`, `TourPlacement`.
  - `loadTourState(userId: number): TourStorageState`
  - `saveTourState(userId: number, state: TourStorageState): void`
  - `markTourCompleted(userId: number, tourId: string, version: number): TourStorageState`
  - `markTourDismissed(userId: number, tourId: string, version: number, stepIndex: number): TourStorageState`
  - `shouldShowWelcomePrompt(state: TourStorageState, getStartedTourId: string): boolean`
  - `recordWelcomeDeferral(userId: number): TourStorageState`
  - `dismissWelcomePrompt(userId: number): TourStorageState`
  - `wasDeferredThisSession(): boolean`, `MAX_WELCOME_DEFERRALS = 3`
  - `TOURS: TourDefinition[]`, `getTour(id: string): TourDefinition | undefined` from registry.

- [ ] **Step 1: Write the failing test**

Create `front-end/src/lib/tours/__tests__/storage.test.ts`:

```ts
import { beforeEach, describe, expect, it } from 'vitest';
import {
  loadTourState,
  saveTourState,
  markTourCompleted,
  markTourDismissed,
  shouldShowWelcomePrompt,
  recordWelcomeDeferral,
  dismissWelcomePrompt,
  wasDeferredThisSession,
  defaultTourState,
  MAX_WELCOME_DEFERRALS,
} from '@/lib/tours/storage';

const USER_ID = 42;
const KEY = `oscal-hub.tours.v1.${USER_ID}`;

describe('tour storage', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('returns the default state when nothing is stored', () => {
    const state = loadTourState(USER_ID);
    expect(state).toEqual({ welcomePrompt: { seen: false, deferrals: 0 }, tours: {} });
  });

  it('round-trips state through localStorage under a per-user key', () => {
    const state = defaultTourState();
    state.tours['get-started'] = { completedVersion: 1 };
    saveTourState(USER_ID, state);
    expect(localStorage.getItem(KEY)).toBeTruthy();
    expect(loadTourState(USER_ID)).toEqual(state);
    // A different user sees a clean slate.
    expect(loadTourState(7)).toEqual(defaultTourState());
  });

  it('falls back to the default state on corrupted JSON', () => {
    localStorage.setItem(KEY, '{not json');
    expect(loadTourState(USER_ID)).toEqual(defaultTourState());
  });

  it('markTourCompleted records the version and clears any dismissal', () => {
    markTourDismissed(USER_ID, 'get-started', 1, 3);
    const state = markTourCompleted(USER_ID, 'get-started', 1);
    expect(state.tours['get-started']).toEqual({ completedVersion: 1 });
    expect(loadTourState(USER_ID).tours['get-started']).toEqual({ completedVersion: 1 });
  });

  it('markTourDismissed records the step and version', () => {
    const state = markTourDismissed(USER_ID, 'get-started', 1, 2);
    expect(state.tours['get-started']).toEqual({ dismissedVersion: 1, dismissedAtStep: 2 });
  });

  describe('shouldShowWelcomePrompt', () => {
    it('is true for a brand-new user', () => {
      expect(shouldShowWelcomePrompt(defaultTourState(), 'get-started')).toBe(true);
    });

    it('is false once the prompt was seen', () => {
      const state = dismissWelcomePrompt(USER_ID);
      expect(shouldShowWelcomePrompt(state, 'get-started')).toBe(false);
    });

    it('is false after MAX_WELCOME_DEFERRALS deferrals', () => {
      let state = defaultTourState();
      for (let i = 0; i < MAX_WELCOME_DEFERRALS; i++) {
        state = recordWelcomeDeferral(USER_ID);
      }
      expect(state.welcomePrompt.deferrals).toBe(MAX_WELCOME_DEFERRALS);
      expect(shouldShowWelcomePrompt(state, 'get-started')).toBe(false);
    });

    it('is false when the get-started tour was completed or dismissed', () => {
      const completed = markTourCompleted(USER_ID, 'get-started', 1);
      expect(shouldShowWelcomePrompt(completed, 'get-started')).toBe(false);
      localStorage.clear();
      const dismissed = markTourDismissed(USER_ID, 'get-started', 1, 0);
      expect(shouldShowWelcomePrompt(dismissed, 'get-started')).toBe(false);
    });
  });

  it('recordWelcomeDeferral also flags the current session', () => {
    expect(wasDeferredThisSession()).toBe(false);
    recordWelcomeDeferral(USER_ID);
    expect(wasDeferredThisSession()).toBe(true);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run (from `front-end/`): `npx vitest run src/lib/tours/__tests__/storage.test.ts`
Expected: FAIL — `Cannot find module '@/lib/tours/storage'` (or equivalent resolution error).

- [ ] **Step 3: Write the implementation**

Create `front-end/src/lib/tours/types.ts`:

```ts
import type { ReactNode } from 'react';
import type { User } from '@/types/auth';
import type { HelpSlug } from '@/lib/help-targets';

export type TourPlacement = 'top' | 'bottom' | 'left' | 'right';

export interface TourStep {
  /** Unique within the tour; also used for aria ids. */
  id: string;
  /** Matches an element with [data-tour="<target>"]. Omit for a centered modal step. */
  target?: string;
  title: string;
  body: ReactNode;
  /** Renders a "Learn more in the guide" link (opens in a new tab). */
  helpSlug?: HelpSlug;
  /** Preferred popover side; auto-flips when it doesn't fit. */
  placement?: TourPlacement;
  /** When the target is absent, skip the step instead of ending the tour. Default true. */
  skipIfMissing?: boolean;
}

export interface TourDefinition {
  id: string;
  /** Bump after major UI changes so the launcher can badge the tour as updated. */
  version: number;
  title: string;
  description: string;
  /** Route the tour runs on; startTour navigates here first. */
  startRoute: string;
  eligible: (user: User | null) => boolean;
  /** Anchored steps target sm:-hidden nav items; block starting below this width. */
  minViewportWidth?: number;
  steps: TourStep[];
}

export interface TourRecord {
  completedVersion?: number;
  dismissedVersion?: number;
  dismissedAtStep?: number;
}

export interface TourStorageState {
  welcomePrompt: { seen: boolean; deferrals: number };
  tours: Record<string, TourRecord>;
}
```

Create `front-end/src/lib/tours/storage.ts`:

```ts
import type { TourStorageState } from './types';

const KEY_PREFIX = 'oscal-hub.tours.v1.';
const SESSION_DEFER_KEY = 'oscal-hub.tours.prompt-deferred';

export const MAX_WELCOME_DEFERRALS = 3;

function storageKey(userId: number): string {
  return `${KEY_PREFIX}${userId}`;
}

export function defaultTourState(): TourStorageState {
  return { welcomePrompt: { seen: false, deferrals: 0 }, tours: {} };
}

export function loadTourState(userId: number): TourStorageState {
  if (typeof window === 'undefined') return defaultTourState();
  try {
    const raw = window.localStorage.getItem(storageKey(userId));
    if (!raw) return defaultTourState();
    const parsed = JSON.parse(raw);
    return {
      welcomePrompt: {
        seen: Boolean(parsed?.welcomePrompt?.seen),
        deferrals: Number(parsed?.welcomePrompt?.deferrals) || 0,
      },
      tours: parsed?.tours && typeof parsed.tours === 'object' ? parsed.tours : {},
    };
  } catch {
    return defaultTourState();
  }
}

export function saveTourState(userId: number, state: TourStorageState): void {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.setItem(storageKey(userId), JSON.stringify(state));
  } catch {
    // Quota exceeded / private mode: tour state is a nicety, never fatal.
  }
}

export function markTourCompleted(userId: number, tourId: string, version: number): TourStorageState {
  const state = loadTourState(userId);
  // Completion supersedes any earlier dismissal.
  state.tours[tourId] = { completedVersion: version };
  saveTourState(userId, state);
  return state;
}

export function markTourDismissed(
  userId: number,
  tourId: string,
  version: number,
  stepIndex: number,
): TourStorageState {
  const state = loadTourState(userId);
  const existing = state.tours[tourId] ?? {};
  state.tours[tourId] = { ...existing, dismissedVersion: version, dismissedAtStep: stepIndex };
  saveTourState(userId, state);
  return state;
}

export function shouldShowWelcomePrompt(state: TourStorageState, getStartedTourId: string): boolean {
  if (state.welcomePrompt.seen) return false;
  if (state.welcomePrompt.deferrals >= MAX_WELCOME_DEFERRALS) return false;
  const record = state.tours[getStartedTourId];
  if (record && (record.completedVersion !== undefined || record.dismissedAtStep !== undefined)) {
    return false;
  }
  return true;
}

export function recordWelcomeDeferral(userId: number): TourStorageState {
  const state = loadTourState(userId);
  state.welcomePrompt.deferrals += 1;
  saveTourState(userId, state);
  try {
    window.sessionStorage.setItem(SESSION_DEFER_KEY, '1');
  } catch {
    // Non-fatal.
  }
  return state;
}

/** "Maybe later" suppresses the prompt for the rest of the browser session. */
export function wasDeferredThisSession(): boolean {
  if (typeof window === 'undefined') return false;
  try {
    return window.sessionStorage.getItem(SESSION_DEFER_KEY) === '1';
  } catch {
    return false;
  }
}

export function dismissWelcomePrompt(userId: number): TourStorageState {
  const state = loadTourState(userId);
  state.welcomePrompt.seen = true;
  saveTourState(userId, state);
  return state;
}
```

Create `front-end/src/lib/tours/registry.ts` (stub — the Get Started tour is added in Task 4):

```ts
import type { TourDefinition } from './types';

export const GET_STARTED_TOUR_ID = 'get-started';

/** All registered tours. Populated as tour definitions land (Task 4+). */
export const TOURS: TourDefinition[] = [];

export function getTour(id: string): TourDefinition | undefined {
  return TOURS.find((tour) => tour.id === id);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run src/lib/tours/__tests__/storage.test.ts`
Expected: PASS (9 tests).

- [ ] **Step 5: Commit**

```bash
git add front-end/src/lib/tours
git commit -m "feat(tours): add tour types, per-user localStorage persistence, registry stub"
```

---

### Task 2: Popover positioning helper

**Files:**
- Create: `front-end/src/lib/tours/position.ts`
- Test: `front-end/src/lib/tours/__tests__/position.test.ts`

**Interfaces:**
- Consumes: `TourPlacement` from `./types`.
- Produces (used by Task 3's `TourPopover`):
  - `computePopoverPosition(target: Rect, popover: Size, viewport: Size, preferred?: TourPlacement): PopoverPosition`
  - `interface Rect { x: number; y: number; width: number; height: number }`
  - `interface Size { width: number; height: number }`
  - `interface PopoverPosition { top: number; left: number; placement: TourPlacement }`

- [ ] **Step 1: Write the failing test**

Create `front-end/src/lib/tours/__tests__/position.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { computePopoverPosition } from '@/lib/tours/position';

const VIEWPORT = { width: 1280, height: 800 };
const POPOVER = { width: 320, height: 180 };

describe('computePopoverPosition', () => {
  it('places below the target when there is room (default)', () => {
    const target = { x: 480, y: 100, width: 200, height: 40 };
    const pos = computePopoverPosition(target, POPOVER, VIEWPORT);
    expect(pos.placement).toBe('bottom');
    expect(pos.top).toBe(100 + 40 + 12); // target bottom + 12px gap
    expect(pos.left).toBe(480 + 100 - 160); // centered on target
  });

  it('flips above when the target is near the bottom edge', () => {
    const target = { x: 480, y: 740, width: 200, height: 40 };
    const pos = computePopoverPosition(target, POPOVER, VIEWPORT);
    expect(pos.placement).toBe('top');
    expect(pos.top).toBe(740 - 12 - 180);
  });

  it('honors a preferred placement that fits', () => {
    const target = { x: 600, y: 400, width: 100, height: 40 };
    const pos = computePopoverPosition(target, POPOVER, VIEWPORT, 'right');
    expect(pos.placement).toBe('right');
    expect(pos.left).toBe(600 + 100 + 12);
  });

  it('clamps fully inside the viewport when nothing fits (oversized target)', () => {
    const target = { x: 0, y: 0, width: 1280, height: 800 };
    const pos = computePopoverPosition(target, POPOVER, VIEWPORT);
    expect(pos.top).toBeGreaterThanOrEqual(16);
    expect(pos.left).toBeGreaterThanOrEqual(16);
    expect(pos.top + POPOVER.height).toBeLessThanOrEqual(VIEWPORT.height - 16);
    expect(pos.left + POPOVER.width).toBeLessThanOrEqual(VIEWPORT.width - 16);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run src/lib/tours/__tests__/position.test.ts`
Expected: FAIL — cannot resolve `@/lib/tours/position`.

- [ ] **Step 3: Write the implementation**

Create `front-end/src/lib/tours/position.ts`:

```ts
import type { TourPlacement } from './types';

export interface Rect {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface Size {
  width: number;
  height: number;
}

export interface PopoverPosition {
  top: number;
  left: number;
  placement: TourPlacement;
}

/** Gap between the target's spotlight and the popover. */
const GAP = 12;
/** Minimum distance from any viewport edge. */
const MARGIN = 16;

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), Math.max(min, max));
}

function positionFor(placement: TourPlacement, target: Rect, popover: Size): { top: number; left: number } {
  switch (placement) {
    case 'top':
      return { top: target.y - GAP - popover.height, left: target.x + target.width / 2 - popover.width / 2 };
    case 'bottom':
      return { top: target.y + target.height + GAP, left: target.x + target.width / 2 - popover.width / 2 };
    case 'left':
      return { top: target.y + target.height / 2 - popover.height / 2, left: target.x - GAP - popover.width };
    case 'right':
      return { top: target.y + target.height / 2 - popover.height / 2, left: target.x + target.width + GAP };
  }
}

function fits(pos: { top: number; left: number }, popover: Size, viewport: Size): boolean {
  return (
    pos.top >= MARGIN &&
    pos.left >= MARGIN &&
    pos.top + popover.height <= viewport.height - MARGIN &&
    pos.left + popover.width <= viewport.width - MARGIN
  );
}

export function computePopoverPosition(
  target: Rect,
  popover: Size,
  viewport: Size,
  preferred?: TourPlacement,
): PopoverPosition {
  const order: TourPlacement[] = preferred
    ? [preferred, 'bottom', 'top', 'right', 'left']
    : ['bottom', 'top', 'right', 'left'];
  for (const placement of order) {
    const pos = positionFor(placement, target, popover);
    if (fits(pos, popover, viewport)) {
      return { ...pos, placement };
    }
  }
  // Oversized target or tiny viewport: fall back to the first choice, clamped
  // fully inside the viewport so the dialog is always reachable.
  const fallback = positionFor(order[0], target, popover);
  return {
    top: clamp(fallback.top, MARGIN, viewport.height - popover.height - MARGIN),
    left: clamp(fallback.left, MARGIN, viewport.width - popover.width - MARGIN),
    placement: order[0],
  };
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run src/lib/tours/__tests__/position.test.ts`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add front-end/src/lib/tours/position.ts front-end/src/lib/tours/__tests__/position.test.ts
git commit -m "feat(tours): add pure popover positioning helper with flip and clamp"
```

---

### Task 3: Tour engine — TourProvider, TourSpotlight, TourPopover

**Files:**
- Create: `front-end/src/components/tour/TourProvider.tsx`
- Create: `front-end/src/components/tour/TourSpotlight.tsx`
- Create: `front-end/src/components/tour/TourPopover.tsx`
- Test: `front-end/src/components/tour/__tests__/TourProvider.test.tsx`

**Interfaces:**
- Consumes: Task 1 storage functions + types; Task 2 `computePopoverPosition`; `useAuth` from `@/contexts/AuthContext`; `useRouter`/`usePathname` from `next/navigation`; `Button` from `@/components/ui/button`; `HELP_TARGETS` from `@/lib/help-targets`; `TOURS` from `@/lib/tours/registry`.
- Produces (used by Tasks 5–6):
  - `TourProvider({ children, tours?, targetTimeoutMs? })` — `tours` defaults to registry `TOURS`; `targetTimeoutMs` defaults to 2000 (overridable for tests).
  - `useTour(): { activeTour: TourDefinition | null; stepIndex: number; startTour(tourId: string): void; endTour(reason: 'completed' | 'dismissed'): void; next(): void; back(): void }`

Behavior contract (all covered by tests):
- `startTour` no-ops if the tour is unknown, ineligible for the current user, or the viewport is narrower than `minViewportWidth`. If the current pathname differs from `startRoute` it navigates there first and waits for arrival before treating route changes as dismissal.
- Steps with a `target` poll `[data-tour="<target>"]` every 100 ms up to `targetTimeoutMs`; a missing target skips the step in the direction of travel (`skipIfMissing !== false`), else ends the tour as dismissed.
- Advancing past the last step completes the tour (`markTourCompleted`). Esc anywhere dismisses (`markTourDismissed` with the current step). A user-initiated route change dismisses — except on the final step, where it counts as completed (finish-step CTAs are links).
- Each step change announces "Step N of M: <title>" via a persistent `aria-live="polite"` region and moves focus into the dialog. Focus returns to the previously focused element when the tour ends.

- [ ] **Step 1: Write the failing test**

Create `front-end/src/components/tour/__tests__/TourProvider.test.tsx`:

```tsx
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TourProvider, useTour } from '@/components/tour/TourProvider';
import type { TourDefinition } from '@/lib/tours/types';

const TEST_USER = {
  userId: 42,
  username: 'tester',
  email: 't@example.com',
  organizationId: 1,
};

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ user: TEST_USER }),
}));

function makeTour(overrides: Partial<TourDefinition> = {}): TourDefinition {
  return {
    id: 'test-tour',
    version: 1,
    title: 'Test tour',
    description: 'A tour for tests',
    startRoute: '/',
    eligible: () => true,
    steps: [
      { id: 'intro', title: 'Welcome step', body: 'Intro body' },
      { id: 'anchored', target: 'test-target', title: 'Anchored step', body: 'Anchored body' },
      { id: 'done', title: 'Finish step', body: 'Finish body' },
    ],
    ...overrides,
  };
}

function LaunchButton({ tourId }: { tourId: string }) {
  const { startTour } = useTour();
  return (
    <button type="button" onClick={() => startTour(tourId)}>
      launch
    </button>
  );
}

function renderHarness(tour: TourDefinition) {
  return render(
    <TourProvider tours={[tour]} targetTimeoutMs={300}>
      <div data-tour="test-target">Target content</div>
      <LaunchButton tourId={tour.id} />
    </TourProvider>,
  );
}

describe('TourProvider', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('starts a tour and shows the first (modal) step as a dialog', async () => {
    const user = userEvent.setup();
    renderHarness(makeTour());
    await user.click(screen.getByRole('button', { name: 'launch' }));
    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveTextContent('Welcome step');
    expect(dialog).toHaveTextContent('Step 1 of 3');
  });

  it('advances to an anchored step and back', async () => {
    const user = userEvent.setup();
    renderHarness(makeTour());
    await user.click(screen.getByRole('button', { name: 'launch' }));
    await screen.findByRole('dialog');
    await user.click(screen.getByRole('button', { name: 'Next' }));
    await waitFor(() => expect(screen.getByRole('dialog')).toHaveTextContent('Anchored step'));
    await user.click(screen.getByRole('button', { name: 'Back' }));
    await waitFor(() => expect(screen.getByRole('dialog')).toHaveTextContent('Welcome step'));
  });

  it('skips a step whose target is missing', async () => {
    const user = userEvent.setup();
    const tour = makeTour({
      steps: [
        { id: 'intro', title: 'Welcome step', body: 'Intro' },
        { id: 'ghost', target: 'does-not-exist', title: 'Ghost step', body: 'Never shown' },
        { id: 'done', title: 'Finish step', body: 'Finish' },
      ],
    });
    renderHarness(tour);
    await user.click(screen.getByRole('button', { name: 'launch' }));
    await screen.findByRole('dialog');
    await user.click(screen.getByRole('button', { name: 'Next' }));
    // Ghost step times out (300ms) and is skipped forward to the finish step.
    await waitFor(() => expect(screen.getByRole('dialog')).toHaveTextContent('Finish step'), {
      timeout: 2000,
    });
  });

  it('completes the tour from the last step and persists completion', async () => {
    const user = userEvent.setup();
    const tour = makeTour({ steps: [{ id: 'only', title: 'Only step', body: 'One and done' }] });
    renderHarness(tour);
    await user.click(screen.getByRole('button', { name: 'launch' }));
    await screen.findByRole('dialog');
    await user.click(screen.getByRole('button', { name: 'Finish' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    const stored = JSON.parse(localStorage.getItem('oscal-hub.tours.v1.42') ?? '{}');
    expect(stored.tours['test-tour']).toEqual({ completedVersion: 1 });
  });

  it('dismisses on Escape and records the step index', async () => {
    const user = userEvent.setup();
    renderHarness(makeTour());
    await user.click(screen.getByRole('button', { name: 'launch' }));
    await screen.findByRole('dialog');
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    const stored = JSON.parse(localStorage.getItem('oscal-hub.tours.v1.42') ?? '{}');
    expect(stored.tours['test-tour']).toEqual({ dismissedVersion: 1, dismissedAtStep: 0 });
  });

  it('refuses to start an ineligible tour', async () => {
    const user = userEvent.setup();
    renderHarness(makeTour({ eligible: () => false }));
    await user.click(screen.getByRole('button', { name: 'launch' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('refuses to start below the minimum viewport width', async () => {
    const user = userEvent.setup();
    renderHarness(makeTour({ minViewportWidth: 99999 }));
    await user.click(screen.getByRole('button', { name: 'launch' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run src/components/tour/__tests__/TourProvider.test.tsx`
Expected: FAIL — cannot resolve `@/components/tour/TourProvider`.

- [ ] **Step 3: Write TourSpotlight**

Create `front-end/src/components/tour/TourSpotlight.tsx`:

```tsx
'use client';

/** Padding between the target's bounding box and the spotlight cutout. */
const PADDING = 8;

interface TourSpotlightProps {
  /** Viewport-relative rect of the highlighted element; null dims everything. */
  targetRect: DOMRect | null;
}

/**
 * Full-viewport dimming layer with a rounded cutout over the tour target.
 * Purely decorative: hidden from AT, and the parent overlay blocks pointer
 * events so the page can't be interacted with mid-tour.
 */
export function TourSpotlight({ targetRect }: TourSpotlightProps) {
  const cutout = targetRect
    ? {
        x: targetRect.x - PADDING,
        y: targetRect.y - PADDING,
        width: targetRect.width + PADDING * 2,
        height: targetRect.height + PADDING * 2,
      }
    : null;

  return (
    <div className="absolute inset-0" aria-hidden="true">
      <svg className="h-full w-full">
        <defs>
          <mask id="tour-spotlight-mask">
            <rect width="100%" height="100%" fill="white" />
            {cutout && <rect {...cutout} rx={8} fill="black" />}
          </mask>
        </defs>
        <rect width="100%" height="100%" fill="black" fillOpacity={0.65} mask="url(#tour-spotlight-mask)" />
        {cutout && <rect {...cutout} rx={8} fill="none" stroke="var(--ring)" strokeWidth={2} />}
      </svg>
    </div>
  );
}
```

- [ ] **Step 4: Write TourPopover**

Create `front-end/src/components/tour/TourPopover.tsx`:

```tsx
'use client';

import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { Button } from '@/components/ui/button';
import { HELP_TARGETS } from '@/lib/help-targets';
import { computePopoverPosition } from '@/lib/tours/position';
import type { TourStep } from '@/lib/tours/types';

interface TourPopoverProps {
  step: TourStep;
  stepIndex: number;
  totalSteps: number;
  /** Viewport-relative rect of the target; null renders a centered modal step. */
  targetRect: DOMRect | null;
  onNext: () => void;
  onBack: () => void;
  onSkip: () => void;
}

/**
 * The step card: a focus-trapped dialog positioned next to the spotlighted
 * element (or centered for modal steps). Keyboard: Tab cycles inside,
 * ArrowRight/Enter advance, ArrowLeft goes back, Esc (handled by the
 * provider) exits.
 */
export function TourPopover({ step, stepIndex, totalSteps, targetRect, onNext, onBack, onSkip }: TourPopoverProps) {
  const ref = useRef<HTMLDivElement>(null);
  const [pos, setPos] = useState<{ top: number; left: number } | null>(null);
  const isLast = stepIndex === totalSteps - 1;

  useLayoutEffect(() => {
    if (!targetRect || !ref.current) {
      setPos(null);
      return;
    }
    const { width, height } = ref.current.getBoundingClientRect();
    const computed = computePopoverPosition(
      { x: targetRect.x, y: targetRect.y, width: targetRect.width, height: targetRect.height },
      { width: width || 320, height: height || 200 },
      { width: window.innerWidth, height: window.innerHeight },
      step.placement,
    );
    setPos({ top: computed.top, left: computed.left });
  }, [targetRect, step]);

  // Move focus into the dialog on every step change.
  useEffect(() => {
    ref.current?.focus();
  }, [step.id]);

  function handleKeyDown(event: React.KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'ArrowRight' || (event.key === 'Enter' && event.target === ref.current)) {
      event.preventDefault();
      onNext();
      return;
    }
    if (event.key === 'ArrowLeft' && stepIndex > 0) {
      event.preventDefault();
      onBack();
      return;
    }
    if (event.key !== 'Tab') return;
    // Minimal focus trap: cycle Tab within the dialog's focusable elements.
    const focusables = ref.current?.querySelectorAll<HTMLElement>('a[href], button:not([disabled])');
    if (!focusables || focusables.length === 0) return;
    const first = focusables[0];
    const last = focusables[focusables.length - 1];
    if (event.shiftKey && (document.activeElement === first || document.activeElement === ref.current)) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }

  const anchored = targetRect !== null && pos !== null;

  return (
    <div
      ref={ref}
      role="dialog"
      aria-modal="true"
      aria-labelledby={`tour-step-title-${step.id}`}
      aria-describedby={`tour-step-body-${step.id}`}
      tabIndex={-1}
      onKeyDown={handleKeyDown}
      className={`fixed z-[101] w-80 max-w-[calc(100vw-2rem)] rounded-lg border border-border bg-popover p-4 text-popover-foreground shadow-lg focus:outline-none focus:ring-2 focus:ring-ring ${
        anchored ? '' : 'left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2'
      }`}
      style={anchored ? { top: pos.top, left: pos.left } : undefined}
    >
      <p className="text-xs text-muted-foreground">
        Step {stepIndex + 1} of {totalSteps}
      </p>
      <h2 id={`tour-step-title-${step.id}`} className="mt-1 text-base font-semibold">
        {step.title}
      </h2>
      <div id={`tour-step-body-${step.id}`} className="mt-2 text-sm text-muted-foreground">
        {step.body}
      </div>
      {step.helpSlug && (
        <a
          href={`/guide/${HELP_TARGETS[step.helpSlug]}`}
          target="_blank"
          rel="noopener noreferrer"
          className="mt-3 inline-block text-sm text-primary underline"
        >
          Learn more in the guide
        </a>
      )}
      <div className="mt-4 flex items-center justify-between gap-2">
        <Button variant="ghost" size="sm" onClick={onSkip}>
          Skip tour
        </Button>
        <div className="flex gap-2">
          {stepIndex > 0 && (
            <Button variant="outline" size="sm" onClick={onBack}>
              Back
            </Button>
          )}
          <Button size="sm" onClick={onNext}>
            {isLast ? 'Finish' : 'Next'}
          </Button>
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 5: Write TourProvider**

Create `front-end/src/components/tour/TourProvider.tsx`:

```tsx
'use client';

import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { usePathname, useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { TOURS } from '@/lib/tours/registry';
import { markTourCompleted, markTourDismissed } from '@/lib/tours/storage';
import type { TourDefinition } from '@/lib/tours/types';
import { TourSpotlight } from './TourSpotlight';
import { TourPopover } from './TourPopover';

const TARGET_POLL_INTERVAL_MS = 100;
const DEFAULT_TARGET_TIMEOUT_MS = 2000;

interface TourContextValue {
  activeTour: TourDefinition | null;
  stepIndex: number;
  startTour: (tourId: string) => void;
  endTour: (reason: 'completed' | 'dismissed') => void;
  next: () => void;
  back: () => void;
}

const TourContext = createContext<TourContextValue | null>(null);

export function useTour(): TourContextValue {
  const ctx = useContext(TourContext);
  if (!ctx) throw new Error('useTour must be used within a TourProvider');
  return ctx;
}

interface TourProviderProps {
  children: React.ReactNode;
  /** Overridable for tests; defaults to the app registry. */
  tours?: TourDefinition[];
  /** How long to wait for a step's [data-tour] target before skipping it. */
  targetTimeoutMs?: number;
}

export function TourProvider({ children, tours = TOURS, targetTimeoutMs = DEFAULT_TARGET_TIMEOUT_MS }: TourProviderProps) {
  const { user } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  const [activeTour, setActiveTour] = useState<TourDefinition | null>(null);
  const [stepIndex, setStepIndex] = useState(0);
  const [targetEl, setTargetEl] = useState<HTMLElement | null>(null);
  const [resolving, setResolving] = useState(false);
  const [announcement, setAnnouncement] = useState('');
  // Repaint trigger so the spotlight/popover track resize and scroll.
  const [, setRepaintTick] = useState(0);

  const directionRef = useRef<1 | -1>(1);
  // Set while startTour's router.push is in flight so the route-change
  // watcher doesn't mistake our own navigation for the user leaving.
  const awaitingRouteRef = useRef<string | null>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);

  const endTour = useCallback(
    (reason: 'completed' | 'dismissed') => {
      setActiveTour((tour) => {
        if (!tour) return null;
        if (user) {
          if (reason === 'completed') {
            markTourCompleted(user.userId, tour.id, tour.version);
          } else {
            markTourDismissed(user.userId, tour.id, tour.version, stepIndex);
          }
        }
        setAnnouncement(reason === 'completed' ? 'Tour completed.' : 'Tour closed.');
        previousFocusRef.current?.focus?.();
        previousFocusRef.current = null;
        return null;
      });
      setStepIndex(0);
      setTargetEl(null);
      setResolving(false);
    },
    [user, stepIndex],
  );

  const startTour = useCallback(
    (tourId: string) => {
      const tour = tours.find((t) => t.id === tourId);
      if (!tour || !tour.eligible(user)) return;
      if (tour.minViewportWidth && window.innerWidth < tour.minViewportWidth) return;
      previousFocusRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
      directionRef.current = 1;
      if (pathname !== tour.startRoute) {
        awaitingRouteRef.current = tour.startRoute;
        router.push(tour.startRoute);
      } else {
        awaitingRouteRef.current = null;
      }
      setStepIndex(0);
      setActiveTour(tour);
    },
    [tours, user, pathname, router],
  );

  const goTo = useCallback(
    (index: number, direction: 1 | -1) => {
      if (!activeTour) return;
      if (index >= activeTour.steps.length) {
        endTour('completed');
        return;
      }
      if (index < 0) {
        // Backward-skipped past the first step (only possible when step 0's
        // target vanished): nothing sensible to show, so close out.
        endTour('dismissed');
        return;
      }
      directionRef.current = direction;
      setStepIndex(index);
    },
    [activeTour, endTour],
  );

  const next = useCallback(() => goTo(stepIndex + 1, 1), [goTo, stepIndex]);
  const back = useCallback(() => goTo(stepIndex - 1, -1), [goTo, stepIndex]);

  // Resolve the current step's target element. Targets can mount a tick after
  // navigation, so poll briefly before declaring the step missing.
  useEffect(() => {
    if (!activeTour) return;
    const step = activeTour.steps[stepIndex];
    if (!step.target) {
      setTargetEl(null);
      setResolving(false);
      return;
    }
    let cancelled = false;
    let elapsed = 0;
    setResolving(true);
    setTargetEl(null);
    const tick = () => {
      if (cancelled) return;
      const el = document.querySelector<HTMLElement>(`[data-tour="${step.target}"]`);
      if (el) {
        el.scrollIntoView?.({ block: 'center', behavior: 'auto' });
        setTargetEl(el);
        setResolving(false);
        return;
      }
      elapsed += TARGET_POLL_INTERVAL_MS;
      if (elapsed >= targetTimeoutMs) {
        setResolving(false);
        if (step.skipIfMissing !== false) {
          goTo(stepIndex + directionRef.current, directionRef.current);
        } else {
          endTour('dismissed');
        }
        return;
      }
      window.setTimeout(tick, TARGET_POLL_INTERVAL_MS);
    };
    tick();
    return () => {
      cancelled = true;
    };
  }, [activeTour, stepIndex, targetTimeoutMs, goTo, endTour]);

  // Announce each step for screen readers.
  useEffect(() => {
    if (!activeTour) return;
    const step = activeTour.steps[stepIndex];
    setAnnouncement(`Step ${stepIndex + 1} of ${activeTour.steps.length}: ${step.title}`);
  }, [activeTour, stepIndex]);

  // Esc exits from anywhere.
  useEffect(() => {
    if (!activeTour) return;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') endTour('dismissed');
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [activeTour, endTour]);

  // A route change the tour didn't initiate ends it. On the final step the
  // finish-screen CTAs are links, so leaving then counts as completion.
  useEffect(() => {
    if (!activeTour) return;
    if (awaitingRouteRef.current) {
      if (pathname === awaitingRouteRef.current) awaitingRouteRef.current = null;
      return;
    }
    if (pathname !== activeTour.startRoute) {
      endTour(stepIndex === activeTour.steps.length - 1 ? 'completed' : 'dismissed');
    }
  }, [pathname, activeTour, stepIndex, endTour]);

  // Track resize/scroll so the spotlight and popover follow the target.
  useEffect(() => {
    if (!activeTour || !targetEl) return;
    const bump = () => setRepaintTick((t) => t + 1);
    window.addEventListener('resize', bump);
    window.addEventListener('scroll', bump, true);
    return () => {
      window.removeEventListener('resize', bump);
      window.removeEventListener('scroll', bump, true);
    };
  }, [activeTour, targetEl]);

  const step = activeTour ? activeTour.steps[stepIndex] : null;
  const targetRect = step?.target && targetEl ? targetEl.getBoundingClientRect() : null;

  const overlay =
    activeTour && step && !resolving
      ? createPortal(
          <div className="fixed inset-0 z-[100]" role="presentation">
            <TourSpotlight targetRect={targetRect} />
            <TourPopover
              step={step}
              stepIndex={stepIndex}
              totalSteps={activeTour.steps.length}
              targetRect={targetRect}
              onNext={next}
              onBack={back}
              onSkip={() => endTour('dismissed')}
            />
          </div>,
          document.body,
        )
      : null;

  return (
    <TourContext.Provider value={{ activeTour, stepIndex, startTour, endTour, next, back }}>
      {children}
      <div aria-live="polite" className="sr-only">
        {announcement}
      </div>
      {overlay}
    </TourContext.Provider>
  );
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `npx vitest run src/components/tour/__tests__/TourProvider.test.tsx`
Expected: PASS (7 tests). If the missing-target test is flaky on timing, raise its `waitFor` timeout — the poll timeout in the harness is 300 ms.

- [ ] **Step 7: Run the full unit suite to catch regressions**

Run: `npx vitest run`
Expected: PASS (all pre-existing tests still green).

- [ ] **Step 8: Commit**

```bash
git add front-end/src/components/tour
git commit -m "feat(tours): add tour engine - provider, spotlight overlay, step dialog"
```

---

### Task 4: Get Started tour definition + data-tour anchors

**Files:**
- Create: `front-end/src/lib/tours/get-started.tsx`
- Modify: `front-end/src/lib/tours/registry.ts` (register the tour)
- Modify: `front-end/src/components/Navigation.tsx` (anchors on Browse/Docs/Actions/org-switcher/avatar)
- Modify: `front-end/src/app/page.tsx:305` (anchor on the tiles grid)
- Test: `front-end/src/lib/tours/__tests__/get-started.test.ts`

**Interfaces:**
- Consumes: `TourDefinition` from `./types`.
- Produces: `getStartedTour: TourDefinition` (id `'get-started'`, version 1); registry `TOURS` now contains it. DOM anchors: `nav-browse`, `nav-docs`, `nav-actions`, `nav-org-switcher`, `nav-avatar`, `dashboard-tiles`.

- [ ] **Step 1: Write the failing test**

Create `front-end/src/lib/tours/__tests__/get-started.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { getStartedTour } from '@/lib/tours/get-started';
import { TOURS, getTour, GET_STARTED_TOUR_ID } from '@/lib/tours/registry';
import type { User } from '@/types/auth';

const regularUser: User = {
  userId: 1,
  username: 'u',
  email: 'u@example.com',
  organizationId: 1,
};

describe('get-started tour', () => {
  it('is registered under the expected id', () => {
    expect(getStartedTour.id).toBe(GET_STARTED_TOUR_ID);
    expect(TOURS).toContain(getStartedTour);
    expect(getTour(GET_STARTED_TOUR_ID)).toBe(getStartedTour);
  });

  it('has unique step ids and starts/ends with modal steps', () => {
    const ids = getStartedTour.steps.map((s) => s.id);
    expect(new Set(ids).size).toBe(ids.length);
    expect(getStartedTour.steps[0].target).toBeUndefined();
    expect(getStartedTour.steps[getStartedTour.steps.length - 1].target).toBeUndefined();
  });

  it('is eligible only for regular users with an organization', () => {
    expect(getStartedTour.eligible(regularUser)).toBe(true);
    expect(getStartedTour.eligible(null)).toBe(false);
    expect(getStartedTour.eligible({ ...regularUser, globalRole: 'SUPER_ADMIN' })).toBe(false);
    expect(getStartedTour.eligible({ ...regularUser, organizationId: undefined })).toBe(false);
  });

  it('requires the sm viewport because it anchors to sm:-hidden nav links', () => {
    expect(getStartedTour.minViewportWidth).toBe(640);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run src/lib/tours/__tests__/get-started.test.ts`
Expected: FAIL — cannot resolve `@/lib/tours/get-started`.

- [ ] **Step 3: Write the tour definition**

Create `front-end/src/lib/tours/get-started.tsx`:

```tsx
import type { TourDefinition } from './types';

/**
 * The basic orientation tour. Runs on the dashboard; anchors to nav elements
 * that exist on every page. Eligibility mirrors who actually sees the
 * dashboard: authenticated, not a super admin (they're redirected to /admin),
 * and org membership (no-org users get the org-onboarding empty state).
 */
export const getStartedTour: TourDefinition = {
  id: 'get-started',
  version: 1,
  title: 'Get Started with OSCAL Hub',
  description: 'A two-minute orientation: where the tools live and how to find help.',
  startRoute: '/',
  minViewportWidth: 640,
  eligible: (user) => user != null && user.globalRole !== 'SUPER_ADMIN' && user.organizationId != null,
  steps: [
    {
      id: 'welcome',
      title: 'Welcome to OSCAL Hub',
      body: (
        <>
          <p>
            OSCAL Hub helps you create, validate, and share security compliance documents in{' '}
            <strong>OSCAL</strong> — the Open Security Controls Assessment Language.
          </p>
          <p className="mt-2">
            This tour takes about two minutes. Press <kbd>Esc</kbd> anytime to exit — you can replay it later
            from your avatar menu.
          </p>
        </>
      ),
    },
    {
      id: 'tiles',
      target: 'dashboard-tiles',
      placement: 'top',
      title: 'Your toolbox',
      body: (
        <p>
          Each tile is a tool: validate a document, convert between XML/JSON/YAML, build documents visually,
          resolve profiles, and more. Click any tile to jump in.
        </p>
      ),
    },
    {
      id: 'actions',
      target: 'nav-actions',
      placement: 'bottom',
      title: 'Actions menu',
      body: (
        <p>
          The same tools are one click away from anywhere in the app — open the <strong>Actions</strong> menu
          to jump between them without returning to the dashboard.
        </p>
      ),
    },
    {
      id: 'browse',
      target: 'nav-browse',
      placement: 'bottom',
      title: 'Browse the public catalog',
      helpSlug: 'public-catalog',
      body: (
        <p>
          <strong>Browse</strong> opens the public catalog of OSCAL documents shared by the community — a
          great place to grab an example file to try the tools with.
        </p>
      ),
    },
    {
      id: 'docs',
      target: 'nav-docs',
      placement: 'bottom',
      title: 'Documentation',
      body: (
        <p>
          The full user guide lives under <strong>Documentation</strong> — every feature page also has a{' '}
          <strong>?</strong> button linking to its guide page.
        </p>
      ),
    },
    {
      id: 'org',
      target: 'nav-org-switcher',
      placement: 'bottom',
      title: 'Your organization',
      body: (
        <p>
          You work inside an organization — documents and settings are scoped to it. If you belong to more
          than one, switch here.
        </p>
      ),
    },
    {
      id: 'account',
      target: 'nav-avatar',
      placement: 'bottom',
      title: 'Your account',
      body: (
        <p>
          Manage your profile, open a support ticket, or replay guided tours from the avatar menu — it's
          always in the top-right corner.
        </p>
      ),
    },
    {
      id: 'finish',
      title: "You're all set",
      body: (
        <>
          <p>That's the lay of the land. Good next steps:</p>
          <ul className="mt-2 list-disc space-y-1 pl-5">
            <li>
              <a href="/catalog" className="text-primary underline">
                Browse the public catalog
              </a>{' '}
              for an example document.
            </li>
            <li>
              <a href="/guide/getting-started/overview" className="text-primary underline">
                Read the getting-started guide
              </a>{' '}
              for a deeper walkthrough.
            </li>
          </ul>
        </>
      ),
    },
  ],
};
```

- [ ] **Step 4: Register the tour**

Modify `front-end/src/lib/tours/registry.ts` — replace the whole file:

```ts
import type { TourDefinition } from './types';
import { getStartedTour } from './get-started';

export const GET_STARTED_TOUR_ID = 'get-started';

/** All registered tours, in launcher display order. */
export const TOURS: TourDefinition[] = [getStartedTour];

export function getTour(id: string): TourDefinition | undefined {
  return TOURS.find((tour) => tour.id === id);
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npx vitest run src/lib/tours/__tests__/get-started.test.ts`
Expected: PASS (4 tests).

- [ ] **Step 6: Add the data-tour anchors**

In `front-end/src/components/Navigation.tsx`:

a) Browse link (the `<Link href="/catalog"` around line 94) — add `data-tour="nav-browse"`:

```tsx
              <Link
                href="/catalog"
                data-tour="nav-browse"
                className="hidden sm:inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
              >
```

b) Documentation link (the `<Link href="/guide"` around line 104) — add `data-tour="nav-docs"`:

```tsx
              <Link
                href="/guide"
                data-tour="nav-docs"
                className="hidden sm:inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
              >
```

c) Actions trigger button (inside `<PopoverTrigger asChild>` around line 129) — add `data-tour="nav-actions"`:

```tsx
                  <button
                    type="button"
                    data-tour="nav-actions"
                    className="hidden sm:inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors"
                  >
```

d) Wrap the org switcher and avatar menu (around line 163) so we don't touch those components' internals:

```tsx
              <>
                {!isSuperAdmin() && (
                  <span data-tour="nav-org-switcher">
                    <OrganizationSwitcher />
                  </span>
                )}
                <span data-tour="nav-avatar">
                  <UserAvatarMenu />
                </span>
              </>
```

In `front-end/src/app/page.tsx` (line 305), add the anchor to the tiles grid:

```tsx
          <div data-tour="dashboard-tiles" className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-16">
```

- [ ] **Step 7: Run the full unit suite**

Run: `npx vitest run`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add front-end/src/lib/tours front-end/src/components/Navigation.tsx front-end/src/app/page.tsx
git commit -m "feat(tours): add Get Started tour definition and data-tour anchors"
```

---

### Task 5: Mount the provider; TourMenu launcher in the avatar menu

**Files:**
- Create: `front-end/src/components/tour/TourMenu.tsx`
- Modify: `front-end/src/app/layout.tsx` (wrap app in `TourProvider`)
- Modify: `front-end/src/components/UserAvatarMenu.tsx` (add "Guided Tours" item)
- Test: `front-end/src/components/tour/__tests__/TourMenu.test.tsx`

**Interfaces:**
- Consumes: `useTour` (Task 3), `TOURS` (Task 4), `loadTourState` (Task 1), shadcn `Dialog`/`Button`.
- Produces: `TourMenu({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void })` — a dialog listing eligible tours with Start/Replay buttons and a completed badge.

- [ ] **Step 1: Write the failing test**

Create `front-end/src/components/tour/__tests__/TourMenu.test.tsx`:

```tsx
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TourMenu } from '@/components/tour/TourMenu';
import { markTourCompleted } from '@/lib/tours/storage';

const TEST_USER = {
  userId: 42,
  username: 'tester',
  email: 't@example.com',
  organizationId: 1,
};

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ user: TEST_USER }),
}));

const startTour = vi.fn();
vi.mock('@/components/tour/TourProvider', () => ({
  useTour: () => ({ startTour, activeTour: null, stepIndex: 0, endTour: vi.fn(), next: vi.fn(), back: vi.fn() }),
}));

describe('TourMenu', () => {
  beforeEach(() => {
    localStorage.clear();
    startTour.mockClear();
  });

  it('lists the Get Started tour with a Start button and launches it', async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    render(<TourMenu open onOpenChange={onOpenChange} />);
    expect(screen.getByText('Get Started with OSCAL Hub')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /start/i }));
    expect(onOpenChange).toHaveBeenCalledWith(false);
    expect(startTour).toHaveBeenCalledWith('get-started');
  });

  it('shows a completed badge and a Replay button for finished tours', () => {
    markTourCompleted(TEST_USER.userId, 'get-started', 1);
    render(<TourMenu open onOpenChange={() => {}} />);
    expect(screen.getByText(/completed/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /replay/i })).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run src/components/tour/__tests__/TourMenu.test.tsx`
Expected: FAIL — cannot resolve `@/components/tour/TourMenu`.

- [ ] **Step 3: Write TourMenu**

Create `front-end/src/components/tour/TourMenu.tsx`. Check the exact export names in `front-end/src/components/ui/dialog.tsx` first (standard shadcn: `Dialog`, `DialogContent`, `DialogHeader`, `DialogTitle`, `DialogDescription`) and adjust imports if they differ:

```tsx
'use client';

import { CheckCircle2 } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/contexts/AuthContext';
import { useTour } from '@/components/tour/TourProvider';
import { TOURS } from '@/lib/tours/registry';
import { loadTourState } from '@/lib/tours/storage';

interface TourMenuProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/** Dialog listing every tour the current user is eligible for. */
export function TourMenu({ open, onOpenChange }: TourMenuProps) {
  const { user } = useAuth();
  const { startTour } = useTour();

  if (!user) return null;

  const state = loadTourState(user.userId);
  const available = TOURS.filter((tour) => tour.eligible(user));

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Guided tours</DialogTitle>
          <DialogDescription>Short interactive walkthroughs of OSCAL Hub features.</DialogDescription>
        </DialogHeader>
        <ul className="space-y-2">
          {available.map((tour) => {
            const completed = state.tours[tour.id]?.completedVersion !== undefined;
            return (
              <li
                key={tour.id}
                className="flex items-start justify-between gap-3 rounded-md border border-border p-3"
              >
                <div>
                  <div className="flex items-center gap-2 text-sm font-medium">
                    {tour.title}
                    {completed && (
                      <span className="inline-flex items-center gap-1 text-xs text-green-500">
                        <CheckCircle2 className="h-3.5 w-3.5" aria-hidden="true" />
                        Completed
                      </span>
                    )}
                  </div>
                  <p className="mt-0.5 text-xs text-muted-foreground">{tour.description}</p>
                </div>
                <Button
                  size="sm"
                  variant={completed ? 'outline' : 'default'}
                  onClick={() => {
                    onOpenChange(false);
                    startTour(tour.id);
                  }}
                >
                  {completed ? 'Replay' : 'Start'}
                </Button>
              </li>
            );
          })}
        </ul>
      </DialogContent>
    </Dialog>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run src/components/tour/__tests__/TourMenu.test.tsx`
Expected: PASS (2 tests).

- [ ] **Step 5: Mount TourProvider in the root layout**

Modify `front-end/src/app/layout.tsx` — add the import and wrap everything inside `AuthProvider`:

```tsx
import { TourProvider } from "@/components/tour/TourProvider";
```

```tsx
        <QueryProvider>
          <AuthProvider>
            <TourProvider>
              <a
                href="#main-content"
                className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:px-4 focus:py-2 focus:bg-primary focus:text-primary-foreground focus:rounded-md focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
              >
                Skip to main content
              </a>
              <Navigation />
              <main id="main-content">
                {children}
              </main>
              <Footer />
              <Toaster />
            </TourProvider>
          </AuthProvider>
        </QueryProvider>
```

- [ ] **Step 6: Add the "Guided Tours" item to UserAvatarMenu**

Modify `front-end/src/components/UserAvatarMenu.tsx`:

a) Extend the imports:

```tsx
import { Bug, Cog, Compass, Inbox, LogOut, Settings, UserCog } from 'lucide-react';
import { TourMenu } from '@/components/tour/TourMenu';
```

b) Add state next to the existing `open` state:

```tsx
  const [toursOpen, setToursOpen] = useState(false);
```

c) Insert the menu item between the "My Tickets" link and the org-admin block (super admins have no eligible tours, so hide it for them):

```tsx
          {!isSuperAdmin && (
            <button
              type="button"
              onClick={() => {
                setOpen(false);
                setToursOpen(true);
              }}
              className="flex w-full items-center gap-2 rounded-md px-3 py-2 text-left text-sm transition-colors hover:bg-accent hover:text-accent-foreground"
            >
              <Compass className="h-4 w-4" />
              Guided Tours
            </button>
          )}
```

d) Render the menu dialog after the closing `</Popover>` — wrap the return in a fragment:

```tsx
  return (
    <>
      <Popover open={open} onOpenChange={setOpen}>
        {/* ...existing content unchanged... */}
      </Popover>
      <TourMenu open={toursOpen} onOpenChange={setToursOpen} />
    </>
  );
```

- [ ] **Step 7: Run the full unit suite**

Run: `npx vitest run`
Expected: PASS (existing `HelpButton`/`user-picker` tests unaffected).

- [ ] **Step 8: Commit**

```bash
git add front-end/src/components/tour front-end/src/app/layout.tsx front-end/src/components/UserAvatarMenu.tsx
git commit -m "feat(tours): mount TourProvider and add Guided Tours launcher to avatar menu"
```

---

### Task 6: First-run welcome prompt + dashboard wiring

**Files:**
- Create: `front-end/src/components/tour/TourWelcomeDialog.tsx`
- Modify: `front-end/src/app/page.tsx` (render the dialog; add "Take the interactive tour" link to the Getting Started card)
- Test: `front-end/src/components/tour/__tests__/TourWelcomeDialog.test.tsx`

**Interfaces:**
- Consumes: Task 1 storage helpers, Task 3 `useTour`, Task 4 registry, shadcn `Dialog`/`Button`.
- Produces: `TourWelcomeDialog()` — self-gating component; renders nothing unless the current user should be prompted.

- [ ] **Step 1: Write the failing test**

Create `front-end/src/components/tour/__tests__/TourWelcomeDialog.test.tsx`:

```tsx
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TourWelcomeDialog } from '@/components/tour/TourWelcomeDialog';
import { dismissWelcomePrompt, loadTourState } from '@/lib/tours/storage';

const TEST_USER = {
  userId: 42,
  username: 'tester',
  email: 't@example.com',
  organizationId: 1,
};

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ user: TEST_USER }),
}));

const startTour = vi.fn();
vi.mock('@/components/tour/TourProvider', () => ({
  useTour: () => ({ startTour, activeTour: null, stepIndex: 0, endTour: vi.fn(), next: vi.fn(), back: vi.fn() }),
}));

describe('TourWelcomeDialog', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    startTour.mockClear();
  });

  it('prompts a brand-new user and starts the tour on accept', async () => {
    const user = userEvent.setup();
    render(<TourWelcomeDialog />);
    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /start tour/i }));
    expect(startTour).toHaveBeenCalledWith('get-started');
    // Accepting marks the prompt as seen so it never re-fires.
    expect(loadTourState(TEST_USER.userId).welcomePrompt.seen).toBe(true);
  });

  it('records a deferral on "Maybe later"', async () => {
    const user = userEvent.setup();
    render(<TourWelcomeDialog />);
    await screen.findByRole('dialog');
    await user.click(screen.getByRole('button', { name: /maybe later/i }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(loadTourState(TEST_USER.userId).welcomePrompt.deferrals).toBe(1);
    expect(startTour).not.toHaveBeenCalled();
  });

  it('marks the prompt seen on "Don\'t ask again"', async () => {
    const user = userEvent.setup();
    render(<TourWelcomeDialog />);
    await screen.findByRole('dialog');
    await user.click(screen.getByRole('button', { name: /don't ask again/i }));
    expect(loadTourState(TEST_USER.userId).welcomePrompt.seen).toBe(true);
  });

  it('does not prompt when already seen', () => {
    dismissWelcomePrompt(TEST_USER.userId);
    render(<TourWelcomeDialog />);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run src/components/tour/__tests__/TourWelcomeDialog.test.tsx`
Expected: FAIL — cannot resolve `@/components/tour/TourWelcomeDialog`.

- [ ] **Step 3: Write TourWelcomeDialog**

Create `front-end/src/components/tour/TourWelcomeDialog.tsx` (adjust `DialogFooter` import if the shadcn dialog exports differ):

```tsx
'use client';

import { useEffect, useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/contexts/AuthContext';
import { useTour } from '@/components/tour/TourProvider';
import { GET_STARTED_TOUR_ID, getTour } from '@/lib/tours/registry';
import {
  dismissWelcomePrompt,
  loadTourState,
  recordWelcomeDeferral,
  shouldShowWelcomePrompt,
  wasDeferredThisSession,
} from '@/lib/tours/storage';

/**
 * One-time "Take a 2-minute tour?" offer, rendered on the dashboard.
 * Self-gating: shows only for eligible users who haven't completed, dismissed,
 * or opted out of the Get Started tour. "Maybe later" defers to the next
 * session (max 3 times, then treated as "don't ask again").
 */
export function TourWelcomeDialog() {
  const { user } = useAuth();
  const { startTour, activeTour } = useTour();
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!user || activeTour) return;
    const tour = getTour(GET_STARTED_TOUR_ID);
    if (!tour || !tour.eligible(user)) return;
    if (tour.minViewportWidth && window.innerWidth < tour.minViewportWidth) return;
    if (wasDeferredThisSession()) return;
    if (shouldShowWelcomePrompt(loadTourState(user.userId), GET_STARTED_TOUR_ID)) {
      setOpen(true);
    }
  }, [user, activeTour]);

  if (!user) return null;

  const handleStart = () => {
    dismissWelcomePrompt(user.userId);
    setOpen(false);
    startTour(GET_STARTED_TOUR_ID);
  };

  const handleLater = () => {
    recordWelcomeDeferral(user.userId);
    setOpen(false);
  };

  const handleNever = () => {
    dismissWelcomePrompt(user.userId);
    setOpen(false);
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        // Closing via overlay click / Esc counts as "maybe later".
        if (!next) handleLater();
      }}
    >
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>New here? Take a two-minute tour</DialogTitle>
          <DialogDescription>
            A quick walkthrough of where the tools live and how to find help. You can replay it anytime from
            your avatar menu.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter className="gap-2 sm:justify-between">
          <Button variant="ghost" onClick={handleNever}>
            Don&apos;t ask again
          </Button>
          <div className="flex gap-2">
            <Button variant="outline" onClick={handleLater}>
              Maybe later
            </Button>
            <Button onClick={handleStart}>Start tour</Button>
          </div>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run src/components/tour/__tests__/TourWelcomeDialog.test.tsx`
Expected: PASS (4 tests).

- [ ] **Step 5: Wire the dashboard**

Modify `front-end/src/app/page.tsx`:

a) Add imports (top of file, with the other component imports):

```tsx
import { Compass } from 'lucide-react'; // merge into the existing lucide-react import list
import { TourWelcomeDialog } from '@/components/tour/TourWelcomeDialog';
import { useTour } from '@/components/tour/TourProvider';
import { GET_STARTED_TOUR_ID } from '@/lib/tours/registry';
```

b) Inside the `Dashboard` component, next to the other hooks:

```tsx
  const { startTour } = useTour();
```

c) In the authenticated dashboard return (the block starting `// Show dashboard for authenticated users`, line ~294), render the welcome dialog just inside the outer div:

```tsx
    <div className="min-h-screen bg-background">
      <TourWelcomeDialog />
      <div className="container mx-auto py-12 px-4">
```

d) In the Getting Started card's link list (after the "View User Guide" div, line ~551), add a tour launcher. Hidden below `sm` to match the tour's viewport requirement:

```tsx
                <div>
                  <button
                    type="button"
                    onClick={() => startTour(GET_STARTED_TOUR_ID)}
                    className="hidden sm:inline-flex text-primary hover:underline font-medium items-center"
                  >
                    Take the interactive tour
                    <Compass className="h-4 w-4 ml-2" aria-hidden="true" />
                  </button>
                </div>
```

- [ ] **Step 6: Run the full unit suite**

Run: `npx vitest run`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add front-end/src/components/tour front-end/src/app/page.tsx
git commit -m "feat(tours): first-run welcome prompt and dashboard tour launcher"
```

---

### Task 7: E2E tests — tour flows and mid-tour axe scan

**Files:**
- Modify: `front-end/e2e/auth.setup.ts` (seed tour state so existing specs are undisturbed)
- Create: `front-end/e2e/tours.spec.ts`

**Interfaces:**
- Consumes: everything shipped in Tasks 1–6; `@axe-core/playwright`; the seeded mock user (`userId: 1`).
- Produces: e2e coverage; the seeded key `oscal-hub.tours.v1.1` in the shared storage state.

- [ ] **Step 1: Seed tour state in auth setup**

Modify `front-end/e2e/auth.setup.ts` — inside the existing `page.evaluate` that sets `token`/`user`, add one line so the welcome prompt never fires in unrelated specs (tour specs explicitly clear this key):

```ts
  await page.evaluate((user) => {
    // Set a mock JWT token (doesn't need to be valid for frontend-only tests)
    localStorage.setItem('token', 'mock-e2e-test-token-12345');
    localStorage.setItem('user', JSON.stringify(user));
    // Mark the tour welcome prompt as seen so it doesn't overlay unrelated
    // specs; tours.spec.ts removes this key to test the prompt itself.
    localStorage.setItem(
      `oscal-hub.tours.v1.${user.userId}`,
      JSON.stringify({ welcomePrompt: { seen: true, deferrals: 0 }, tours: {} }),
    );
  }, mockUser);
```

- [ ] **Step 2: Write the tour e2e spec**

Create `front-end/e2e/tours.spec.ts`:

```ts
import { test, expect, type Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

const TOUR_STORAGE_KEY = 'oscal-hub.tours.v1.1';

/** Remove the seeded "prompt seen" state so the welcome dialog can fire. */
async function resetTourState(page: Page) {
  await page.goto('/');
  await page.evaluate((key) => {
    localStorage.removeItem(key);
    sessionStorage.clear();
  }, TOUR_STORAGE_KEY);
  await page.reload();
  await page.waitForLoadState('networkidle');
}

async function readTourState(page: Page) {
  return page.evaluate((key) => {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : null;
  }, TOUR_STORAGE_KEY);
}

test.describe('Guided tours', () => {
  test('welcome prompt offers the tour; completing it persists and never re-prompts', async ({ page }) => {
    await resetTourState(page);

    const welcome = page.getByRole('dialog', { name: /take a two-minute tour/i });
    await expect(welcome).toBeVisible();
    await welcome.getByRole('button', { name: 'Start tour' }).click();

    // Step 1 is the welcome modal step.
    const tourDialog = page.getByRole('dialog', { name: 'Welcome to OSCAL Hub' });
    await expect(tourDialog).toBeVisible();
    await expect(tourDialog).toContainText('Step 1 of 8');

    // Walk every step; the last button is Finish.
    for (let i = 0; i < 7; i++) {
      await page.getByRole('dialog').getByRole('button', { name: /next|finish/i }).click();
    }
    await expect(page.getByRole('dialog')).toHaveCount(0);

    const state = await readTourState(page);
    expect(state.tours['get-started'].completedVersion).toBe(1);

    // Reload: no welcome prompt again.
    await page.reload();
    await page.waitForLoadState('networkidle');
    await expect(page.getByRole('dialog', { name: /take a two-minute tour/i })).toHaveCount(0);
  });

  test('Escape dismisses the tour and records the step', async ({ page }) => {
    await resetTourState(page);
    await page.getByRole('button', { name: 'Start tour' }).click();
    await expect(page.getByRole('dialog', { name: 'Welcome to OSCAL Hub' })).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog')).toHaveCount(0);

    const state = await readTourState(page);
    expect(state.tours['get-started'].dismissedAtStep).toBe(0);

    await page.reload();
    await page.waitForLoadState('networkidle');
    await expect(page.getByRole('dialog', { name: /take a two-minute tour/i })).toHaveCount(0);
  });

  test('"Maybe later" defers the prompt and increments the deferral count', async ({ page }) => {
    await resetTourState(page);
    await page.getByRole('button', { name: 'Maybe later' }).click();
    await expect(page.getByRole('dialog')).toHaveCount(0);
    const state = await readTourState(page);
    expect(state.welcomePrompt.deferrals).toBe(1);
  });

  test('tour can be replayed from the avatar menu', async ({ page }) => {
    // Seeded state: prompt seen, tour never run — the launcher must still work.
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.getByRole('button', { name: 'User menu' }).click();
    await page.getByRole('button', { name: 'Guided Tours' }).click();
    const menu = page.getByRole('dialog', { name: 'Guided tours' });
    await expect(menu).toBeVisible();
    await menu.getByRole('button', { name: /start|replay/i }).click();
    await expect(page.getByRole('dialog', { name: 'Welcome to OSCAL Hub' })).toBeVisible();
    await page.keyboard.press('Escape');
  });

  test('tour is fully keyboard operable', async ({ page }) => {
    await resetTourState(page);
    // Reach "Start tour" via keyboard: the dialog traps focus, Tab to the button.
    const welcome = page.getByRole('dialog', { name: /take a two-minute tour/i });
    await expect(welcome).toBeVisible();
    await welcome.getByRole('button', { name: 'Start tour' }).focus();
    await page.keyboard.press('Enter');
    await expect(page.getByRole('dialog', { name: 'Welcome to OSCAL Hub' })).toBeVisible();
    // ArrowRight advances (dialog container has focus after each step change).
    await page.keyboard.press('ArrowRight');
    await expect(page.getByRole('dialog')).toContainText('Step 2 of 8');
    await page.keyboard.press('ArrowLeft');
    await expect(page.getByRole('dialog')).toContainText('Step 1 of 8');
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog')).toHaveCount(0);
  });

  test('open tour has no automatically detectable accessibility issues', async ({ page }) => {
    // Same zero-exclusion policy as e2e/accessibility.spec.ts.
    await resetTourState(page);
    await page.getByRole('button', { name: 'Start tour' }).click();
    await expect(page.getByRole('dialog', { name: 'Welcome to OSCAL Hub' })).toBeVisible();
    const modalStepScan = await new AxeBuilder({ page }).analyze();
    expect(modalStepScan.violations).toEqual([]);

    // Also scan an anchored (spotlight) step.
    await page.getByRole('dialog').getByRole('button', { name: 'Next' }).click();
    await expect(page.getByRole('dialog')).toContainText('Step 2 of 8');
    const anchoredStepScan = await new AxeBuilder({ page }).analyze();
    expect(anchoredStepScan.violations).toEqual([]);
  });
});
```

- [ ] **Step 3: Run the tour e2e spec**

Run (from `front-end/`): `npm run test:e2e -- tours.spec.ts`
Expected: PASS (6 tests). Notes for debugging:
- Step counts assume the 8-step Get Started tour from Task 4; update `Step 1 of 8` assertions if the step list changed.
- If the welcome-prompt selector fails, check the `DialogTitle` text matches `New here? Take a two-minute tour`.
- If axe reports contrast violations on the overlay, raise `fillOpacity` in `TourSpotlight.tsx` — the popover itself uses standard tokens and should be clean.

- [ ] **Step 4: Run the existing e2e suites to confirm no regressions**

Run: `npm run test:e2e -- accessibility.spec.ts onboarding.spec.ts`
Expected: PASS — the seeded tour state keeps the welcome prompt out of these specs (onboarding.spec.ts users have no org, so they're ineligible anyway).

- [ ] **Step 5: Commit**

```bash
git add front-end/e2e/auth.setup.ts front-end/e2e/tours.spec.ts
git commit -m "test(tours): e2e coverage for tour flows, keyboard operation, and axe scans"
```

---

### Task 8: Documentation and final verification

**Files:**
- Create: `front-end/src/components/tour/README.md`
- Modify: `docs/ONBOARDING-TOURS-PLAN.md` (status line)

- [ ] **Step 1: Write the tour system README**

Create `front-end/src/components/tour/README.md`:

```markdown
# Guided Tour System

Opt-in product tours. Design doc: `docs/ONBOARDING-TOURS-PLAN.md`.

## Adding a tour

1. Create `src/lib/tours/<tour-id>.tsx` exporting a `TourDefinition`
   (see `get-started.tsx`).
2. Register it in `src/lib/tours/registry.ts` (`TOURS` array, display order).
3. Add `data-tour="<area>-<element>"` attributes (kebab-case) to the target
   elements. Targets missing at runtime are skipped, not fatal — but add them
   before shipping the tour.
4. Add e2e coverage to `e2e/tours.spec.ts`, including an axe scan on at least
   one anchored step (zero-exclusion policy).

## Rules

- Steps with no `target` render as centered modal steps (first/last step).
- `helpSlug` renders a "Learn more" link into `/guide` via `HELP_TARGETS`.
- Bump the tour's `version` after major UI changes; completion is recorded
  per version.
- Persistence: localStorage `oscal-hub.tours.v1.<userId>` (see
  `src/lib/tours/storage.ts`). No backend involvement.
- Eligibility (`eligible(user)`) must mirror who can actually see the target
  UI (role and org gating).
- Set `minViewportWidth: 640` when anchoring to `sm:`-hidden nav elements.
```

- [ ] **Step 2: Update the design doc status**

In `docs/ONBOARDING-TOURS-PLAN.md`, update the status line:

```markdown
**Status:** Phase 1 implemented (engine + Get Started tour). Phases 2–4 pending.
```

- [ ] **Step 3: Full verification**

Run from `front-end/`:

```bash
npx vitest run
npm run lint
npm run test:e2e -- tours.spec.ts accessibility.spec.ts
```

Expected: all PASS. Fix anything that fails before committing.

- [ ] **Step 4: Manual smoke test in the browser**

Start the app (`./dev.sh` from repo root if not already running), log in as a regular org user at `http://localhost:3010`, and verify: welcome prompt appears → Start tour → all 8 steps render with the spotlight tracking each anchor → Finish → avatar menu → Guided Tours shows "Completed" → Replay works.

- [ ] **Step 5: Commit**

```bash
git add front-end/src/components/tour/README.md docs/ONBOARDING-TOURS-PLAN.md
git commit -m "docs(tours): tour-system README and design doc status update"
```
