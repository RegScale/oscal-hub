import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { JsonPreview } from './JsonPreview';

describe('JsonPreview', () => {
  const writeText = vi.fn().mockResolvedValue(undefined);
  beforeEach(() => {
    writeText.mockClear();
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      writable: true,
      value: { writeText },
    });
  });

  it('renders the value as pretty-printed JSON', () => {
    render(<JsonPreview value={{ hello: 'world' }} />);
    const code = screen.getByText(/"hello": "world"/);
    expect(code).toBeInTheDocument();
  });

  it('handles non-serializable values gracefully', () => {
    const cyclic: Record<string, unknown> = {};
    cyclic.self = cyclic;
    render(<JsonPreview value={cyclic} />);
    expect(screen.getByText(/Unable to serialize value/)).toBeInTheDocument();
  });

  it('copies JSON to the clipboard when Copy is clicked', async () => {
    render(<JsonPreview value={{ a: 1 }} />);
    fireEvent.click(screen.getByRole('button', { name: /copy/i }));
    await waitFor(() => {
      expect(writeText).toHaveBeenCalledWith(JSON.stringify({ a: 1 }, null, 2));
    });
  });
});
