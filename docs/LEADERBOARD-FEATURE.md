# Leaderboard Feature

**Date:** 2026-07-31
**Status:** Implemented

## Overview

OSCAL Hub has a global leaderboard at `/leaderboard`, linked from the top
navigation bar (trophy icon, visible to authenticated non-admin users). It
shows two boards, each with a "Last 30 days" / "All time" toggle:

- **Most Active Users** — total meaningful activity across the platform:
  operations run (validate/convert/resolve/batch), items published to the
  library, artifacts created, documents built, and authorizations created.
  Logins and page views deliberately do not count.
- **Top Contributors** — items shared into the library (visibility
  `ORGANIZATION` or `PUBLIC`), i.e. content made available for others.

Design decisions (confirmed with the product owner):
- Boards are **global** — all users across organizations rank together.
- Ranks are ordinal; ties break alphabetically by username.
- Boards show the top 20; disabled accounts are excluded.
- Ranks 1–3 get gold/silver/bronze medal icons; the signed-in user's row is
  highlighted with a "You" badge wherever they appear.

## Architecture

**Aggregate-on-read** — there is no activity-event table, no migration, and
no write-path changes. `LeaderboardService` runs one `GROUP BY user` count
query per source table and merges the results in memory:

| Source | Table | Repository method |
|---|---|---|
| Operations | `operation_history` | `HistoryRepository.countOperationsPerUserSince` |
| Library publishes | `library_items` (non-PRIVATE, `COALESCE(published_at, created_at)`) | `LibraryItemRepository.countSharedItemsPerUserSince` |
| Artifacts | `artifacts` | `ArtifactRepository.countCreatedPerUserSince` |
| Documents | `oscal_documents` | `OscalDocumentRepository.countCreatedPerUserSince` |
| Authorizations | `authorizations` | `AuthorizationRepository.countCreatedPerUserSince` |

All five methods take a `LocalDateTime cutoff`; all-time is expressed as
epoch (1970-01-01) rather than a nullable parameter, which keeps the JPQL
portable.

### API

`GET /api/leaderboard?window=30d|all` (default `all`). Any authenticated
user. Invalid window → 400. Response includes `window`, `generatedAt`,
`mostActive[]`, and `topContributors[]`; most-active entries carry a
`breakdown` map (zero-count sources omitted) that the UI renders as
"40 operations · 2 library publishes".

### Key files

Backend:
- `back-end/src/main/java/gov/nist/oscal/tools/api/controller/LeaderboardController.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/service/LeaderboardService.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/model/LeaderboardResponse.java`, `LeaderboardEntry.java`

Frontend:
- `front-end/src/app/leaderboard/page.tsx` (ProtectedRoute wrapper)
- `front-end/src/app/leaderboard/leaderboard-content.tsx` (boards, tabs, states)
- `front-end/src/components/Navigation.tsx` (nav link, `data-tour="nav-leaderboard"`)
- `front-end/src/lib/api-client.ts` → `getLeaderboard(window)`
- `front-end/src/types/oscal.ts` → `LeaderboardResponse`, `LeaderboardEntry`, `LeaderboardWindow`

## Testing

- `LeaderboardQueriesTest` — seeded-database checks of every aggregation
  query: window cutoffs, PRIVATE exclusion, `publishedAt` fallback.
- `LeaderboardServiceTest` — merge/scoring, ordinal ranks, tie-breaks,
  top-20 truncation, disabled-user exclusion, breakdown contents, window
  validation.
- `LeaderboardControllerTest` — 401 anonymous, 200 authenticated with JSON
  shape, default window, 400 on bad window.
- `leaderboard-client.test.ts` — URL/auth-header/error behavior of the API
  client method.
- `leaderboard-content.test.tsx` — boards render, tab refetch, medals,
  self-highlight, breakdown text, empty/error/retry states.
- `navigation-leaderboard.test.tsx` — link shown to authenticated users,
  hidden when logged out or super admin.
- `e2e/leaderboard.spec.ts` — Playwright (API mocked via `page.route`):
  nav flow, board rendering, tab switching, keyboard operation, axe
  accessibility scan. Runs on Chromium/Firefox/WebKit.

## Future ideas (explicitly out of scope)

Points weighting, badges/streaks, per-organization boards, and caching
(queries are cheap at current scale; revisit if the tables grow).
