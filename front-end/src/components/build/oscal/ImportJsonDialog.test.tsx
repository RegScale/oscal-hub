import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ImportJsonDialog } from './ImportJsonDialog';

describe('ImportJsonDialog', () => {
  it('does not render content when closed', () => {
    render(
      <ImportJsonDialog open={false} onOpenChange={() => {}} target="catalog" onImport={() => null} />,
    );
    expect(screen.queryByText(/Import catalog JSON/)).toBeNull();
  });

  it('parses pasted JSON and calls onImport with the result', () => {
    const onImport = vi.fn().mockReturnValue(null);
    const onOpenChange = vi.fn();
    render(
      <ImportJsonDialog open={true} onOpenChange={onOpenChange} target="catalog" onImport={onImport} />,
    );
    fireEvent.click(screen.getByRole('tab', { name: /paste json/i }));
    const ta = screen.getByPlaceholderText(/"catalog"/);
    fireEvent.change(ta, { target: { value: '{"catalog":{"uuid":"u","metadata":{}}}' } });
    fireEvent.click(screen.getByRole('button', { name: /^import$/i }));
    expect(onImport).toHaveBeenCalledWith({ catalog: { uuid: 'u', metadata: {} } });
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it('shows a validation error when the JSON is malformed', () => {
    const onImport = vi.fn();
    render(
      <ImportJsonDialog open={true} onOpenChange={() => {}} target="catalog" onImport={onImport} />,
    );
    fireEvent.click(screen.getByRole('tab', { name: /paste json/i }));
    const ta = screen.getByPlaceholderText(/"catalog"/);
    fireEvent.change(ta, { target: { value: '{not json' } });
    fireEvent.click(screen.getByRole('button', { name: /^import$/i }));
    expect(screen.getByText(/Invalid JSON/i)).toBeInTheDocument();
    expect(onImport).not.toHaveBeenCalled();
  });

  it('surfaces the error string returned by onImport', () => {
    const onImport = vi.fn().mockReturnValue('Profile is missing required "uuid".');
    render(
      <ImportJsonDialog open={true} onOpenChange={() => {}} target="profile" onImport={onImport} />,
    );
    fireEvent.click(screen.getByRole('tab', { name: /paste json/i }));
    const ta = screen.getByPlaceholderText(/"catalog"/);
    fireEvent.change(ta, { target: { value: '{}' } });
    fireEvent.click(screen.getByRole('button', { name: /^import$/i }));
    expect(screen.getByText(/missing required "uuid"/)).toBeInTheDocument();
  });
});
