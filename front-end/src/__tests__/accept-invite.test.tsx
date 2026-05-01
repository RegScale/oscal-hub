import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import AcceptInvitePage from '@/app/accept-invite/page';

vi.mock('@/lib/api-client', () => ({
  apiClient: {
    getInvitation: vi.fn(),
    acceptInvitation: vi.fn(),
  },
}));
const { apiClient } = await import('@/lib/api-client');

const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, replace: vi.fn(), back: vi.fn(), prefetch: vi.fn() }),
  useSearchParams: () => new URLSearchParams('?token=tok-123'),
  usePathname: () => '/accept-invite',
}));

const mockUseAuth = vi.fn(() => ({ user: null, isAuthenticated: false }));
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
  AuthProvider: ({ children }: any) => <>{children}</>,
}));

describe('Accept invite page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseAuth.mockReturnValue({ user: null, isAuthenticated: false });
    // Reset localStorage
    localStorage.clear();
  });

  it('shows expired state on 410', async () => {
    const err: any = new Error('expired');
    err.status = 410;
    vi.mocked(apiClient.getInvitation).mockRejectedValue(err);
    render(<AcceptInvitePage />);
    await screen.findByText(/no longer valid/i);
  });

  it('shows not-found state on 404', async () => {
    const err: any = new Error('missing');
    err.status = 404;
    vi.mocked(apiClient.getInvitation).mockRejectedValue(err);
    render(<AcceptInvitePage />);
    await screen.findByText(/not found/i);
  });

  it('logged-out new user → shows signup form prefilled with email', async () => {
    vi.mocked(apiClient.getInvitation).mockResolvedValue({
      email: 'teammate@example.com',
      organizationName: 'Acme',
      inviterName: 'admin',
    });
    vi.mocked(apiClient.acceptInvitation).mockResolvedValue({ userId: 1, username: 'tm' });
    render(<AcceptInvitePage />);
    const emailField = await screen.findByLabelText(/email/i) as HTMLInputElement;
    expect(emailField.value).toBe('teammate@example.com');
    expect(emailField.readOnly).toBe(true);
    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'tm' } });
    fireEvent.change(screen.getByLabelText(/^password/i), { target: { value: 'CorrectH0rse!Batt' } });
    fireEvent.click(screen.getByRole('button', { name: /^accept$/i }));
    await waitFor(() =>
      expect(apiClient.acceptInvitation).toHaveBeenCalledWith(
        'tok-123',
        { username: 'tm', password: 'CorrectH0rse!Batt' }
      )
    );
  });

  it('new user accept with returned JWT → stores token in localStorage', async () => {
    // Mock location.href assignment
    const originalHref = window.location.href;
    Object.defineProperty(window, 'location', {
      writable: true,
      value: { href: originalHref },
    });

    vi.mocked(apiClient.getInvitation).mockResolvedValue({
      email: 'newuser@example.com',
      organizationName: 'Acme',
      inviterName: 'admin',
    });
    vi.mocked(apiClient.acceptInvitation).mockResolvedValue({
      token: 'jwt-abc-123',
      userId: 42,
      username: 'newuser',
    });
    render(<AcceptInvitePage />);
    await screen.findByLabelText(/username/i);
    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'newuser' } });
    fireEvent.change(screen.getByLabelText(/^password/i), { target: { value: 'CorrectH0rse!Batt' } });
    fireEvent.click(screen.getByRole('button', { name: /^accept$/i }));
    await waitFor(() => expect(localStorage.getItem('token')).toBe('jwt-abc-123'));
  });

  it('logged-in user → one-click accept', async () => {
    mockUseAuth.mockReturnValue({ user: { username: 'me' } as any, isAuthenticated: true });
    vi.mocked(apiClient.getInvitation).mockResolvedValue({
      email: 'me@example.com',
      organizationName: 'Acme',
      inviterName: 'admin',
    });
    vi.mocked(apiClient.acceptInvitation).mockResolvedValue({ userId: 1, username: 'me' });
    render(<AcceptInvitePage />);
    await screen.findByText(/click accept to join/i);
    fireEvent.click(screen.getByRole('button', { name: /accept invitation/i }));
    await waitFor(() => expect(apiClient.acceptInvitation).toHaveBeenCalledWith('tok-123', {}));
    await waitFor(() => expect(mockPush).toHaveBeenCalledWith('/'));
  });
});
