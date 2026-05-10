import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SaveToLibraryModal } from './SaveToLibraryModal';

describe('<SaveToLibraryModal>', () => {
  it('requires a title before submit is enabled', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(
      <SaveToLibraryModal
        open
        onClose={() => {}}
        onSubmit={onSubmit}
        defaultTitle=""
      />,
    );
    const button = screen.getByRole('button', { name: /save to library/i });
    expect(button).toBeDisabled();
    await user.type(screen.getByLabelText(/title/i), 'Hello');
    expect(button).not.toBeDisabled();
  });

  it('submits the form payload with selected visibility', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(
      <SaveToLibraryModal
        open
        onClose={() => {}}
        onSubmit={onSubmit}
        defaultTitle="My Catalog"
      />,
    );
    await user.click(screen.getByLabelText(/public/i));
    fireEvent.click(screen.getByRole('button', { name: /save to library/i }));
    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith(
        expect.objectContaining({
          title: 'My Catalog',
          visibility: 'PUBLIC',
        }),
      ),
    );
  });

  it('disables Organization radio when user has no organization', () => {
    render(
      <SaveToLibraryModal
        open
        onClose={() => {}}
        onSubmit={() => {}}
        defaultTitle=""
        userOrganizationId={null}
      />,
    );
    expect(screen.getByLabelText(/organization/i)).toBeDisabled();
  });
});
