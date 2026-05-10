import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { SharingAccessCard } from '../sharing-access-card';
import { apiClient } from '@/lib/api-client';
import { toast } from 'sonner';
import type {
  AuthorizationResponse,
  AuthorizationGrantResponse,
  OrgMemberResponse,
} from '@/types/oscal';

vi.mock('@/lib/api-client', () => ({
  apiClient: {
    listGrants: vi.fn(),
    addGrant: vi.fn(),
    updateGrant: vi.fn(),
    removeGrant: vi.fn(),
    setShareWithOrg: vi.fn(),
    listMyOrgMembers: vi.fn(),
  },
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

// Mock RoleHelpDialog to avoid Dialog portal issues in happy-dom
vi.mock('../role-help-dialog', () => ({
  RoleHelpDialog: ({ open }: { open: boolean; onOpenChange: (v: boolean) => void }) =>
    open ? <div data-testid="role-help-dialog">Role Help</div> : null,
}));

function makeAuth(overrides: Partial<AuthorizationResponse> = {}): AuthorizationResponse {
  return {
    id: 42,
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
    effectiveRole: 'OWNER',
    shareWithOrgDefaultRole: null,
    ...overrides,
  } as AuthorizationResponse;
}

const grant1: AuthorizationGrantResponse = {
  id: 11,
  userId: 2,
  username: 'bob',
  email: 'bob@org',
  firstName: 'Bob',
  lastName: 'B',
  role: 'EDITOR',
  grantedByUsername: 'alice',
  grantedAt: '2026-05-07T00:00:00Z',
};

const member1: OrgMemberResponse = {
  userId: 3,
  username: 'carol',
  email: 'carol@org',
  firstName: 'Carol',
  lastName: 'C',
};

describe('SharingAccessCard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (apiClient.listGrants as ReturnType<typeof vi.fn>).mockResolvedValue([]);
    (apiClient.listMyOrgMembers as ReturnType<typeof vi.fn>).mockResolvedValue([]);
  });

  it('loads grants and members on mount', async () => {
    render(<SharingAccessCard authorization={makeAuth()} onAuthorizationUpdated={vi.fn()} />);
    await waitFor(() => {
      expect(apiClient.listGrants).toHaveBeenCalledWith(42);
      expect(apiClient.listMyOrgMembers).toHaveBeenCalled();
    });
  });

  it('renders empty state when there are no grants and no shareWithOrg', async () => {
    render(<SharingAccessCard authorization={makeAuth()} onAuthorizationUpdated={vi.fn()} />);
    await waitFor(() => {
      expect(screen.getByText(/No explicit grants yet/)).toBeInTheDocument();
      expect(
        screen.getByText(/Only the creator and org admins can see this authorization/)
      ).toBeInTheDocument();
    });
  });

  it('mentions the org-wide default when shareWithOrg is set', async () => {
    render(
      <SharingAccessCard
        authorization={makeAuth({ shareWithOrgDefaultRole: 'VIEWER' })}
        onAuthorizationUpdated={vi.fn()}
      />
    );
    await waitFor(() => {
      expect(screen.getByText(/Org-wide VIEWER sharing is active/)).toBeInTheDocument();
    });
  });

  it('lists existing grants', async () => {
    (apiClient.listGrants as ReturnType<typeof vi.fn>).mockResolvedValue([grant1]);
    render(<SharingAccessCard authorization={makeAuth()} onAuthorizationUpdated={vi.fn()} />);
    await waitFor(() => {
      expect(screen.getByText(/Bob B/)).toBeInTheDocument();
      expect(screen.getByText(/bob@org/)).toBeInTheDocument();
    });
  });

  it('removes a grant when the trash button is clicked', async () => {
    (apiClient.listGrants as ReturnType<typeof vi.fn>).mockResolvedValue([grant1]);
    (apiClient.removeGrant as ReturnType<typeof vi.fn>).mockResolvedValue(undefined);
    render(<SharingAccessCard authorization={makeAuth()} onAuthorizationUpdated={vi.fn()} />);
    await waitFor(() => screen.getByText(/Bob B/));

    fireEvent.click(screen.getByLabelText(/Remove bob/i));

    await waitFor(() => {
      expect(apiClient.removeGrant).toHaveBeenCalledWith(42, 11);
      expect(toast.success).toHaveBeenCalledWith('Grant removed');
    });
  });

  it('adds a grant via UserPicker + role + Add button', async () => {
    (apiClient.listMyOrgMembers as ReturnType<typeof vi.fn>).mockResolvedValue([member1]);
    (apiClient.addGrant as ReturnType<typeof vi.fn>).mockResolvedValue({ ...grant1, userId: 3 });
    render(<SharingAccessCard authorization={makeAuth()} onAuthorizationUpdated={vi.fn()} />);

    // Wait for members to load.
    await waitFor(() => expect(apiClient.listMyOrgMembers).toHaveBeenCalled());

    // Open the picker (the first button containing "Select a user").
    const pickerButton = screen
      .getAllByRole('button')
      .find((b) => b.textContent?.includes('Select a user'));
    expect(pickerButton).toBeDefined();
    fireEvent.click(pickerButton!);

    // Select Carol from the dropdown.
    fireEvent.click(await screen.findByText(/Carol C/));

    // Click Add.
    fireEvent.click(screen.getByRole('button', { name: /^Add$/ }));

    await waitFor(() => {
      expect(apiClient.addGrant).toHaveBeenCalledWith(42, 3, 'VIEWER');
      expect(toast.success).toHaveBeenCalledWith('Grant added');
    });
  });

  it('shows "Not shared" as the current org-wide share display when shareWithOrgDefaultRole is null', async () => {
    render(<SharingAccessCard authorization={makeAuth()} onAuthorizationUpdated={vi.fn()} />);
    await waitFor(() => {
      // The Select trigger should display the current value = NONE → "Not shared"
      expect(screen.getByText(/Not shared/i)).toBeInTheDocument();
    });
  });
});
