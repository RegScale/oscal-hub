import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { DateTimeField } from './DateTimeField';

describe('DateTimeField', () => {
  it('renders the date and time inputs with the parsed value', () => {
    render(<DateTimeField value="2026-05-02T13:45:00Z" onChange={() => {}} label="Last modified" />);
    expect(screen.getByLabelText(/Time/i)).toHaveValue('13:45');
    // The native date input lives next to the popover trigger
    const dateInputs = document.querySelectorAll('input[type="date"]');
    expect(dateInputs[0]).toHaveValue('2026-05-02');
    // The full ISO is shown for confirmation
    expect(screen.getByText(/2026-05-02T13:45:00/)).toBeInTheDocument();
  });

  it('emits an updated ISO string when the time changes', () => {
    const onChange = vi.fn();
    render(<DateTimeField value="2026-05-02T13:00:00Z" onChange={onChange} />);
    fireEvent.change(screen.getByLabelText(/Time/i), { target: { value: '08:30' } });
    expect(onChange).toHaveBeenCalled();
    const emitted = onChange.mock.calls.at(-1)?.[0] as string;
    expect(emitted).toContain('T08:30:');
    expect(emitted.endsWith('Z') || emitted.endsWith('+00:00')).toBe(true);
  });

  it('emits an ISO string when the date input changes', () => {
    const onChange = vi.fn();
    render(<DateTimeField value="2026-05-02T12:00:00Z" onChange={onChange} />);
    const dateInput = document.querySelector('input[type="date"]') as HTMLInputElement;
    fireEvent.change(dateInput, { target: { value: '2030-01-15' } });
    const emitted = onChange.mock.calls.at(-1)?.[0] as string;
    expect(emitted).toMatch(/^2030-01-15T/);
  });

  it('clears the value when Clear is clicked', () => {
    const onChange = vi.fn();
    render(<DateTimeField value="2026-05-02T12:00:00Z" onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /clear/i }));
    expect(onChange).toHaveBeenCalledWith('');
  });

  it('hides the Clear button when allowClear=false', () => {
    render(<DateTimeField value="2026-05-02T12:00:00Z" onChange={() => {}} allowClear={false} />);
    expect(screen.queryByRole('button', { name: /clear/i })).toBeNull();
  });

  it('emits a current ISO string when Now is clicked', () => {
    const onChange = vi.fn();
    render(<DateTimeField value="" onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /now/i }));
    const emitted = onChange.mock.calls.at(-1)?.[0] as string;
    expect(emitted).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/);
  });

  it('renders empty inputs when value is blank', () => {
    render(<DateTimeField value="" onChange={() => {}} label="Published" />);
    const dateInput = document.querySelector('input[type="date"]') as HTMLInputElement;
    expect(dateInput.value).toBe('');
    expect(screen.getByLabelText(/Time/i)).toHaveValue('');
  });
});
