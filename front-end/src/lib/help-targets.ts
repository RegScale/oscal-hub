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

  // Build
  build: 'build/overview',
  'build-component': 'build/component',

  // AI
  'ai-wizard': 'ai/overview',
  'ai-catalog': 'ai/catalog-wizard',
  'ai-component': 'ai/component-wizard',
  'ai-rule-generator': 'ai/rule-generator',

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
