import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TourProvider, useTour } from '@/components/tour/TourProvider';
import type { TourDefinition } from '@/lib/tours/types';

const TEST_USER = {
  userId: 42,
  username: 'tester',
  email: 't@example.com',
  organizationId: 1,
};

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ user: TEST_USER }),
}));

function makeTour(overrides: Partial<TourDefinition> = {}): TourDefinition {
  return {
    id: 'test-tour',
    version: 1,
    title: 'Test tour',
    description: 'A tour for tests',
    startRoute: '/',
    eligible: () => true,
    steps: [
      { id: 'intro', title: 'Welcome step', body: 'Intro body' },
      { id: 'anchored', target: 'test-target', title: 'Anchored step', body: 'Anchored body' },
      { id: 'done', title: 'Finish step', body: 'Finish body' },
    ],
    ...overrides,
  };
}

function LaunchButton({ tourId }: { tourId: string }) {
  const { startTour } = useTour();
  return (
    <button type="button" onClick={() => startTour(tourId)}>
      launch
    </button>
  );
}

function renderHarness(tour: TourDefinition) {
  return render(
    <TourProvider tours={[tour]} targetTimeoutMs={300}>
      <div data-tour="test-target">Target content</div>
      <LaunchButton tourId={tour.id} />
    </TourProvider>,
  );
}

describe('TourProvider', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('starts a tour and shows the first (modal) step as a dialog', async () => {
    const user = userEvent.setup();
    renderHarness(makeTour());
    await user.click(screen.getByRole('button', { name: 'launch' }));
    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveTextContent('Welcome step');
    expect(dialog).toHaveTextContent('Step 1 of 3');
  });

  it('advances to an anchored step and back', async () => {
    const user = userEvent.setup();
    renderHarness(makeTour());
    await user.click(screen.getByRole('button', { name: 'launch' }));
    await screen.findByRole('dialog');
    await user.click(screen.getByRole('button', { name: 'Next' }));
    await waitFor(() => expect(screen.getByRole('dialog')).toHaveTextContent('Anchored step'));
    await user.click(screen.getByRole('button', { name: 'Back' }));
    await waitFor(() => expect(screen.getByRole('dialog')).toHaveTextContent('Welcome step'));
  });

  it('skips a step whose target is missing', async () => {
    const user = userEvent.setup();
    const tour = makeTour({
      steps: [
        { id: 'intro', title: 'Welcome step', body: 'Intro' },
        { id: 'ghost', target: 'does-not-exist', title: 'Ghost step', body: 'Never shown' },
        { id: 'done', title: 'Finish step', body: 'Finish' },
      ],
    });
    renderHarness(tour);
    await user.click(screen.getByRole('button', { name: 'launch' }));
    await screen.findByRole('dialog');
    await user.click(screen.getByRole('button', { name: 'Next' }));
    // Ghost step times out (300ms) and is skipped forward to the finish step.
    await waitFor(() => expect(screen.getByRole('dialog')).toHaveTextContent('Finish step'), {
      timeout: 2000,
    });
  });

  it('completes the tour from the last step and persists completion', async () => {
    const user = userEvent.setup();
    const tour = makeTour({ steps: [{ id: 'only', title: 'Only step', body: 'One and done' }] });
    renderHarness(tour);
    await user.click(screen.getByRole('button', { name: 'launch' }));
    await screen.findByRole('dialog');
    await user.click(screen.getByRole('button', { name: 'Finish' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    const stored = JSON.parse(localStorage.getItem('oscal-hub.tours.v1.42') ?? '{}');
    expect(stored.tours['test-tour']).toEqual({ completedVersion: 1 });
  });

  it('dismisses on Escape and records the step index', async () => {
    const user = userEvent.setup();
    renderHarness(makeTour());
    await user.click(screen.getByRole('button', { name: 'launch' }));
    await screen.findByRole('dialog');
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    const stored = JSON.parse(localStorage.getItem('oscal-hub.tours.v1.42') ?? '{}');
    expect(stored.tours['test-tour']).toEqual({ dismissedVersion: 1, dismissedAtStep: 0 });
  });

  it('refuses to start an ineligible tour', async () => {
    const user = userEvent.setup();
    renderHarness(makeTour({ eligible: () => false }));
    await user.click(screen.getByRole('button', { name: 'launch' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('refuses to start below the minimum viewport width', async () => {
    const user = userEvent.setup();
    renderHarness(makeTour({ minViewportWidth: 99999 }));
    await user.click(screen.getByRole('button', { name: 'launch' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
