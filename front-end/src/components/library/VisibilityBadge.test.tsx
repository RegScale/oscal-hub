import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { VisibilityBadge } from './VisibilityBadge';

describe('<VisibilityBadge>', () => {
  it('renders Private with slate styling', () => {
    render(<VisibilityBadge visibility="PRIVATE" />);
    const el = screen.getByText(/private/i);
    expect(el).toBeInTheDocument();
    expect(el.className).toMatch(/slate|gray/);
  });

  it('renders Organization with blue styling', () => {
    render(<VisibilityBadge visibility="ORGANIZATION" />);
    expect(screen.getByText(/organization/i).className).toMatch(/blue/);
  });

  it('renders Public with green styling', () => {
    render(<VisibilityBadge visibility="PUBLIC" />);
    expect(screen.getByText(/public/i).className).toMatch(/green/);
  });
});
