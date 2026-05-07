import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { UserPicker } from '@/components/user-picker';
import type { OrgMemberResponse } from '@/types/oscal';

const members: OrgMemberResponse[] = [
  { userId: 1, username: 'alice', email: 'alice@org', firstName: 'Alice', lastName: 'A' },
  { userId: 2, username: 'bob', email: 'bob@org', firstName: 'Bob', lastName: 'B' },
  { userId: 3, username: 'carol', email: 'carol@org', firstName: 'Carol', lastName: 'C' },
];

describe('UserPicker', () => {
  it('shows the placeholder when no value is selected', () => {
    render(<UserPicker value={null} onChange={vi.fn()} members={members} placeholder="Pick someone" />);
    expect(screen.getByText('Pick someone')).toBeInTheDocument();
  });

  it('shows the selected user when value matches a member', () => {
    render(<UserPicker value={1} onChange={vi.fn()} members={members} />);
    expect(screen.getByText(/Alice A/)).toBeInTheDocument();
    expect(screen.getByText(/alice/)).toBeInTheDocument();
  });

  it('opens the dropdown and shows all members on click', () => {
    render(<UserPicker value={null} onChange={vi.fn()} members={members} />);
    fireEvent.click(screen.getByRole('button'));
    expect(screen.getByText(/Bob B/)).toBeInTheDocument();
    expect(screen.getByText(/Carol C/)).toBeInTheDocument();
  });

  it('filters by search query (username)', () => {
    render(<UserPicker value={null} onChange={vi.fn()} members={members} />);
    fireEvent.click(screen.getByRole('button'));
    fireEvent.change(screen.getByPlaceholderText(/Search users/i), { target: { value: 'bob' } });
    expect(screen.getByText(/Bob B/)).toBeInTheDocument();
    expect(screen.queryByText(/Alice A/)).not.toBeInTheDocument();
  });

  it('filters by search query (email)', () => {
    render(<UserPicker value={null} onChange={vi.fn()} members={members} />);
    fireEvent.click(screen.getByRole('button'));
    fireEvent.change(screen.getByPlaceholderText(/Search users/i), { target: { value: 'carol@' } });
    expect(screen.getByText(/Carol C/)).toBeInTheDocument();
    expect(screen.queryByText(/Bob B/)).not.toBeInTheDocument();
  });

  it('excludes users in excludeUserIds', () => {
    render(<UserPicker value={null} onChange={vi.fn()} members={members} excludeUserIds={[2]} />);
    fireEvent.click(screen.getByRole('button'));
    expect(screen.getByText(/Alice A/)).toBeInTheDocument();
    expect(screen.queryByText(/Bob B/)).not.toBeInTheDocument();
    expect(screen.getByText(/Carol C/)).toBeInTheDocument();
  });

  it('calls onChange when a user is picked', () => {
    const onChange = vi.fn();
    render(<UserPicker value={null} onChange={onChange} members={members} />);
    fireEvent.click(screen.getByRole('button'));
    fireEvent.click(screen.getByText(/Alice A/));
    expect(onChange).toHaveBeenCalledWith(1);
  });
});
