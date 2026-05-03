import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { SchemaValidationPanel } from './SchemaValidationPanel';

const validateMock = vi.fn();

vi.mock('@/lib/api-client', () => ({
  apiClient: {
    validate: (...args: unknown[]) => validateMock(...args),
  },
}));

describe('SchemaValidationPanel', () => {
  beforeEach(() => {
    validateMock.mockReset();
  });

  it('renders a Validate button initially', () => {
    render(<SchemaValidationPanel jsonContent="{}" modelType="catalog" />);
    expect(screen.getByRole('button', { name: /validate/i })).toBeInTheDocument();
  });

  it('shows a success alert when the API returns valid=true', async () => {
    validateMock.mockResolvedValue({ valid: true, errors: [], warnings: [], timestamp: '2026-05-02' });
    render(<SchemaValidationPanel jsonContent="{}" modelType="catalog" />);
    fireEvent.click(screen.getByRole('button', { name: /validate/i }));
    await waitFor(() => {
      expect(screen.getByText(/Document is valid/i)).toBeInTheDocument();
    });
    expect(validateMock).toHaveBeenCalledWith('{}', 'catalog', 'json');
  });

  it('lists individual errors when validation fails', async () => {
    validateMock.mockResolvedValue({
      valid: false,
      errors: [{ message: 'Missing uuid', severity: 'error' }],
      warnings: [{ message: 'Deprecated field', severity: 'warning' }],
      timestamp: '2026-05-02',
    });
    render(<SchemaValidationPanel jsonContent="{}" modelType="profile" />);
    fireEvent.click(screen.getByRole('button', { name: /validate/i }));
    await waitFor(() => {
      expect(screen.getByText(/Missing uuid/)).toBeInTheDocument();
    });
    expect(screen.getByText(/Deprecated field/)).toBeInTheDocument();
  });

  it('shows an error message when the API call rejects', async () => {
    validateMock.mockRejectedValue(new Error('Server error'));
    render(<SchemaValidationPanel jsonContent="{}" modelType="catalog" />);
    fireEvent.click(screen.getByRole('button', { name: /validate/i }));
    await waitFor(() => {
      expect(screen.getByText(/Server error/i)).toBeInTheDocument();
    });
  });
});
