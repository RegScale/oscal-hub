import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LeaderboardContent } from '../leaderboard-content';
import type { LeaderboardResponse } from '@/types/oscal';

const getLeaderboard = vi.fn();

vi.mock('@/lib/api-client', () => ({
  apiClient: {
    getLeaderboard: (...args: unknown[]) => getLeaderboard(...args),
  },
}));

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    user: { username: 'carol' },
    isAuthenticated: true,
  }),
}));

function makeResponse(overrides: Partial<LeaderboardResponse> = {}): LeaderboardResponse {
  return {
    window: 'all',
    generatedAt: '2026-07-31T12:00:00Z',
    mostActive: [
      {
        rank: 1,
        username: 'alice',
        displayName: 'Alice Ames',
        score: 42,
        breakdown: { operations: 40, libraryPublishes: 2 },
      },
      { rank: 2, username: 'carol', displayName: 'Carol Cruz', score: 17, breakdown: { operations: 17 } },
      { rank: 3, username: 'bob', displayName: 'Bob Brown', score: 9, breakdown: { operations: 9 } },
      { rank: 4, username: 'dave', displayName: 'Dave Diaz', score: 3, breakdown: { operations: 3 } },
    ],
    topContributors: [
      { rank: 1, username: 'bob', displayName: 'Bob Brown', score: 7 },
    ],
    ...overrides,
  };
}

describe('LeaderboardContent', () => {
  beforeEach(() => {
    getLeaderboard.mockReset();
  });

  it('loads the all-time boards on mount and renders rows', async () => {
    getLeaderboard.mockResolvedValue(makeResponse());

    render(<LeaderboardContent />);

    await waitFor(() => expect(screen.getByText('Alice Ames')).toBeInTheDocument());
    expect(getLeaderboard).toHaveBeenCalledWith('all');
    expect(screen.getByText('Most Active Users')).toBeInTheDocument();
    expect(screen.getByText('Top Contributors')).toBeInTheDocument();
    expect(screen.getByText('Bob Brown', { selector: '[data-board="top-contributors"] *' })).toBeInTheDocument();
    expect(screen.getByText('42')).toBeInTheDocument();
  });

  it('shows medals for the top three and plain rank numbers after', async () => {
    getLeaderboard.mockResolvedValue(makeResponse());

    render(<LeaderboardContent />);
    await waitFor(() => expect(screen.getByText('Alice Ames')).toBeInTheDocument());

    const activeBoard = screen.getByTestId('board-most-active');
    expect(activeBoard.querySelector('[data-testid="medal-1"]')).not.toBeNull();
    expect(activeBoard.querySelector('[data-testid="medal-2"]')).not.toBeNull();
    expect(activeBoard.querySelector('[data-testid="medal-3"]')).not.toBeNull();
    expect(screen.getByText('4', { selector: '[data-testid="rank-number"]' })).toBeInTheDocument();
  });

  it('highlights the signed-in user with a You badge', async () => {
    getLeaderboard.mockResolvedValue(makeResponse());

    render(<LeaderboardContent />);
    await waitFor(() => expect(screen.getByText('Carol Cruz')).toBeInTheDocument());

    expect(screen.getByText('You')).toBeInTheDocument();
  });

  it('renders the activity breakdown for most-active rows', async () => {
    getLeaderboard.mockResolvedValue(makeResponse());

    render(<LeaderboardContent />);
    await waitFor(() => expect(screen.getByText('Alice Ames')).toBeInTheDocument());

    expect(screen.getByText('40 operations · 2 library publishes')).toBeInTheDocument();
  });

  it('refetches with the 30-day window when the tab changes', async () => {
    getLeaderboard.mockResolvedValue(makeResponse());

    render(<LeaderboardContent />);
    await waitFor(() => expect(screen.getByText('Alice Ames')).toBeInTheDocument());

    getLeaderboard.mockResolvedValue(makeResponse({ window: '30d', mostActive: [], topContributors: [] }));
    await userEvent.click(screen.getByRole('tab', { name: /last 30 days/i }));

    await waitFor(() => expect(getLeaderboard).toHaveBeenLastCalledWith('30d'));
  });

  it('shows empty states when there is no activity', async () => {
    getLeaderboard.mockResolvedValue(makeResponse({ mostActive: [], topContributors: [] }));

    render(<LeaderboardContent />);

    await waitFor(() =>
      expect(screen.getAllByText(/no activity yet/i).length).toBeGreaterThanOrEqual(1)
    );
  });

  it('shows an error state with a retry button when the fetch fails', async () => {
    getLeaderboard.mockRejectedValueOnce(new Error('boom'));
    getLeaderboard.mockResolvedValueOnce(makeResponse());

    render(<LeaderboardContent />);

    const retry = await screen.findByRole('button', { name: /try again/i });
    await userEvent.click(retry);

    await waitFor(() => expect(screen.getByText('Alice Ames')).toBeInTheDocument());
  });
});
