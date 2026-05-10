import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { TicketStatusBadge } from '../TicketStatusBadge';

describe('TicketStatusBadge', () => {
  it('renders status label', () => {
    render(<TicketStatusBadge status="IN_PROGRESS" />);
    expect(screen.getByText('In Progress')).toBeInTheDocument();
  });
});
