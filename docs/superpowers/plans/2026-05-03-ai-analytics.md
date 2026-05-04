# OSCAL Hub AI Usage Analytics — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to execute this plan task-by-task.

**Goal:** Per-org visibility into AI usage. Org admins can see jobs run over time, total tokens per job, total cost per job, and the full event log for any run. Backed by event buffering on the existing `AiSession` row + a model-pricing lookup.

**Architecture:** Reuse the foundation. `AsyncWizardRunner` already finalises the session row; extend it to also buffer the SSE events emitted during the run and persist them as `events_json` on the row, plus compute `cost_usd_micros` from `tokens_in/out` × per-model rates. New `AiAnalyticsController` exposes paginated session lists, single-session detail with events, and aggregate totals. New frontend page `/org-admin/ai-analytics` renders summary cards + a sessions table + a detail drawer.

**Plan structure:**
- **Phase J — Backend** J1–J5
- **Phase K — Frontend** K1–K4

---

## Phase J — Backend

### Task J1: Migration `V1.23__ai_session_events_and_cost.sql`

**Files:**
- Create `back-end/src/main/resources/db/migration/V1.23__ai_session_events_and_cost.sql`

- [ ] **Step 1: Write the migration**

```sql
-- V1.23: AI session events log + computed cost

ALTER TABLE ai_sessions
    ADD COLUMN events_json     TEXT,
    ADD COLUMN cost_usd_micros BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_ai_sessions_org_status_time
    ON ai_sessions(organization_id, status, started_at DESC);
```

- [ ] **Step 2: Commit**

```bash
git add back-end/src/main/resources/db/migration/V1.23__ai_session_events_and_cost.sql
git commit -m "feat(ai): events_json + cost_usd_micros columns on ai_sessions"
```

---

### Task J2: Entity fields + ModelPricing

**Files:**
- Modify `back-end/src/main/java/gov/nist/oscal/tools/api/entity/AiSession.java` — add `eventsJson` and `costUsdMicros` fields with getters/setters
- Create `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/ModelPricing.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/ModelPricingTest.java`

- [ ] **Step 1: Add columns to entity**

Add to `AiSession`:

```java
@Column(name = "events_json", columnDefinition = "TEXT")
private String eventsJson;

@Column(name = "cost_usd_micros", nullable = false)
private long costUsdMicros = 0;

public String getEventsJson() { return eventsJson; }
public void setEventsJson(String eventsJson) { this.eventsJson = eventsJson; }
public long getCostUsdMicros() { return costUsdMicros; }
public void setCostUsdMicros(long costUsdMicros) { this.costUsdMicros = costUsdMicros; }
```

- [ ] **Step 2: Implement `ModelPricing`**

```java
package gov.nist.oscal.tools.api.service.ai;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ModelPricing {

    // USD per million tokens. Update when Anthropic publishes pricing changes.
    // https://www.anthropic.com/pricing
    private static final Map<String, double[]> RATES = Map.of(
            // model -> [input USD/MTok, output USD/MTok]
            "claude-opus-4-7",          new double[]{15.00, 75.00},
            "claude-opus-4-6",          new double[]{15.00, 75.00},
            "claude-sonnet-4-6",        new double[]{ 3.00, 15.00},
            "claude-sonnet-4-5",        new double[]{ 3.00, 15.00},
            "claude-haiku-4-5-20251001",new double[]{ 1.00,  5.00},
            "claude-haiku-4-5",         new double[]{ 1.00,  5.00}
    );
    private static final double[] FALLBACK = new double[]{15.00, 75.00};

    public long computeMicros(String model, int tokensIn, int tokensOut) {
        double[] r = RATES.getOrDefault(model, FALLBACK);
        double usd = (tokensIn * r[0] + tokensOut * r[1]) / 1_000_000.0;
        return Math.round(usd * 1_000_000.0);
    }
}
```

- [ ] **Step 3: Test**

```java
package gov.nist.oscal.tools.api.service.ai;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ModelPricingTest {
    private final ModelPricing p = new ModelPricing();

    @Test
    void opus47PricingMatchesPublished() {
        // 1M in + 1M out at Opus rate = $15 + $75 = $90 = 90_000_000 micros
        assertThat(p.computeMicros("claude-opus-4-7", 1_000_000, 1_000_000)).isEqualTo(90_000_000L);
    }

    @Test
    void haiku45PricingMatchesPublished() {
        // 1M in + 1M out at Haiku rate = $1 + $5 = $6 = 6_000_000 micros
        assertThat(p.computeMicros("claude-haiku-4-5-20251001", 1_000_000, 1_000_000)).isEqualTo(6_000_000L);
    }

    @Test
    void unknownModelFallsBackToOpusRate() {
        assertThat(p.computeMicros("unknown-model", 1_000_000, 1_000_000)).isEqualTo(90_000_000L);
    }
}
```

- [ ] **Step 4: Commit**

```bash
mvn -q -Dtest=ModelPricingTest test
git add back-end/src/main/java/gov/nist/oscal/tools/api/entity/AiSession.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/ModelPricing.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/ModelPricingTest.java
git commit -m "feat(ai): ModelPricing lookup + AiSession.eventsJson/costUsdMicros"
```

---

### Task J3: Wire event buffering + cost into the run lifecycle

**Files:**
- Modify `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/stream/AiSessionEventStream.java` — buffer published events per session, expose `drainBuffer(sessionId)`
- Modify `back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AsyncWizardRunner.java` — after wizard completes, drain the buffer + persist as JSON, compute cost via `ModelPricing`

- [ ] **Step 1: Add buffer to `AiSessionEventStream`**

Add a `ConcurrentHashMap<UUID, List<SessionEvent>> buffers`. In `publish()`, append to the buffer (creating the list if absent) before forwarding to the SSE emitter. New method:

```java
public List<SessionEvent> drainBuffer(UUID sessionId) {
    List<SessionEvent> events = buffers.remove(sessionId);
    return events == null ? List.of() : List.copyOf(events);
}
```

Subscribers don't get the buffer — only `drainBuffer` does. The buffer is for persistence, not replay.

- [ ] **Step 2: Persist on completion in `AsyncWizardRunner`**

Inject `AiSessionEventStream stream`, `ModelPricing pricing`, `ObjectMapper mapper`. After the existing `sessions.save(session)` block, before returning, drain + serialize + persist + compute cost:

```java
List<SessionEvent> events = stream.drainBuffer(ctx.sessionId());
try {
    List<Map<String, Object>> arr = events.stream()
        .map(e -> Map.<String, Object>of(
            "type", e.type().name().toLowerCase(),
            "data", e.dataJson()))
        .toList();
    session.setEventsJson(mapper.writeValueAsString(arr));
} catch (Exception e) {
    log.warn("Failed to serialize events for session {}", ctx.sessionId(), e);
}
session.setCostUsdMicros(pricing.computeMicros(session.getModel(),
        session.getTokensIn(), session.getTokensOut()));
sessions.save(session);
```

- [ ] **Step 3: Update `AsyncWizardRunnerTest`**

Add an `AiSessionEventStream stream` mock, `ModelPricing pricing` real instance, and `ObjectMapper mapper`. Pass to constructor. Stub `stream.drainBuffer(any())` to return `List.of()` on the existing tests so they keep passing. Add one new test asserting `costUsdMicros` is set after a successful run.

- [ ] **Step 4: Commit**

```bash
mvn -q -Dtest=AsyncWizardRunnerTest test
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/stream/AiSessionEventStream.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/ai/AsyncWizardRunner.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/ai/AsyncWizardRunnerTest.java
git commit -m "feat(ai): buffer + persist session events; compute cost on completion"
```

---

### Task J4: `AiAnalyticsController` + DTOs + repository queries

**Files:**
- Create `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AiAnalyticsController.java`
- Create `back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/AiSessionSummary.java` (record)
- Create `back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/AiSessionDetail.java` (record)
- Create `back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/AiUsageTotals.java` (record)
- Modify `back-end/src/main/java/gov/nist/oscal/tools/api/repository/AiSessionRepository.java` — add `findByOrganizationIdOrderByStartedAtDesc(Long orgId, Pageable pageable)` and `@Query` aggregate sums
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/controller/AiAnalyticsControllerTest.java`

- [ ] **Step 1: DTOs**

```java
public record AiSessionSummary(
        UUID id, Long userId, String username, WizardKind wizardKind, AiSessionMode mode,
        String model, AiSessionStatus status, int tokensIn, int tokensOut, long costUsdMicros,
        LocalDateTime startedAt, LocalDateTime endedAt, String errorCode) { }
```

```java
public record AiSessionDetail(
        AiSessionSummary summary, List<Map<String, Object>> events, String errorMessage) { }
```

```java
public record AiUsageTotals(
        int totalSessions, long totalTokensIn, long totalTokensOut,
        long totalCostUsdMicros, int sessionsThisMonth, long costThisMonthUsdMicros) { }
```

- [ ] **Step 2: Repository methods**

```java
List<AiSession> findByOrganizationIdOrderByStartedAtDesc(Long orgId, org.springframework.data.domain.Pageable pageable);

@Query("SELECT new map(" +
       "COUNT(s) as count, SUM(s.tokensIn) as ti, SUM(s.tokensOut) as to, SUM(s.costUsdMicros) as cost) " +
       "FROM AiSession s WHERE s.organizationId = :orgId")
java.util.Map<String, Object> sumForOrg(@Param("orgId") Long orgId);

@Query("SELECT new map(" +
       "COUNT(s) as count, SUM(s.costUsdMicros) as cost) " +
       "FROM AiSession s WHERE s.organizationId = :orgId AND s.startedAt >= :since")
java.util.Map<String, Object> sumForOrgSince(@Param("orgId") Long orgId, @Param("since") LocalDateTime since);
```

- [ ] **Step 3: Controller**

Three endpoints, all `@PreAuthorize("hasAnyRole('ORG_ADMIN','SUPER_ADMIN')")` + the existing `requireOrgMembership`-style check pattern from foundation:

- `GET /api/ai/analytics/sessions?organizationId=&limit=20&offset=0` → `List<AiSessionSummary>`
- `GET /api/ai/analytics/sessions/{id}?organizationId=` → `AiSessionDetail`
- `GET /api/ai/analytics/totals?organizationId=` → `AiUsageTotals`

The controller resolves usernames by looking up `User` entities by id (you can join in JPA or do a per-row lookup with a small `Map<Long,String>` cache populated up front from `userRepository.findAllById(ids)`).

- [ ] **Step 4: Controller test**

`@WebMvcTest(AiAnalyticsController.class)` with the same `@MockitoBean` set foundation tests use. Two happy-path tests: list + totals. One auth test: non-admin → 403.

- [ ] **Step 5: Run + commit**

```bash
mvn -q -Dtest=AiAnalyticsControllerTest test
git add back-end/src/main/java/gov/nist/oscal/tools/api/controller/AiAnalyticsController.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/AiSession*.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/model/ai/AiUsageTotals.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/repository/AiSessionRepository.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/controller/AiAnalyticsControllerTest.java
git commit -m "feat(ai): analytics REST API — sessions list, detail, totals"
```

---

### Task J5: Backend test sweep on the new bits

- [ ] Run `mvn -q -Dtest='*Ai*Test,*Catalog*Test,*Knowledge*Test,*Document*Test,*Source*Test,*Tool*Test,*Pricing*Test,*Async*Test,*Smoke*Test' test`. Expect all pass.

- [ ] No commit unless something needed fixing.

---

## Phase K — Frontend

### Task K1: Analytics methods on `aiClient`

**Files:**
- Modify `front-end/src/lib/ai-client.ts`
- Modify `front-end/src/lib/ai-client.test.ts`

- [ ] **Step 1: Add types + methods**

```typescript
export type AiSessionStatus = 'RUNNING' | 'AWAITING_INPUT' | 'COMPLETED' | 'CANCELLED' | 'FAILED';

export interface AiSessionSummary {
  id: string;
  userId: number;
  username: string | null;
  wizardKind: WizardKind;
  mode: SessionMode;
  model: string;
  status: AiSessionStatus;
  tokensIn: number;
  tokensOut: number;
  costUsdMicros: number;
  startedAt: string;
  endedAt: string | null;
  errorCode: string | null;
}

export interface AiSessionDetail {
  summary: AiSessionSummary;
  events: Array<{ type: string; data: string }>;
  errorMessage: string | null;
}

export interface AiUsageTotals {
  totalSessions: number;
  totalTokensIn: number;
  totalTokensOut: number;
  totalCostUsdMicros: number;
  sessionsThisMonth: number;
  costThisMonthUsdMicros: number;
}
```

Then methods inside `aiClient`:

```typescript
async listSessions(orgId: number, limit = 20, offset = 0): Promise<AiSessionSummary[]> { ... }
async getSession(orgId: number, id: string): Promise<AiSessionDetail> { ... }
async getUsageTotals(orgId: number): Promise<AiUsageTotals> { ... }
```

All three GET, with `authHeaders()`.

- [ ] **Step 2: Tests** — one per method, asserting URL shape and response handling.

- [ ] **Step 3: Commit** `feat(ai/fe): aiClient analytics methods`.

---

### Task K2: Analytics page `/org-admin/ai-analytics`

**Files:**
- Create `front-end/src/app/org-admin/ai-analytics/page.tsx`

- [ ] **Step 1: Page layout**

Match the existing org-admin sub-page pattern (back link to dashboard, role-gate redirect). Top: three summary cards (total sessions / total tokens / total cost) using the existing `Card` primitives. Below: a sessions table with these columns:

| Started | User | Wizard | Status | Model | Tokens (in / out) | Cost | Action |

- Status renders as a colored pill — `COMPLETED` green, `FAILED` red, `RUNNING` indigo with a pulsing dot, `CANCELLED` gray.
- Cost formats `micros / 1_000_000` to `$X.XXXX`.
- "Action" column is a "View" button that opens the detail drawer (Task K3).
- Pagination: simple "Load more" button at table bottom.

- [ ] **Step 2: Commit** `feat(ai/fe): org-admin AI usage analytics page`.

---

### Task K3: Session detail drawer

**Files:**
- Create `front-end/src/components/ai/SessionDetailDrawer.tsx`
- Modify `front-end/src/app/org-admin/ai-analytics/page.tsx` — wire the drawer

- [ ] **Step 1: Drawer component**

shadcn/ui has a `Sheet` (right-side drawer). Use it. Drawer header: wizard kind + status pill + start/end times + duration. Body sections:
- **Tokens & cost** — two-column grid: tokens in/out, model, cost USD, duration.
- **Event log** — render the persisted events array using the same `describeEvent` helper from `[kind]/page.tsx`. Move that helper into a shared file `front-end/src/lib/ai-events.ts` so both run page and drawer import it.
- **Error** — only if `summary.status === 'FAILED'`. Shows `errorCode` + `errorMessage` in a destructive-tinted callout.

- [ ] **Step 2: Commit** `feat(ai/fe): session detail drawer with event log + token + cost breakdown`.

---

### Task K4: Org-admin dashboard card for AI Analytics

**Files:**
- Modify `front-end/src/app/org-admin/page.tsx` — add a sibling card to "AI Settings"

- [ ] **Step 1: Add quick-link**

```typescript
{
  label: 'AI Usage Analytics',
  description: 'Sessions, tokens, and estimated cost for this organization',
  href: '/org-admin/ai-analytics',
  icon: BarChart3,  // import from lucide-react
  color: 'indigo',
}
```

The quick-link only renders inside the `AiFeatureGate` — if AI isn't configured, hide it (no analytics to show anyway).

- [ ] **Step 2: Commit** `feat(ai/fe): AI Usage Analytics card on org-admin dashboard`.

---

## Self-Review

```bash
cd back-end && mvn -q test 2>&1 | grep BUILD
cd front-end && npx vitest run 2>&1 | tail -3
```

Manual smoke: log in as org admin → `/org-admin` → click "AI Usage Analytics" → confirm cards populate, table renders, click View → drawer shows the event log of a real run from the catalog wizard.
