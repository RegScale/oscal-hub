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

  it('opens a confirmation dialog and calls changeVisibility only after confirm', async () => {
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

    // Confirmation dialog appears, but no API call yet.
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    expect(libraryPublishApi.changeVisibility).not.toHaveBeenCalled();

    // Confirm — API call fires.
    await user.click(screen.getByRole('button', { name: /yes, publish/i }));
    await waitFor(() => {
      expect(libraryPublishApi.changeVisibility).toHaveBeenCalledWith('item-42', {
        visibility: 'PUBLIC',
        reason: undefined,
      });
    });
    expect(onChanged).toHaveBeenCalled();
  });

  it('cancel in the confirmation dialog leaves visibility unchanged', async () => {
    const user = userEvent.setup();
    render(
      <VisibilityActionMenu
        itemId="item-42"
        currentVisibility="PRIVATE"
        isCreator
        isSuperAdmin={false}
        onChanged={() => {}}
      />,
    );
    await user.click(screen.getByRole('button', { name: /^publish$/i }));
    await user.click(screen.getByRole('button', { name: /^cancel$/i }));
    expect(libraryPublishApi.changeVisibility).not.toHaveBeenCalled();
  });

  it('force-unpublish requires a reason before the confirm button calls the API', async () => {
    const user = userEvent.setup();
    render(
      <VisibilityActionMenu
        itemId="item-9"
        currentVisibility="PUBLIC"
        isCreator={false}
        isSuperAdmin
        onChanged={() => {}}
      />,
    );
    await user.click(screen.getByRole('button', { name: /force unpublish/i }));
    // Click confirm with empty reason — should show validation error, no API call.
    await user.click(screen.getByRole('button', { name: /^force-unpublish$/i }));
    expect(libraryPublishApi.changeVisibility).not.toHaveBeenCalled();
    expect(screen.getByText(/please provide a reason/i)).toBeInTheDocument();

    // Type a reason and confirm — API call fires with the reason.
    await user.type(screen.getByRole('textbox'), 'wrong control mappings');
    await user.click(screen.getByRole('button', { name: /^force-unpublish$/i }));
    await waitFor(() => {
      expect(libraryPublishApi.changeVisibility).toHaveBeenCalledWith('item-9', {
        visibility: 'PRIVATE',
        reason: 'wrong control mappings',
      });
    });
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
