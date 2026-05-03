import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, within } from '@testing-library/react';
import { MarkdownField } from './MarkdownField';

describe('MarkdownField', () => {
  it('renders a textarea in edit mode by default', () => {
    render(<MarkdownField label="Body" value="hello" onChange={() => {}} />);
    const textarea = screen.getByDisplayValue('hello');
    expect(textarea.tagName).toBe('TEXTAREA');
  });

  it('emits onChange with the typed text', () => {
    const onChange = vi.fn();
    render(<MarkdownField value="" onChange={onChange} />);
    const ta = screen.getByRole('textbox');
    fireEvent.change(ta, { target: { value: 'new text' } });
    expect(onChange).toHaveBeenCalledWith('new text');
  });

  it('strips newlines when singleLine is set', () => {
    const onChange = vi.fn();
    render(<MarkdownField value="" onChange={onChange} singleLine />);
    const ta = screen.getByRole('textbox');
    fireEvent.change(ta, { target: { value: 'line1\nline2' } });
    expect(onChange).toHaveBeenCalledWith('line1 line2');
  });

  it('switches to a markdown preview when Preview is clicked', () => {
    render(<MarkdownField value="**bold**" onChange={() => {}} />);
    fireEvent.click(screen.getByRole('button', { name: /preview/i }));
    const preview = screen.getByTestId('markdown-preview');
    const strong = within(preview).getByText('bold');
    expect(strong.tagName).toBe('STRONG');
  });

  it('shows a fallback message in preview when value is empty', () => {
    render(<MarkdownField value="" onChange={() => {}} />);
    fireEvent.click(screen.getByRole('button', { name: /preview/i }));
    expect(screen.getByText(/nothing to preview/i)).toBeInTheDocument();
  });
});
