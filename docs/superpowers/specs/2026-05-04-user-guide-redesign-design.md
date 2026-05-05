# User Guide Redesign — Design Doc

**Date:** 2026-05-04
**Status:** Approved
**Branch:** `ai-foundation` (work stays on this branch, no new branches)

## Problem

The current user guide at `/guide` is a single, long, scroll-jacked TSX page. It documents about ten features. The product now has roughly thirty distinct user-facing features — AI wizards, the Build suite for all seven OSCAL types, public catalog, library visibility (Private / Org / Public), artifacts, rules explorer, org-admin features, super-admin features, MFA, account-recovery flows. The existing guide also has stale content (port `8080` instead of `8090`, "OSCAL UX" branding instead of "OSCAL Hub", `cd front-end && ./dev.sh` which is wrong).

We also lack any in-app help affordance. A user on `/library` has no fast path to documentation about the page they are looking at.

## Goals

1. Rewrite the guide so every feature has a documentation page.
2. Switch from a single long page to a multi-page layout with a left-rail Table of Contents and main-column content. Each guide topic gets its own URL.
3. Add a small help icon to every authenticated feature page that opens the relevant guide page in a new tab.
4. Fix stale content during the rewrite.

## Non-goals (v1)

- Client-side full-text search across the guide.
- A CMS-backed editing flow. Authoring stays as MDX files in the repo, edited through pull requests.
- A light-mode pass on the guide. Guide inherits the app-wide dark theme.
- Screenshots, videos, or animated illustrations. Text plus code blocks only. A `<Screenshot>` placeholder component is added so we can drop images in later without re-architecting.
- Internationalization. English only.
- Auth-gating any guide page. Admin docs are publicly readable.

## Architecture

### Route structure

A new shared layout at `front-end/src/app/guide/layout.tsx` wraps every guide page with a sticky left sidebar (`<DocSidebar>`) and a main content column. Below 768px the sidebar collapses to a hamburger drawer.

The guide is publicly accessible — middleware allowlists the entire `/guide/**` tree (already the case for the existing guide, just verify the new paths are covered). Implementation step: confirm the middleware matcher in `front-end/src/middleware.ts` does not block `/guide/**`. If it does, add an allow rule.

The top of `/guide` redirects to `/guide/getting-started/overview` via a `page.tsx` that calls `redirect('/guide/getting-started/overview')` from `next/navigation`.

Every guide topic is its own route. The full route list:

```
/guide                                     redirect → getting-started/overview

/guide/getting-started/overview
/guide/getting-started/requirements
/guide/getting-started/accessibility
/guide/getting-started/sign-in-and-mfa

/guide/tools/validate
/guide/tools/convert
/guide/tools/resolve
/guide/tools/batch
/guide/tools/visualize
/guide/tools/history

/guide/build/overview
/guide/build/catalog
/guide/build/profile
/guide/build/component
/guide/build/ssp
/guide/build/assessment-plan
/guide/build/assessment-results
/guide/build/poam

/guide/ai/overview
/guide/ai/catalog-wizard
/guide/ai/component-wizard

/guide/library/overview
/guide/library/visibility
/guide/library/save-to-library          (cross-listed under Build group too)
/guide/library/public-catalog
/guide/library/artifacts

/guide/authorizations/overview
/guide/authorizations/templates
/guide/authorizations/create

/guide/account/profile
/guide/account/password
/guide/account/mfa-setup
/guide/account/service-tokens
/guide/account/accept-invite
/guide/account/request-access

/guide/org-admin/overview
/guide/org-admin/users
/guide/org-admin/invitations
/guide/org-admin/access-requests
/guide/org-admin/ai-settings
/guide/org-admin/ai-analytics
/guide/org-admin/analytics

/guide/admin/overview
/guide/admin/users
/guide/admin/organizations
/guide/admin/health
/guide/admin/logs
/guide/admin/analytics
/guide/admin/security
/guide/admin/security-policy

/guide/reference/model-types
/guide/reference/rules
/guide/reference/keyboard-shortcuts
/guide/reference/api-automation
/guide/reference/deployment/local
/guide/reference/deployment/cli
/guide/reference/deployment/aws
/guide/reference/deployment/azure
/guide/reference/troubleshooting
```

That is forty-six leaf pages.

### Migration of existing guide pages

- `front-end/src/app/guide/page.tsx` (the current single-page guide) is deleted. Its content is split across the new pages, with stale parts rewritten.
- `front-end/src/app/guide/automation/page.tsx` moves to `front-end/src/app/guide/reference/api-automation/page.mdx`. Convert TSX to MDX during the move; layout from `/guide/layout.tsx` provides the sidebar.
- `front-end/src/app/guide/rules/page.tsx` moves to `front-end/src/app/guide/reference/rules/page.mdx`.
- `front-end/src/app/guide/deployment/{aws,azure,cli,local}/page.tsx` move to `front-end/src/app/guide/reference/deployment/{aws,azure,cli,local}/page.mdx`.

The old paths are not preserved with redirects in v1 — there are no known external links to them.

### Sidebar (TOC) structure

`<DocSidebar>` reads a static configuration file:

```ts
// front-end/src/lib/guide-toc.ts
export type TocEntry = { label: string; slug: string };
export type TocGroup = { label: string; entries: TocEntry[] };
export const TOC: TocGroup[] = [
  {
    label: 'Getting Started',
    entries: [
      { label: 'Overview', slug: 'getting-started/overview' },
      { label: 'Requirements', slug: 'getting-started/requirements' },
      // ...
    ],
  },
  // ... eight more groups
];
```

Behavior:

- Active group expanded; other groups collapsed.
- Expansion state persisted to `localStorage` under key `guide.toc.expanded`.
- A page slug may appear under multiple groups (cross-listing). For example, `library/save-to-library` shows up under both `Library & Sharing` and `Build`. The actual MDX file lives once.
- The active link is highlighted by comparing `usePathname()` to the entry's slug.

### Content authoring (MDX)

Each guide page is a co-located `page.mdx` file under its route directory. Frontmatter at the top:

```mdx
---
title: Library Visibility
description: Private, organization, and public sharing modes.
lastUpdated: 2026-05-04
---
```

The shared layout reads `title` for the `<title>` tag and the page heading, `description` for the meta description, and `lastUpdated` for a small "Last updated" line under the heading.

Custom MDX components, registered in `front-end/mdx-components.tsx`:

- `<Callout type="info" | "warn" | "danger">` — colored note boxes.
- `<Steps>` and `<Step>` — numbered procedural lists.
- `<Screenshot src=… alt=… />` — placeholder that renders nothing in v1 if `src` is absent; image otherwise.
- Standard markdown elements (`h1`–`h4`, `p`, `ul`, `ol`, `li`, `code`, `pre`, `a`) get styled via the same component map so we don't depend on `@tailwindcss/typography`.

### MDX setup (one-time infra)

Add to `front-end/`:

- npm packages: `@next/mdx`, `@mdx-js/loader`, `@mdx-js/react`, `@types/mdx`, `rehype-slug`, `rehype-autolink-headings`.
- `next.config.ts`: wrap the existing config with `withMDX()`. Enable `mdx` extension in `pageExtensions`.
- `front-end/mdx-components.tsx`: the component map described above.

### Help-icon system

A typed centralized map plus a small reusable component.

```ts
// front-end/src/lib/help-targets.ts
export const HELP_TARGETS = {
  // Tools
  validate: 'tools/validate',
  convert: 'tools/convert',
  resolve: 'tools/resolve',
  batch: 'tools/batch',
  visualize: 'tools/visualize',
  history: 'tools/history',

  // Library & catalog
  library: 'library/overview',
  'library-visibility': 'library/visibility',
  'public-catalog': 'library/public-catalog',
  artifacts: 'library/artifacts',

  // Build (most builders are tabs within /build, so only two app-page slugs;
  // the per-OSCAL-type guide pages are reachable via the sidebar)
  build: 'build/overview',
  'build-component': 'build/component',

  // AI
  'ai-wizard': 'ai/overview',
  'ai-catalog': 'ai/catalog-wizard',
  'ai-component': 'ai/component-wizard',

  // Authorizations
  authorizations: 'authorizations/overview',
  'authorization-template': 'authorizations/templates',
  'authorization-create': 'authorizations/create',

  // Account
  profile: 'account/profile',
  'change-password': 'account/password',
  'mfa-setup': 'account/mfa-setup',
  'service-tokens': 'account/service-tokens',
  'request-access': 'account/request-access',

  // Org admin
  'org-admin': 'org-admin/overview',
  'org-admin-users': 'org-admin/users',
  'org-admin-invitations': 'org-admin/invitations',
  'org-admin-requests': 'org-admin/access-requests',
  'org-admin-ai-settings': 'org-admin/ai-settings',
  'org-admin-ai-analytics': 'org-admin/ai-analytics',
  'org-admin-analytics': 'org-admin/analytics',

  // Super admin
  admin: 'admin/overview',
  'admin-users': 'admin/users',
  'admin-organizations': 'admin/organizations',
  'admin-health': 'admin/health',
  'admin-logs': 'admin/logs',
  'admin-analytics': 'admin/analytics',
  'admin-security': 'admin/security',
  'admin-security-policy': 'admin/security-policy',

  // Reference
  rules: 'reference/rules',
} as const;

export type HelpSlug = keyof typeof HELP_TARGETS;
```

```tsx
// front-end/src/components/HelpButton.tsx
import { HelpCircle } from 'lucide-react';
import { HELP_TARGETS, HelpSlug } from '@/lib/help-targets';

export function HelpButton({ slug }: { slug: HelpSlug }) {
  return (
    <a
      href={`/guide/${HELP_TARGETS[slug]}`}
      target="_blank"
      rel="noopener noreferrer"
      aria-label="Open help for this page in a new tab"
      className="inline-flex h-8 w-8 items-center justify-center rounded-full text-muted-foreground hover:bg-muted hover:text-foreground transition-colors focus:outline-none focus:ring-2 focus:ring-ring"
    >
      <HelpCircle className="h-5 w-5" aria-hidden="true" />
    </a>
  );
}
```

Pages place the button next to the page title:

```tsx
<div className="flex items-center gap-2 mb-6">
  <h1 className="text-3xl font-bold">Library</h1>
  <HelpButton slug="library" />
</div>
```

The `HelpSlug` union type means TypeScript catches any typo at the call site. Adding a new help target is three steps: (1) add an entry to `HELP_TARGETS`, (2) write the MDX file at the matching path under `app/guide/`, (3) add `<HelpButton slug="…">` to the relevant app page.

### App pages that receive a HelpButton

Roughly thirty pages. Concrete list:

- `/library` (slug=`library`), `/library/[itemId]` (slug=`library`), `/catalog` (slug=`public-catalog`), `/catalog/[itemId]` (slug=`public-catalog`)
- `/validate`, `/convert`, `/resolve`, `/batch`, `/visualize`, `/history`
- `/build` (slug=`build`), `/build/component/[componentId]` (slug=`build-component`). Other OSCAL-type builders run as tabs/wizards inside `/build`; their guide pages exist for sidebar navigation but no in-app help icon points at them. If the build wizards later get their own routes, add slugs and icons then.
- `/ai/wizard`, `/ai/wizard/catalog`, `/ai/wizard/component_def`
- `/authorizations`, `/authorizations/template/[templateId]`, `/authorizations/authorization/[authorizationId]`
- `/profile`, `/change-password`, `/mfa-setup`, `/request-access`
- `/artifacts`, `/artifacts/[artifactId]`
- `/rules`
- `/admin`, `/admin/users`, `/admin/organizations`, `/admin/health`, `/admin/logs`, `/admin/analytics`, `/admin/security`, `/admin/security-policy`
- `/org-admin`, `/org-admin/users`, `/org-admin/invitations`, `/org-admin/requests`, `/org-admin/ai-settings`, `/org-admin/ai-analytics`, `/org-admin/analytics`

Skipped: `/login`, `/mfa-verify`, `/accept-invite`, `/select-organization`, `/license`, `/guide/*`.

## Stale content fixes during rewrite

- `localhost:8080` → `localhost:8090` and `localhost:3000` → `localhost:3010` in dev-mode references. Production references stay at 8080 / 3000 with a note.
- `cd front-end && ./dev.sh` → `./dev.sh` from project root.
- "OSCAL UX" → "OSCAL Hub" everywhere.
- Library docs rewritten end-to-end to cover Private / Organization / Public visibility, the Save-to-Library buttons in the builders, the publish workflow, and the public catalog browse experience.
- All cURL examples re-checked against current controller paths and response shapes.

## Testing & verification

- `npm run typecheck` (front-end) passes — `HelpSlug` union catches typos in `<HelpButton slug=…>` calls.
- `npm run lint` passes.
- `npm run build` succeeds with the new MDX configuration.
- Manual smoke: visit each top-level group's first page, verify sidebar highlights the right entry, verify the page renders without console errors. Click a `<HelpButton>` from a sample feature page (e.g., `/library`) and verify it opens `/guide/library/overview` in a new tab.
- Mobile smoke: resize to <768px on `/guide/tools/validate`, verify the sidebar collapses to a drawer that opens on hamburger tap.

## Out of scope, explicitly

- A guide-wide search box. Add later (Pagefind, MiniSearch, or Algolia DocSearch) if user feedback warrants it.
- Per-page mini-TOC right rail (`<h2>` outline). Optional. Skip if behind on time.
- Redirect rules from old anchors (`/guide#library`) to new pages. No external traffic to redirect.
- Versioning the guide (e.g., for release branches). Single canonical version on `main`.

## Implementation order (high-level)

1. MDX setup (packages, `next.config.ts`, `mdx-components.tsx`) and a single proof-of-concept MDX page.
2. Shared `/guide/layout.tsx` with `<DocSidebar>` and `<TOC>` config skeleton.
3. `<HelpButton>` component and `help-targets.ts` map (entries added incrementally as pages land).
4. Migrate existing guide content (the long single page split into the new structure; existing automation/rules/deployment subpages converted to MDX).
5. Write the new pages for features not previously documented.
6. Add `<HelpButton>` to each app page (one PR-sized batch).
7. Stale-content audit pass.
8. Manual smoke and typecheck/lint/build verification.

Detailed step-by-step is the job of the implementation plan, not this design doc.
