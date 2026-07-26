import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ResetPasswordPage from '@/app/reset-password/page';
import { apiClient } from '@/lib/api-client';

let searchParams = new URLSearchParams('token=abc123');

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), prefetch: vi.fn(), back: vi.fn() }),
  usePathname: () => '/reset-password',
  useSearchParams: () => searchParams,
}));

vi.mock('@/lib/api-client', () => ({
  apiClient: {
    resetPassword: vi.fn(),
  },
}));

describe('ResetPasswordPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    searchParams = new URLSearchParams('token=abc123');
  });

  it('submits a policy-compliant password with the token from the URL', async () => {
    vi.mocked(apiClient.resetPassword).mockResolvedValue({ message: 'ok' });
    render(<ResetPasswordPage />);

    fireEvent.change(screen.getByLabelText(/^new password/i), {
      target: { value: 'CorrectH0rse!Batt' },
    });
    fireEvent.change(screen.getByLabelText(/confirm new password/i), {
      target: { value: 'CorrectH0rse!Batt' },
    });
    fireEvent.click(screen.getByRole('button', { name: /reset password/i }));

    await waitFor(() => {
      expect(apiClient.resetPassword).toHaveBeenCalledWith('abc123', 'CorrectH0rse!Batt');
      expect(screen.getByText(/has been reset/i)).toBeInTheDocument();
    });
  });

  it('blocks a policy-violating password without calling the API', async () => {
    render(<ResetPasswordPage />);

    fireEvent.change(screen.getByLabelText(/^new password/i), {
      target: { value: 'lowercase0nly!pw' },
    });
    fireEvent.change(screen.getByLabelText(/confirm new password/i), {
      target: { value: 'lowercase0nly!pw' },
    });
    fireEvent.click(screen.getByRole('button', { name: /reset password/i }));

    await waitFor(() => {
      expect(screen.getByText(/does not meet all the requirements/i)).toBeInTheDocument();
    });
    expect(apiClient.resetPassword).not.toHaveBeenCalled();
  });

  it('shows the server message when the token is rejected', async () => {
    vi.mocked(apiClient.resetPassword).mockRejectedValue(
      new Error('This password reset link is invalid or has expired. Please request a new one.')
    );
    render(<ResetPasswordPage />);

    fireEvent.change(screen.getByLabelText(/^new password/i), {
      target: { value: 'CorrectH0rse!Batt' },
    });
    fireEvent.change(screen.getByLabelText(/confirm new password/i), {
      target: { value: 'CorrectH0rse!Batt' },
    });
    fireEvent.click(screen.getByRole('button', { name: /reset password/i }));

    await waitFor(() => {
      expect(screen.getByText(/invalid or has expired/i)).toBeInTheDocument();
    });
  });

  it('explains when the token is missing and offers to request a new link', () => {
    searchParams = new URLSearchParams('');
    render(<ResetPasswordPage />);

    expect(screen.getByText(/missing its token/i)).toBeInTheDocument();
    expect(screen.getByText(/request a new reset link/i)).toBeInTheDocument();
  });
});
