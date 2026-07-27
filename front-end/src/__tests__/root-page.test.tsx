import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import Dashboard from '@/app/page';
import { AuthProvider } from '@/contexts/AuthContext';
import { apiClient } from '@/lib/api-client';

// ---------------------------------------------------------------------------
// Mocks
// ---------------------------------------------------------------------------
const mockPush = vi.fn();
const mockReload = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    replace: vi.fn(),
    prefetch: vi.fn(),
    back: vi.fn(),
  }),
  usePathname: () => '/',
}));

// Mock apiClient — we'll configure individual methods per test
vi.mock('@/lib/api-client', () => ({
  apiClient: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    refreshToken: vi.fn(),
    getMyPendingRequests: vi.fn(),
    getMyOrganizations: vi.fn(),
    createMyOrganization: vi.fn(),
  },
}));

// Dashboard now consumes the tour system (mounted app-wide in layout.tsx);
// stub it out so these tests stay focused on org-onboarding behavior.
vi.mock('@/components/tour/TourProvider', () => ({
  useTour: () => ({
    startTour: vi.fn(),
    activeTour: null,
    stepIndex: 0,
    endTour: vi.fn(),
    next: vi.fn(),
    back: vi.fn(),
  }),
}));
vi.mock('@/components/tour/TourWelcomeDialog', () => ({
  TourWelcomeDialog: () => null,
}));

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: (k: string) => store[k] ?? null,
    setItem: (k: string, v: string) => { store[k] = v; },
    removeItem: (k: string) => { delete store[k]; },
    clear: () => { store = {}; },
  };
})();
Object.defineProperty(window, 'localStorage', { value: localStorageMock, writable: true });

// Mock window.location.reload
Object.defineProperty(window, 'location', {
  value: { ...window.location, reload: mockReload, href: '/', pathname: '/' },
  writable: true,
});

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Set localStorage to simulate a logged-in user WITH org access.
 */
function setUserWithOrg() {
  localStorageMock.setItem('token', 'fake-jwt');
  localStorageMock.setItem('user', JSON.stringify({
    userId: 1,
    username: 'testuser',
    email: 'test@example.com',
    globalRole: 'USER',
    organizationId: 42,
    organizationName: 'Acme Corp',
  }));
}

/**
 * Set localStorage to simulate a logged-in user WITHOUT org access.
 */
function setUserWithoutOrg() {
  localStorageMock.setItem('token', 'fake-jwt');
  localStorageMock.setItem('user', JSON.stringify({
    userId: 2,
    username: 'newuser',
    email: 'new@example.com',
    globalRole: 'USER',
    // no organizationId
  }));
}

function renderDashboard() {
  return render(<AuthProvider><Dashboard /></AuthProvider>);
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('Dashboard root page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorageMock.clear();
    // Default: pending requests returns empty list
    vi.mocked(apiClient.getMyPendingRequests).mockResolvedValue([]);
    vi.mocked(apiClient.refreshToken).mockResolvedValue({} as any);
  });

  // -------------------------------------------------------------------------
  // Branch B: zero memberships + zero pending requests → "Get started" view
  // -------------------------------------------------------------------------
  it('renders "Get started" with two cards when user has no org and no pending requests', async () => {
    setUserWithoutOrg();
    vi.mocked(apiClient.getMyPendingRequests).mockResolvedValue([]);

    renderDashboard();

    // Wait for the pending-request fetch to complete — the h1 heading is unique to this branch
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Welcome to OSCAL Hub/i, level: 1 })).toBeInTheDocument();
    });

    // The create-org button has data-testid
    expect(screen.getByTestId('create-org-card')).toBeInTheDocument();
    // The request-access link exists as text (Link renders to <a> in jsdom but may not forward data-testid)
    expect(screen.getByText(/Create an organization/i)).toBeInTheDocument();
    expect(screen.getByText(/Request access/i)).toBeInTheDocument();
    // "Get started" tagline
    expect(screen.getByText(/requesting access to an existing one/i)).toBeInTheDocument();
  });

  // -------------------------------------------------------------------------
  // Branch A: zero memberships + has pending request → pending status view
  // -------------------------------------------------------------------------
  it('renders "Access request pending" with create-your-own CTA when user has pending request', async () => {
    setUserWithoutOrg();
    vi.mocked(apiClient.getMyPendingRequests).mockResolvedValue([
      {
        requestId: 1,
        organizationId: 10,
        organizationName: 'RegScale LLC',
        requestDate: '2024-01-01T00:00:00Z',
        status: 'PENDING',
        message: null,
      },
    ]);

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText(/Access request pending/i)).toBeInTheDocument();
    });

    expect(screen.getByText(/RegScale LLC/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Create your own organization/i })).toBeInTheDocument();
  });

  // -------------------------------------------------------------------------
  // Has memberships → existing dashboard renders (empty state NOT shown)
  // -------------------------------------------------------------------------
  it('does not render the empty state when user has org access', async () => {
    setUserWithOrg();
    // getMyPendingRequests should not even be called
    vi.mocked(apiClient.getMyPendingRequests).mockResolvedValue([]);

    renderDashboard();

    // The dashboard cards (Library, etc.) should be present
    await waitFor(() => {
      expect(screen.getByText('Library')).toBeInTheDocument();
    });

    // The empty-state h1 "Welcome to OSCAL Hub" (level 1) should NOT appear
    // (the dashboard has "Welcome to OSCAL Hub!" in a <p> inside the Getting Started card,
    // but NOT as an h1 level heading)
    expect(screen.queryByRole('heading', { name: /Welcome to OSCAL Hub/i, level: 1 })).not.toBeInTheDocument();
    expect(screen.queryByText(/Access request pending/i)).not.toBeInTheDocument();
    // The "Get started" tagline is unique to the empty state
    expect(screen.queryByText(/requesting access to an existing one/i)).not.toBeInTheDocument();
  });

  // -------------------------------------------------------------------------
  // Modal: click "Create your own organization" → modal opens → submit
  // -------------------------------------------------------------------------
  it('opens create-org modal when clicking "Create your own organization" from pending state', async () => {
    setUserWithoutOrg();
    vi.mocked(apiClient.getMyPendingRequests).mockResolvedValue([
      {
        requestId: 1,
        organizationId: 10,
        organizationName: 'Existing Corp',
        requestDate: '2024-01-01T00:00:00Z',
        status: 'PENDING',
        message: null,
      },
    ]);
    vi.mocked(apiClient.createMyOrganization).mockResolvedValue({ id: 99, name: 'My New Org' });

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Create your own organization/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Create your own organization/i }));

    // Modal should appear
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByLabelText(/Organization name/i)).toBeInTheDocument();

    // Fill in and submit
    fireEvent.change(screen.getByLabelText(/Organization name/i), {
      target: { value: 'My New Org' },
    });
    fireEvent.click(screen.getByRole('button', { name: /Create organization/i }));

    await waitFor(() => {
      expect(apiClient.createMyOrganization).toHaveBeenCalledWith({ name: 'My New Org' });
    });
  });

  // -------------------------------------------------------------------------
  // Modal: 409 error surfaces inline field error
  // -------------------------------------------------------------------------
  it('shows inline name error when createMyOrganization returns a 409-style error', async () => {
    setUserWithoutOrg();
    vi.mocked(apiClient.getMyPendingRequests).mockResolvedValue([]);

    const conflictError = new Error('That organization name is already taken. Try another.') as any;
    conflictError.field = 'name';
    conflictError.code = 'ORGANIZATION_NAME_IN_USE';
    vi.mocked(apiClient.createMyOrganization).mockRejectedValue(conflictError);

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByTestId('create-org-card')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('create-org-card'));

    // Modal should appear
    expect(screen.getByRole('dialog')).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/Organization name/i), {
      target: { value: 'Taken Corp' },
    });
    fireEvent.click(screen.getByRole('button', { name: /Create organization/i }));

    await waitFor(() => {
      expect(screen.getByText(/already taken/i)).toBeInTheDocument();
    });
  });
});
