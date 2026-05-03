import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ResourcesEditor } from './ResourcesEditor';

describe('ResourcesEditor', () => {
  it('renders the empty state', () => {
    render(<ResourcesEditor value={undefined} onChange={() => {}} />);
    expect(screen.getByText(/no back-matter resources added yet/i)).toBeInTheDocument();
  });

  it('Add creates a resource with a generated UUID', () => {
    const onChange = vi.fn();
    render(<ResourcesEditor value={undefined} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /add resource/i }));
    const next = onChange.mock.calls[0][0];
    expect(next).toHaveLength(1);
    expect(next[0].uuid).toMatch(/[0-9a-f-]{30,}/);
  });

  it('updates title independently', () => {
    const onChange = vi.fn();
    render(
      <ResourcesEditor
        value={[{ uuid: 'u', title: 'Old' }]}
        onChange={onChange}
      />,
    );
    fireEvent.change(screen.getByDisplayValue('Old'), { target: { value: 'New' } });
    const next = onChange.mock.calls.at(-1)?.[0];
    expect(next[0].title).toBe('New');
  });

  it('emits undefined when the last resource is removed', () => {
    const onChange = vi.fn();
    render(<ResourcesEditor value={[{ uuid: 'u' }]} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /remove item/i }));
    expect(onChange).toHaveBeenCalledWith(undefined);
  });
});
