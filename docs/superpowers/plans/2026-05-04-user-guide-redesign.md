# User Guide Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the single-page `/guide` with a page-per-topic MDX docs site (sidebar TOC + content) covering all current features, and add a typed `<HelpButton>` to every major app page that opens the relevant guide page in a new tab.

**Architecture:** Shared `app/guide/layout.tsx` renders a sticky `<DocSidebar>` (driven by a static TOC config) + main column. Guide pages are MDX files (`page.mdx`) co-located under `app/guide/<group>/<slug>/`. A typed `HELP_TARGETS` map plus a `<HelpButton slug=…>` component routes from app pages to guide pages.

**Tech Stack:** Next.js 16, React 19, MDX (`@next/mdx`), shadcn/ui, Tailwind v4, Vitest + Testing Library.

**Branch:** `ai-foundation` (do not switch branches; user requested all work stay on this branch).

**Spec:** [`docs/superpowers/specs/2026-05-04-user-guide-redesign-design.md`](../specs/2026-05-04-user-guide-redesign-design.md)

---

## File Structure

### New files

| Path | Responsibility |
|---|---|
| `front-end/mdx-components.tsx` | Maps markdown elements + custom MDX components |
| `front-end/src/lib/guide-toc.ts` | Static TOC configuration consumed by the sidebar |
| `front-end/src/lib/help-targets.ts` | Typed `HELP_TARGETS` map + `HelpSlug` union |
| `front-end/src/components/HelpButton.tsx` | Help-icon component for app page headers |
| `front-end/src/components/guide/DocSidebar.tsx` | Sidebar (desktop + mobile drawer) |
| `front-end/src/components/guide/Callout.tsx` | MDX callout box (info / warn / danger) |
| `front-end/src/components/guide/Steps.tsx` | Numbered procedure list wrapper |
| `front-end/src/components/guide/Step.tsx` | Single step within `<Steps>` |
| `front-end/src/components/guide/Screenshot.tsx` | Image placeholder (renders nothing if `src` empty) |
| `front-end/src/app/guide/layout.tsx` | Shared layout: sidebar + main column |
| `front-end/src/app/guide/page.tsx` | Redirect to `/guide/getting-started/overview` |
| `front-end/src/app/guide/<group>/<slug>/page.mdx` | 46 content pages (see spec route list) |

### Modified files

| Path | Change |
|---|---|
| `front-end/next.config.ts` | Wrap with `withMDX()`; add `mdx` to `pageExtensions` |
| `front-end/package.json` | Add MDX deps |
| `front-end/src/app/guide/automation/page.tsx` | Delete (moved to `reference/api-automation/page.mdx`) |
| `front-end/src/app/guide/rules/page.tsx` | Delete (moved to `reference/rules/page.mdx`) |
| `front-end/src/app/guide/deployment/{aws,azure,cli,local}/page.tsx` | Delete (moved under `reference/deployment/*`) |
| Old `front-end/src/app/guide/page.tsx` | Replace with redirect (content split into new pages) |
| ~30 app pages (`/library`, `/validate`, etc.) | Add `<HelpButton slug=…>` next to page heading |

---

## Conventions used in this plan

- **All commands** assume current working directory is `front-end/` unless noted otherwise.
- **Test runner:** `npm test -- --run <pattern>` runs Vitest non-interactively.
- **Build verification:** `npm run typecheck` is not a script; run `npx tsc --noEmit` instead. `npm run lint` and `npm run build` exist.
- **MDX content tasks** don't have unit tests (you can't TDD prose). Verification is "page renders without error in dev mode" plus `npx tsc --noEmit` passing. The infrastructure tasks DO use TDD.
- **Commits** at the end of every task, even small ones, to keep the branch's history readable.

---

## Phase 1 — MDX infrastructure

### Task 1: Install MDX dependencies and configure Next.js

**Files:**
- Modify: `front-end/package.json`
- Modify: `front-end/next.config.ts`

- [ ] **Step 1: Install MDX packages**

```bash
cd front-end
npm install @next/mdx @mdx-js/loader @mdx-js/react @types/mdx rehype-slug rehype-autolink-headings
```

- [ ] **Step 2: Wrap next.config.ts with withMDX**

Replace the entire contents of `front-end/next.config.ts` with:

```ts
import type { NextConfig } from "next";
import createMDX from "@next/mdx";
import rehypeSlug from "rehype-slug";
import rehypeAutolinkHeadings from "rehype-autolink-headings";

const nextConfig: NextConfig = {
  output: "standalone",
  pageExtensions: ["ts", "tsx", "md", "mdx"],
};

const withMDX = createMDX({
  options: {
    remarkPlugins: [],
    rehypePlugins: [rehypeSlug, [rehypeAutolinkHeadings, { behavior: "wrap" }]],
  },
});

export default withMDX(nextConfig);
```

- [ ] **Step 3: Verify build still works**

```bash
npm run build
```

Expected: build succeeds (it'll show warnings about no MDX content yet — that's fine).

- [ ] **Step 4: Commit**

```bash
git add package.json package-lock.json next.config.ts
git commit -m "feat(guide): add MDX support via @next/mdx with rehype-slug"
```

---

### Task 2: Create custom MDX components

**Files:**
- Create: `front-end/src/components/guide/Callout.tsx`
- Create: `front-end/src/components/guide/Steps.tsx`
- Create: `front-end/src/components/guide/Step.tsx`
- Create: `front-end/src/components/guide/Screenshot.tsx`
- Create: `front-end/src/components/guide/__tests__/Callout.test.tsx`

- [ ] **Step 1: Write the failing Callout test**

Create `front-end/src/components/guide/__tests__/Callout.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { Callout } from '../Callout';

describe('Callout', () => {
  it('renders children with role=note', () => {
    render(<Callout type="info">Hello world</Callout>);
    const note = screen.getByRole('note');
    expect(note).toHaveTextContent('Hello world');
  });

  it('applies the correct class for type=warn', () => {
    render(<Callout type="warn">Warning text</Callout>);
    const note = screen.getByRole('note');
    expect(note.className).toMatch(/warn/);
  });

  it('applies the correct class for type=danger', () => {
    render(<Callout type="danger">Danger text</Callout>);
    const note = screen.getByRole('note');
    expect(note.className).toMatch(/danger/);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

```bash
npm test -- --run src/components/guide/__tests__/Callout.test.tsx
```

Expected: FAIL — `Cannot find module '../Callout'`.

- [ ] **Step 3: Implement Callout**

Create `front-end/src/components/guide/Callout.tsx`:

```tsx
import { ReactNode } from 'react';
import { Info, AlertTriangle, AlertOctagon } from 'lucide-react';
import { cn } from '@/lib/utils';

type CalloutType = 'info' | 'warn' | 'danger';

const STYLES: Record<CalloutType, { wrap: string; icon: React.ComponentType<{ className?: string }> }> = {
  info:   { wrap: 'border-blue-500/40 bg-blue-500/10 text-blue-100 callout-info',   icon: Info },
  warn:   { wrap: 'border-amber-500/40 bg-amber-500/10 text-amber-100 callout-warn', icon: AlertTriangle },
  danger: { wrap: 'border-red-500/40 bg-red-500/10 text-red-100 callout-danger',     icon: AlertOctagon },
};

export function Callout({ type = 'info', children }: { type?: CalloutType; children: ReactNode }) {
  const { wrap, icon: Icon } = STYLES[type];
  return (
    <div role="note" className={cn('my-6 flex gap-3 rounded-lg border p-4', wrap)}>
      <Icon className="h-5 w-5 mt-0.5 shrink-0" aria-hidden="true" />
      <div className="prose-callout">{children}</div>
    </div>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
npm test -- --run src/components/guide/__tests__/Callout.test.tsx
```

Expected: PASS (3 tests).

- [ ] **Step 5: Implement Steps and Step (no separate test — purely structural)**

Create `front-end/src/components/guide/Steps.tsx`:

```tsx
import { ReactNode } from 'react';

export function Steps({ children }: { children: ReactNode }) {
  return <ol className="my-6 space-y-4 list-decimal list-inside marker:font-semibold marker:text-primary">{children}</ol>;
}
```

Create `front-end/src/components/guide/Step.tsx`:

```tsx
import { ReactNode } from 'react';

export function Step({ title, children }: { title?: string; children: ReactNode }) {
  return (
    <li className="pl-2">
      {title && <span className="font-medium text-foreground">{title}</span>}
      <div className="mt-1 text-muted-foreground space-y-2">{children}</div>
    </li>
  );
}
```

- [ ] **Step 6: Implement Screenshot placeholder**

Create `front-end/src/components/guide/Screenshot.tsx`:

```tsx
export function Screenshot({ src, alt, caption }: { src?: string; alt: string; caption?: string }) {
  if (!src) return null;
  return (
    <figure className="my-6 rounded-lg border border-border overflow-hidden">
      <img src={src} alt={alt} className="block w-full" />
      {caption && <figcaption className="px-4 py-2 text-sm text-muted-foreground bg-muted">{caption}</figcaption>}
    </figure>
  );
}
```

- [ ] **Step 7: Commit**

```bash
git add src/components/guide/
git commit -m "feat(guide): add Callout, Steps, Step, Screenshot MDX components"
```

---

### Task 3: Create mdx-components.tsx component map

**Files:**
- Create: `front-end/mdx-components.tsx`

- [ ] **Step 1: Create the component map**

Create `front-end/mdx-components.tsx`:

```tsx
import type { MDXComponents } from 'mdx/types';
import { ReactNode } from 'react';
import { Callout } from '@/components/guide/Callout';
import { Steps } from '@/components/guide/Steps';
import { Step } from '@/components/guide/Step';
import { Screenshot } from '@/components/guide/Screenshot';

export function useMDXComponents(components: MDXComponents): MDXComponents {
  return {
    h1: ({ children }) => <h1 className="text-4xl font-bold tracking-tight mb-3">{children}</h1>,
    h2: ({ children, id }) => <h2 id={id} className="text-2xl font-semibold tracking-tight mt-12 mb-4 scroll-mt-20 group">{children}</h2>,
    h3: ({ children, id }) => <h3 id={id} className="text-xl font-semibold mt-8 mb-3 scroll-mt-20">{children}</h3>,
    h4: ({ children }) => <h4 className="text-lg font-semibold mt-6 mb-2">{children}</h4>,
    p: ({ children }) => <p className="text-muted-foreground leading-7 my-4">{children}</p>,
    ul: ({ children }) => <ul className="list-disc list-inside my-4 space-y-1 text-muted-foreground">{children}</ul>,
    ol: ({ children }) => <ol className="list-decimal list-inside my-4 space-y-1 text-muted-foreground">{children}</ol>,
    li: ({ children }) => <li className="leading-7">{children}</li>,
    code: ({ children }) => <code className="rounded bg-muted px-1.5 py-0.5 text-sm font-mono">{children}</code>,
    pre: ({ children }) => <pre className="my-4 overflow-x-auto rounded-lg border border-border bg-muted p-4 text-sm font-mono">{children}</pre>,
    a: ({ href, children }) => <a href={href} className="text-primary underline-offset-4 hover:underline">{children}</a>,
    blockquote: ({ children }: { children?: ReactNode }) => <blockquote className="my-4 border-l-4 border-border pl-4 italic text-muted-foreground">{children}</blockquote>,
    hr: () => <hr className="my-8 border-border" />,
    Callout,
    Steps,
    Step,
    Screenshot,
    ...components,
  };
}
```

- [ ] **Step 2: Verify typecheck passes**

```bash
npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add mdx-components.tsx
git commit -m "feat(guide): wire MDX component map (heading anchors + custom components)"
```

---

### Task 4: Smoke-test MDX with a temporary page

**Files:**
- Create (temporary): `front-end/src/app/guide/_smoke/page.mdx`

- [ ] **Step 1: Create the smoke page**

Create `front-end/src/app/guide/_smoke/page.mdx`:

```mdx
# MDX Smoke Test

This is a paragraph. **Bold**, _italic_, `inline code`.

## A heading

<Callout type="info">This is an info callout from a custom MDX component.</Callout>

```ts
const x: number = 1;
```

- Item one
- Item two

<Steps>
  <Step title="First step">Do the first thing.</Step>
  <Step title="Second step">Do the second thing.</Step>
</Steps>
```

- [ ] **Step 2: Start dev server and visit the page**

```bash
./dev.sh    # from project root, if not already running
```

Open `http://localhost:3010/guide/_smoke` in a browser. Verify:
- The headings render (with autolink anchors).
- The Callout renders with the info styling.
- The Steps list renders with numbered items.

- [ ] **Step 3: Stop dev server, delete smoke page, commit**

Delete the file:

```bash
rm -rf src/app/guide/_smoke
git add -A
git commit --allow-empty -m "test(guide): MDX smoke test passed; removing temporary page"
```

(The `--allow-empty` is in case `git add -A` finds no tracked changes — depending on whether the smoke page was committed first; if it wasn't, no commit is needed and you can skip this step.)

---

## Phase 2 — Guide layout, sidebar, help button

### Task 5: Create the TOC configuration

**Files:**
- Create: `front-end/src/lib/guide-toc.ts`
- Create: `front-end/src/lib/__tests__/guide-toc.test.ts`

- [ ] **Step 1: Write the failing test**

Create `front-end/src/lib/__tests__/guide-toc.test.ts`:

```ts
import { describe, it, expect } from 'vitest';
import { TOC } from '../guide-toc';

describe('guide-toc', () => {
  it('has at least 10 groups', () => {
    expect(TOC.length).toBeGreaterThanOrEqual(10);
  });

  it('every entry has a non-empty label and slug', () => {
    for (const group of TOC) {
      expect(group.label).toBeTruthy();
      expect(group.entries.length).toBeGreaterThan(0);
      for (const entry of group.entries) {
        expect(entry.label).toBeTruthy();
        expect(entry.slug).toBeTruthy();
        expect(entry.slug).not.toMatch(/^\//); // no leading slash
      }
    }
  });

  it('every slug uses lowercase, hyphens, and forward slashes only', () => {
    const valid = /^[a-z0-9-]+(\/[a-z0-9-]+)*$/;
    for (const group of TOC) {
      for (const entry of group.entries) {
        expect(entry.slug, `slug "${entry.slug}" is invalid`).toMatch(valid);
      }
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

```bash
npm test -- --run src/lib/__tests__/guide-toc.test.ts
```

Expected: FAIL — module not found.

- [ ] **Step 3: Create guide-toc.ts**

Create `front-end/src/lib/guide-toc.ts`:

```ts
export type TocEntry = { label: string; slug: string };
export type TocGroup = { label: string; entries: TocEntry[] };

export const TOC: TocGroup[] = [
  {
    label: 'Getting Started',
    entries: [
      { label: 'Overview', slug: 'getting-started/overview' },
      { label: 'Requirements', slug: 'getting-started/requirements' },
      { label: 'Accessibility', slug: 'getting-started/accessibility' },
      { label: 'Sign-in & MFA', slug: 'getting-started/sign-in-and-mfa' },
    ],
  },
  {
    label: 'Core Tools',
    entries: [
      { label: 'Validate', slug: 'tools/validate' },
      { label: 'Convert', slug: 'tools/convert' },
      { label: 'Resolve', slug: 'tools/resolve' },
      { label: 'Batch', slug: 'tools/batch' },
      { label: 'Visualize', slug: 'tools/visualize' },
      { label: 'History', slug: 'tools/history' },
    ],
  },
  {
    label: 'Build',
    entries: [
      { label: 'Overview', slug: 'build/overview' },
      { label: 'Catalog', slug: 'build/catalog' },
      { label: 'Profile', slug: 'build/profile' },
      { label: 'Component', slug: 'build/component' },
      { label: 'System Security Plan', slug: 'build/ssp' },
      { label: 'Assessment Plan', slug: 'build/assessment-plan' },
      { label: 'Assessment Results', slug: 'build/assessment-results' },
      { label: 'POA&M', slug: 'build/poam' },
      // Cross-listed
      { label: 'Save to Library', slug: 'library/save-to-library' },
    ],
  },
  {
    label: 'AI',
    entries: [
      { label: 'Overview', slug: 'ai/overview' },
      { label: 'Catalog Wizard', slug: 'ai/catalog-wizard' },
      { label: 'Component Wizard', slug: 'ai/component-wizard' },
    ],
  },
  {
    label: 'Library & Sharing',
    entries: [
      { label: 'Library Overview', slug: 'library/overview' },
      { label: 'Visibility', slug: 'library/visibility' },
      { label: 'Save to Library', slug: 'library/save-to-library' },
      { label: 'Public Catalog', slug: 'library/public-catalog' },
      { label: 'Artifacts', slug: 'library/artifacts' },
    ],
  },
  {
    label: 'Authorizations',
    entries: [
      { label: 'Overview', slug: 'authorizations/overview' },
      { label: 'Templates', slug: 'authorizations/templates' },
      { label: 'Create an Authorization', slug: 'authorizations/create' },
    ],
  },
  {
    label: 'Account',
    entries: [
      { label: 'Profile', slug: 'account/profile' },
      { label: 'Password', slug: 'account/password' },
      { label: 'MFA Setup', slug: 'account/mfa-setup' },
      { label: 'Service Tokens', slug: 'account/service-tokens' },
      { label: 'Accept an Invite', slug: 'account/accept-invite' },
      { label: 'Request Access', slug: 'account/request-access' },
    ],
  },
  {
    label: 'Org Admin',
    entries: [
      { label: 'Overview', slug: 'org-admin/overview' },
      { label: 'Users', slug: 'org-admin/users' },
      { label: 'Invitations', slug: 'org-admin/invitations' },
      { label: 'Access Requests', slug: 'org-admin/access-requests' },
      { label: 'AI Settings', slug: 'org-admin/ai-settings' },
      { label: 'AI Analytics', slug: 'org-admin/ai-analytics' },
      { label: 'Analytics', slug: 'org-admin/analytics' },
    ],
  },
  {
    label: 'Super Admin',
    entries: [
      { label: 'Overview', slug: 'admin/overview' },
      { label: 'Users', slug: 'admin/users' },
      { label: 'Organizations', slug: 'admin/organizations' },
      { label: 'Health', slug: 'admin/health' },
      { label: 'Logs', slug: 'admin/logs' },
      { label: 'Analytics', slug: 'admin/analytics' },
      { label: 'Security', slug: 'admin/security' },
      { label: 'Security Policy', slug: 'admin/security-policy' },
    ],
  },
  {
    label: 'Reference',
    entries: [
      { label: 'OSCAL Model Types', slug: 'reference/model-types' },
      { label: 'Validation Rules', slug: 'reference/rules' },
      { label: 'Keyboard Shortcuts', slug: 'reference/keyboard-shortcuts' },
      { label: 'API Automation', slug: 'reference/api-automation' },
      { label: 'Deployment — Local', slug: 'reference/deployment/local' },
      { label: 'Deployment — CLI', slug: 'reference/deployment/cli' },
      { label: 'Deployment — AWS', slug: 'reference/deployment/aws' },
      { label: 'Deployment — Azure', slug: 'reference/deployment/azure' },
      { label: 'Troubleshooting', slug: 'reference/troubleshooting' },
    ],
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

```bash
npm test -- --run src/lib/__tests__/guide-toc.test.ts
```

Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add src/lib/guide-toc.ts src/lib/__tests__/guide-toc.test.ts
git commit -m "feat(guide): TOC config covering 10 groups and 50 entries"
```

---

### Task 6: Create help-targets and HelpButton

**Files:**
- Create: `front-end/src/lib/help-targets.ts`
- Create: `front-end/src/components/HelpButton.tsx`
- Create: `front-end/src/components/__tests__/HelpButton.test.tsx`
- Create: `front-end/src/lib/__tests__/help-targets.test.ts`

- [ ] **Step 1: Write the failing help-targets test**

Create `front-end/src/lib/__tests__/help-targets.test.ts`:

```ts
import { describe, it, expect } from 'vitest';
import { HELP_TARGETS } from '../help-targets';
import { TOC } from '../guide-toc';

describe('help-targets', () => {
  it('every help target points to a slug that exists in the TOC', () => {
    const tocSlugs = new Set(TOC.flatMap((g) => g.entries.map((e) => e.slug)));
    for (const [key, target] of Object.entries(HELP_TARGETS)) {
      expect(tocSlugs.has(target), `slug "${target}" (key="${key}") is not in the TOC`).toBe(true);
    }
  });

  it('has the expected core slugs', () => {
    expect(HELP_TARGETS.library).toBe('library/overview');
    expect(HELP_TARGETS.validate).toBe('tools/validate');
    expect(HELP_TARGETS.admin).toBe('admin/overview');
    expect(HELP_TARGETS['org-admin']).toBe('org-admin/overview');
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

```bash
npm test -- --run src/lib/__tests__/help-targets.test.ts
```

Expected: FAIL — module not found.

- [ ] **Step 3: Create help-targets.ts**

Create `front-end/src/lib/help-targets.ts`:

```ts
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

  // Build (only routes that actually exist as standalone app pages)
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

- [ ] **Step 4: Verify help-targets test passes**

```bash
npm test -- --run src/lib/__tests__/help-targets.test.ts
```

Expected: PASS (2 tests).

- [ ] **Step 5: Write the failing HelpButton test**

Create `front-end/src/components/__tests__/HelpButton.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { HelpButton } from '../HelpButton';

describe('HelpButton', () => {
  it('renders a link to the right guide path', () => {
    render(<HelpButton slug="library" />);
    const link = screen.getByRole('link', { name: /open help/i });
    expect(link).toHaveAttribute('href', '/guide/library/overview');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', expect.stringContaining('noopener'));
  });

  it('uses the admin overview slug', () => {
    render(<HelpButton slug="admin" />);
    expect(screen.getByRole('link')).toHaveAttribute('href', '/guide/admin/overview');
  });
});
```

- [ ] **Step 6: Run test to verify it fails**

```bash
npm test -- --run src/components/__tests__/HelpButton.test.tsx
```

Expected: FAIL — module not found.

- [ ] **Step 7: Implement HelpButton**

Create `front-end/src/components/HelpButton.tsx`:

```tsx
import { HelpCircle } from 'lucide-react';
import { HELP_TARGETS, type HelpSlug } from '@/lib/help-targets';

export function HelpButton({ slug, className }: { slug: HelpSlug; className?: string }) {
  return (
    <a
      href={`/guide/${HELP_TARGETS[slug]}`}
      target="_blank"
      rel="noopener noreferrer"
      aria-label="Open help for this page in a new tab"
      title="Open help for this page"
      className={`inline-flex h-8 w-8 items-center justify-center rounded-full text-muted-foreground hover:bg-muted hover:text-foreground transition-colors focus:outline-none focus:ring-2 focus:ring-ring ${className ?? ''}`}
    >
      <HelpCircle className="h-5 w-5" aria-hidden="true" />
    </a>
  );
}
```

- [ ] **Step 8: Verify HelpButton test passes**

```bash
npm test -- --run src/components/__tests__/HelpButton.test.tsx
```

Expected: PASS (2 tests).

- [ ] **Step 9: Commit**

```bash
git add src/lib/help-targets.ts src/lib/__tests__/help-targets.test.ts src/components/HelpButton.tsx src/components/__tests__/HelpButton.test.tsx
git commit -m "feat(guide): typed HelpButton + HELP_TARGETS map"
```

---

### Task 7: Create DocSidebar (desktop)

**Files:**
- Create: `front-end/src/components/guide/DocSidebar.tsx`
- Create: `front-end/src/components/guide/__tests__/DocSidebar.test.tsx`

- [ ] **Step 1: Write the failing test**

Create `front-end/src/components/guide/__tests__/DocSidebar.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { DocSidebar } from '../DocSidebar';

vi.mock('next/navigation', () => ({
  usePathname: () => '/guide/tools/validate',
}));

describe('DocSidebar', () => {
  it('renders all group labels', () => {
    render(<DocSidebar />);
    expect(screen.getByText('Getting Started')).toBeInTheDocument();
    expect(screen.getByText('Core Tools')).toBeInTheDocument();
    expect(screen.getByText('Reference')).toBeInTheDocument();
  });

  it('expands the group containing the active page', () => {
    render(<DocSidebar />);
    // Active is /guide/tools/validate; "Validate" entry should be visible.
    expect(screen.getByRole('link', { name: 'Validate' })).toBeInTheDocument();
  });

  it('marks the active link with aria-current="page"', () => {
    render(<DocSidebar />);
    const active = screen.getByRole('link', { name: 'Validate' });
    expect(active).toHaveAttribute('aria-current', 'page');
  });
});
```

- [ ] **Step 2: Run test, verify it fails**

```bash
npm test -- --run src/components/guide/__tests__/DocSidebar.test.tsx
```

Expected: FAIL — module not found.

- [ ] **Step 3: Implement DocSidebar**

Create `front-end/src/components/guide/DocSidebar.tsx`:

```tsx
'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useEffect, useState } from 'react';
import { ChevronRight } from 'lucide-react';
import { TOC } from '@/lib/guide-toc';
import { cn } from '@/lib/utils';

const STORAGE_KEY = 'guide.toc.expanded';

function readExpanded(): Record<string, boolean> {
  if (typeof window === 'undefined') return {};
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}'); }
  catch { return {}; }
}

export function DocSidebar({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname();
  const activeSlug = pathname.replace(/^\/guide\//, '');

  const activeGroupLabels = TOC
    .filter((g) => g.entries.some((e) => e.slug === activeSlug))
    .map((g) => g.label);

  const [expanded, setExpanded] = useState<Record<string, boolean>>({});

  useEffect(() => {
    const stored = readExpanded();
    const initial: Record<string, boolean> = { ...stored };
    for (const label of activeGroupLabels) initial[label] = true;
    setExpanded(initial);
  }, [pathname]); // eslint-disable-line react-hooks/exhaustive-deps

  const toggle = (label: string) => {
    setExpanded((prev) => {
      const next = { ...prev, [label]: !prev[label] };
      try { localStorage.setItem(STORAGE_KEY, JSON.stringify(next)); } catch { /* ignore */ }
      return next;
    });
  };

  return (
    <nav aria-label="User guide table of contents" className="text-sm">
      <ul className="space-y-1">
        {TOC.map((group) => {
          const isOpen = expanded[group.label] ?? activeGroupLabels.includes(group.label);
          return (
            <li key={group.label}>
              <button
                type="button"
                onClick={() => toggle(group.label)}
                aria-expanded={isOpen}
                className="flex w-full items-center justify-between rounded-md px-2 py-1.5 font-semibold text-foreground hover:bg-muted focus:outline-none focus:ring-2 focus:ring-ring"
              >
                <span>{group.label}</span>
                <ChevronRight className={cn('h-4 w-4 transition-transform', isOpen && 'rotate-90')} aria-hidden="true" />
              </button>
              {isOpen && (
                <ul className="mt-1 ml-3 border-l border-border pl-2 space-y-0.5">
                  {group.entries.map((entry) => {
                    const isActive = entry.slug === activeSlug;
                    return (
                      <li key={`${group.label}-${entry.slug}`}>
                        <Link
                          href={`/guide/${entry.slug}`}
                          aria-current={isActive ? 'page' : undefined}
                          onClick={onNavigate}
                          className={cn(
                            'block rounded-md px-2 py-1 text-muted-foreground hover:bg-muted hover:text-foreground focus:outline-none focus:ring-2 focus:ring-ring',
                            isActive && 'bg-muted text-foreground font-medium',
                          )}
                        >
                          {entry.label}
                        </Link>
                      </li>
                    );
                  })}
                </ul>
              )}
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
```

- [ ] **Step 4: Verify test passes**

```bash
npm test -- --run src/components/guide/__tests__/DocSidebar.test.tsx
```

Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add src/components/guide/DocSidebar.tsx src/components/guide/__tests__/DocSidebar.test.tsx
git commit -m "feat(guide): DocSidebar with collapsible groups + active highlighting"
```

---

### Task 8: Create the shared guide layout

**Files:**
- Create: `front-end/src/app/guide/layout.tsx`

- [ ] **Step 1: Implement the layout**

Create `front-end/src/app/guide/layout.tsx`:

```tsx
'use client';

import Link from 'next/link';
import { useState, ReactNode } from 'react';
import { Menu, X, BookOpen } from 'lucide-react';
import { DocSidebar } from '@/components/guide/DocSidebar';
import { cn } from '@/lib/utils';

export default function GuideLayout({ children }: { children: ReactNode }) {
  const [mobileOpen, setMobileOpen] = useState(false);
  return (
    <div className="container mx-auto px-4 py-6 max-w-7xl">
      <div className="lg:grid lg:grid-cols-[260px_minmax(0,1fr)] lg:gap-10">
        {/* Mobile toggle */}
        <button
          type="button"
          onClick={() => setMobileOpen(true)}
          className="lg:hidden mb-4 inline-flex items-center gap-2 rounded-md border border-border px-3 py-2 text-sm hover:bg-muted focus:outline-none focus:ring-2 focus:ring-ring"
          aria-label="Open table of contents"
        >
          <Menu className="h-4 w-4" aria-hidden="true" />
          Contents
        </button>

        {/* Desktop sidebar */}
        <aside className="hidden lg:block sticky top-4 self-start max-h-[calc(100vh-2rem)] overflow-y-auto pr-2">
          <Link href="/guide" className="mb-4 flex items-center gap-2 text-base font-semibold text-foreground hover:text-primary">
            <BookOpen className="h-4 w-4" aria-hidden="true" />
            User Guide
          </Link>
          <DocSidebar />
        </aside>

        {/* Mobile drawer */}
        {mobileOpen && (
          <div className="fixed inset-0 z-50 lg:hidden" role="dialog" aria-modal="true" aria-label="Table of contents">
            <div className="absolute inset-0 bg-black/60" onClick={() => setMobileOpen(false)} />
            <div className="absolute left-0 top-0 h-full w-72 max-w-[85vw] bg-background border-r border-border p-4 overflow-y-auto">
              <div className="mb-4 flex items-center justify-between">
                <span className="font-semibold">Contents</span>
                <button type="button" onClick={() => setMobileOpen(false)} aria-label="Close" className="rounded-md p-1 hover:bg-muted">
                  <X className="h-4 w-4" aria-hidden="true" />
                </button>
              </div>
              <DocSidebar onNavigate={() => setMobileOpen(false)} />
            </div>
          </div>
        )}

        <article className={cn('min-w-0 prose-guide')}>{children}</article>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Verify typecheck passes**

```bash
npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add src/app/guide/layout.tsx
git commit -m "feat(guide): shared layout with sticky sidebar + mobile drawer"
```

---

### Task 9: Replace /guide/page.tsx with redirect

**Files:**
- Modify: `front-end/src/app/guide/page.tsx`

- [ ] **Step 1: Replace contents**

The file currently is the long single-page guide (~1100 lines). Don't worry about losing content; we'll re-author each section in Phase 4. Replace the entire file with:

```tsx
import { redirect } from 'next/navigation';

export default function GuideIndex() {
  redirect('/guide/getting-started/overview');
}
```

- [ ] **Step 2: Verify typecheck passes**

```bash
npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add src/app/guide/page.tsx
git commit -m "refactor(guide): /guide redirects to /guide/getting-started/overview (content moves to per-topic pages)"
```

---

## Phase 3 — Migrate existing guide pages to MDX

### Task 10: Migrate /guide/automation to /guide/reference/api-automation

**Files:**
- Create: `front-end/src/app/guide/reference/api-automation/page.mdx`
- Delete: `front-end/src/app/guide/automation/page.tsx`

- [ ] **Step 1: Read the existing TSX page to capture all content**

```bash
sed -n '1,$p' src/app/guide/automation/page.tsx | head -400
```

(There may be more — read the whole file before authoring.)

- [ ] **Step 2: Create the MDX equivalent**

Create `front-end/src/app/guide/reference/api-automation/page.mdx`. Use this skeleton, then port every section, code example, and link from the TSX page into MDX:

```mdx
---
title: API Automation
description: Use the OSCAL Hub REST API from scripts, CI/CD, and external services.
lastUpdated: 2026-05-04
---

# API Automation

[Intro paragraph from the existing page goes here.]

## Authentication

[Section content. Update any cURL examples to use port 8090 in dev or note prod port 8080.]

## Examples

[Port each cURL / Python / TypeScript example.]

<Callout type="info">
For interactive API exploration, open the Swagger UI at `http://localhost:8090/swagger-ui/index.html` (dev) or your production equivalent.
</Callout>
```

Faithfully port every code block. Convert TSX `<code className="block …">{`...`}</code>` patterns to triple-backtick MDX code blocks. Convert `<a target="_blank">` to plain markdown links — MDX's auto-generated `<a>` doesn't get `target="_blank"` by default; if a particular link must open externally, use raw HTML inline.

- [ ] **Step 3: Delete the old TSX page**

```bash
rm src/app/guide/automation/page.tsx
rmdir src/app/guide/automation 2>/dev/null || true
```

- [ ] **Step 4: Verify the new path renders**

Visit `http://localhost:3010/guide/reference/api-automation` — the page should render with the sidebar.

- [ ] **Step 5: Update the cross-link from old guide TOC**

Search the codebase for `'/guide/automation'`:

```bash
grep -rn "/guide/automation" src/
```

Replace each occurrence with `/guide/reference/api-automation`.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor(guide): migrate /guide/automation to /guide/reference/api-automation MDX"
```

---

### Task 11: Migrate /guide/rules to /guide/reference/rules

**Files:**
- Create: `front-end/src/app/guide/reference/rules/page.mdx`
- Delete: `front-end/src/app/guide/rules/page.tsx`

- [ ] **Step 1: Read existing TSX**

```bash
cat src/app/guide/rules/page.tsx
```

- [ ] **Step 2: Author MDX equivalent**

Create `front-end/src/app/guide/reference/rules/page.mdx` with frontmatter:

```mdx
---
title: Validation Rules
description: All rules checked when validating OSCAL documents — schema and custom constraints.
lastUpdated: 2026-05-04
---
```

Port the TSX content (rule categories, severity meaning, examples). Cross-link to `/rules` in the app for the live, filterable rules explorer.

- [ ] **Step 3: Delete the old page and update links**

```bash
rm src/app/guide/rules/page.tsx
rmdir src/app/guide/rules 2>/dev/null || true
grep -rn "/guide/rules" src/  # update any references to /guide/reference/rules
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(guide): migrate /guide/rules to /guide/reference/rules MDX"
```

---

### Task 12: Migrate /guide/deployment/* to /guide/reference/deployment/*

**Files:**
- Create: `front-end/src/app/guide/reference/deployment/local/page.mdx`
- Create: `front-end/src/app/guide/reference/deployment/cli/page.mdx`
- Create: `front-end/src/app/guide/reference/deployment/aws/page.mdx`
- Create: `front-end/src/app/guide/reference/deployment/azure/page.mdx`
- Delete: `front-end/src/app/guide/deployment/{local,cli,aws,azure}/page.tsx`

- [ ] **Step 1: Read all four existing TSX pages**

```bash
for d in local cli aws azure; do echo "=== $d ==="; cat src/app/guide/deployment/$d/page.tsx; done
```

- [ ] **Step 2: Author all four MDX files**

For each, frontmatter:

```mdx
---
title: Deployment — <Local|CLI|AWS|Azure>
description: <one-line>
lastUpdated: 2026-05-04
---
```

Port the content. Update any `localhost:8080` → `localhost:8090` (dev). Keep `8080` where the doc explicitly references production.

- [ ] **Step 3: Delete old pages, update links**

```bash
rm -rf src/app/guide/deployment
grep -rn "/guide/deployment" src/  # update any references
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(guide): migrate /guide/deployment/* to /guide/reference/deployment/* MDX"
```

---

## Phase 4 — Author new content pages

> **Important note for content tasks:** Each MDX page below has a **content outline** (sections + key facts to cover). Write each page in full — no "TODO" or "stub" content. Where you need to verify a UI detail, run the relevant page in dev mode (`./dev.sh`) and look. Use a consistent structure per page:
>
> 1. Frontmatter (`title`, `description`, `lastUpdated`).
> 2. `<h1>` matching the title.
> 3. A 1–3 sentence intro paragraph.
> 4. Sections: "What it does", "How to use it" (Steps), "Tips & limits", optional "Related" links to sibling guide pages.
>
> Use `<Steps>` and `<Step title="…">` for procedural lists. Use `<Callout type="info|warn|danger">` for tips, warnings, gotchas. Use fenced code blocks for cURL / shell / configuration examples. Cross-link to sibling guide pages with markdown links.

The tasks in this phase commit one group at a time so each commit is reviewable.

---

### Task 13: Author Getting Started pages (4)

**Files:**
- Create: `front-end/src/app/guide/getting-started/overview/page.mdx`
- Create: `front-end/src/app/guide/getting-started/requirements/page.mdx`
- Create: `front-end/src/app/guide/getting-started/accessibility/page.mdx`
- Create: `front-end/src/app/guide/getting-started/sign-in-and-mfa/page.mdx`

- [ ] **Step 1: Author overview/page.mdx**

Frontmatter title "Welcome to OSCAL Hub". Cover:
- What OSCAL Hub is (web platform for OSCAL documents — validate, convert, build, share, get help from AI).
- Who it's for (compliance teams, security engineers, FedRAMP CSPs).
- Quick links to the most-used groups: Core Tools, Build, Library.
- Note: signing in unlocks builders, AI, library; the Public Catalog is browseable without an account.

- [ ] **Step 2: Author requirements/page.mdx**

Cover:
- Modern browser (Chrome / Firefox / Safari / Edge — current versions).
- For self-hosting: Java 11+, Maven 3.9+, Node 24+, PostgreSQL.
- For cloud-hosted: just the browser.
- File-size limit (10 MB per upload).

- [ ] **Step 3: Author accessibility/page.mdx**

Port the existing accessibility section from the old single-page guide. WCAG 2.1 Level AA, Section 508, keyboard navigation, screen-reader compat, skip-to-content, semantic HTML, ARIA. Add a sentence on contrast in dark mode.

- [ ] **Step 4: Author sign-in-and-mfa/page.mdx**

Cover:
- Logging in (username + password, optional MFA prompt).
- New account flow: register, verify email, set up MFA if enforced.
- Forgot-password reset.
- Linking to `/guide/account/mfa-setup` and `/guide/account/password`.

- [ ] **Step 5: Verify all four render**

Visit each in dev mode, confirm sidebar highlights "Getting Started" and the right entry is bold.

- [ ] **Step 6: Commit**

```bash
git add src/app/guide/getting-started/
git commit -m "docs(guide): author Getting Started group (overview, requirements, accessibility, sign-in & MFA)"
```

---

### Task 14: Author Core Tools pages (6)

**Files:** `tools/{validate,convert,resolve,batch,visualize,history}/page.mdx`

Each page covers **what it does**, **how to use it** (Steps), **tips/limits**, **related**. Use the corresponding TSX section in the *old* `/guide/page.tsx` (now in git history; recover with `git show HEAD~N:src/app/guide/page.tsx | sed -n '/Validate/,/Convert/p'`) as a starting point — but rewrite, do not copy verbatim. Update stale ports.

- [ ] **Step 1: Author validate/page.mdx**

Cover the 5-step workflow (navigate → upload → select model → validate → review). Mention all 7 model types are supported, line-numbered errors, jump-to-line in preview. Add a Callout: validation requires sign-in.

- [ ] **Step 2: Author convert/page.mdx**

Cover XML/JSON/YAML conversion, side-by-side preview, download. Note auto-detection of source format.

- [ ] **Step 3: Author resolve/page.mdx**

Cover profile-resolution flow (upload profile → choose output format → resolve → preview & download). Note that referenced catalogs must be reachable.

- [ ] **Step 4: Author batch/page.mdx**

Cover bulk uploads, choose operation (validate / convert / resolve), per-file progress, ZIP download of results.

- [ ] **Step 5: Author visualize/page.mdx**

Cover: upload OSCAL document → choose model type → interactive charts (control families, hierarchies). Note which model types have rich visualizations.

- [ ] **Step 6: Author history/page.mdx**

Cover: timestamped past operations, success/failure status, duration, delete, re-run, statistics dashboard.

- [ ] **Step 7: Verify all six render and commit**

```bash
git add src/app/guide/tools/
git commit -m "docs(guide): author Core Tools group (validate, convert, resolve, batch, visualize, history)"
```

---

### Task 15: Author Build pages (8)

**Files:** `build/{overview,catalog,profile,component,ssp,assessment-plan,assessment-results,poam}/page.mdx`

Run `./dev.sh` (if not running) and walk each builder yourself before authoring. The Build hub is at `/build`. Click each tab and the "Create new" wizard to capture the exact workflow.

- [ ] **Step 1: Author build/overview/page.mdx**

Cover:
- What the Build hub is (visual builder for all 7 OSCAL types).
- Tabs available (Catalogs, Profiles, Components, SSP, AP, AR, POA&Ms, Library).
- Create / edit / delete docs from the hub.
- "Save to Library" — link to `/guide/library/save-to-library`.
- Visibility settings — link to `/guide/library/visibility`.

- [ ] **Step 2–8: Author one page per OSCAL type**

For each (`catalog`, `profile`, `component`, `ssp`, `assessment-plan`, `assessment-results`, `poam`):

- 1-paragraph "What is a [type]" definition (link to `/guide/reference/model-types`).
- Step-by-step "Create a new [type]" walkthrough — the actual fields and tabs.
- Step-by-step "Edit an existing [type]" walkthrough.
- Validation tips (each builder has a Validate panel — describe what it surfaces).
- Save-to-library + visibility note (cross-link).

For `component`, also cover the `/build/component/[componentId]` editor route.

- [ ] **Step 9: Commit**

```bash
git add src/app/guide/build/
git commit -m "docs(guide): author Build group (overview + 7 OSCAL-type builders)"
```

---

### Task 16: Author AI pages (3)

**Files:** `ai/{overview,catalog-wizard,component-wizard}/page.mdx`

- [ ] **Step 1: Author ai/overview/page.mdx**

Cover:
- AI features summary (catalog draft from PDFs, component-def from STIG/CIS).
- Feature-gating: org admin must enable AI in `/org-admin/ai-settings` — link there.
- Cost note: AI usage incurs Anthropic API charges; `/org-admin/ai-analytics` shows usage and cost.
- Privacy note: uploaded content is sent to Anthropic for processing.

- [ ] **Step 2: Author ai/catalog-wizard/page.mdx**

Cover:
- What it does (turn unstructured docs — PDF, Word, HTML, plain text — into a draft OSCAL catalog).
- Inputs accepted (file upload or paste).
- Walkthrough: open `/ai/wizard`, choose Catalog, upload source, click Generate, review draft, hand off to the catalog builder.
- Limits: large STIGs are chunked; very large inputs may take several minutes; expect to review and edit.

- [ ] **Step 3: Author ai/component-wizard/page.mdx**

Cover:
- What it does (map STIG/CIS/vendor configs to NIST 800-53 in component-definition format).
- Supported source formats (XCCDF, JSON, YAML, CSV; PDFs convert with limited fidelity).
- Walkthrough: open `/ai/wizard`, choose Component-Definition, upload STIG/CIS, click Generate, review mapping, save to library or edit further.
- Limits: STIG noise is trimmed; mapping confidence varies — always review.

- [ ] **Step 4: Commit**

```bash
git add src/app/guide/ai/
git commit -m "docs(guide): author AI group (overview + catalog & component wizards)"
```

---

### Task 17: Author Library & Sharing pages (5)

**Files:** `library/{overview,visibility,save-to-library,public-catalog,artifacts}/page.mdx`

- [ ] **Step 1: Author library/overview/page.mdx**

Cover:
- The Library is your storehouse of OSCAL documents — yours, your org's, and (if you choose) the public's.
- Three visibility modes (Private / Organization / Public) — link to `/guide/library/visibility`.
- Search, filter by type/tag, view item detail, download, rate.
- Saving from a builder: link to `/guide/library/save-to-library`.

- [ ] **Step 2: Author library/visibility/page.mdx**

Cover:
- The three visibility modes with concrete examples of who sees what.
- How to change visibility (action menu on a library card → publish/unpublish/share).
- Audit: visibility changes are logged.
- Public visibility makes content show up in `/catalog` (no sign-in needed) — link to public-catalog page.
- Add a `<Callout type="warn">`: think before you publish; public means **anyone**.

- [ ] **Step 3: Author library/save-to-library/page.mdx**

Cover:
- "Save to Library" buttons live in every builder (catalog, profile, component, SSP, AP, AR, POA&M).
- First save creates a new library item; subsequent saves append a new version.
- The modal lets you name, tag, and describe the document, plus pick visibility.

- [ ] **Step 4: Author library/public-catalog/page.mdx**

Cover:
- The Public Catalog at `/catalog` is browseable without signing in.
- Search, filter by type, sort by newest / downloads / rating.
- Download requires sign-in (this is by design, to support analytics & rate limiting).
- Detail page (`/catalog/[itemId]`) shows full metadata.

- [ ] **Step 5: Author library/artifacts/page.mdx**

Cover:
- Artifacts are markdown templates with variables — useful for SOPs, policies, plans.
- Same Private/Organization/Public visibility model as library items.
- Browse at `/artifacts`; create from the Create tab; preview with sample variable values.
- Cross-link to authorizations, which use a similar template-with-variables pattern.

- [ ] **Step 6: Commit**

```bash
git add src/app/guide/library/
git commit -m "docs(guide): author Library & Sharing group (overview, visibility, save-to-library, public-catalog, artifacts)"
```

---

### Task 18: Author Authorizations pages (3)

**Files:** `authorizations/{overview,templates,create}/page.mdx`

The existing single-page guide has detailed authorization content (lines ~548–756 of the old file). Use it as a *starting point*, then split into three pages and update for the current UI.

- [ ] **Step 1: Author authorizations/overview/page.mdx**

What authorizations are (ATO documents, FedRAMP, internal approvals). The two-tab UI: Templates and Authorizations. Links to the other two pages.

- [ ] **Step 2: Author authorizations/templates/page.mdx**

How to create / edit templates with `{{ variable }}` substitution. Variable naming rules. Live-preview with variable highlighting. Example FedRAMP template.

- [ ] **Step 3: Author authorizations/create/page.mdx**

The four-step wizard: Select SSP → Choose Template → Fill Variables → Review & Name. Note the link to a System Security Plan creates the audit trail.

- [ ] **Step 4: Commit**

```bash
git add src/app/guide/authorizations/
git commit -m "docs(guide): author Authorizations group (overview, templates, create)"
```

---

### Task 19: Author Account pages (6)

**Files:** `account/{profile,password,mfa-setup,service-tokens,accept-invite,request-access}/page.mdx`

- [ ] **Step 1: Author account/profile/page.mdx**

Editing personal info (name, email, address, phone, title, org). Uploading profile logo. Where the page lives (`/profile`).

- [ ] **Step 2: Author account/password/page.mdx**

Changing password, complexity rules, what happens to sessions on change.

- [ ] **Step 3: Author account/mfa-setup/page.mdx**

Enabling MFA: scan QR with authenticator app, enter 6-digit TOTP, save backup codes. How to use backup codes if you lose your authenticator. How to disable.

- [ ] **Step 4: Author account/service-tokens/page.mdx**

Port the service-tokens content from the existing single-page guide. Update curl examples to port 8090 dev / 8080 prod. Add a `<Callout type="warn">`: tokens are shown once and cannot be retrieved.

- [ ] **Step 5: Author account/accept-invite/page.mdx**

What an invite link looks like, the flow when a user clicks one, how to join an existing account vs. create a new one.

- [ ] **Step 6: Author account/request-access/page.mdx**

How to request to join an existing org, what the org admin sees, how to track pending requests.

- [ ] **Step 7: Commit**

```bash
git add src/app/guide/account/
git commit -m "docs(guide): author Account group (profile, password, MFA, tokens, invite, access request)"
```

---

### Task 20: Author Org Admin pages (7)

**Files:** `org-admin/{overview,users,invitations,access-requests,ai-settings,ai-analytics,analytics}/page.mdx`

- [ ] **Step 1: Author org-admin/overview/page.mdx**

What an org admin can do, summary of the dashboard, links to the other six pages. Note the role gate.

- [ ] **Steps 2–7: Author each operations page**

For each (`users`, `invitations`, `access-requests`, `ai-settings`, `ai-analytics`, `analytics`):
- What's on the page.
- Common operations (CRUD walkthroughs as Steps).
- Notes & gotchas (e.g., revoking an invite invalidates the token; AI settings require an Anthropic API key).

- [ ] **Step 8: Commit**

```bash
git add src/app/guide/org-admin/
git commit -m "docs(guide): author Org Admin group (overview + 6 operations pages)"
```

---

### Task 21: Author Super Admin pages (8)

**Files:** `admin/{overview,users,organizations,health,logs,analytics,security,security-policy}/page.mdx`

- [ ] **Step 1: Author admin/overview/page.mdx**

Super-admin role responsibilities, dashboard cards, links to all sub-pages.

- [ ] **Steps 2–8: Author each operations page**

Same template as org-admin. For `health`, link to the health-check API doc (`/admin/health`) and `/api/health/ping`. For `security-policy`, document MFA enforcement and password complexity policy.

- [ ] **Step 9: Commit**

```bash
git add src/app/guide/admin/
git commit -m "docs(guide): author Super Admin group (overview + 7 operations pages)"
```

---

### Task 22: Author Reference (new) pages (3)

**Files:** `reference/{model-types,keyboard-shortcuts,troubleshooting}/page.mdx`

These are the pages NOT migrated from existing TSX. (The migrated ones — api-automation, rules, deployment/* — are already done in Phase 3.)

- [ ] **Step 1: Author reference/model-types/page.mdx**

Port the seven OSCAL model-type definitions from the old guide. One section per type with a link to the corresponding `build/*` and (where relevant) `tools/*` pages.

- [ ] **Step 2: Author reference/keyboard-shortcuts/page.mdx**

Port from old guide. Tab/Shift-Tab/Enter/Escape/Space/Arrow keys.

- [ ] **Step 3: Author reference/troubleshooting/page.mdx**

Port from old guide and add new entries:
- Backend not responding (port 8090 dev, 8080 prod). Update from old "8080" references.
- Validation errors — read messages, click to jump.
- Conversion fails — validate first.
- Profile resolution — referenced catalogs must be reachable.
- File size limit (10 MB).
- Browser compat.
- **NEW:** "I see 403 Forbidden after a backend restart" → log out and back in (token regenerated).
- **NEW:** "AI features greyed out" → org admin must enable in `/org-admin/ai-settings`.

- [ ] **Step 4: Commit**

```bash
git add src/app/guide/reference/model-types src/app/guide/reference/keyboard-shortcuts src/app/guide/reference/troubleshooting
git commit -m "docs(guide): author Reference new pages (model-types, keyboard-shortcuts, troubleshooting)"
```

---

## Phase 5 — Add HelpButton to app pages

> **Pattern for every page below:** Find the page heading (usually `<h1>` near the top of the page component) and wrap it with a flex container plus the `<HelpButton>`. Example transformation:
>
> ```tsx
> // Before:
> <h1 className="text-3xl font-bold mb-6">Library</h1>
>
> // After:
> import { HelpButton } from '@/components/HelpButton';
> ...
> <div className="flex items-center gap-2 mb-6">
>   <h1 className="text-3xl font-bold">Library</h1>
>   <HelpButton slug="library" />
> </div>
> ```
>
> If the existing heading has its own margin classes, move them to the wrapper to keep spacing identical.

### Task 23: Add HelpButton to Tools pages

**Files:**
- Modify: `front-end/src/app/validate/page.tsx`
- Modify: `front-end/src/app/convert/page.tsx`
- Modify: `front-end/src/app/resolve/page.tsx`
- Modify: `front-end/src/app/batch/page.tsx`
- Modify: `front-end/src/app/visualize/page.tsx`
- Modify: `front-end/src/app/history/page.tsx`

- [ ] **Step 1: Add HelpButton to each**

For each file, import `HelpButton` from `@/components/HelpButton` and wrap the page heading. Slugs: `validate`, `convert`, `resolve`, `batch`, `visualize`, `history`.

- [ ] **Step 2: Verify each renders**

Visit each in dev mode. Click the help icon — confirm a new tab opens at `/guide/tools/<slug>`.

- [ ] **Step 3: Commit**

```bash
git add src/app/{validate,convert,resolve,batch,visualize,history}/page.tsx
git commit -m "feat(help): HelpButton on Core Tools pages (validate, convert, resolve, batch, visualize, history)"
```

---

### Task 24: Add HelpButton to Library, Catalog, Artifacts, Rules

**Files:**
- Modify: `front-end/src/app/library/page.tsx`
- Modify: `front-end/src/app/library/[itemId]/page.tsx`
- Modify: `front-end/src/app/(public)/catalog/page.tsx`
- Modify: `front-end/src/app/(public)/catalog/[itemId]/page.tsx`
- Modify: `front-end/src/app/artifacts/page.tsx`
- Modify: `front-end/src/app/artifacts/[artifactId]/page.tsx`
- Modify: `front-end/src/app/rules/page.tsx`

- [ ] **Step 1: Add HelpButton to each page**

Slugs: `library`, `library`, `public-catalog`, `public-catalog`, `artifacts`, `artifacts`, `rules`.

- [ ] **Step 2: Verify and commit**

```bash
git add -A
git commit -m "feat(help): HelpButton on Library, Catalog, Artifacts, Rules pages"
```

---

### Task 25: Add HelpButton to Build, AI, Authorizations

**Files:**
- Modify: `front-end/src/app/build/page.tsx`
- Modify: `front-end/src/app/build/component/[componentId]/page.tsx`
- Modify: `front-end/src/app/ai/wizard/page.tsx`
- Modify: `front-end/src/app/ai/wizard/catalog/page.tsx` (if exists)
- Modify: `front-end/src/app/ai/wizard/component_def/page.tsx` (if exists)
- Modify: `front-end/src/app/authorizations/page.tsx`
- Modify: `front-end/src/app/authorizations/template/[templateId]/page.tsx`
- Modify: `front-end/src/app/authorizations/authorization/[authorizationId]/page.tsx`

- [ ] **Step 1: Verify which AI sub-routes exist**

```bash
ls src/app/ai/wizard/
```

Apply HelpButton only to existing pages.

- [ ] **Step 2: Add HelpButton with these slugs**

`build`, `build-component`, `ai-wizard`, `ai-catalog`, `ai-component`, `authorizations`, `authorization-template`, `authorization-create`.

- [ ] **Step 3: Verify and commit**

```bash
git add -A
git commit -m "feat(help): HelpButton on Build, AI Wizard, Authorizations pages"
```

---

### Task 26: Add HelpButton to Account pages

**Files:**
- Modify: `front-end/src/app/profile/page.tsx`
- Modify: `front-end/src/app/change-password/page.tsx`
- Modify: `front-end/src/app/mfa-setup/page.tsx`
- Modify: `front-end/src/app/request-access/page.tsx`

- [ ] **Step 1: Add HelpButton with these slugs**

`profile`, `change-password`, `mfa-setup`, `request-access`.

- [ ] **Step 2: Verify and commit**

```bash
git add -A
git commit -m "feat(help): HelpButton on Account pages (profile, password, MFA setup, request access)"
```

---

### Task 27: Add HelpButton to Org Admin pages

**Files:**
- Modify: `front-end/src/app/org-admin/page.tsx`
- Modify: `front-end/src/app/org-admin/users/page.tsx`
- Modify: `front-end/src/app/org-admin/invitations/page.tsx`
- Modify: `front-end/src/app/org-admin/requests/page.tsx`
- Modify: `front-end/src/app/org-admin/ai-settings/page.tsx`
- Modify: `front-end/src/app/org-admin/ai-analytics/page.tsx`
- Modify: `front-end/src/app/org-admin/analytics/page.tsx`

- [ ] **Step 1: Add HelpButton with these slugs**

`org-admin`, `org-admin-users`, `org-admin-invitations`, `org-admin-requests`, `org-admin-ai-settings`, `org-admin-ai-analytics`, `org-admin-analytics`.

- [ ] **Step 2: Verify and commit**

```bash
git add -A
git commit -m "feat(help): HelpButton on Org Admin pages"
```

---

### Task 28: Add HelpButton to Super Admin pages

**Files:**
- Modify: `front-end/src/app/admin/page.tsx`
- Modify: `front-end/src/app/admin/users/page.tsx`
- Modify: `front-end/src/app/admin/organizations/page.tsx`
- Modify: `front-end/src/app/admin/health/page.tsx`
- Modify: `front-end/src/app/admin/logs/page.tsx`
- Modify: `front-end/src/app/admin/analytics/page.tsx`
- Modify: `front-end/src/app/admin/security/page.tsx`
- Modify: `front-end/src/app/admin/security-policy/page.tsx`

- [ ] **Step 1: Add HelpButton with these slugs**

`admin`, `admin-users`, `admin-organizations`, `admin-health`, `admin-logs`, `admin-analytics`, `admin-security`, `admin-security-policy`.

- [ ] **Step 2: Verify and commit**

```bash
git add -A
git commit -m "feat(help): HelpButton on Super Admin pages"
```

---

## Phase 6 — Final verification

### Task 29: Typecheck, lint, build, and unit tests

- [ ] **Step 1: Typecheck**

```bash
cd front-end
npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 2: Lint**

```bash
npm run lint
```

Expected: no errors. Fix any introduced.

- [ ] **Step 3: Run unit tests**

```bash
npm test -- --run
```

Expected: all tests pass.

- [ ] **Step 4: Production build**

```bash
npm run build
```

Expected: build succeeds. Note: Next builds all 46 MDX pages — review build output for any per-page errors.

---

### Task 30: Manual smoke test

- [ ] **Step 1: Start the app**

```bash
./dev.sh   # from project root
```

- [ ] **Step 2: Click through one page in each TOC group**

For each of the 10 TOC groups, visit the first entry. Verify:
- Page renders without console errors.
- Sidebar highlights the correct group + entry.
- Heading anchors work (click an `<h2>` link, URL gets a fragment).

- [ ] **Step 3: Test help icon round-trip**

From `/library`, click the `<HelpButton>`. Confirm a **new tab** opens at `http://localhost:3010/guide/library/overview` with that page rendered.

Repeat from one page per major feature group: `/validate`, `/build`, `/ai/wizard`, `/profile`, `/org-admin`, `/admin`. Confirm each opens the right guide page.

- [ ] **Step 4: Mobile responsive check**

In dev tools, switch to a mobile viewport (375×812). Visit `/guide/tools/validate`. Confirm:
- The sidebar is hidden by default.
- A "Contents" hamburger button is visible above the content.
- Clicking it opens the drawer; clicking outside or the X closes it.
- Clicking a sidebar link in the drawer navigates and closes the drawer.

- [ ] **Step 5: Stale-content audit**

Search for stragglers:

```bash
cd front-end
grep -rn "OSCAL UX" src/app/guide/ || echo "clean"
grep -rn "localhost:8080" src/app/guide/ | grep -v "production" || echo "clean"
grep -rn "cd front-end && \./dev\.sh" src/app/guide/ || echo "clean"
```

Fix any matches by editing the offending MDX files. Commit any fixes:

```bash
git add -A
git commit -m "docs(guide): final stale-content audit fixes"
```

- [ ] **Step 6: Final commit (allow empty if nothing to fix)**

If steps 1–5 produced no fixes, no extra commit is needed. The plan is complete.

---

## Spec coverage check

Verifying every spec section maps to a task:

| Spec section | Task |
|---|---|
| Architecture > Route structure (46 leaf pages, redirect, public access) | 8, 9, 13–22 |
| Architecture > Migration of existing guide pages | 10, 11, 12 |
| Architecture > Sidebar (TOC) structure | 5, 7, 8 |
| Architecture > Content authoring (MDX, frontmatter, custom components) | 2, 3, 4 |
| Architecture > MDX setup | 1, 3 |
| Architecture > Help-icon system | 6, 23–28 |
| Stale content fixes | 10, 11, 12, 22, 30 |
| Testing & verification | 29, 30 |

All sections covered.

---

## Type/identifier consistency check

- `HelpSlug` — defined in Task 6, referenced in Tasks 23–28 ✓
- `HELP_TARGETS` — defined in Task 6, referenced (transitively via `<HelpButton>`) in Tasks 23–28 ✓
- `TOC` / `TocGroup` / `TocEntry` — defined in Task 5, consumed in Task 7 (`DocSidebar`) ✓
- `DocSidebar` — defined in Task 7, consumed in Task 8 (layout) ✓
- `useMDXComponents` — defined in Task 3 (Next.js auto-discovers `mdx-components.tsx` at the project root, so no explicit import is needed) ✓
- `Callout`, `Steps`, `Step`, `Screenshot` — defined in Task 2, registered in Task 3, used throughout Phase 4 content ✓
