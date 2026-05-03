import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { CatalogBuilderWizard } from './CatalogBuilderWizard';

const createMock = vi.fn();
const updateMock = vi.fn();
const validateMock = vi.fn();

vi.mock('@/lib/api-client', async () => {
  const actual = await vi.importActual<typeof import('@/lib/api-client')>('@/lib/api-client');
  return {
    ...actual,
    catalogBuilderApi: {
      create: (...args: unknown[]) => createMock(...args),
      update: (...args: unknown[]) => updateMock(...args),
      get: vi.fn(),
      getContent: vi.fn(),
      list: vi.fn(),
      search: vi.fn(),
      remove: vi.fn(),
      statistics: vi.fn(),
    },
    apiClient: {
      ...(actual.apiClient ?? {}),
      validate: (...args: unknown[]) => validateMock(...args),
    },
  };
});

describe('CatalogBuilderWizard', () => {
  beforeEach(() => {
    createMock.mockReset();
    updateMock.mockReset();
    validateMock.mockReset();
  });

  it('renders the metadata step on mount', () => {
    render(<CatalogBuilderWizard />);
    expect(screen.getByText(/New Catalog/i)).toBeInTheDocument();
    expect(screen.getByText(/1\. Metadata/i)).toBeInTheDocument();
    // Title input is required and visible
    expect(screen.getByPlaceholderText(/My Catalog/i)).toBeInTheDocument();
  });

  it('blocks Next when the title is cleared', () => {
    render(<CatalogBuilderWizard />);
    const titleInput = screen.getByPlaceholderText(/My Catalog/i);
    fireEvent.change(titleInput, { target: { value: '' } });
    const nextBtn = screen.getByRole('button', { name: /next/i });
    expect(nextBtn).toBeDisabled();
  });

  it('walks to step 5 once metadata title is set and shows validation', () => {
    render(<CatalogBuilderWizard />);
    fireEvent.change(screen.getByPlaceholderText(/My Catalog/i), {
      target: { value: 'Test Catalog' },
    });
    // Click step buttons to jump
    fireEvent.click(screen.getByRole('button', { name: /5\. Review/i }));
    // Validation alert mentions "at least one group or control"
    expect(screen.getByText(/at least one group or control/i)).toBeInTheDocument();
    // Save button disabled while validation fails
    const saveBtns = screen.getAllByRole('button', { name: /save catalog/i });
    saveBtns.forEach((b) => expect(b).toBeDisabled());
  });

  it('allows save after a control is added and posts to the API', async () => {
    createMock.mockResolvedValue({ id: 1 });

    const onSaveComplete = vi.fn();
    render(<CatalogBuilderWizard onSaveComplete={onSaveComplete} />);

    fireEvent.change(screen.getByPlaceholderText(/My Catalog/i), {
      target: { value: 'Test Catalog' },
    });

    // Step 3: Controls
    fireEvent.click(screen.getByRole('button', { name: /3\. Controls/i }));

    // Add a top-level control
    fireEvent.click(screen.getByRole('button', { name: /add control/i }));

    // Step 5: Review
    fireEvent.click(screen.getByRole('button', { name: /5\. Review/i }));

    // The Save button at the bottom of the review pane
    const saveBtn = screen.getAllByRole('button', { name: /save catalog/i }).find((b) => !b.hasAttribute('disabled'));
    expect(saveBtn).toBeDefined();
    await act(async () => {
      fireEvent.click(saveBtn!);
    });

    await waitFor(() => {
      expect(createMock).toHaveBeenCalledTimes(1);
    });
    const arg = createMock.mock.calls[0][0];
    expect(arg.title).toBe('Test Catalog');
    expect(arg.oscalVersion).toBeTruthy();
    expect(arg.jsonContent).toContain('"catalog"');
    expect(arg.controlCount).toBe(1);
    await waitFor(() => {
      expect(onSaveComplete).toHaveBeenCalled();
    });
  });

  it('imports a catalog JSON via the import dialog and pre-fills metadata', () => {
    render(<CatalogBuilderWizard />);
    fireEvent.click(screen.getByRole('button', { name: /import json/i }));
    fireEvent.click(screen.getByRole('tab', { name: /paste json/i }));
    const ta = screen.getByPlaceholderText(/"catalog"/);
    fireEvent.change(ta, {
      target: {
        value: JSON.stringify({
          catalog: {
            uuid: 'imported-uuid',
            metadata: {
              title: 'Imported Catalog',
              'oscal-version': '1.1.3',
              version: '2.0.0',
              'last-modified': '2026-05-01T00:00:00Z',
            },
          },
        }),
      },
    });
    fireEvent.click(screen.getByRole('button', { name: /^import$/i }));
    // After import the dialog closes and the title shows the imported value
    expect((screen.getByPlaceholderText(/My Catalog/i) as HTMLInputElement).value).toBe('Imported Catalog');
  });

  it('rejects an import that is missing required fields', () => {
    render(<CatalogBuilderWizard />);
    fireEvent.click(screen.getByRole('button', { name: /import json/i }));
    fireEvent.click(screen.getByRole('tab', { name: /paste json/i }));
    const ta = screen.getByPlaceholderText(/"catalog"/);
    fireEvent.change(ta, { target: { value: '{}' } });
    fireEvent.click(screen.getByRole('button', { name: /^import$/i }));
    expect(screen.getByText(/missing required/i)).toBeInTheDocument();
  });

  it('saves as draft without requiring a full catalog', async () => {
    createMock.mockResolvedValue({ id: 5, draft: true });
    const onSaveComplete = vi.fn();
    render(<CatalogBuilderWizard onSaveComplete={onSaveComplete} />);

    fireEvent.change(screen.getByPlaceholderText(/My Catalog/i), {
      target: { value: 'Half-finished Catalog' },
    });

    // No groups, no controls — would fail final validation.
    const draftBtn = screen.getByRole('button', { name: /save draft/i });
    expect(draftBtn).not.toBeDisabled();
    await act(async () => {
      fireEvent.click(draftBtn);
    });

    await waitFor(() => {
      expect(createMock).toHaveBeenCalledTimes(1);
    });
    const arg = createMock.mock.calls[0][0];
    expect(arg.draft).toBe(true);
    expect(arg.title).toBe('Half-finished Catalog');
    await waitFor(() => {
      expect(onSaveComplete).toHaveBeenCalled();
    });
  });

  it('disables Save draft when title is empty', () => {
    render(<CatalogBuilderWizard />);
    fireEvent.change(screen.getByPlaceholderText(/My Catalog/i), { target: { value: '' } });
    expect(screen.getByRole('button', { name: /save draft/i })).toBeDisabled();
  });

  it('shows a Draft badge when editing an existing draft', async () => {
    // Mock getContent so the load succeeds
    const apiClient = await import('@/lib/api-client');
    (apiClient.catalogBuilderApi.getContent as unknown as ReturnType<typeof vi.fn>) = vi
      .fn()
      .mockResolvedValue(JSON.stringify({ catalog: { uuid: 'u', metadata: { title: 'Existing draft', 'oscal-version': '1.1.3', version: '1.0.0', 'last-modified': '2026-05-02T00:00:00Z' } } }));

    render(
      <CatalogBuilderWizard
        editingCatalog={{
          id: 1,
          oscalUuid: 'u',
          title: 'Existing draft',
          oscalVersion: '1.1.3',
          storagePath: '',
          filename: 'x.json',
          fileSize: 0,
          draft: true,
          createdBy: 'me',
          createdAt: '',
          updatedAt: '',
        }}
      />,
    );
    await waitFor(() => {
      expect(screen.getByText('Draft')).toBeInTheDocument();
    });
  });
});
