import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { UploadSnapshotDialog } from '../upload-snapshot-dialog';
import { apiClient } from '@/lib/api-client';

vi.mock('@/lib/api-client', () => ({
  apiClient: { uploadConMonSnapshot: vi.fn() },
}));
vi.mock('sonner', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

describe('UploadSnapshotDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders nothing when closed', () => {
    render(
      <UploadSnapshotDialog
        authorizationId={1}
        open={false}
        onOpenChange={vi.fn()}
        onUploaded={vi.fn()}
      />
    );
    expect(screen.queryByText('Upload POAM snapshot')).not.toBeInTheDocument();
  });

  it('renders the form when open', () => {
    render(
      <UploadSnapshotDialog
        authorizationId={1}
        open={true}
        onOpenChange={vi.fn()}
        onUploaded={vi.fn()}
      />
    );
    expect(screen.getByText('Upload POAM snapshot')).toBeInTheDocument();
    expect(screen.getByLabelText(/^File$/)).toBeInTheDocument();
  });

  it('disables Upload until a file is picked', () => {
    render(
      <UploadSnapshotDialog
        authorizationId={1}
        open={true}
        onOpenChange={vi.fn()}
        onUploaded={vi.fn()}
      />
    );
    expect(screen.getByRole('button', { name: /^Upload$/ })).toBeDisabled();
  });

  it('calls uploadConMonSnapshot when form is submitted', async () => {
    (apiClient.uploadConMonSnapshot as any).mockResolvedValue({ id: 55 });
    const onUploaded = vi.fn();
    const onOpenChange = vi.fn();
    render(
      <UploadSnapshotDialog
        authorizationId={42}
        open={true}
        onOpenChange={onOpenChange}
        onUploaded={onUploaded}
      />
    );

    const file = new File(['content'], 'poam.json', { type: 'application/json' });
    const input = screen.getByLabelText(/^File$/) as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: [file] });
    fireEvent.change(input);

    fireEvent.click(screen.getByRole('button', { name: /^Upload$/ }));

    await waitFor(() => {
      expect(apiClient.uploadConMonSnapshot).toHaveBeenCalledWith(42, file, undefined);
      expect(onUploaded).toHaveBeenCalled();
      expect(onOpenChange).toHaveBeenCalledWith(false);
    });
  });
});
