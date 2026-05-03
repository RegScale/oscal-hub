import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ImportEditor } from './ImportEditor';

describe('ImportEditor', () => {
  it('renders an empty list and an Add button', () => {
    render(<ImportEditor value={[]} onChange={() => {}} />);
    expect(screen.getByText(/no imports added yet/i)).toBeInTheDocument();
  });

  it('Add creates an import with include-all set to {}', () => {
    const onChange = vi.fn();
    render(<ImportEditor value={[]} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /add import/i }));
    expect(onChange).toHaveBeenCalledWith([{ href: '', 'include-all': {} }]);
  });

  it('updates href as the user types', () => {
    const onChange = vi.fn();
    render(
      <ImportEditor
        value={[{ href: '#initial', 'include-all': {} }]}
        onChange={onChange}
      />,
    );
    fireEvent.change(screen.getByDisplayValue('#initial'), { target: { value: '#new' } });
    const next = onChange.mock.calls.at(-1)?.[0];
    expect(next[0].href).toBe('#new');
  });

  it('toggling Include all off swaps in include-controls and removes include-all', () => {
    const onChange = vi.fn();
    render(
      <ImportEditor
        value={[{ href: '#x', 'include-all': {} }]}
        onChange={onChange}
      />,
    );
    fireEvent.click(screen.getByRole('switch'));
    const next = onChange.mock.calls.at(-1)?.[0];
    expect(next[0]['include-all']).toBeUndefined();
    expect(next[0]['include-controls']).toEqual([]);
  });

  it('toggling Include all on removes include-controls and sets include-all to {}', () => {
    const onChange = vi.fn();
    render(
      <ImportEditor
        value={[{ href: '#x', 'include-controls': [{ 'with-ids': ['ac-1'] }] }]}
        onChange={onChange}
      />,
    );
    fireEvent.click(screen.getByRole('switch'));
    const next = onChange.mock.calls.at(-1)?.[0];
    expect(next[0]['include-all']).toEqual({});
    expect(next[0]['include-controls']).toBeUndefined();
  });
});
