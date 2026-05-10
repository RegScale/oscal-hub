import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ReconciliationBanner } from '../reconciliation-banner';
import type { ConMonReconciliationCounts, ConMonReconciliationDetail } from '@/types/oscal';

function makeCounts(overrides: Partial<ConMonReconciliationCounts> = {}): ConMonReconciliationCounts {
  return {
    newCount: 3,
    closedCount: 5,
    reopenedCount: 1,
    stillOpenCount: 10,
    removedCount: 0,
    changedCount: 0,
    previousSnapshotId: 99,
    ...overrides,
  };
}

function makeDetail(): ConMonReconciliationDetail {
  return {
    snapshotId: 2,
    previousSnapshotId: 99,
    newCount: 3,
    closedCount: 5,
    reopenedCount: 1,
    stillOpenCount: 10,
    removedCount: 0,
    changedCount: 0,
    newItems: [
      { id: 1, externalId: 'POA-1', title: 'New vuln', status: 'OPEN' },
      { id: 2, externalId: 'POA-2', title: 'Another new', status: 'OPEN' },
      { id: 3, externalId: 'POA-3', title: 'Third new', status: 'OPEN' },
    ],
    newlyClosedItems: [],
    reopenedItems: [{ id: 4, externalId: 'POA-4', title: 'Reopened one', status: 'OPEN' }],
    removedItems: [],
    changedItems: [],
  };
}

describe('ReconciliationBanner', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('displays counts inline without expanding', () => {
    render(
      <ReconciliationBanner
        counts={makeCounts()}
        previousSnapshotDate="2026-04-01T00:00:00Z"
        onLoadDetail={vi.fn()}
      />
    );

    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument();
    expect(screen.getByText('10')).toBeInTheDocument();
    // Detail section should not be visible yet
    expect(screen.queryByText('New vuln')).not.toBeInTheDocument();
  });

  it('calls onLoadDetail and shows items when expanded', async () => {
    const onLoadDetail = vi.fn().mockResolvedValue(makeDetail());

    render(
      <ReconciliationBanner
        counts={makeCounts()}
        previousSnapshotDate={null}
        onLoadDetail={onLoadDetail}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: /Show details/i }));

    await waitFor(() => {
      expect(onLoadDetail).toHaveBeenCalledTimes(1);
      expect(screen.getByText('New vuln')).toBeInTheDocument();
    });

    // Clicking again hides details
    fireEvent.click(screen.getByRole('button', { name: /Hide details/i }));
    expect(screen.queryByText('New vuln')).not.toBeInTheDocument();
  });
});
