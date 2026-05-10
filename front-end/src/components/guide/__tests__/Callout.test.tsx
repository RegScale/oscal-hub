import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { Callout } from '../Callout';

describe('Callout', () => {
  it('renders children with role=note', () => {
    render(<Callout type="info">Hello world</Callout>);
    const note = screen.getByRole('note');
    expect(note).toHaveTextContent('Hello world');
  });

  it('applies the correct class for type=warn', () => {
    render(<Callout type="warn">Warning text</Callout>);
    const note = screen.getByRole('note');
    expect(note.className).toMatch(/warn/);
  });

  it('applies the correct class for type=danger', () => {
    render(<Callout type="danger">Danger text</Callout>);
    const note = screen.getByRole('note');
    expect(note.className).toMatch(/danger/);
  });
});
