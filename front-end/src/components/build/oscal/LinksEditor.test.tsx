import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { LinksEditor } from './LinksEditor';

describe('LinksEditor', () => {
  it('shows the empty-state hint and an Add button', () => {
    render(<LinksEditor value={undefined} onChange={() => {}} />);
    expect(screen.getByText(/no links added yet/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /add link/i })).toBeInTheDocument();
  });

  it('emits a new link with empty href when Add is clicked', () => {
    const onChange = vi.fn();
    render(<LinksEditor value={undefined} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /add link/i }));
    expect(onChange).toHaveBeenCalledWith([{ href: '' }]);
  });

  it('updates href when the input changes', () => {
    const onChange = vi.fn();
    render(<LinksEditor value={[{ href: 'https://old.example' }]} onChange={onChange} />);
    fireEvent.change(screen.getByDisplayValue('https://old.example'), {
      target: { value: 'https://new.example' },
    });
    expect(onChange).toHaveBeenCalledWith([{ href: 'https://new.example' }]);
  });

  it('clears empty optional fields to undefined', () => {
    const onChange = vi.fn();
    render(
      <LinksEditor value={[{ href: 'h', rel: 'reference' }]} onChange={onChange} />,
    );
    fireEvent.change(screen.getByDisplayValue('reference'), { target: { value: '' } });
    expect(onChange).toHaveBeenCalledWith([{ href: 'h', rel: undefined }]);
  });

  it('emits undefined when the last link is removed', () => {
    const onChange = vi.fn();
    render(<LinksEditor value={[{ href: 'h' }]} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /remove item/i }));
    expect(onChange).toHaveBeenCalledWith(undefined);
  });
});
