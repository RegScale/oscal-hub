# Leaderboard Feature — Design

**Date:** 2026-07-31
**Status:** Approved

## Problem

OSCAL Hub has no gamification. Users who do lots of work in the platform, or
who share content into the library, get no visibility or recognition. We want
a leaderboard that rewards real activity and encourages library contributions.

## Decisions (confirmed with user)

- **Scope:** Global — all users across all organizations appear on one board.
- **Activity definition:** Meaningful operations only (no logins/page views).
- **Time windows:** Two tabs — last 30 days and all-time.
- **Navigation:** Top-level "Leaderboard" nav link (next to Browse/Docs/API),
  trophy icon.

## Approach

**Aggregate-on-read.** No new tables, no Flyway migration, no event ledger.
The data already exists in `operation_history`, `library_items`, `artifacts`,
`oscal_documents`, and `authorizations`. A new `LeaderboardService` runs
per-source GROUP-BY aggregation queries and merges counts per user.

Rejected alternatives:
- Dedicated activity-events table + rollups — needs migration, backfill, and
  touching every write path; unjustified at current scale.
- Full gamification engine (points config, badges, streaks) — out of scope.

## Backend

### Endpoint

`GET /api/leaderboard?window=30d|all` (default `all`) — any authenticated
user. Invalid `window` → 400. Response:

```json
{
  "window": "30d",
  "generatedAt": "2026-07-31T12:00:00Z",
  "mostActive": [
    {
      "rank": 1,
      "username": "thowerton",
      "displayName": "Travis Howerton",
      "score": 42,
      "breakdown": {
        "operations": 30,
        "libraryPublishes": 4,
        "artifacts": 3,
        "documents": 3,
        "authorizations": 2
      }
    }
  ],
  "topContributors": [
    { "rank": 1, "username": "...", "displayName": "...", "score": 7 }
  ]
}
```

### Scoring

**Most Active** — per-user sum of:
- operations run (`operation_history` rows, any type, by `timestamp`)
- library items published (`library_items` where visibility != PRIVATE,
  windowed by `published_at`, falling back to `created_at` when null)
- artifacts created (`artifacts` by `created_at`)
- documents built (`oscal_documents` by `created_at`)
- authorizations created (`authorizations` by `created_at`)

**Top Contributors** — count of `library_items` with visibility
`ORGANIZATION` or `PUBLIC` per `created_by`, same window logic on
`published_at`/`created_at`.

Rules:
- Top 20 per board.
- Ties broken by username ascending (deterministic ranking). Tied scores get
  distinct sequential ranks (standard ordinal ranking, not dense).
- Disabled users (`users.enabled = false`) excluded.
- 30d window = `now minus 30 days`, computed server-side.

### Components

- `controller/LeaderboardController.java` — endpoint, window param
  validation, `@Tag` for Swagger.
- `service/LeaderboardService.java` — runs the per-source queries, merges
  per user, ranks, builds DTOs.
- `model/LeaderboardResponse.java`, `model/LeaderboardEntry.java` — DTOs.
- Aggregation queries added to existing repositories
  (`HistoryRepository`, `LibraryItemRepository`, `ArtifactRepository`,
  `OscalDocumentRepository`, `AuthorizationRepository`) as JPQL
  `GROUP BY user` count methods returning `(userId, count)` projections,
  each with an optional cutoff parameter (`null` = all-time).
- Security: endpoint added to authenticated group (default — no config
  change needed since anything not whitelisted requires auth).

No database schema changes.

## Frontend

- `app/leaderboard/page.tsx` — login-protected page (same pattern as other
  feature pages):
  - Tab switcher: "Last 30 days" / "All time".
  - Two boards side by side (stacked on mobile): Most Active Users, Top
    Contributors.
  - Gold/silver/bronze medal treatment for ranks 1–3.
  - Signed-in user's row highlighted if present.
  - Most Active rows show a breakdown tooltip (what made up the score).
  - Loading skeletons, empty state ("No activity yet — go validate
    something!"), error state with retry.
- `components/Navigation.tsx` — top-level `Leaderboard` link with Trophy
  icon (lucide) next to Browse/Docs, visible when logged in.
- API client: `getLeaderboard(window)` added to the existing api lib.

## Testing

**Backend**
- `@DataJpaTest`-style repository tests: seeded users + rows in each source
  table; verify counts, window cutoffs, visibility filtering, disabled-user
  exclusion.
- Service unit tests (Mockito): merge across sources, ranking, tie-break by
  username, top-20 truncation, empty data.
- Controller tests (MockMvc + security): 401/403 unauthenticated, 200
  authenticated, 400 on bad window, response shape.

**Frontend**
- Component tests: renders both boards, tab switching refetches, medals for
  top 3, self-highlight, empty/error/loading states, breakdown tooltip.
- Playwright e2e: nav link present and navigates, page renders boards, tabs
  work via keyboard, axe accessibility scan (same pattern as tours e2e).

## Non-goals

- Points weighting/config, badges, streaks, notifications.
- Per-organization boards (may come later).
- Caching (queries are cheap at current scale; add later if needed).
