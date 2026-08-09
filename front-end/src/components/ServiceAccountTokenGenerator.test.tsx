import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ServiceAccountTokenGenerator } from '@/components/ServiceAccountTokenGenerator';
import { apiClient } from '@/lib/api-client';

vi.mock('@/lib/api-client', () => ({
  apiClient: {
    generateServiceAccountToken: vi.fn(),
    listServiceAccountTokens: vi.fn(),
    revokeServiceAccountToken: vi.fn(),
  },
}));

vi.mock('sonner', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

const activeToken = {
  id: 5,
  tokenName: 'CI Pipeline',
  globalRole: 'SUPER_ADMIN',
  orgRole: null,
  organizationId: null,
  createdAt: '2026-08-01T10:00:00',
  expiresAt: '2027-08-01T10:00:00',
  lastUsedAt: null,
  revokedAt: null,
  status: 'ACTIVE' as const,
};

describe('ServiceAccountTokenGenerator', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(apiClient.listServiceAccountTokens).mockResolvedValue([activeToken]);
    vi.mocked(apiClient.revokeServiceAccountToken).mockResolvedValue(undefined);
  });

  it('lists existing tokens with their snapshotted permissions', async () => {
    render(<ServiceAccountTokenGenerator />);

    expect(await screen.findByText('CI Pipeline')).toBeInTheDocument();
    expect(screen.getByText('SUPER_ADMIN')).toBeInTheDocument();
    expect(screen.getByText('ACTIVE')).toBeInTheDocument();
    expect(screen.getByText('Never')).toBeInTheDocument();
  });

  it('revokes a token and refreshes the list', async () => {
    render(<ServiceAccountTokenGenerator />);

    await screen.findByText('CI Pipeline');
    fireEvent.click(screen.getByRole('button', { name: /^revoke$/i }));
    fireEvent.click(screen.getByRole('button', { name: /^confirm$/i }));

    await waitFor(() => {
      expect(apiClient.revokeServiceAccountToken).toHaveBeenCalledWith(5);
    });
    await waitFor(() => {
      expect(apiClient.listServiceAccountTokens).toHaveBeenCalledTimes(2);
    });
  });

  /** A revoked token stays visible for the audit trail but cannot be re-revoked. */
  it('does not offer revoke on a token that is already revoked', async () => {
    vi.mocked(apiClient.listServiceAccountTokens).mockResolvedValue([
      { ...activeToken, status: 'REVOKED' as const, revokedAt: '2026-08-05T10:00:00' },
    ]);

    render(<ServiceAccountTokenGenerator />);

    expect(await screen.findByText('REVOKED')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^revoke$/i })).not.toBeInTheDocument();
  });

  it('tells the user when they have no tokens yet', async () => {
    vi.mocked(apiClient.listServiceAccountTokens).mockResolvedValue([]);

    render(<ServiceAccountTokenGenerator />);

    expect(await screen.findByText(/have not generated any service account tokens/i))
      .toBeInTheDocument();
  });
});
