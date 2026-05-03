import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MetadataEditor } from './MetadataEditor';
import type { Metadata } from '@/types/oscal-models';

function meta(overrides: Partial<Metadata> = {}): Metadata {
  return {
    title: 'Example',
    'last-modified': '2026-05-02T12:00:00Z',
    version: '1.0.0',
    'oscal-version': '1.1.3',
    ...overrides,
  };
}

describe('MetadataEditor', () => {
  it('renders the basic section open by default with title/version inputs', () => {
    render(<MetadataEditor value={meta()} onChange={() => {}} />);
    expect(screen.getByDisplayValue('Example')).toBeInTheDocument();
    expect(screen.getByDisplayValue('1.0.0')).toBeInTheDocument();
    expect(screen.getByDisplayValue('1.1.3')).toBeInTheDocument();
  });

  it('emits onChange when the title is edited', () => {
    const onChange = vi.fn();
    render(<MetadataEditor value={meta()} onChange={onChange} />);
    fireEvent.change(screen.getByDisplayValue('Example'), { target: { value: 'Renamed' } });
    expect(onChange).toHaveBeenCalled();
    const arg = onChange.mock.calls.at(-1)?.[0] as Metadata;
    expect(arg.title).toBe('Renamed');
    expect(arg.version).toBe('1.0.0');
  });

  it('reflects role/party counts in section headers', () => {
    render(
      <MetadataEditor
        value={meta({
          roles: [{ id: 'creator', title: 'Creator' }],
          parties: [
            { uuid: '1', type: 'organization', name: 'A' },
            { uuid: '2', type: 'person', name: 'B' },
          ],
        })}
        onChange={() => {}}
      />,
    );
    expect(screen.getByText(/Roles \(1\)/i)).toBeInTheDocument();
    expect(screen.getByText(/Parties \(2\)/i)).toBeInTheDocument();
  });

  it('switches to a different collapsible section when clicked', () => {
    render(<MetadataEditor value={meta()} onChange={() => {}} />);
    // Initially "Basic" is open and "Roles" is collapsed
    fireEvent.click(screen.getByText(/Roles/i));
    expect(screen.getByRole('button', { name: /add role/i })).toBeInTheDocument();
  });

  it('passes through props edits via the embedded PropsEditor', () => {
    const onChange = vi.fn();
    render(<MetadataEditor value={meta({ props: [{ name: 'k', value: 'v' }] })} onChange={onChange} />);
    fireEvent.click(screen.getByText(/Properties/i));
    fireEvent.change(screen.getByDisplayValue('k'), { target: { value: 'k2' } });
    const arg = onChange.mock.calls.at(-1)?.[0] as Metadata;
    expect(arg.props?.[0].name).toBe('k2');
  });
});
