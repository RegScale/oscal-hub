import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ResolvePreviewPanel } from './ResolvePreviewPanel';

const resolveMock = vi.fn();

vi.mock('@/lib/api-client', () => ({
  apiClient: {
    resolveProfile: (...args: unknown[]) => resolveMock(...args),
  },
}));

describe('ResolvePreviewPanel', () => {
  beforeEach(() => {
    resolveMock.mockReset();
  });

  it('renders a Resolve button initially', () => {
    render(<ResolvePreviewPanel jsonContent="{}" />);
    expect(screen.getByRole('button', { name: /resolve/i })).toBeInTheDocument();
  });

  it('renders a JSON preview and control count on success', async () => {
    resolveMock.mockResolvedValue({
      success: true,
      resolvedCatalog: JSON.stringify({ catalog: { uuid: 'r' } }),
      controlCount: 17,
    });
    render(<ResolvePreviewPanel jsonContent="{}" />);
    fireEvent.click(screen.getByRole('button', { name: /resolve/i }));
    await waitFor(() => {
      expect(screen.getByText(/Resolved successfully/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/17 controls/i)).toBeInTheDocument();
    expect(resolveMock).toHaveBeenCalledWith({ profileContent: '{}', format: 'json' });
  });

  it('shows the error returned by the API when resolution fails', async () => {
    resolveMock.mockResolvedValue({ success: false, error: 'Could not load catalog' });
    render(<ResolvePreviewPanel jsonContent="{}" />);
    fireEvent.click(screen.getByRole('button', { name: /resolve/i }));
    await waitFor(() => {
      expect(screen.getByText(/Could not load catalog/i)).toBeInTheDocument();
    });
  });
});
