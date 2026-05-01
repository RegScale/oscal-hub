import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import LoginPage from '@/app/login/page';
import { AuthProvider } from '@/contexts/AuthContext';
import { apiClient } from '@/lib/api-client';

// Mock next/navigation (matches AuthContext.test.tsx pattern)
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    prefetch: vi.fn(),
    back: vi.fn(),
  }),
  usePathname: () => '/login',
  useSearchParams: () => new URLSearchParams(''),
}));

// Mock apiClient
vi.mock('@/lib/api-client', () => ({
  apiClient: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    refreshToken: vi.fn(),
  },
}));

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: (k: string) => store[k] || null,
    setItem: (k: string, v: string) => { store[k] = v; },
    removeItem: (k: string) => { delete store[k]; },
    clear: () => { store = {}; },
  };
})();
Object.defineProperty(window, 'localStorage', { value: localStorageMock, writable: true });

const sessionStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: (k: string) => store[k] || null,
    setItem: (k: string, v: string) => { store[k] = v; },
    removeItem: (k: string) => { delete store[k]; },
    clear: () => { store = {}; },
  };
})();
Object.defineProperty(window, 'sessionStorage', { value: sessionStorageMock, writable: true });

function renderWithProviders() {
  return render(<AuthProvider><LoginPage /></AuthProvider>);
}

describe('LoginPage signup with organization', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorageMock.clear();
    sessionStorageMock.clear();
  });

  it('sends organizationName when filled', async () => {
    vi.mocked(apiClient.register).mockResolvedValue({
      token: 't', userId: 1, username: 'travis', email: 'travis@example.com',
    } as any);
    renderWithProviders();

    // Switch to signup mode
    fireEvent.click(screen.getByText(/sign up/i));
    fireEvent.change(screen.getByLabelText(/^username/i), { target: { value: 'travis' } });
    fireEvent.change(screen.getByLabelText(/^email/i), { target: { value: 't@example.com' } });
    fireEvent.change(screen.getByLabelText(/^password$/i), { target: { value: 'CorrectH0rse!Batt' } });
    fireEvent.change(screen.getByLabelText(/confirm password/i), { target: { value: 'CorrectH0rse!Batt' } });
    fireEvent.change(screen.getByLabelText(/organization name/i), { target: { value: 'Acme' } });
    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => {
      expect(apiClient.register).toHaveBeenCalledWith('travis', 'CorrectH0rse!Batt', 't@example.com', 'Acme');
    });
  });

  it('shows inline org-name error on 409', async () => {
    const err = new Error('That organization name is already taken. Try another.');
    (err as any).field = 'organizationName';
    (err as any).code = 'ORGANIZATION_NAME_IN_USE';
    vi.mocked(apiClient.register).mockRejectedValue(err);
    renderWithProviders();

    fireEvent.click(screen.getByText(/sign up/i));
    fireEvent.change(screen.getByLabelText(/^username/i), { target: { value: 'x' } });
    fireEvent.change(screen.getByLabelText(/^email/i), { target: { value: 'x@example.com' } });
    fireEvent.change(screen.getByLabelText(/^password$/i), { target: { value: 'CorrectH0rse!Batt' } });
    fireEvent.change(screen.getByLabelText(/confirm password/i), { target: { value: 'CorrectH0rse!Batt' } });
    fireEvent.change(screen.getByLabelText(/organization name/i), { target: { value: 'Already Taken' } });
    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => {
      expect(screen.getByText(/already taken/i)).toBeInTheDocument();
    });
  });

  it('request-access link writes sessionStorage', async () => {
    renderWithProviders();
    fireEvent.click(screen.getByText(/sign up/i));
    fireEvent.change(screen.getByLabelText(/^email/i), { target: { value: 't@example.com' } });
    fireEvent.change(screen.getByLabelText(/^username/i), { target: { value: 'travis' } });

    // Override window.location.href setter to avoid navigation in jsdom
    const originalLocation = window.location;
    delete (window as any).location;
    (window as any).location = { ...originalLocation, href: '' };

    const link = screen.getByText(/looking to join an existing organization/i);
    fireEvent.click(link);

    expect(sessionStorage.getItem('pendingRegistration.email')).toBe('t@example.com');
    expect(sessionStorage.getItem('pendingRegistration.username')).toBe('travis');

    (window as any).location = originalLocation;
  });
});
