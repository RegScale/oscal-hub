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
          Manage your profile, open a support ticket, or replay guided tours from the avatar menu — it&apos;s
          always in the top-right corner.
        </p>
      ),
    },
    {
      id: 'finish',
      title: "You're all set",
      body: (
        <>
          <p>That&apos;s the lay of the land. Good next steps:</p>
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
