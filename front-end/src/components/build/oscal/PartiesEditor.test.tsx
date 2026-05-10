import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { PartiesEditor } from './PartiesEditor';

describe('PartiesEditor', () => {
  it('renders empty state', () => {
    render(<PartiesEditor value={undefined} onChange={() => {}} />);
    expect(screen.getByText(/no parties added yet/i)).toBeInTheDocument();
  });

  it('Add creates an organization party with a generated UUID', () => {
    const onChange = vi.fn();
    render(<PartiesEditor value={undefined} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /add party/i }));
    expect(onChange).toHaveBeenCalledTimes(1);
    const next = onChange.mock.calls[0][0];
    expect(next).toHaveLength(1);
    expect(next[0].type).toBe('organization');
    expect(next[0].uuid).toMatch(/[0-9a-f-]{30,}/);
  });

  it('parses comma-separated email addresses into an array', () => {
    const onChange = vi.fn();
    render(
      <PartiesEditor
        value={[{ uuid: 'u', type: 'organization', name: 'Acme' }]}
        onChange={onChange}
      />,
    );
    const emailInput = screen.getByPlaceholderText(/contact@example\.com/);
    fireEvent.change(emailInput, { target: { value: 'a@x.com, b@x.com,  c@x.com' } });
    const next = onChange.mock.calls.at(-1)?.[0];
    expect(next[0]['email-addresses']).toEqual(['a@x.com', 'b@x.com', 'c@x.com']);
  });

  it('clears email-addresses to undefined when input is empty', () => {
    const onChange = vi.fn();
    render(
      <PartiesEditor
        value={[
          {
            uuid: 'u',
            type: 'organization',
            name: 'Acme',
            'email-addresses': ['a@x.com'],
          },
        ]}
        onChange={onChange}
      />,
    );
    const emailInput = screen.getByPlaceholderText(/contact@example\.com/);
    fireEvent.change(emailInput, { target: { value: '' } });
    const next = onChange.mock.calls.at(-1)?.[0];
    expect(next[0]['email-addresses']).toBeUndefined();
  });
});
