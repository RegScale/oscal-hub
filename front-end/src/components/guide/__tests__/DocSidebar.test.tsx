import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { DocSidebar } from '../DocSidebar';

vi.mock('next/navigation', () => ({
  usePathname: () => '/guide/tools/validate',
}));

describe('DocSidebar', () => {
  it('renders all group labels', () => {
    render(<DocSidebar />);
    expect(screen.getByText('Getting Started')).toBeInTheDocument();
    expect(screen.getByText('Core Tools')).toBeInTheDocument();
    expect(screen.getByText('Reference')).toBeInTheDocument();
  });

  it('expands the group containing the active page', () => {
    render(<DocSidebar />);
    expect(screen.getByRole('link', { name: 'Validate' })).toBeInTheDocument();
  });

  it('marks the active link with aria-current="page"', () => {
    render(<DocSidebar />);
    const active = screen.getByRole('link', { name: 'Validate' });
    expect(active).toHaveAttribute('aria-current', 'page');
  });
});
