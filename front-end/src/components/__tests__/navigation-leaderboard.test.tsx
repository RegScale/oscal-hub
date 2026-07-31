import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Navigation } from '../Navigation';

const mockUseAuth = vi.fn();

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

vi.mock('next/navigation', () => ({
  usePathname: () => '/',
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}));

vi.mock('@/components/organization-switcher', () => ({
  OrganizationSwitcher: () => <div data-testid="org-switcher" />,
}));

vi.mock('@/components/UserAvatarMenu', () => ({
  UserAvatarMenu: () => <div data-testid="avatar-menu" />,
}));

function setUser(user: { username: string; globalRole?: string } | null) {
  mockUseAuth.mockReturnValue({
    user,
    isAuthenticated: user != null,
  });
  if (user) {
    localStorage.setItem('user', JSON.stringify(user));
  } else {
    localStorage.removeItem('user');
  }
}

describe('Navigation leaderboard link', () => {
  beforeEach(() => {
    mockUseAuth.mockReset();
    localStorage.clear();
  });

  it('shows the Leaderboard link for authenticated non-admin users', async () => {
    setUser({ username: 'carol' });

    render(<Navigation />);

    const link = await screen.findByRole('link', { name: /leaderboard/i });
    expect(link).toHaveAttribute('href', '/leaderboard');
  });

  it('hides the Leaderboard link when logged out', () => {
    setUser(null);

    render(<Navigation />);

    expect(screen.queryByRole('link', { name: /leaderboard/i })).not.toBeInTheDocument();
  });

  it('hides the Leaderboard link for super admins', async () => {
    setUser({ username: 'root', globalRole: 'SUPER_ADMIN' });

    render(<Navigation />);

    // Wait for the mount effect that reads the stored user to settle.
    await screen.findByTestId('avatar-menu');
    expect(screen.queryByRole('link', { name: /leaderboard/i })).not.toBeInTheDocument();
  });
});
