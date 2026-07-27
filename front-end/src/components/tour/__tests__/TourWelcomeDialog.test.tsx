import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TourWelcomeDialog } from '@/components/tour/TourWelcomeDialog';
import { dismissWelcomePrompt, loadTourState } from '@/lib/tours/storage';

const TEST_USER = {
  userId: 42,
  username: 'tester',
  email: 't@example.com',
  organizationId: 1,
};

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ user: TEST_USER }),
}));

const startTour = vi.fn();
vi.mock('@/components/tour/TourProvider', () => ({
  useTour: () => ({
    startTour,
    activeTour: null,
    stepIndex: 0,
    endTour: vi.fn(),
    next: vi.fn(),
    back: vi.fn(),
  }),
}));

describe('TourWelcomeDialog', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    startTour.mockClear();
  });

  it('prompts a brand-new user and starts the tour on accept', async () => {
    const user = userEvent.setup();
    render(<TourWelcomeDialog />);
    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /start tour/i }));
    expect(startTour).toHaveBeenCalledWith('get-started');
    // Accepting marks the prompt as seen so it never re-fires.
    expect(loadTourState(TEST_USER.userId).welcomePrompt.seen).toBe(true);
  });

  it('records a deferral on "Maybe later"', async () => {
    const user = userEvent.setup();
    render(<TourWelcomeDialog />);
    await screen.findByRole('dialog');
    await user.click(screen.getByRole('button', { name: /maybe later/i }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(loadTourState(TEST_USER.userId).welcomePrompt.deferrals).toBe(1);
    expect(startTour).not.toHaveBeenCalled();
  });

  it("marks the prompt seen on \"Don't ask again\"", async () => {
    const user = userEvent.setup();
    render(<TourWelcomeDialog />);
    await screen.findByRole('dialog');
    await user.click(screen.getByRole('button', { name: /don't ask again/i }));
    expect(loadTourState(TEST_USER.userId).welcomePrompt.seen).toBe(true);
  });

  it('does not prompt when already seen', () => {
    dismissWelcomePrompt(TEST_USER.userId);
    render(<TourWelcomeDialog />);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
