# Leaderboard Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Global leaderboard (Most Active Users + Top Contributors) with 30-day/all-time tabs, served by `GET /api/leaderboard` and rendered at `/leaderboard` with a top-level nav link.

**Architecture:** Aggregate-on-read. New JPQL GROUP-BY count queries on five existing repositories feed a new `LeaderboardService` that merges per-user counts, ranks, and truncates to top 20. One authenticated REST endpoint. Next.js page with tabs, two board cards, medals, and self-highlight. No schema changes.

**Tech Stack:** Spring Boot 3.5 / Spring Data JPA / MockMvc + Mockito tests; Next.js App Router / vitest + Testing Library / Playwright + axe.

**Spec:** `docs/superpowers/specs/2026-07-31-leaderboard-design.md`

## Global Constraints

- No database schema changes; no Flyway migration.
- Endpoint requires authentication (any role); anonymous → 403 (matches existing filter behavior).
- Windows: `30d` (now − 30 days) and `all` (cutoff = epoch). Invalid → 400.
- Top 20 per board; ties broken by username ascending; ordinal ranks (1,2,3…).
- Disabled users (`enabled=false`) excluded from both boards.
- Backend tests run from `back-end/`: `mvn test -Dtest=<Class>`. Frontend unit: `cd front-end && npx vitest run <file>`. E2E: `npx playwright test e2e/leaderboard.spec.ts`.
- Commit after each task.

---

### Task 1: Repository aggregation queries

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/HistoryRepository.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/LibraryItemRepository.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/ArtifactRepository.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/OscalDocumentRepository.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/AuthorizationRepository.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/repository/LeaderboardQueriesTest.java`

**Interfaces:**
- Produces (used by Task 2's service):
  - `HistoryRepository.countOperationsPerUserSince(LocalDateTime cutoff): List<Object[]>` — rows of `[Long userId, Long count]`
  - `LibraryItemRepository.countSharedItemsPerUserSince(LocalDateTime cutoff): List<Object[]>`
  - `ArtifactRepository.countCreatedPerUserSince(LocalDateTime cutoff): List<Object[]>`
  - `OscalDocumentRepository.countCreatedPerUserSince(LocalDateTime cutoff): List<Object[]>`
  - `AuthorizationRepository.countCreatedPerUserSince(LocalDateTime cutoff): List<Object[]>`
  - All-time is expressed by passing `LocalDateTime.of(1970, 1, 1, 0, 0)` — no null handling in queries.

- [ ] **Step 1: Write the failing test** — follow the style of `AuthorizationRepositoryOrgScopeTest` (`@SpringBootTest @ActiveProfiles("test") @Transactional`, seed with `EntityManager`). Seed two users (one later used as disabled — repo layer does NOT filter enabled; that's service concern, so here both count), rows in each source table, some older than the cutoff, one PRIVATE library item, one library item with null `publishedAt`. Assert per-repo counts for epoch cutoff and 30-day cutoff.

```java
package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LeaderboardQueriesTest {

    private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);

    @PersistenceContext
    EntityManager em;

    @Autowired HistoryRepository historyRepository;
    @Autowired LibraryItemRepository libraryItemRepository;
    @Autowired ArtifactRepository artifactRepository;
    @Autowired OscalDocumentRepository oscalDocumentRepository;
    @Autowired AuthorizationRepository authorizationRepository;

    User alice;
    User bob;
    LocalDateTime recent;
    LocalDateTime old;

    @BeforeEach
    void setUp() {
        recent = LocalDateTime.now().minusDays(1);
        old = LocalDateTime.now().minusDays(90);
        alice = newUser("lb-alice");
        bob = newUser("lb-bob");

        // operations: alice 2 recent + 1 old, bob 1 recent
        newOperation(alice, recent);
        newOperation(alice, recent);
        newOperation(alice, old);
        newOperation(bob, recent);

        // library: alice 1 recent PUBLIC (publishedAt set), 1 old ORGANIZATION,
        // 1 recent PRIVATE (never counts); bob 1 recent PUBLIC with null
        // publishedAt (falls back to createdAt)
        newLibraryItem(alice, Visibility.PUBLIC, recent, recent);
        newLibraryItem(alice, Visibility.ORGANIZATION, old, old);
        newLibraryItem(alice, Visibility.PRIVATE, recent, recent);
        newLibraryItem(bob, Visibility.PUBLIC, recent, null);

        em.flush();
        em.clear();
    }

    @Test
    void countsOperationsPerUserWithCutoff() {
        Map<Long, Long> allTime = toMap(historyRepository.countOperationsPerUserSince(EPOCH));
        assertThat(allTime).containsEntry(alice.getId(), 3L).containsEntry(bob.getId(), 1L);

        Map<Long, Long> recent30 = toMap(historyRepository.countOperationsPerUserSince(LocalDateTime.now().minusDays(30)));
        assertThat(recent30).containsEntry(alice.getId(), 2L).containsEntry(bob.getId(), 1L);
    }

    @Test
    void countsSharedLibraryItemsExcludingPrivateWithPublishedAtFallback() {
        Map<Long, Long> allTime = toMap(libraryItemRepository.countSharedItemsPerUserSince(EPOCH));
        assertThat(allTime).containsEntry(alice.getId(), 2L).containsEntry(bob.getId(), 1L);

        Map<Long, Long> recent30 = toMap(libraryItemRepository.countSharedItemsPerUserSince(LocalDateTime.now().minusDays(30)));
        assertThat(recent30).containsEntry(alice.getId(), 1L).containsEntry(bob.getId(), 1L);
    }

    private Map<Long, Long> toMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    private User newUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setPassword("x");
        u.setEmail(username + "@example.com");
        u.setCreatedAt(LocalDateTime.now());
        u.setEnabled(true);
        em.persist(u);
        return u;
    }

    private void newOperation(User user, LocalDateTime ts) {
        OperationHistory op = new OperationHistory();
        op.setOperationType("VALIDATE");
        op.setFileName("f.json");
        op.setTimestamp(ts);
        op.setSuccess(true);
        op.setUser(user);
        em.persist(op);
    }

    private void newLibraryItem(User user, Visibility vis, LocalDateTime createdAt, LocalDateTime publishedAt) {
        LibraryItem item = new LibraryItem();
        item.setItemId(UUID.randomUUID().toString());
        item.setTitle("Item");
        item.setOscalType("catalog");
        item.setCreatedBy(user);
        item.setCreatedAt(createdAt);
        item.setUpdatedAt(createdAt);
        item.setVisibility(vis);
        item.setPublishedAt(publishedAt);
        em.persist(item);
    }
}
```

Note: entity setter names must be checked against the actual entities when writing this test (e.g., `LibraryItem` may require other non-null fields). Add analogous seed helpers + a combined test for `Artifact`, `OscalDocument`, and `Authorization` counts once you've read those entities' required (nullable=false) fields — `Authorization` requires a persisted `AuthorizationTemplate` and `Organization`, mirroring `AuthorizationRepositoryOrgScopeTest.newAuthorization`. Assert `countCreatedPerUserSince(EPOCH)` returns the seeded counts per user for all three.

- [ ] **Step 2: Run to verify failure**: `cd back-end && mvn test -Dtest=LeaderboardQueriesTest` — compile error: methods not defined.

- [ ] **Step 3: Add the queries.**

`HistoryRepository.java` (append inside interface):

```java
    /**
     * Leaderboard: operations per user since the cutoff.
     * Rows are [userId, count]. Pass epoch for all-time.
     */
    @Query("SELECT h.user.id, COUNT(h) FROM OperationHistory h "
            + "WHERE h.user IS NOT NULL AND h.timestamp >= :cutoff "
            + "GROUP BY h.user.id")
    List<Object[]> countOperationsPerUserSince(@Param("cutoff") LocalDateTime cutoff);
```

`LibraryItemRepository.java`:

```java
    /**
     * Leaderboard: items shared into the library (non-PRIVATE) per creator
     * since the cutoff. Uses publishedAt when set, else createdAt.
     * Rows are [userId, count].
     */
    @Query("SELECT li.createdBy.id, COUNT(li) FROM LibraryItem li "
            + "WHERE li.visibility <> gov.nist.oscal.tools.api.entity.Visibility.PRIVATE "
            + "AND COALESCE(li.publishedAt, li.createdAt) >= :cutoff "
            + "GROUP BY li.createdBy.id")
    List<Object[]> countSharedItemsPerUserSince(@Param("cutoff") LocalDateTime cutoff);
```

`ArtifactRepository.java`:

```java
    /** Leaderboard: artifacts created per user since the cutoff. Rows are [userId, count]. */
    @Query("SELECT a.createdBy.id, COUNT(a) FROM Artifact a "
            + "WHERE a.createdAt >= :cutoff GROUP BY a.createdBy.id")
    List<Object[]> countCreatedPerUserSince(@Param("cutoff") LocalDateTime cutoff);
```

`OscalDocumentRepository.java`:

```java
    /** Leaderboard: documents created per user since the cutoff. Rows are [userId, count]. */
    @Query("SELECT d.createdBy.id, COUNT(d) FROM OscalDocument d "
            + "WHERE d.createdAt >= :cutoff GROUP BY d.createdBy.id")
    List<Object[]> countCreatedPerUserSince(@Param("cutoff") LocalDateTime cutoff);
```

`AuthorizationRepository.java`:

```java
    /** Leaderboard: authorizations created per user since the cutoff. Rows are [userId, count]. */
    @Query("SELECT a.authorizedBy.id, COUNT(a) FROM Authorization a "
            + "WHERE a.createdAt >= :cutoff GROUP BY a.authorizedBy.id")
    List<Object[]> countCreatedPerUserSince(@Param("cutoff") LocalDateTime cutoff);
```

Add `import java.time.LocalDateTime;`, `import org.springframework.data.jpa.repository.Query;`, `import org.springframework.data.repository.query.Param;`, `import java.util.List;` where missing.

- [ ] **Step 4: Run to verify pass**: `mvn test -Dtest=LeaderboardQueriesTest` — PASS.

- [ ] **Step 5: Commit**: `git add -A && git commit -m "feat(leaderboard): per-user aggregation queries on activity sources"`

---

### Task 2: DTOs + LeaderboardService

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/LeaderboardEntry.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/LeaderboardResponse.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/LeaderboardService.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/LeaderboardServiceTest.java`

**Interfaces:**
- Consumes: the five repository methods from Task 1, `UserRepository.findAllById(Iterable<Long>)`.
- Produces (used by Task 3):
  - `LeaderboardService.getLeaderboard(String window): LeaderboardResponse` — `window` is `"30d"` or `"all"`; throws `IllegalArgumentException` for anything else.
  - `LeaderboardResponse` getters: `getWindow(): String`, `getGeneratedAt(): Instant`, `getMostActive(): List<LeaderboardEntry>`, `getTopContributors(): List<LeaderboardEntry>`.
  - `LeaderboardEntry` getters: `getRank(): int`, `getUsername(): String`, `getDisplayName(): String`, `getScore(): long`, `getBreakdown(): Map<String, Long>` (null for topContributors; keys `operations`, `libraryPublishes`, `artifacts`, `documents`, `authorizations` — zero-count keys omitted).

DTOs are plain classes with a constructor + getters (match the style of `model/AnalyticsResponse.java`). `LeaderboardEntry(int rank, String username, String displayName, long score, Map<String, Long> breakdown)`. `LeaderboardResponse(String window, Instant generatedAt, List<LeaderboardEntry> mostActive, List<LeaderboardEntry> topContributors)`.

Service behavior (unit-test each):
1. `"30d"` → cutoff `LocalDateTime.now().minusDays(30)`; `"all"` → `LocalDateTime.of(1970,1,1,0,0)`; anything else → `IllegalArgumentException`.
2. Most Active score = sum of the five sources per user; breakdown map has only non-zero sources.
3. Top Contributors = library counts only.
4. Users fetched via `UserRepository.findAllById`; users with `enabled=false` (or missing) dropped from both boards.
5. Sort score desc, then username asc; ranks 1..n ordinal; truncate to 20.
6. Display name = trimmed `firstName + " " + lastName` if either present, else username.
7. Empty sources → empty lists, no exception.

Tests: Mockito `@ExtendWith(MockitoExtension.class)`, mock all five repos + `UserRepository`, build `Object[]` rows, assert merge/rank/tie-break (two users same score → username order decides, ranks 1 and 2), top-20 truncation (seed 25 users), disabled exclusion, breakdown contents, invalid window throws.

Steps: write failing tests → `mvn test -Dtest=LeaderboardServiceTest` (compile failure) → implement DTOs + service → PASS → commit `feat(leaderboard): scoring/ranking service and DTOs`.

---

### Task 3: LeaderboardController + security tests

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/LeaderboardController.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/controller/LeaderboardControllerTest.java`

**Interfaces:**
- Consumes: `LeaderboardService.getLeaderboard(String)`.
- Produces: `GET /api/leaderboard?window=30d|all` (default `all`), JSON `LeaderboardResponse`; 400 on invalid window; 403 anonymous.

Controller:

```java
package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.model.LeaderboardResponse;
import gov.nist.oscal.tools.api.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Leaderboard Controller
 * Global gamification boards: most active users and top library contributors.
 */
@RestController
@RequestMapping("/api/leaderboard")
@Tag(name = "Leaderboard", description = "Global activity and contribution leaderboards")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    @Operation(summary = "Get leaderboards",
               description = "Most active users and top library contributors for a time window (30d or all)")
    public ResponseEntity<LeaderboardResponse> getLeaderboard(
            @RequestParam(name = "window", defaultValue = "all") String window) {
        try {
            return ResponseEntity.ok(leaderboardService.getLeaderboard(window));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
```

Test with `@WebMvcTest(LeaderboardController.class)` copying the `@MockitoBean` set from `SecurityComplianceControllerTest` (JwtUtil, UserDetailsService, RateLimitService, RateLimitConfig, SecurityHeadersConfig) + `LeaderboardService`:
- anonymous GET → `status().isForbidden()`
- `@WithMockUser` GET → 200, `jsonPath("$.mostActive[0].username")` etc. from a stubbed response
- `@WithMockUser` GET `?window=bogus` with service throwing `IllegalArgumentException` → `status().isBadRequest()`
- `@WithMockUser` GET without param → service called with `"all"` (verify)

Steps: failing test → implement → `mvn test -Dtest=LeaderboardControllerTest` PASS → commit `feat(leaderboard): authenticated REST endpoint`.

---

### Task 4: Frontend types + API client

**Files:**
- Modify: `front-end/src/types/oscal.ts` (append types)
- Modify: `front-end/src/lib/api-client.ts` (append method near the history methods)
- Test: `front-end/src/lib/__tests__/leaderboard-client.test.ts`

**Interfaces:**
- Produces (used by Tasks 5–6):

```typescript
export type LeaderboardWindow = '30d' | 'all';

export interface LeaderboardEntry {
  rank: number;
  username: string;
  displayName: string;
  score: number;
  breakdown?: Record<string, number> | null;
}

export interface LeaderboardResponse {
  window: LeaderboardWindow;
  generatedAt: string;
  mostActive: LeaderboardEntry[];
  topContributors: LeaderboardEntry[];
}
```

- `apiClient.getLeaderboard(window: LeaderboardWindow): Promise<LeaderboardResponse>` — GET `${API_BASE_URL}/leaderboard?window=…` with `this.getAuthHeaders()` via `this.fetchWithTimeout`, throws on `!response.ok` (mirror `getOperationStats`, but rethrow instead of returning a fallback so the page can show its error state).

Test with vitest: mock `global.fetch`; assert URL + Authorization header + parsed body; assert rejection on 500. Mirror mocking style from `front-end/src/lib/api-client.test.ts`.

Steps: failing test → implement → `npx vitest run src/lib/__tests__/leaderboard-client.test.ts` PASS → commit `feat(leaderboard): frontend API client + types`.

---

### Task 5: Leaderboard page

**Files:**
- Create: `front-end/src/app/leaderboard/page.tsx` (thin ProtectedRoute wrapper)
- Create: `front-end/src/app/leaderboard/leaderboard-content.tsx` (the real UI, testable)
- Test: `front-end/src/app/leaderboard/__tests__/leaderboard-content.test.tsx`

**Interfaces:**
- Consumes: `apiClient.getLeaderboard`, `useAuth()` from `@/contexts/AuthContext` (for `user.username` self-highlight), ui kit `Card`, `Tabs`, `Skeleton`-equivalent (use pulse divs like history page if no skeleton component), `Tooltip`, lucide `Trophy`, `Medal`, `Award`.
- Produces: default export `LeaderboardPage` route component.

Behavior (test each):
- Fetches on mount with `all` → shows both boards' rows (rank, display name, score).
- Tab switch to "Last 30 days" refetches with `30d`.
- Ranks 1–3 render medal icons (assert via `data-testid="medal-1"` etc.); others render the rank number.
- Row for `useAuth().user.username` gets highlight class + "You" badge.
- Empty arrays → empty-state copy ("No activity yet").
- Rejected fetch → error state with a Retry button that refetches.
- Most Active rows show breakdown text (render breakdown inline under the name as muted text, e.g. "30 operations · 4 library publishes" — simpler and more testable than a hover tooltip).

Test setup mirrors existing app tests (`vi.mock('@/lib/api-client')`, `vi.mock('@/contexts/AuthContext')`). Use `data-tour="leaderboard-page"` root attr for future tours.

Steps: failing tests → implement → `npx vitest run src/app/leaderboard` PASS → commit `feat(leaderboard): /leaderboard page with tabs, medals, self-highlight`.

---

### Task 6: Navigation link

**Files:**
- Modify: `front-end/src/components/Navigation.tsx`
- Test: `front-end/src/components/__tests__/navigation-leaderboard.test.tsx`

Add after the APIs link (authenticated non-admin only, same guard):

```tsx
{mounted && isAuthenticated && !isSuperAdmin() && (
  <Link
    href="/leaderboard"
    data-tour="nav-leaderboard"
    className="hidden sm:inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
  >
    <Trophy className="h-4 w-4" />
    Leaderboard
  </Link>
)}
```

Import `Trophy` from `lucide-react`. Test: mock `useAuth` authenticated non-admin → link rendered with href `/leaderboard`; unauthenticated → absent; super admin → absent.

Steps: failing test → implement → PASS → commit `feat(leaderboard): top-level nav link`.

---

### Task 7: Playwright e2e

**Files:**
- Create: `front-end/e2e/leaderboard.spec.ts`

Follows `tours.spec.ts` conventions (auth via existing `auth.setup.ts` storage state). Cases:
- Nav link visible; clicking navigates to `/leaderboard`.
- Both board headings render ("Most Active Users", "Top Contributors").
- Tab switch to "Last 30 days" triggers a `/api/leaderboard?window=30d` request (`page.waitForResponse`).
- Keyboard: tabs reachable and switchable with arrow keys (radix Tabs semantics).
- Axe scan on the page: no violations (same AxeBuilder usage as `accessibility.spec.ts`).

Run: `npx playwright test e2e/leaderboard.spec.ts` against dev servers. Commit `test(leaderboard): e2e coverage + axe scan`.

---

### Task 8: Full test sweep + local run

- Backend: `cd back-end && mvn test -Dtest='Leaderboard*'` then full `mvn test` if time allows.
- Frontend: `cd front-end && npx vitest run` and `npx tsc --noEmit` (or `npm run lint`).
- Start dev servers (`./dev.sh` or launch.json preview) and verify in browser: log in, click Leaderboard, check boards, switch tabs, confirm own row highlight.
- Screenshot proof for the user.
