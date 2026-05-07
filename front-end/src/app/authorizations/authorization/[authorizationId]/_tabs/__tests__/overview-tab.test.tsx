import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { OverviewTab } from '../overview-tab';
import type { AuthorizationResponse } from '@/types/oscal';

vi.mock('../sharing-access-card', () => ({
  SharingAccessCard: () => <div data-testid="sharing-access-card">Mock Sharing Card</div>,
}));

function makeAuth(overrides: Partial<AuthorizationResponse> = {}): AuthorizationResponse {
  return {
    id: 1,
    organizationId: 100,
    name: 'A1',
    sspItemId: 'ssp-1',
    templateId: 1,
    templateName: 'T',
    variableValues: {},
    completedContent: '',
    authorizedBy: 'alice',
    authorizedAt: '2026-05-07T00:00:00Z',
    createdAt: '2026-05-07T00:00:00Z',
    ...overrides,
  } as AuthorizationResponse;
}

describe('OverviewTab', () => {
  it('renders SharingAccessCard for OWNER', () => {
    const auth = makeAuth({ effectiveRole: 'OWNER' });
    render(
      <OverviewTab authorization={auth} onAuthorizationUpdated={vi.fn()}>
        <div data-testid="children">existing detail content</div>
      </OverviewTab>
    );
    expect(screen.getByTestId('sharing-access-card')).toBeInTheDocument();
    expect(screen.getByTestId('children')).toBeInTheDocument();
  });

  it('does not render SharingAccessCard for EDITOR', () => {
    const auth = makeAuth({ effectiveRole: 'EDITOR' });
    render(
      <OverviewTab authorization={auth} onAuthorizationUpdated={vi.fn()}>
        <div data-testid="children">existing detail content</div>
      </OverviewTab>
    );
    expect(screen.queryByTestId('sharing-access-card')).not.toBeInTheDocument();
    expect(screen.getByTestId('children')).toBeInTheDocument();
  });

  it('does not render SharingAccessCard for CONTRIBUTOR', () => {
    const auth = makeAuth({ effectiveRole: 'CONTRIBUTOR' });
    render(
      <OverviewTab authorization={auth} onAuthorizationUpdated={vi.fn()}>
        <div data-testid="children">existing detail content</div>
      </OverviewTab>
    );
    expect(screen.queryByTestId('sharing-access-card')).not.toBeInTheDocument();
  });

  it('does not render SharingAccessCard for VIEWER', () => {
    const auth = makeAuth({ effectiveRole: 'VIEWER' });
    render(
      <OverviewTab authorization={auth} onAuthorizationUpdated={vi.fn()}>
        <div data-testid="children">existing detail content</div>
      </OverviewTab>
    );
    expect(screen.queryByTestId('sharing-access-card')).not.toBeInTheDocument();
  });

  it('does not render SharingAccessCard when effectiveRole is undefined', () => {
    const auth = makeAuth({ effectiveRole: undefined });
    render(
      <OverviewTab authorization={auth} onAuthorizationUpdated={vi.fn()}>
        <div data-testid="children">existing detail content</div>
      </OverviewTab>
    );
    expect(screen.queryByTestId('sharing-access-card')).not.toBeInTheDocument();
  });
});
