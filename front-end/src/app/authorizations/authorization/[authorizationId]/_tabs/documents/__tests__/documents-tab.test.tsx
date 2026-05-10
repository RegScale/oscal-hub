import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { DocumentsTab } from '../../documents-tab';
import { apiClient } from '@/lib/api-client';
import type { AuthorizationResponse, AuthorizationDocumentResponse } from '@/types/oscal';

vi.mock('@/lib/api-client', () => ({
  apiClient: {
    listDocuments: vi.fn(),
    getPackageCompleteness: vi.fn(),
    downloadDocument: vi.fn(),
    deleteDocument: vi.fn(),
  },
}));

vi.mock('sonner', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

// Mock the dialogs to avoid Radix portal complications.
vi.mock('../upload-document-dialog', () => ({
  UploadDocumentDialog: ({ open }: { open: boolean }) =>
    open ? <div data-testid="upload-dialog">upload</div> : null,
}));
vi.mock('../edit-document-metadata-dialog', () => ({
  EditDocumentMetadataDialog: ({ open }: { open: boolean }) =>
    open ? <div data-testid="edit-dialog">edit</div> : null,
}));

function makeAuth(overrides: Partial<AuthorizationResponse> = {}): AuthorizationResponse {
  return {
    id: 1,
    organizationId: 100,
    name: 'A',
    sspItemId: 'ssp',
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

const doc: AuthorizationDocumentResponse = {
  id: 1, authorizationId: 1, originalFilename: 'pen.pdf', fileSize: 1024,
  contentType: 'application/pdf', documentType: 'PENETRATION_TEST',
  description: 'Q3', tags: null, version: 'v1', effectiveDate: null,
  expiresAt: null, uploadedByUsername: 'alice', uploadedAt: '2026-05-07T00:00:00Z',
};

describe('DocumentsTab', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (apiClient.listDocuments as any).mockResolvedValue([]);
    (apiClient.getPackageCompleteness as any).mockResolvedValue({ coreDocuments: [] });
  });

  it('shows upload button for OWNER', async () => {
    render(<DocumentsTab authorization={makeAuth({ effectiveRole: 'OWNER' })} />);
    await waitFor(() => expect(apiClient.listDocuments).toHaveBeenCalled());
    expect(screen.getByRole('button', { name: /Upload document/i })).toBeInTheDocument();
  });

  it('shows upload button for CONTRIBUTOR', async () => {
    render(<DocumentsTab authorization={makeAuth({ effectiveRole: 'CONTRIBUTOR' })} />);
    await waitFor(() => expect(apiClient.listDocuments).toHaveBeenCalled());
    expect(screen.getByRole('button', { name: /Upload document/i })).toBeInTheDocument();
  });

  it('hides upload button for VIEWER', async () => {
    render(<DocumentsTab authorization={makeAuth({ effectiveRole: 'VIEWER' })} />);
    await waitFor(() => expect(apiClient.listDocuments).toHaveBeenCalled());
    expect(screen.queryByRole('button', { name: /Upload document/i })).not.toBeInTheDocument();
  });

  it('shows empty state when no documents', async () => {
    render(<DocumentsTab authorization={makeAuth()} />);
    await waitFor(() => expect(screen.getByText(/No documents yet/i)).toBeInTheDocument());
  });

  it('renders documents in a table', async () => {
    (apiClient.listDocuments as any).mockResolvedValue([doc]);
    render(<DocumentsTab authorization={makeAuth()} />);
    await waitFor(() => expect(screen.getByText('pen.pdf')).toBeInTheDocument());
  });
});
