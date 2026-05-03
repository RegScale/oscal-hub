import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { SelectControlEditor } from './SelectControlEditor';

describe('SelectControlEditor', () => {
  it('renders the empty state', () => {
    render(<SelectControlEditor value={undefined} onChange={() => {}} label="Include" />);
    expect(screen.getByText(/Include/i)).toBeInTheDocument();
    expect(screen.getByText(/none\./i)).toBeInTheDocument();
  });

  it('Add appends a selector with empty with-ids', () => {
    const onChange = vi.fn();
    render(<SelectControlEditor value={undefined} onChange={onChange} label="Include" />);
    fireEvent.click(screen.getByRole('button', { name: /add selector/i }));
    expect(onChange).toHaveBeenCalledWith([{ 'with-ids': [] }]);
  });

  it('parses comma-separated control IDs into an array', () => {
    const onChange = vi.fn();
    render(
      <SelectControlEditor
        value={[{ 'with-ids': [] }]}
        onChange={onChange}
        label="Include controls"
      />,
    );
    const idsInput = screen.getByPlaceholderText(/ac-1, ac-2, ac-3/);
    fireEvent.change(idsInput, { target: { value: 'ac-1, au-2,si-3' } });
    expect(onChange).toHaveBeenLastCalledWith([{ 'with-ids': ['ac-1', 'au-2', 'si-3'] }]);
  });

  it('parses match-pattern textarea, one per line', () => {
    const onChange = vi.fn();
    render(
      <SelectControlEditor
        value={[{ 'with-ids': [] }]}
        onChange={onChange}
        label="Include controls"
      />,
    );
    const ta = screen.getByPlaceholderText(/ac-\*/);
    fireEvent.change(ta, { target: { value: 'ac-*\nau-?\n' } });
    expect(onChange).toHaveBeenLastCalledWith([
      { 'with-ids': [], matching: [{ pattern: 'ac-*' }, { pattern: 'au-?' }] },
    ]);
  });

  it('clears with-ids to undefined when input is emptied', () => {
    const onChange = vi.fn();
    render(
      <SelectControlEditor
        value={[{ 'with-ids': ['ac-1'] }]}
        onChange={onChange}
        label="Include"
      />,
    );
    const idsInput = screen.getByDisplayValue('ac-1');
    fireEvent.change(idsInput, { target: { value: '  ,  ' } });
    expect(onChange).toHaveBeenLastCalledWith([{ 'with-ids': undefined }]);
  });

  it('removes the selector when the trash button is clicked', () => {
    const onChange = vi.fn();
    render(
      <SelectControlEditor
        value={[{ 'with-ids': ['ac-1'] }, { 'with-ids': ['au-2'] }]}
        onChange={onChange}
        label="Include"
      />,
    );
    const removeButtons = screen.getAllByRole('button').filter((b) =>
      b.querySelector('svg.lucide-trash2'),
    );
    fireEvent.click(removeButtons[0]);
    expect(onChange).toHaveBeenLastCalledWith([{ 'with-ids': ['au-2'] }]);
  });
});
