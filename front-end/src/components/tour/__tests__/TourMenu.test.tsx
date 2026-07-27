import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TourMenu } from '@/components/tour/TourMenu';
import { markTourCompleted } from '@/lib/tours/storage';

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

describe('TourMenu', () => {
  beforeEach(() => {
    localStorage.clear();
    startTour.mockClear();
  });

  it('lists the Get Started tour with a Start button and launches it', async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    render(<TourMenu open onOpenChange={onOpenChange} />);
    expect(screen.getByText('Get Started with OSCAL Hub')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /start/i }));
    expect(onOpenChange).toHaveBeenCalledWith(false);
    expect(startTour).toHaveBeenCalledWith('get-started');
  });

  it('shows a completed badge and a Replay button for finished tours', () => {
    markTourCompleted(TEST_USER.userId, 'get-started', 1);
    render(<TourMenu open onOpenChange={() => {}} />);
    expect(screen.getByText(/completed/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /replay/i })).toBeInTheDocument();
  });
});
