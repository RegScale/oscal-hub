import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { PropsEditor } from './PropsEditor';

describe('PropsEditor', () => {
  it('renders no-rows hint and an Add button when value is undefined', () => {
    render(<PropsEditor value={undefined} onChange={() => {}} />);
    expect(screen.getByText(/no properties added yet/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /add property/i })).toBeInTheDocument();
  });

  it('emits a new prop array when Add is clicked', () => {
    const onChange = vi.fn();
    render(<PropsEditor value={undefined} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /add property/i }));
    expect(onChange).toHaveBeenCalledWith([{ name: '', value: '' }]);
  });

  it('updates name when the name input changes', () => {
    const onChange = vi.fn();
    render(<PropsEditor value={[{ name: 'oldname', value: 'v' }]} onChange={onChange} />);
    const nameInput = screen.getByDisplayValue('oldname');
    fireEvent.change(nameInput, { target: { value: 'newname' } });
    expect(onChange).toHaveBeenCalledWith([{ name: 'newname', value: 'v' }]);
  });

  it('emits undefined (not empty array) when last prop is removed', () => {
    const onChange = vi.fn();
    render(<PropsEditor value={[{ name: 'k', value: 'v' }]} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /remove item/i }));
    expect(onChange).toHaveBeenCalledWith(undefined);
  });

  it('clears optional fields by setting them to undefined when emptied', () => {
    const onChange = vi.fn();
    render(<PropsEditor value={[{ name: 'k', value: 'v', class: 'foo' }]} onChange={onChange} />);
    const classInput = screen.getByDisplayValue('foo');
    fireEvent.change(classInput, { target: { value: '' } });
    expect(onChange).toHaveBeenCalledWith([{ name: 'k', value: 'v', class: undefined }]);
  });
});
