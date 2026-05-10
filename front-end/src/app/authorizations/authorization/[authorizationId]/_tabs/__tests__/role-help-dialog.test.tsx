import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { RoleHelpDialog } from '../role-help-dialog';

describe('RoleHelpDialog', () => {
  it('renders nothing when closed', () => {
    render(<RoleHelpDialog open={false} onOpenChange={vi.fn()} />);
    expect(screen.queryByText(/Access levels/)).not.toBeInTheDocument();
  });

  it('renders the role descriptions when open', () => {
    render(<RoleHelpDialog open={true} onOpenChange={vi.fn()} />);
    expect(screen.getByText(/Access levels/)).toBeInTheDocument();
    expect(screen.getByText(/Full control/)).toBeInTheDocument();
    expect(screen.getByText(/Read-only/)).toBeInTheDocument();
  });

  it('renders the permission matrix with all four roles in headers', () => {
    render(<RoleHelpDialog open={true} onOpenChange={vi.fn()} />);
    expect(screen.getByRole('columnheader', { name: 'OWNER' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'EDITOR' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /CONTRIB/ })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'VIEWER' })).toBeInTheDocument();
  });

  it('explains the share-with-org behavior', () => {
    render(<RoleHelpDialog open={true} onOpenChange={vi.fn()} />);
    expect(
      screen.getByText(/OWNER cannot be set as the org-wide default/)
    ).toBeInTheDocument();
  });
});
