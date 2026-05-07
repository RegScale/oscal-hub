import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { ContinuousMonitoringTab } from '../../conmon-tab';
import { apiClient } from '@/lib/api-client';
import type { AuthorizationResponse } from '@/types/oscal';

vi.mock('@/lib/api-client', () => ({
  apiClient: {
    listConMonSnapshots: vi.fn(),
    getConMonAnalytics: vi.fn(),
  },
}));

vi.mock('sonner', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

// Mock heavy sub-components to avoid Radix portal complications and chart deps.
vi.mock('../upload-snapshot-dialog', () => ({
  UploadSnapshotDialog: ({ open }: { open: boolean }) =>
    open ? <div data-testid="upload-snapshot-dialog">upload-dialog</div> : null,
}));
vi.mock('../items-drawer', () => ({
  ItemsDrawer: () => <div data-testid="items-drawer" />,
}));
vi.mock('../analytics-dashboard', () => ({
  AnalyticsDashboard: () => <div data-testid="analytics-dashboard" />,
}));
vi.mock('../reconciliation-banner', () => ({
  ReconciliationBanner: () => <div data-testid="reconciliation-banner" />,
}));

function makeAuth(overrides: Partial<AuthorizationResponse> = {}): AuthorizationResponse {
  return {
    id: 7,
    organizationId: 100,
    name: 'Test Auth',
    sspItemId: 'ssp-1',
    templateId: 1,
    templateName: 'T',
    variableValues: {},
    completedContent: '',
    authorizedBy: 'alice',
    authorizedAt: '',
    createdAt: '',
    effectiveRole: 'OWNER',
    ...overrides,
  } as AuthorizationResponse;
}

describe('ContinuousMonitoringTab', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (apiClient.listConMonSnapshots as any).mockResolvedValue([]);
    (apiClient.getConMonAnalytics as any).mockResolvedValue({
      openCountSeries: [],
      severitySeriesByDate: [],
      currentStatusBreakdown: [],
      agingBuckets: [],
    });
  });

  it('shows Upload snapshot button for CONTRIBUTOR', async () => {
    render(<ContinuousMonitoringTab authorization={makeAuth({ effectiveRole: 'CONTRIBUTOR' })} />);
    await waitFor(() => expect(apiClient.listConMonSnapshots).toHaveBeenCalled());
    expect(screen.getByRole('button', { name: /Upload snapshot/i })).toBeInTheDocument();
  });

  it('hides Upload snapshot button for VIEWER', async () => {
    render(<ContinuousMonitoringTab authorization={makeAuth({ effectiveRole: 'VIEWER' })} />);
    await waitFor(() => expect(apiClient.listConMonSnapshots).toHaveBeenCalled());
    expect(screen.queryByRole('button', { name: /Upload snapshot/i })).not.toBeInTheDocument();
  });
});
