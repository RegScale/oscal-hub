import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { HelpButton } from '../HelpButton';

describe('HelpButton', () => {
  it('renders a link to the right guide path', () => {
    render(<HelpButton slug="library" />);
    const link = screen.getByRole('link', { name: /open help/i });
    expect(link).toHaveAttribute('href', '/guide/library/overview');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', expect.stringContaining('noopener'));
  });

  it('uses the admin overview slug', () => {
    render(<HelpButton slug="admin" />);
    expect(screen.getByRole('link')).toHaveAttribute('href', '/guide/admin/overview');
  });
});
