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
- Radix dialogs that can be open when a tour starts must unmount when closed
  (`if (!open) return null`) so no stale `role="dialog"` lingers under the
  tour overlay.
