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
