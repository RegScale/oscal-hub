import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { UploadDocumentDialog } from '../upload-document-dialog';
import { apiClient } from '@/lib/api-client';

vi.mock('@/lib/api-client', () => ({
  apiClient: { uploadDocument: vi.fn() },
}));
vi.mock('sonner', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

describe('UploadDocumentDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders nothing when closed', () => {
    render(<UploadDocumentDialog authorizationId={1} open={false} onOpenChange={vi.fn()} onUploaded={vi.fn()} />);
    expect(screen.queryByText('Upload document')).not.toBeInTheDocument();
  });

  it('renders the form when open', () => {
    render(<UploadDocumentDialog authorizationId={1} open={true} onOpenChange={vi.fn()} onUploaded={vi.fn()} />);
    expect(screen.getByText('Upload document')).toBeInTheDocument();
    expect(screen.getByLabelText(/^File$/)).toBeInTheDocument();
  });

  it('disables Upload until a file is picked', () => {
    render(<UploadDocumentDialog authorizationId={1} open={true} onOpenChange={vi.fn()} onUploaded={vi.fn()} />);
    expect(screen.getByRole('button', { name: /^Upload$/ })).toBeDisabled();
  });

  it('calls uploadDocument when form is submitted', async () => {
    (apiClient.uploadDocument as any).mockResolvedValue({ id: 99 });
    const onUploaded = vi.fn();
    const onOpenChange = vi.fn();
    render(<UploadDocumentDialog authorizationId={42} open={true} onOpenChange={onOpenChange} onUploaded={onUploaded} />);

    const file = new File(['body'], 'doc.pdf', { type: 'application/pdf' });
    const input = screen.getByLabelText(/^File$/) as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: [file] });
    fireEvent.change(input);

    fireEvent.click(screen.getByRole('button', { name: /^Upload$/ }));

    await waitFor(() => {
      expect(apiClient.uploadDocument).toHaveBeenCalledWith(
        42,
        file,
        expect.objectContaining({ documentType: 'OTHER' })
      );
      expect(onUploaded).toHaveBeenCalled();
      expect(onOpenChange).toHaveBeenCalledWith(false);
    });
  });
});
