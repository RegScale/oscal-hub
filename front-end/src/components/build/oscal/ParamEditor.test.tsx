import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ParamEditor } from './ParamEditor';

describe('ParamEditor', () => {
  it('renders the empty state', () => {
    render(<ParamEditor value={undefined} onChange={() => {}} />);
    expect(screen.getByText(/no parameters added yet/i)).toBeInTheDocument();
  });

  it('Add creates a parameter with an empty id', () => {
    const onChange = vi.fn();
    render(<ParamEditor value={undefined} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /add parameter/i }));
    expect(onChange).toHaveBeenCalledWith([{ id: '' }]);
  });

  it('updates the parameter id', () => {
    const onChange = vi.fn();
    render(<ParamEditor value={[{ id: 'old' }]} onChange={onChange} />);
    fireEvent.change(screen.getByDisplayValue('old'), { target: { value: 'ac-1_prm_1' } });
    expect(onChange).toHaveBeenLastCalledWith([{ id: 'ac-1_prm_1' }]);
  });

  it('Add value adds an empty string to the values array', () => {
    const onChange = vi.fn();
    render(
      <ParamEditor
        value={[{ id: 'p', values: ['existing'] }]}
        onChange={onChange}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: /add value/i }));
    expect(onChange).toHaveBeenLastCalledWith([{ id: 'p', values: ['existing', ''] }]);
  });

  it('emits undefined when the last parameter is removed', () => {
    const onChange = vi.fn();
    render(<ParamEditor value={[{ id: 'p' }]} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /remove item/i }));
    expect(onChange).toHaveBeenCalledWith(undefined);
  });
});
