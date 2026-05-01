import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import InvitationsPage from '@/app/org-admin/invitations/page';

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => '/org-admin/invitations',
}));

vi.mock('@/lib/api-client', () => ({
  apiClient: {
    listInvitations: vi.fn().mockResolvedValue([]),
    createInvitation: vi.fn(),
    revokeInvitation: vi.fn(),
  },
}));

// Mock localStorage — org admin with an org selected
const localStorageMock = (() => {
  let store: Record<string, string> = {
    user: JSON.stringify({ username: 'admin', orgRole: 'ORG_ADMIN', organizationId: 1 }),
    currentOrganization: JSON.stringify({ id: 1, name: 'Acme' }),
  };
  return {
    getItem: (k: string) => store[k] ?? null,
    setItem: (k: string, v: string) => { store[k] = v; },
    removeItem: (k: string) => { delete store[k]; },
    clear: () => { store = {}; },
    get store() { return store; },
    set store(v) { store = v; },
  };
})();
Object.defineProperty(window, 'localStorage', { value: localStorageMock, writable: true });

const mockUseAuth = vi.fn(() => ({
  currentOrganization: { id: 1, name: 'Acme' },
  user: { username: 'admin' },
  isAuthenticated: true,
}));
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
  AuthProvider: ({ children }: any) => <>{children}</>,
}));

describe('Invitations admin page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Reset localStorage to org admin with org
    localStorageMock.store = {
      user: JSON.stringify({ username: 'admin', orgRole: 'ORG_ADMIN', organizationId: 1 }),
      currentOrganization: JSON.stringify({ id: 1, name: 'Acme' }),
    };
    mockUseAuth.mockReturnValue({
      currentOrganization: { id: 1, name: 'Acme' },
      user: { username: 'admin' },
      isAuthenticated: true,
    });
  });

  it('renders form and empty state when no invitations', async () => {
    render(<InvitationsPage />);
    // Wait for loading to complete (listInvitations resolves async)
    await screen.findByRole('heading', { name: /invite teammate/i });
    await screen.findByText(/no pending invitations/i);
  });

  it('shows guidance when no org is selected', () => {
    // Remove org from localStorage
    localStorageMock.store = {
      user: JSON.stringify({ username: 'admin', orgRole: 'ORG_ADMIN' }),
    };
    render(<InvitationsPage />);
    expect(screen.getByText(/select an organization/i)).toBeInTheDocument();
  });
});
