import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { VisibilityActionMenu } from './VisibilityActionMenu';

// Mock the API surface so tests don't hit fetch.
vi.mock('@/lib/api/library', () => ({
  libraryPublishApi: {
    changeVisibility: vi.fn().mockResolvedValue({}),
  },
}));

import { libraryPublishApi } from '@/lib/api/library';

describe('<VisibilityActionMenu>', () => {
  beforeEach(() => {
    vi.mocked(libraryPublishApi.changeVisibility).mockClear();
  });

  it('renders nothing when caller is neither creator nor super-admin', () => {
    const { container } = render(
      <VisibilityActionMenu
        itemId="item-1"
        currentVisibility="PRIVATE"
        isCreator={false}
        isSuperAdmin={false}
        onChanged={() => {}}
      />,
    );
    expect(container).toBeEmptyDOMElement();
  });

  it('shows the two valid transitions for a creator on a PRIVATE item', () => {
    render(
      <VisibilityActionMenu
        itemId="item-1"
        currentVisibility="PRIVATE"
        isCreator
        isSuperAdmin={false}
        onChanged={() => {}}
      />,
    );
    // From PRIVATE: cannot Make Private; can Share-with-Org and Publish.
    expect(screen.queryByRole('button', { name: /make private/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /share with org/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^publish$/i })).toBeInTheDocument();
  });

  it('calls libraryPublishApi.changeVisibility with PUBLIC and triggers onChanged', async () => {
    const user = userEvent.setup();
    const onChanged = vi.fn();
    render(
      <VisibilityActionMenu
        itemId="item-42"
        currentVisibility="PRIVATE"
        isCreator
        isSuperAdmin={false}
        onChanged={onChanged}
      />,
    );
    await user.click(screen.getByRole('button', { name: /^publish$/i }));
    await waitFor(() => {
      expect(libraryPublishApi.changeVisibility).toHaveBeenCalledWith('item-42', {
        visibility: 'PUBLIC',
        reason: undefined,
      });
    });
    expect(onChanged).toHaveBeenCalled();
  });

  it('shows force-unpublish only for super-admins acting on someone else\'s PUBLIC item', () => {
    const { rerender } = render(
      <VisibilityActionMenu
        itemId="item-1"
        currentVisibility="PUBLIC"
        isCreator
        isSuperAdmin
        onChanged={() => {}}
      />,
    );
    // Creator + super-admin: no force-unpublish (creator can just Make Private themselves).
    expect(screen.queryByRole('button', { name: /force unpublish/i })).not.toBeInTheDocument();

    rerender(
      <VisibilityActionMenu
        itemId="item-1"
        currentVisibility="PUBLIC"
        isCreator={false}
        isSuperAdmin
        onChanged={() => {}}
      />,
    );
    expect(screen.getByRole('button', { name: /force unpublish/i })).toBeInTheDocument();
  });
});
