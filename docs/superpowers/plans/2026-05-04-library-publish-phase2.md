# Library Publish — Phase 2: Public Catalog UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose PUBLIC library items to anonymous visitors through `/api/public/catalog/*` (unauthenticated REST endpoints) and a Next.js `(public)` route group with `/catalog` browse and `/catalog/[itemId]` detail pages. Authenticated visitors on the same pages get download / rate / comment affordances; anonymous visitors see "Sign in to download" / "Sign in to rate" CTAs.

**Architecture:** New `PublicCatalogController` exposes a small JSON surface restricted to `visibility=PUBLIC` items, whitelisted in `SecurityConfig`. Reuses the existing `LibraryItemRepository` with a new visibility-aware FTS query (uses the GIN index added in Phase 1's V1.1 migration). Frontend `(public)` route group has its own minimal layout (no auth context); middleware allows the `/catalog` prefix without a JWT. Same backing query for both authenticated and anonymous viewers — auth state only changes the UI affordances, not the data.

**Tech Stack:** Spring Boot, JPA + Postgres tsvector for FTS, Next.js 15 App Router with route groups, vanilla Tailwind classes.

**Spec:** `docs/superpowers/specs/2026-05-04-library-publishing-design.md` §7.4.1, §8.1.

**Branch policy:** Continue on `ai-foundation`. Commit after each task. Backend builds may run autonomously per project memory.

**Phase 1 facts to reuse:**
- `LibraryItem.visibility` enum is `PRIVATE | ORGANIZATION | PUBLIC`. PUBLIC items have non-null `published_at`.
- GIN FTS index `idx_library_items_search_fts` exists on `to_tsvector('english', coalesce(title,'') || ' ' || coalesce(description,''))`.
- `LibraryItemRepository.findByItemId(String)` returns `Optional<LibraryItem>`.
- `LibraryStorageService.downloadComponent(String path)` returns `String` (current bytes-as-string pattern).
- `LibraryItem.incrementDownloadCount()` exists.
- `LibraryItemResponse.fromEntity(item)` is the standard response DTO; it now includes `visibility` and `organizationId` (added in Phase 1 Task 28).
- Frontend API client lives at `front-end/src/lib/api/library.ts`, exported as `libraryPublishApi`. Helpers `fetchJson` and `buildAuthHeaders` are private inside that file.
- `useAuth()` hook in `@/contexts/AuthContext` returns `{user, isAuthenticated, isLoading}`.

---

## Task map

| # | Task | Group |
|---|---|---|
| P1 | `PublicCatalogController` + DTOs + `SecurityConfig` whitelist | Backend |
| P2 | `LibraryService.searchPublic()` / `getPublic()` / `getPublicContent()` + FTS query | Backend |
| P3 | `PublicCatalogControllerTest` end-to-end integration test | Backend |
| P4 | Frontend public API client (no-auth) | Frontend |
| P5 | `(public)` route group + minimal layout + middleware update | Frontend |
| P6 | `/catalog` browse page (search, type filter, tag filter, sort, pagination) | Frontend |
| P7 | `/catalog/[itemId]` detail page (auth-aware CTAs) | Frontend |
| P8 | End-to-end verification | Verification |

---

## Task P1: `PublicCatalogController` + DTOs + SecurityConfig whitelist

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/PublicCatalogController.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/library/PublicItemSummary.java` (lightweight DTO for list/search results)
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/config/SecurityConfig.java`

**`PublicItemSummary` DTO** — slimmer than `LibraryItemResponse` (no creator details, no comment count). Fields:

```java
package gov.nist.oscal.tools.api.model.library;

import gov.nist.oscal.tools.api.entity.LibraryItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record PublicItemSummary(
        String itemId,
        String title,
        String description,
        String oscalType,
        List<String> tags,
        Integer currentVersionNumber,
        LocalDateTime publishedAt,
        LocalDateTime lastPublishedAt,
        Long downloadCount,
        Double averageRating,
        Long totalRatings) {

    public static PublicItemSummary fromEntity(LibraryItem item, Double averageRating, Long totalRatings) {
        return new PublicItemSummary(
            item.getItemId(),
            item.getTitle(),
            item.getDescription(),
            item.getOscalType(),
            item.getTags() == null ? List.of()
                : item.getTags().stream().map(t -> t.getName()).collect(Collectors.toList()),
            item.getCurrentVersion() != null ? item.getCurrentVersion().getVersionNumber() : null,
            item.getPublishedAt(),
            item.getLastPublishedAt(),
            item.getDownloadCount(),
            averageRating,
            totalRatings);
    }
}
```

**`PublicCatalogController`** — REST controller mapped under `/api/public/catalog`:

```java
package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryVersion;
import gov.nist.oscal.tools.api.model.library.PublicItemSummary;
import gov.nist.oscal.tools.api.service.LibraryService;
import gov.nist.oscal.tools.api.service.LibraryStorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/public/catalog")
@Tag(name = "Public Catalog",
     description = "Anonymous-readable browse + download of PUBLIC library items")
public class PublicCatalogController {

    private final LibraryService libraryService;
    private final LibraryStorageService storageService;

    @Autowired
    public PublicCatalogController(LibraryService libraryService,
                                    LibraryStorageService storageService) {
        this.libraryService = libraryService;
        this.storageService = storageService;
    }

    @GetMapping("/items")
    public ResponseEntity<Page<PublicItemSummary>> listPublic(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        size = Math.min(size, 100);
        Sort.Order order = switch (sort) {
            case "downloads" -> Sort.Order.desc("downloadCount");
            case "rating" -> Sort.Order.desc("downloadCount");  // proxy until rating sort added
            default /* "newest" */ -> Sort.Order.desc("lastPublishedAt");
        };
        var pageable = PageRequest.of(page, size, Sort.by(order));
        return ResponseEntity.ok(libraryService.searchPublic(q, type, tag, pageable));
    }

    @GetMapping("/items/{itemId}")
    public ResponseEntity<?> getPublic(@PathVariable String itemId) {
        return libraryService.getPublic(itemId)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "not found")));
    }

    @GetMapping("/items/{itemId}/content")
    public ResponseEntity<?> downloadLatest(@PathVariable String itemId) {
        return libraryService.getPublicLatestContent(itemId)
            .map(this::toFileResponse)
            .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "not found")));
    }

    @GetMapping("/items/{itemId}/versions/{versionId}/content")
    public ResponseEntity<?> downloadVersion(@PathVariable String itemId,
                                              @PathVariable String versionId) {
        return libraryService.getPublicVersionContent(itemId, versionId)
            .map(this::toFileResponse)
            .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "not found")));
    }

    private ResponseEntity<byte[]> toFileResponse(LibraryService.VersionDownload dl) {
        byte[] body = dl.content().getBytes(StandardCharsets.UTF_8);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        h.setContentDispositionFormData("attachment", dl.filename());
        h.setContentLength(body.length);
        return new ResponseEntity<>(body, h, 200);
    }
}
```

**`SecurityConfig` whitelist update** — add `/api/public/catalog/**` to the public endpoints list. Find the existing `requestMatchers(...)` chain in `SecurityConfig.java` (line ~109 per the spec), add `"/api/public/catalog/**"` to the array.

Steps:
1. Read `back-end/src/main/java/gov/nist/oscal/tools/api/config/SecurityConfig.java` to locate the public endpoints whitelist.
2. Create `PublicItemSummary.java`.
3. Create `PublicCatalogController.java` (the service methods don't exist yet — Task P2 adds them; the controller will not compile until P2 lands or you add stub methods returning empty Optionals; mark as a known interim state).
4. Add `"/api/public/catalog/**"` to the SecurityConfig whitelist.
5. **Defer** the compile check until after Task P2; in this commit, just stage the controller + SecurityConfig + DTO. Or, alternately, stub the service methods in this commit and replace them in P2. Pick one — recommend stubbing.
6. Stub `LibraryService` with the three new public methods returning empty results so the controller compiles.

Suggested stubs added in this task (to be replaced in P2):

```java
public org.springframework.data.domain.Page<gov.nist.oscal.tools.api.model.library.PublicItemSummary>
        searchPublic(String q, String type, String tag, org.springframework.data.domain.Pageable pageable) {
    return org.springframework.data.domain.Page.empty(pageable);
}

public java.util.Optional<gov.nist.oscal.tools.api.model.library.PublicItemSummary> getPublic(String itemId) {
    return java.util.Optional.empty();
}

public java.util.Optional<VersionDownload> getPublicLatestContent(String itemId) { return java.util.Optional.empty(); }
public java.util.Optional<VersionDownload> getPublicVersionContent(String itemId, String versionId) { return java.util.Optional.empty(); }

public record VersionDownload(String content, String filename, String format) {}
```

7. `mvn -q compile`. BUILD SUCCESS.
8. Commit: `feat(public-catalog): scaffold PublicCatalogController + SecurityConfig whitelist`

---

## Task P2: `LibraryService` — public query methods + FTS

**File to modify:** `back-end/src/main/java/gov/nist/oscal/tools/api/service/LibraryService.java`. Replace the four stubs from P1 with real implementations.

**Repository additions** in `LibraryItemRepository.java`:

```java
    /**
     * Public catalog search. Visibility filter pinned to PUBLIC.
     * FTS keyword applies to title+description via the GIN tsvector index;
     * type and tag are optional exact filters.
     */
    @org.springframework.data.jpa.repository.Query(value = """
        SELECT li.* FROM library_items li
        LEFT JOIN library_item_tags lit ON lit.library_item_id = li.id
        LEFT JOIN library_tags lt ON lt.id = lit.tag_id
        WHERE li.visibility = 'PUBLIC'
          AND (:q IS NULL OR :q = ''
               OR to_tsvector('english',
                  coalesce(li.title,'') || ' ' || coalesce(li.description,''))
                  @@ plainto_tsquery('english', :q))
          AND (:type IS NULL OR :type = '' OR li.oscal_type = :type)
          AND (:tag  IS NULL OR :tag  = '' OR lt.name = :tag)
        GROUP BY li.id
        """,
        countQuery = """
            SELECT COUNT(DISTINCT li.id) FROM library_items li
            LEFT JOIN library_item_tags lit ON lit.library_item_id = li.id
            LEFT JOIN library_tags lt ON lt.id = lit.tag_id
            WHERE li.visibility = 'PUBLIC'
              AND (:q IS NULL OR :q = ''
                   OR to_tsvector('english',
                      coalesce(li.title,'') || ' ' || coalesce(li.description,''))
                      @@ plainto_tsquery('english', :q))
              AND (:type IS NULL OR :type = '' OR li.oscal_type = :type)
              AND (:tag  IS NULL OR :tag  = '' OR lt.name = :tag)
            """,
        nativeQuery = true)
    org.springframework.data.domain.Page<LibraryItem> searchPublic(
        @org.springframework.data.repository.query.Param("q") String q,
        @org.springframework.data.repository.query.Param("type") String type,
        @org.springframework.data.repository.query.Param("tag") String tag,
        org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
        SELECT li FROM LibraryItem li
        WHERE li.itemId = :itemId AND li.visibility = gov.nist.oscal.tools.api.entity.Visibility.PUBLIC
        """)
    java.util.Optional<LibraryItem> findPublicByItemId(@org.springframework.data.repository.query.Param("itemId") String itemId);
```

**Service implementations** in `LibraryService.java`:

```java
    public Page<PublicItemSummary> searchPublic(String q, String type, String tag, Pageable pageable) {
        return libraryItemRepository.searchPublic(q, type, tag, pageable)
            .map(item -> {
                Double avg = libraryItemRatingRepository.averageRatingForItem(item.getId());
                Long total = libraryItemRatingRepository.countRatingsForItem(item.getId());
                return PublicItemSummary.fromEntity(item, avg, total);
            });
    }

    public Optional<PublicItemSummary> getPublic(String itemId) {
        return libraryItemRepository.findPublicByItemId(itemId)
            .map(item -> {
                Double avg = libraryItemRatingRepository.averageRatingForItem(item.getId());
                Long total = libraryItemRatingRepository.countRatingsForItem(item.getId());
                // increment view count on detail load
                item.incrementViewCount();
                libraryItemRepository.save(item);
                return PublicItemSummary.fromEntity(item, avg, total);
            });
    }

    public Optional<VersionDownload> getPublicLatestContent(String itemId) {
        return libraryItemRepository.findPublicByItemId(itemId)
            .filter(item -> item.getCurrentVersion() != null)
            .map(item -> {
                LibraryVersion v = item.getCurrentVersion();
                String content = storageService.downloadComponent(v.getFilePath());
                item.incrementDownloadCount();
                libraryItemRepository.save(item);
                return new VersionDownload(content == null ? "" : content,
                                            v.getFileName(), v.getFormat());
            });
    }

    public Optional<VersionDownload> getPublicVersionContent(String itemId, String versionId) {
        return libraryItemRepository.findPublicByItemId(itemId)
            .flatMap(item -> item.getVersions().stream()
                .filter(v -> versionId.equals(v.getVersionId()))
                .findFirst()
                .map(v -> {
                    String content = storageService.downloadComponent(v.getFilePath());
                    item.incrementDownloadCount();
                    libraryItemRepository.save(item);
                    return new VersionDownload(content == null ? "" : content,
                                                v.getFileName(), v.getFormat());
                }));
    }
```

The methods `averageRatingForItem` and `countRatingsForItem` may need to be added to `LibraryItemRatingRepository` if not present — read the repository first to confirm.

Steps:
1. Read `LibraryItemRatingRepository.java` to find existing methods. If `averageRatingForItem` / `countRatingsForItem` are not present, add them with simple JPQL `SELECT AVG(r.rating)` / `COUNT(r)`.
2. Add the repository query methods.
3. Replace the stubs in `LibraryService`.
4. `mvn -q compile`. BUILD SUCCESS.
5. Manual sanity check: restart backend (`./stop.sh && ./dev.sh`), `curl -i http://localhost:8090/api/public/catalog/items` → 200 with empty page (no public items yet).
6. Commit: `feat(public-catalog): real public search + content endpoints with FTS`

---

## Task P3: Backend integration test

**File:** `back-end/src/test/java/gov/nist/oscal/tools/api/controller/PublicCatalogControllerTest.java`

Test cases (all without authentication — anonymous access):

1. `listPublicReturnsOnlyPublicItems`: seed 3 items (one PRIVATE, one ORG, one PUBLIC). GET `/api/public/catalog/items` returns one item — the PUBLIC one.
2. `searchByKeywordMatchesTitleAndDescription`: seed two PUBLIC items with different titles. GET with `?q=` matching one returns only that one.
3. `filterByTypeRestrictsResults`: seed PUBLIC items of different `oscalType`. GET with `?type=catalog` returns only catalogs.
4. `getPublicByIdReturns200ForPublic404ForPrivate`: own + others' PRIVATE → 404; others' PUBLIC → 200.
5. `downloadIncrementsDownloadCount`: GET content endpoint, verify `download_count` increments by 1.
6. `viewIncrementsViewCount`: GET detail endpoint, verify `view_count` increments by 1.
7. `unauthenticatedAccessAllowed`: confirm no `Authorization` header is required for any of the above (the matrix above implicitly tests this; one explicit assertion that `MockMvc.perform(get(...))` without auth header returns 200 is fine).

Pattern after `LibraryVisibilityControllerTest` (`@SpringBootTest` + `@AutoConfigureMockMvc` + `@Transactional`).
**Important:** the security whitelist for `/api/public/catalog/**` may behave differently in `@SpringBootTest` if `@MockitoBean` is present. Test without `@WithMockUser` for anonymous cases.

`@MockitoBean` for `LibraryStorageService` to avoid blob backend setup; stub `downloadComponent(...)` to return a fixed string.

Steps:
1. Read existing controller tests to confirm `@MockitoBean` pattern.
2. Write the test.
3. `mvn -q test -Dtest=PublicCatalogControllerTest`. ≥7 tests pass.
4. Commit: `test(public-catalog): integration matrix for anonymous browse + download`

---

## Task P4: Frontend public API client

**File:** `front-end/src/lib/api/public-catalog.ts` (new)

```typescript
const PUBLIC_BASE = "/api/public/catalog";

export interface PublicItemSummary {
  itemId: string;
  title: string;
  description: string | null;
  oscalType: string;
  tags: string[];
  currentVersionNumber: number | null;
  publishedAt: string | null;
  lastPublishedAt: string | null;
  downloadCount: number | null;
  averageRating: number | null;
  totalRatings: number | null;
}

export interface PublicCatalogPage {
  content: PublicItemSummary[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

async function fetchPublicJson(url: string): Promise<any> {
  const r = await fetch(url, { headers: { Accept: "application/json" } });
  if (!r.ok) {
    throw new Error(`public catalog request failed: ${r.status}`);
  }
  return r.json();
}

export const publicCatalogApi = {
  list: (params: {
    q?: string;
    type?: string;
    tag?: string;
    sort?: "newest" | "downloads" | "rating";
    page?: number;
    size?: number;
  } = {}): Promise<PublicCatalogPage> => {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== "" && v !== null) qs.set(k, String(v));
    });
    const path = qs.toString() ? `${PUBLIC_BASE}/items?${qs}` : `${PUBLIC_BASE}/items`;
    return fetchPublicJson(path);
  },
  get: (itemId: string): Promise<PublicItemSummary> =>
    fetchPublicJson(`${PUBLIC_BASE}/items/${encodeURIComponent(itemId)}`),

  contentUrl: (itemId: string): string =>
    `${PUBLIC_BASE}/items/${encodeURIComponent(itemId)}/content`,

  versionContentUrl: (itemId: string, versionId: string): string =>
    `${PUBLIC_BASE}/items/${encodeURIComponent(itemId)}/versions/${encodeURIComponent(versionId)}/content`,
};
```

Steps:
1. Create the file.
2. `cd front-end && npx tsc --noEmit`. Clean.
3. Commit: `feat(public-catalog): frontend public API client`

---

## Task P5: `(public)` Next.js route group + middleware update

**Files:**
- Create: `front-end/src/app/(public)/layout.tsx`
- Modify: `front-end/src/middleware.ts` (or wherever auth-route enforcement lives)

**Layout** — minimal header, no auth context provider, public-friendly footer:

```tsx
import Link from "next/link";

export default function PublicLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex flex-col bg-white">
      <header className="border-b bg-white">
        <div className="container mx-auto px-4 py-3 flex items-center justify-between">
          <Link href="/catalog" className="text-lg font-semibold text-slate-900">
            OSCAL Hub — Public Catalog
          </Link>
          <Link href="/login"
                className="text-sm text-blue-600 hover:underline">
            Sign in
          </Link>
        </div>
      </header>
      <main className="flex-1 container mx-auto px-4 py-6">{children}</main>
      <footer className="border-t bg-slate-50 py-4 text-center text-xs text-slate-500">
        Public OSCAL content. Sign in to download or rate.
      </footer>
    </div>
  );
}
```

**Middleware** — examine `front-end/src/middleware.ts`. If it currently redirects unauthenticated users away from any non-`/login` route, add `/catalog` to the unauthenticated-allowlist. If middleware uses a `publicRoutes` array, append `/catalog`. If pattern matching is path-based, add a regex alternative for `/catalog` and `/catalog/(.*)`.

Steps:
1. Read `front-end/src/middleware.ts` and look for the auth gate.
2. Add `/catalog` (and the dynamic `/catalog/[itemId]`) to whatever public-route list it uses. Common pattern: `const publicPaths = ['/login', '/register', '/forgot-password', '/catalog'];`
3. Create the `(public)` layout file.
4. `npx tsc --noEmit`. Clean.
5. **Don't restart frontend yet** — pages don't exist. Just verify compile.
6. Commit: `feat(public-catalog): (public) route group + middleware allowlist`

---

## Task P6: `/catalog` browse page

**File:** `front-end/src/app/(public)/catalog/page.tsx`

Implementation:

```tsx
"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { publicCatalogApi, type PublicItemSummary, type PublicCatalogPage } from "@/lib/api/public-catalog";

export default function PublicCatalogPage() {
  const [items, setItems] = useState<PublicItemSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [q, setQ] = useState("");
  const [type, setType] = useState("");
  const [tag, setTag] = useState("");
  const [sort, setSort] = useState<"newest" | "downloads" | "rating">("newest");
  const [page, setPage] = useState(0);
  const size = 24;

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    publicCatalogApi.list({ q, type, tag, sort, page, size })
      .then((data: PublicCatalogPage) => {
        if (cancelled) return;
        setItems(data.content);
        setTotal(data.totalElements);
        setError(null);
      })
      .catch((e) => { if (!cancelled) setError(String(e)); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [q, type, tag, sort, page]);

  const types = ["catalog", "profile", "ssp", "ap", "ar", "poam", "component-definition"];

  return (
    <div>
      <div className="flex flex-col md:flex-row gap-3 items-start md:items-center mb-6">
        <input
          className="border rounded px-3 py-2 w-full md:w-72"
          placeholder="Search title or description"
          value={q}
          onChange={(e) => { setPage(0); setQ(e.target.value); }}
        />
        <select className="border rounded px-3 py-2" value={type}
                onChange={(e) => { setPage(0); setType(e.target.value); }}>
          <option value="">All types</option>
          {types.map(t => <option key={t} value={t}>{t}</option>)}
        </select>
        <input
          className="border rounded px-3 py-2 w-full md:w-40"
          placeholder="Tag filter"
          value={tag}
          onChange={(e) => { setPage(0); setTag(e.target.value); }}
        />
        <select className="border rounded px-3 py-2" value={sort}
                onChange={(e) => { setPage(0); setSort(e.target.value as typeof sort); }}>
          <option value="newest">Newest</option>
          <option value="downloads">Most downloaded</option>
          <option value="rating">Top rated</option>
        </select>
      </div>

      {loading && <p className="text-sm text-slate-500">Loading…</p>}
      {error && <p className="text-sm text-red-600">Error: {error}</p>}
      {!loading && !error && items.length === 0 && (
        <p className="text-sm text-slate-500">No public items match these filters.</p>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {items.map(it => (
          <Link key={it.itemId} href={`/catalog/${it.itemId}`}
                className="block border rounded-lg p-4 hover:shadow transition">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs uppercase tracking-wide bg-slate-100 px-2 py-0.5 rounded">{it.oscalType}</span>
              {it.averageRating != null && it.totalRatings != null && it.totalRatings > 0 && (
                <span className="text-xs text-slate-600">★ {it.averageRating.toFixed(1)} ({it.totalRatings})</span>
              )}
            </div>
            <h3 className="font-semibold text-base mb-1 line-clamp-2">{it.title}</h3>
            {it.description && (
              <p className="text-sm text-slate-600 line-clamp-3 mb-2">{it.description}</p>
            )}
            <div className="flex flex-wrap gap-1 mb-2">
              {it.tags.slice(0, 4).map(t => (
                <span key={t} className="text-xs bg-blue-50 text-blue-700 px-1.5 py-0.5 rounded">{t}</span>
              ))}
            </div>
            <div className="text-xs text-slate-500">
              {it.downloadCount ?? 0} downloads · v{it.currentVersionNumber ?? "—"}
            </div>
          </Link>
        ))}
      </div>

      {total > size && (
        <div className="flex justify-center gap-2 mt-6">
          <button disabled={page === 0}
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  className="px-3 py-1 border rounded disabled:opacity-50">Previous</button>
          <span className="px-3 py-1 text-sm">Page {page + 1} / {Math.ceil(total / size)}</span>
          <button disabled={(page + 1) * size >= total}
                  onClick={() => setPage(p => p + 1)}
                  className="px-3 py-1 border rounded disabled:opacity-50">Next</button>
        </div>
      )}
    </div>
  );
}
```

Steps:
1. Create the file.
2. `npx tsc --noEmit`. Clean.
3. Commit: `feat(public-catalog): /catalog browse page`

---

## Task P7: `/catalog/[itemId]` detail page

**File:** `front-end/src/app/(public)/catalog/[itemId]/page.tsx`

```tsx
"use client";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { publicCatalogApi, type PublicItemSummary } from "@/lib/api/public-catalog";

export default function PublicCatalogDetailPage() {
  const params = useParams<{ itemId: string }>();
  const itemId = params.itemId;
  const [item, setItem] = useState<PublicItemSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Auth state — read directly from localStorage rather than useAuth() because
  // this page lives in a (public) route group with no auth context provider.
  const [hasToken, setHasToken] = useState(false);
  useEffect(() => {
    if (typeof window !== "undefined") {
      setHasToken(!!localStorage.getItem("token"));
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    publicCatalogApi.get(itemId)
      .then(data => { if (!cancelled) setItem(data); })
      .catch(e => { if (!cancelled) setError(String(e)); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [itemId]);

  if (loading) return <p className="text-sm text-slate-500">Loading…</p>;
  if (error || !item) return (
    <div>
      <p className="text-sm text-red-600">Item not found.</p>
      <Link href="/catalog" className="text-sm text-blue-600 hover:underline">← Back to catalog</Link>
    </div>
  );

  return (
    <div className="max-w-3xl">
      <Link href="/catalog" className="text-sm text-blue-600 hover:underline">← Back to catalog</Link>

      <div className="flex items-center gap-2 mt-4 mb-2">
        <span className="text-xs uppercase tracking-wide bg-slate-100 px-2 py-0.5 rounded">{item.oscalType}</span>
        <span className="text-xs text-slate-500">v{item.currentVersionNumber ?? "—"}</span>
      </div>

      <h1 className="text-2xl font-semibold mb-3">{item.title}</h1>

      {item.description && (
        <p className="text-sm text-slate-700 mb-4 whitespace-pre-line">{item.description}</p>
      )}

      <div className="flex flex-wrap gap-1 mb-4">
        {item.tags.map(t => (
          <span key={t} className="text-xs bg-blue-50 text-blue-700 px-1.5 py-0.5 rounded">{t}</span>
        ))}
      </div>

      <dl className="grid grid-cols-2 gap-2 text-sm mb-6">
        <dt className="text-slate-500">Last published</dt>
        <dd>{item.lastPublishedAt ? new Date(item.lastPublishedAt).toLocaleString() : "—"}</dd>
        <dt className="text-slate-500">Downloads</dt>
        <dd>{item.downloadCount ?? 0}</dd>
        {item.totalRatings != null && item.totalRatings > 0 && (
          <>
            <dt className="text-slate-500">Average rating</dt>
            <dd>★ {item.averageRating?.toFixed(1)} ({item.totalRatings} ratings)</dd>
          </>
        )}
      </dl>

      <div className="flex gap-2">
        {hasToken ? (
          <a
            href={publicCatalogApi.contentUrl(item.itemId)}
            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
            download>
            Download
          </a>
        ) : (
          <Link href="/login"
                className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
            Sign in to download
          </Link>
        )}
        {hasToken ? (
          <Link href={`/library/${item.itemId}`}
                className="px-4 py-2 border rounded hover:bg-slate-50">
            Rate / comment
          </Link>
        ) : (
          <Link href="/login"
                className="px-4 py-2 border rounded hover:bg-slate-50">
            Sign in to rate
          </Link>
        )}
      </div>
    </div>
  );
}
```

**Note on download:** the `<a download>` approach hits the public endpoint directly. The endpoint is unauthenticated and accessible to anyone, so the "Sign in to download" gating is purely UI-side, satisfying the spec's "logged-in users can download" affordance. The spec is silent on whether downloads should be technically blocked for anonymous visitors; the current approach matches the design (public endpoint, gated CTA). If actual blocking is wanted, we'd need a separate authenticated download path.

Steps:
1. Create the file.
2. `npx tsc --noEmit`. Clean.
3. Commit: `feat(public-catalog): /catalog/[itemId] detail page (auth-aware CTAs)`

---

## Task P8: End-to-end verification

Steps:
1. Restart backend + frontend: `./stop.sh && ./dev.sh`. Confirm both come up.
2. **Anonymous browse:** open `http://localhost:3010/catalog` in a private/incognito browser. Page should load without prompting login. If no public items exist yet, you'll see "No public items match these filters."
3. **Seed a public item:** as an authenticated user (admin), build a catalog → Save to Library with visibility=PUBLIC. (Or take an existing library item and PATCH visibility to PUBLIC.)
4. **Refresh anonymous catalog page:** the public item should appear.
5. **Anonymous detail:** click into the item. Detail page shows title/description/tags/version/download count. CTAs read "Sign in to download" and "Sign in to rate".
6. **Authenticated detail:** sign in (in same browser), navigate back to `/catalog/{itemId}`. CTAs change to "Download" (button hits the API and saves the file) and "Rate / comment" (links to `/library/{itemId}`).
7. **Audit verification:**
   ```bash
   docker exec oscal-postgres-dev psql -U oscal_user -d oscal_dev -c \
     "SELECT view_count, download_count FROM library_items WHERE visibility='PUBLIC';"
   ```
   Counts should reflect interactions.
8. Commit (if any tweaks needed): `fix(public-catalog): <whatever>`

---

## Known limitations

- Downloads are not technically gated for anonymous users — the public endpoint is open. The "Sign in to download" CTA is UI-only. This matches the spec's intent; tighten if needed in v2.
- Ratings/comments still require navigating to the authenticated `/library/{itemId}` page for the rating UI. A future polish pass could embed rating-write inline on the public detail page for authenticated viewers.
- The `(public)` layout doesn't share styling with the authenticated app; it's a deliberately lighter shell.
- FTS uses Postgres `plainto_tsquery` which interprets the query loosely (lowercases, strips operators). No advanced query syntax in v1.
- `download_count` is incremented on every byte fetch — including by web-crawler / browser-prefetch. Acceptable for v1; rate-limit if abuse appears.
