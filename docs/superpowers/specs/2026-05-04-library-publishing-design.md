# Library + Publish to Public — Design

**Date:** 2026-05-04
**Status:** Draft, awaiting user review
**Owner:** Travis Howerton

## 1. Goal

Let any builder in OSCAL Hub save its output to a Library, let users mark Library items public, and expose those public items to the world through both a website and a programmatic API. Authenticated users get extra capabilities (download, rate, comment, get an API key).

## 2. Non-goals (v1)

- Moderation queue / pre-publish review (self-service per Q1).
- Full-text search engines (Elasticsearch/Solr) — Postgres `tsvector` is sufficient.
- Webhooks for "new version published."
- Per-key quotas distinct from per-user quotas.
- Multiple labeled API keys per user (one active key only).
- Org-level shared API keys.
- API access to a user's own *private* items (public items only via API).
- Public-facing "report this content" / takedown form.
- Storage deduplication between builder docs and library snapshots.
- Redis-backed distributed rate limiting.

## 3. What already exists (do not rebuild)

The codebase already has a substantial Library subsystem; the new work extends rather than replaces it.

| Capability | Status | Location |
|---|---|---|
| `library_items`, `library_versions`, `library_tags`, `library_item_tags` | ✓ | `back-end/.../entity/Library*.java`, `db/migration/V1.0__baseline.sql` |
| 5-star ratings (`library_item_ratings`) | ✓ | `LibraryRatingService` |
| Threaded comments (`library_item_comments`) | ✓ | `LibraryCommentService` |
| Library REST endpoints | ✓ | `LibraryController.java` |
| Multi-backend blob storage (Azure/GCS/S3/local), separate `oscal-library` container | ✓ | `LibraryStorageService.java` |
| `/library` browse/upload/search frontend page | ✓ | `front-end/src/app/library/page.tsx` |
| Tag-based filtering, type filtering, popular/recent endpoints | ✓ | `LibraryController.java` |
| Builders for catalog, profile, component, SSP, AP, AR, POA&M, authorization, artifacts | ✓ | various controllers/services |
| `JwtAuthenticationFilter`, JWT claims (username, globalRole, organizationRole, userId, organizationId) | ✓ | `security/JwtAuthenticationFilter.java` |
| `RateLimitFilter` (in-memory bucket cache, per-user) | ✓ | `security/RateLimitFilter.java` |
| `audit_events` table with extensible `event_type` enum | ✓ | `db/migration/V1.0__baseline.sql` |
| Three-tier visibility precedent (`PRIVATE | ORGANIZATION | PUBLIC`) | ✓ in `artifacts` table; **NOT yet in `library_items`** |

## 4. What's new

1. `visibility` + `organization_id` columns on `library_items`, with enforcement on every read.
2. `source_type` + `source_id` soft pointers on `library_items` linking back to the originating builder doc.
3. "Save to Library" action wired into every builder (catalog, profile, component, SSP, AP, AR, POA&M).
4. `PATCH /api/library/{itemId}/visibility` endpoint (replaces the need for separate publish/unpublish endpoints).
5. Two parallel public surfaces:
   - `/api/public/catalog/*` — unauthenticated, IP-rate-limited, used by the public website.
   - `/api/public/v1/*` — `X-Api-Key` required, per-key-rate-limited, used by third-party programmatic consumers.
6. `api_keys` table + `ApiKeyAuthenticationFilter`.
7. `(public)` Next.js route group with `/catalog` and `/catalog/[itemId]` pages, accessible without login.
8. `/account/api-keys` page with key generation, expiration up to 1 year, and inline example code (curl, JavaScript fetch, Python requests).
9. New `audit_events.event_type` values for visibility and API-key lifecycle changes.

## 5. Decisions (locked)

| # | Decision | Rationale |
|---|---|---|
| Q1 | Self-service publishing | User wants velocity; admins can force-unpublish if needed |
| Q2 | Three-tier visibility (`PRIVATE | ORGANIZATION | PUBLIC`) | Mirrors existing `artifacts` pattern; org-internal sharing is a real use case |
| Q3 | One API key per user, 100 req/hr, expiration ≤ 365 days | Lean v1; multiple keys, org keys, per-key quotas deferred |
| Q4 | "Save to Library" creates or updates a single linked item per `(creator, source_type, source_id)`; new versions append; "Save as new" override available for fork case | Matches user mental model; reuses existing `library_versions` |
| — | Public items auto-show latest version (no separate "promote version" step) | Consistent with self-service stance |
| — | Existing `library_items` rows backfill to `PRIVATE` | Safe default; no accidental public exposure |
| — | Existing `/api/library/*` list/search endpoints get visibility filtering applied | Behavior change — non-creators stop seeing strangers' PRIVATE items — but the right behavior once visibility exists |
| — | Comments visible to authenticated users only on public items | Reduces spam/abuse surface; ratings + counts remain public for discovery |
| — | Search is Postgres `tsvector` over `title + description` plus existing tag/type filters | No new infra dependency |
| — | Creator OR `SUPER_ADMIN` may unpublish; force-unpublish writes audit event with reason | Self-service governance |
| — | Rate limiting stays in-memory per-instance (existing `RateLimitFilter`) | Acceptable for v1; flag as known limitation |

## 6. Data model

### 6.1 Migrations

**`V1.X__library_visibility_and_source.sql`** (modifies `library_items`):

```sql
ALTER TABLE library_items
  ADD COLUMN IF NOT EXISTS visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE'
    CHECK (visibility IN ('PRIVATE','ORGANIZATION','PUBLIC')),
  ADD COLUMN IF NOT EXISTS organization_id BIGINT NULL REFERENCES organizations(id),
  ADD COLUMN IF NOT EXISTS source_type VARCHAR(40) NULL
    CHECK (source_type IS NULL OR source_type IN
      ('CATALOG','PROFILE','SSP','AP','AR','POAM','COMPONENT_DEFINITION')),
  ADD COLUMN IF NOT EXISTS source_id UUID NULL,
  ADD COLUMN IF NOT EXISTS published_at TIMESTAMP NULL,
  ADD COLUMN IF NOT EXISTS last_published_at TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_library_items_visibility_type
  ON library_items(visibility, oscal_type);
CREATE INDEX IF NOT EXISTS idx_library_items_visibility_org
  ON library_items(visibility, organization_id);
CREATE INDEX IF NOT EXISTS idx_library_items_source
  ON library_items(created_by, source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_library_items_search_fts
  ON library_items USING GIN (
    to_tsvector('english', coalesce(title,'') || ' ' || coalesce(description,''))
  );
```

`organization_id` is required iff `visibility = 'ORGANIZATION'`. Enforced in `LibraryService` rather than via a SQL `CHECK` (Postgres CHECK on conditional logic involving enum values is awkward; service-layer guard is clearer). If drift becomes a concern post-launch, add a row-level trigger.

JPA entity additions on `LibraryItem.java`:
```java
@Enumerated(EnumType.STRING) @Column(nullable=false)
@ColumnDefault("'PRIVATE'")
private Visibility visibility = Visibility.PRIVATE;

@ManyToOne @JoinColumn(name="organization_id")
private Organization organization;

@Enumerated(EnumType.STRING) @Column(name="source_type")
private SourceType sourceType;

@Column(name="source_id") private UUID sourceId;
@Column(name="published_at") private Instant publishedAt;
@Column(name="last_published_at") private Instant lastPublishedAt;
```

The `@ColumnDefault` annotation matches the SQL default so `ddl-auto=validate` (enforced policy in CLAUDE.md) accepts the schema.

**`V1.Y__api_keys.sql`** (new table):

```sql
CREATE TABLE IF NOT EXISTS api_keys (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  key_hash VARCHAR(255) NOT NULL,
  key_prefix VARCHAR(12) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  expires_at TIMESTAMP NOT NULL,
  revoked_at TIMESTAMP NULL,
  last_used_at TIMESTAMP NULL,
  CONSTRAINT api_keys_expires_within_year
    CHECK (expires_at <= created_at + INTERVAL '366 days')
);

CREATE UNIQUE INDEX IF NOT EXISTS api_keys_one_active_per_user
  ON api_keys(user_id) WHERE revoked_at IS NULL;
```

Two-layer enforcement: the service layer caps at exactly 365 days (authoritative), the SQL CHECK adds a 1-day buffer (defense in depth, accommodates clock skew between app and DB). If the service is bypassed by a direct DB write, the CHECK still catches anything past ~1 year.

**`V1.Z__library_audit_event_types.sql`** — extends the `audit_events.event_type` CHECK constraint:

```sql
ALTER TABLE audit_events
  DROP CONSTRAINT IF EXISTS audit_events_event_type_check;
ALTER TABLE audit_events
  ADD CONSTRAINT audit_events_event_type_check
  CHECK (event_type IN (
    -- existing values preserved here, plus:
    'LIBRARY_ITEM_PUBLISHED',
    'LIBRARY_ITEM_UNPUBLISHED',
    'LIBRARY_ITEM_FORCE_UNPUBLISHED',
    'LIBRARY_ITEM_VISIBILITY_CHANGED',
    'API_KEY_GENERATED',
    'API_KEY_REVOKED',
    'API_KEY_REGENERATED'
  ));
```

The migration author must read the existing constraint, copy its values, and append. Idempotent — drop-if-exists then recreate.

### 6.2 Storage

Library content files continue to live in the `oscal-library` blob container via the existing `LibraryStorageService`. No changes. Public endpoints serve from the same backing files — no copy needed.

## 7. Backend

### 7.1 Builder integration

New `LibraryIngestService` (in `service/library/`):

```java
public class LibraryIngestService {
  public LibraryItem saveToLibrary(SourceType type, UUID sourceId,
                                    String title, String description,
                                    Set<String> tagNames, Visibility visibility,
                                    Long organizationId, Principal principal);
}
```

Algorithm:
1. Resolve source content via a `SourceContentResolver` (one impl per builder type, looked up by `SourceType`). Returns `(byte[] content, String format)`.
2. Look up existing item: `findByCreatorAndSource(principal.userId, type, sourceId)`.
3. If found → write new `library_versions` row (auto-increment `version_number`), persist blob, update `current_version_id`. Audit event optional.
4. If not found → insert `library_item` (with provided visibility/org/title/etc.) and version 1.
5. If `visibility != PRIVATE`, write the appropriate audit event (`LIBRARY_ITEM_PUBLISHED` for PUBLIC, `LIBRARY_ITEM_VISIBILITY_CHANGED` for ORGANIZATION).

Each builder controller adds:
```
POST /api/{catalogs|profiles|component-definitions|oscal-documents}/{id}/save-to-library
Body: { title, description, tags[], visibility, organizationId? }
```

Returns the `LibraryItem` DTO.

`SourceContentResolver` implementations live next to each builder service to keep the library code from depending on every builder. `LibraryIngestService` resolves them via Spring's `Map<SourceType, SourceContentResolver>` injection.

### 7.2 Visibility enforcement

New helper in `LibraryService`:

```java
public boolean canRead(LibraryItem item, Principal principal) {
  if (item.getVisibility() == PUBLIC) return true;
  if (principal == null) return false;
  if (item.getCreatedBy().equals(principal.username())) return true;
  if (item.getVisibility() == ORGANIZATION
      && Objects.equals(item.getOrganizationId(), principal.organizationId())) return true;
  return false;
}
```

Applied:
- In `getById`, `getContent`, `getVersions`, `getVersionContent` — return 404 (not 403) for items the user can't see, to avoid leaking existence.
- In list/search/recent/popular — as a JPA `Specification` that ORs the visibility predicates with the principal's identity. Anonymous callers are blocked at the security layer (these endpoints stay JWT-required).

### 7.3 Visibility change endpoint

```
PATCH /api/library/{itemId}/visibility
Body: { visibility: 'PRIVATE'|'ORGANIZATION'|'PUBLIC', organizationId?: number, reason?: string }
```

Auth: must be the item's creator, OR caller has `SUPER_ADMIN` global role.

Logic:
1. Load item; reject with 404 if not visible to caller.
2. Validate: if `ORGANIZATION`, `organizationId` required and must match the caller's org (unless SUPER_ADMIN).
3. Set `visibility`, `organization_id` (or NULL).
4. If transitioning *to* PUBLIC and `published_at IS NULL` → set `published_at = now()`.
5. If at PUBLIC (regardless of source state) → set `last_published_at = now()`.
6. Write audit event:
   - `LIBRARY_ITEM_PUBLISHED` (PRIVATE/ORG → PUBLIC, by creator)
   - `LIBRARY_ITEM_UNPUBLISHED` (PUBLIC → PRIVATE/ORG, by creator)
   - `LIBRARY_ITEM_FORCE_UNPUBLISHED` (PUBLIC → PRIVATE/ORG, by SUPER_ADMIN against another user's item, with `reason` recorded)
   - `LIBRARY_ITEM_VISIBILITY_CHANGED` (any other transition)

### 7.4 Public surfaces

Two parallel surfaces, **same controller, same query, different security configuration**:

#### 7.4.1 Catalog surface — `/api/public/catalog/*` (unauthenticated)

Whitelisted in `SecurityConfig` (added to the public-endpoints list alongside `/api/auth/login` etc.).

Endpoints:
- `GET /api/public/catalog/items?q=&type=&tag=&sort=&page=&size=` — paginated. `sort` ∈ `{newest, downloads, rating}`. Results filtered to `visibility = PUBLIC`.
- `GET /api/public/catalog/items/{itemId}` — public item detail.
- `GET /api/public/catalog/items/{itemId}/content` — download latest version. Increments `library_items.download_count`.
- `GET /api/public/catalog/items/{itemId}/versions/{versionId}/content` — download specific version (must belong to the item and to a publicly-visible item).

Rate limit: existing `RateLimitFilter` keyed by client IP. The current default cap configured in the filter applies; implementation should confirm the cap is reasonable for an unauthenticated public endpoint and adjust if needed.

#### 7.4.2 Programmatic surface — `/api/public/v1/*` (X-Api-Key required)

Same four endpoints as the catalog surface, with these differences:
- Requires `X-Api-Key` header. Missing/invalid → 401.
- Rate limit: 100 req/hr per active key. Exceeded → 429 with `Retry-After`.
- Responses are JSON (the catalog surface defaults to whatever the frontend asks for via `Accept`; the programmatic surface always emits JSON for predictability).

Both surfaces share a single `PublicLibraryController` that delegates to `LibraryService` with a `null`-or-API-key principal. The split is at the security filter chain, not the controller.

### 7.5 API key lifecycle

`ApiKeyService`:

```java
public final class ApiKeyService {
  private static final int KEY_BODY_LEN = 22;          // ~131 bits entropy
  private static final String PREFIX = "osc_";
  private static final long MAX_LIFETIME_SECONDS = 365L * 86_400L;

  /** Returns the plaintext key exactly once. Caller must show it and discard it. */
  public GeneratedKey generateKey(long userId, Instant expiresAt) {
    if (expiresAt.isBefore(Instant.now()) ||
        expiresAt.isAfter(Instant.now().plusSeconds(MAX_LIFETIME_SECONDS)))
      throw new ValidationException("expires_at must be in the future and ≤ 365 days");

    revokeActiveKey(userId, /*regenerate=*/true);  // audit: API_KEY_REGENERATED if existed
    String body = randomBase62(KEY_BODY_LEN);
    String full = PREFIX + body;                   // e.g. "osc_aB3fK9..."
    String hash = bcrypt(full);
    String displayPrefix = full.substring(0, 8);   // "osc_aB3f"
    ApiKey saved = repo.save(new ApiKey(userId, hash, displayPrefix, expiresAt));
    audit(API_KEY_GENERATED, userId, saved.id());
    return new GeneratedKey(full, saved);
  }

  public Optional<ApiKey> validate(String rawKey) {
    if (!rawKey.startsWith(PREFIX) || rawKey.length() < 8) return Optional.empty();
    String prefix = rawKey.substring(0, 8);
    return repo.findActiveByPrefix(prefix).stream()
      .filter(k -> bcryptVerify(rawKey, k.keyHash()))
      .findFirst()
      .map(k -> {
        if (k.expiresAt().isBefore(Instant.now())) return null;
        if (Duration.between(k.lastUsedAt(), Instant.now()).toMinutes() >= 1)
          repo.touchLastUsed(k.id(), Instant.now());     // debounced write
        return k;
      });
  }

  public void revoke(long userId);  // audit: API_KEY_REVOKED
}
```

Notes:
- `randomBase62` uses `SecureRandom` and the alphabet `[0-9A-Za-z]`.
- bcrypt cost factor matches the project's existing password-hashing cost (read from `BCryptPasswordEncoder` config during implementation; do not hardcode a different value).
- The "find by prefix then bcrypt-verify" pattern lets us index by prefix without exposing the full key. Two collisions on prefix would each be checked; with 8-char prefixes and base62 alphabet (62^4 bits of entropy in the random portion of the prefix) collisions are vanishingly rare.
- `last_used_at` is updated at most once per minute per key to avoid write amplification on a hot key.

REST surface (all require JWT):

```
POST   /api/account/api-keys     { expiresAt: ISO-8601 } → { fullKey, prefix, expiresAt }
GET    /api/account/api-keys                              → { active?: { prefix, createdAt, expiresAt, lastUsedAt } }
DELETE /api/account/api-keys/current                      → 204
```

`POST` is also the regenerate operation (it revokes-then-creates atomically in a single transaction).

### 7.6 `ApiKeyAuthenticationFilter`

New filter, placed in the security chain **before** `JwtAuthenticationFilter`. Applies only to paths matching `/api/public/v1/**`.

```java
class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
  protected void doFilterInternal(...) {
    String key = request.getHeader("X-Api-Key");
    if (key == null) { unauthorized(response); return; }
    Optional<ApiKey> validated = apiKeyService.validate(key);
    if (validated.isEmpty()) { unauthorized(response); return; }

    // Stash for the rate limiter
    request.setAttribute("apiKey", validated.get());
    // Build a Principal carrying the key's user identity
    var auth = new ApiKeyAuthentication(validated.get());
    SecurityContextHolder.getContext().setAuthentication(auth);
    chain.doFilter(request, response);
  }
}
```

`RateLimitFilter` is updated to detect the `apiKey` request attribute and use `apiKey.id` as the rate-limit bucket key (instead of the JWT user). Bucket size: 100, refill window: 1 hour. The existing in-memory `Caffeine` cache continues to back the buckets.

429 response body:
```json
{
  "error": "rate_limit_exceeded",
  "retryAfterSeconds": 1234,
  "limit": 100,
  "windowSeconds": 3600
}
```

with `Retry-After: 1234` header.

## 8. Frontend

### 8.1 Public route group

Create `front-end/src/app/(public)/` with its own `layout.tsx` — minimal header (logo, "Sign in" CTA), no auth context provider, no user-aware components.

Pages:
- `(public)/catalog/page.tsx` — search box (debounced), type filter (multi), tag filter (multi), sort dropdown. Card grid: title, description excerpt, type badge, tag chips, read-only stars, download count, last-published timestamp. Pagination: page size 24, infinite scroll or numbered (TBD during implementation — both are cheap).
- `(public)/catalog/[itemId]/page.tsx` — full description, all tags, version number, last-published date, rating summary (avg + histogram), download count. CTAs:
  - Anonymous: `[Sign in to download]`, `[Sign in to rate]`.
  - Authenticated (token present in localStorage): `[Download]`, `[Rate ★★★★★]`, `[View comments]`.
- Server-side fetched via `GET /api/public/catalog/items[/{itemId}]`. No SDK; plain `fetch`.

`middleware.ts` ensures `/catalog/*` is NOT in the auth-required prefix list. All other `/app/*` routes continue to require auth.

### 8.2 "Save to Library" UI

Each builder's final/save step gets a `[Save to Library]` button. Clicking opens a modal:

```
[ Save to Library ]
─────────────────────
Title         [______________________]   (defaults from builder metadata)
Description   [______________________]
Tags          [tag1] [tag2] [+ add]
Visibility    ( ) Private — only me
              ( ) Organization — my team
              ( ) Public — visible at /catalog
[ Cancel ]                  [ Save to Library ]
```

If the builder doc has previously been saved, the modal pre-fills from the existing library item and the button reads `[Update Library Item]`. A small footer link `[Save as new library item instead]` triggers the fork path (sets `source_id` to a fresh UUID so the lookup misses).

After save, a toast confirms with a link to the library item page (or the public detail page if visibility=PUBLIC).

### 8.3 Library page updates

`/library/page.tsx` currently lists items uploaded by the user. After this change:

- **Visibility column** with badge (gray PRIVATE / blue ORGANIZATION / green PUBLIC).
- **Visibility filter** in the existing filter bar.
- **Action menu** on each row: `[Make Private]`, `[Share with Org]`, `[Publish]`, `[Unpublish]`. Each calls `PATCH /api/library/{itemId}/visibility`.
- Items the user does not own (others' ORGANIZATION items, others' PUBLIC items) are visible in the listing but action menus are read-only.
- On a PUBLIC item the user owns, a banner: `This item is public — visible at /catalog/{itemId}`.

### 8.4 API keys page

New `/account/api-keys/page.tsx`:

**Empty state:**
```
You don't have an API key yet.

Generate one to use OSCAL Hub's public API.
Limit: 100 requests per hour.
Keys can expire in up to 365 days.

[Generate API Key]
```

**Generate flow:**
- Modal with date picker (default = today + 90 days, max = today + 365 days, hard-disabled past max).
- Warning: "You'll see your full key exactly once. Copy it now and store it somewhere safe. We only keep a hashed copy."
- On submit → `POST /api/account/api-keys` → modal shows the full key in a monospace box with a `[Copy]` button and a `[I've saved it — Continue]` button (required to dismiss).

**Active state (after generation):**
```
Active API key
  Prefix: osc_aB3f...
  Created: 2026-05-04
  Expires: 2026-08-02 (89 days remaining)
  Last used: 2 minutes ago

[Regenerate]   [Revoke]
```

Below the key card, an **Example code** panel with three tabs:

```
[ curl ]    [ JavaScript ]    [ Python ]

curl -H "X-Api-Key: osc_aB3f..." \
     "https://oscal-hub.example.com/api/public/v1/items?type=catalog&page=0&size=20"
```

```js
const res = await fetch(
  "https://oscal-hub.example.com/api/public/v1/items?type=catalog",
  { headers: { "X-Api-Key": "osc_aB3f..." } }
);
const data = await res.json();
```

```python
import requests
r = requests.get(
    "https://oscal-hub.example.com/api/public/v1/items",
    headers={"X-Api-Key": "osc_aB3f..."},
    params={"type": "catalog", "page": 0, "size": 20},
)
r.raise_for_status()
print(r.json())
```

The user's actual key is interpolated client-side from the value just returned by the API (kept in component state for the page lifetime, then garbage-collected). Once they navigate away, only the prefix remains visible.

Tabs render from a static template — no server work.

### 8.5 Navigation

- Top nav, authenticated: existing items + "Public Catalog" → `/catalog` (same page works in both contexts; auth state changes the CTAs but not the structure).
- User profile dropdown: add "API Keys" → `/account/api-keys`.
- Top nav, unauthenticated (only on `(public)` routes): logo + "Sign in" + "Public Catalog" → `/catalog`.

## 9. Audit & governance

Every visibility transition and key lifecycle event writes an `audit_events` row. Existing audit infrastructure is reused; only the enum extension is new (V1.Z migration).

Force-unpublish (SUPER_ADMIN against another user's PUBLIC item) requires a `reason` body field, recorded on the audit row. The creator is notified by email (reuses the existing SendGrid transactional channel — see SendGrid memory note).

No "report this content" UI in v1. Admin email (the existing `support@` address) is the takedown channel.

## 10. Phasing

Each phase ships independently and delivers visible value.

### Phase 1 — Visibility + Save-to-Library

Schema migrations (V1.X, V1.Z). `LibraryItem` entity changes. `LibraryIngestService` + per-builder `SourceContentResolver` implementations + per-builder `POST .../save-to-library` endpoint. Visibility filter on every read in `LibraryService`. `PATCH /api/library/{itemId}/visibility`. Frontend: Save-to-Library modal in every builder, visibility column + action menu on `/library`.

Visible result: users can save builder output to the library and mark items public/org/private. Public items aren't yet visible to anonymous users (no public surface yet) but they accumulate.

### Phase 2 — Public catalog UI

`/api/public/catalog/*` endpoints (unauthenticated). `(public)` Next.js route group with `/catalog` and `/catalog/[itemId]` pages. Update `middleware.ts` and `SecurityConfig` whitelist.

Visible result: anyone on the internet can browse, search, filter, and view public OSCAL content. Authenticated visitors get download/rate/comment.

### Phase 3 — API keys + programmatic surface

V1.Y migration. `ApiKeyService`, `ApiKeyAuthenticationFilter`, `/api/account/api-keys` controller. `RateLimitFilter` extension to support API-key buckets. `/api/public/v1/*` endpoints. Frontend `/account/api-keys` page with example code.

Visible result: third-party tools can pull public OSCAL content programmatically with a key the user can self-issue, rotate, and revoke.

## 11. Testing

### 11.1 Backend unit tests

- `LibraryServiceVisibilityTest` — `canRead` matrix covering every combination of (visibility, viewer-is-creator, viewer-is-org-member, anonymous). 12+ cases.
- `LibraryIngestServiceTest` — first save creates item; second save on same source appends version; "save as new" override creates separate item; non-creator cannot update; deleted source still allows library item to live.
- `ApiKeyServiceTest` — generate within bounds; reject expiration past 365 days; reject expiration in the past; regenerate revokes prior; validate rejects revoked / expired / unknown / wrong-prefix; bcrypt round-trip; `last_used_at` debounce.
- `LibraryControllerVisibilityTest` — `PATCH /visibility` permission matrix (creator yes, other user no, super-admin yes, with audit row written for force-unpublish).
- `RateLimitFilterApiKeyTest` — 100 requests succeed, 101st returns 429 with `Retry-After`; separate keys have independent buckets.

### 11.2 Backend integration tests (Spring `@SpringBootTest`)

- `PublicCatalogIntegrationTest` — anonymous `GET /api/public/catalog/items` returns only PUBLIC items; PRIVATE and ORG items not present in result.
- `PublicV1IntegrationTest` — missing key → 401; valid key → 200; revoked key → 401; expired key → 401.
- `SaveToLibraryIntegrationTest` — full round trip from each builder controller through `LibraryIngestService` to a stored blob.
- `MigrationTest` — flyway runs against an empty DB and against a baseline-loaded DB; existing rows backfill to PRIVATE.

### 11.3 Frontend tests

- Component test: Save-to-Library modal — visibility radios, tag chip add/remove, validation (title required), submission triggers correct endpoint per source type.
- Component test: API keys page — empty, generated, regenerate, revoke states; expiration date picker hard-disables past 365 days; example code tabs render correctly.
- E2E (Playwright): anonymous user lands on `/catalog`, searches, opens an item, sees "Sign in to download"; same flow logged in shows the download button and triggers a download.
- E2E: builder → save to library → open in `/library` → flip to PUBLIC → log out → see the item at `/catalog/{itemId}`.

### 11.4 Manual verification

- Click each builder's "Save to Library" and verify it lands in `/library` with the right source link.
- Generate an API key, copy it, run the displayed curl command against the running backend, verify a JSON list comes back.
- Hit the same endpoint 101 times in an hour, verify 101st gets 429.
- Set visibility = PUBLIC on an item, log out, browse to `/catalog`, find it.

## 12. Known limitations

1. **Per-instance rate limiting.** Cloud Run can autoscale to N instances; the in-memory bucket cache is per-instance, so the effective per-key cap is `100 * N` rather than a strict 100. Acceptable for v1; revisit if abuse appears or the user reports any single key exceeding intended cap. Migration path: replace `RateLimitFilter`'s `Caffeine` cache with a Redis-backed implementation. Schema is unaffected.
2. **No storage dedup.** A builder doc and its library snapshot are independent files in blob storage. Doubles storage cost for items kept in both places. Could be addressed by hashing content and pointing multiple `library_versions` rows at a single content blob, but adds GC complexity. Not v1.
3. **`organization_id` requirement enforced in service, not SQL.** `library_items` rows with `visibility='ORGANIZATION'` and a NULL `organization_id` would be invalid but won't be blocked at the DB layer. Service-layer validation is the only line of defense; if drift appears, add a row-level trigger.
4. **One key per user.** Devs end up wanting separate keys for CI vs. laptop; we're deferring this to keep v1 small. Schema already allows multiple keys (revoked + active history); the UI just doesn't expose multiple-active.
5. **No webhook for "new version published."** Consumers polling for changes is acceptable; webhooks are a v2 if anyone asks.
6. **Comments hidden from anonymous viewers.** Could be relaxed in v2 if the volume stays clean.

## 13. Open questions for implementation phase

(None blocking design approval. These can be resolved when the implementation plan is written.)

- Pagination style on `/catalog`: numbered vs. infinite scroll. Both are cheap; pick whichever matches existing list pages in the app.
- Whether the API base URL displayed in example code should come from a config value or be inferred from `window.location.origin`. Probably the latter.
- Should `download_count` increment for API-key downloads as well as web downloads? Default yes, but we may want a separate counter for traffic source.
