import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { RolesEditor } from './RolesEditor';

describe('RolesEditor', () => {
  it('renders the empty-state when no roles are set', () => {
    render(<RolesEditor value={undefined} onChange={() => {}} />);
    expect(screen.getByText(/no roles added yet/i)).toBeInTheDocument();
  });

  it('emits a new empty role on Add', () => {
    const onChange = vi.fn();
    render(<RolesEditor value={undefined} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /add role/i }));
    expect(onChange).toHaveBeenCalledWith([{ id: '', title: '' }]);
  });

  it('updates id and title independently', () => {
    const onChange = vi.fn();
    render(<RolesEditor value={[{ id: 'r1', title: 'Reviewer' }]} onChange={onChange} />);
    fireEvent.change(screen.getByDisplayValue('r1'), { target: { value: 'creator' } });
    expect(onChange).toHaveBeenLastCalledWith([{ id: 'creator', title: 'Reviewer' }]);
    fireEvent.change(screen.getByDisplayValue('Reviewer'), { target: { value: 'New' } });
    expect(onChange).toHaveBeenLastCalledWith([{ id: 'r1', title: 'New' }]);
  });

  it('clears optional short-name to undefined when emptied', () => {
    const onChange = vi.fn();
    render(
      <RolesEditor value={[{ id: 'r1', title: 'T', 'short-name': 'sn' }]} onChange={onChange} />,
    );
    fireEvent.change(screen.getByDisplayValue('sn'), { target: { value: '' } });
    expect(onChange).toHaveBeenCalledWith([{ id: 'r1', title: 'T', 'short-name': undefined }]);
  });
});
