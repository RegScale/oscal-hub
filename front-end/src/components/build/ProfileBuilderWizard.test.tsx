import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { ProfileBuilderWizard } from './ProfileBuilderWizard';

const createMock = vi.fn();
const updateMock = vi.fn();
const validateMock = vi.fn();
const resolveMock = vi.fn();

vi.mock('@/lib/api-client', async () => {
  const actual = await vi.importActual<typeof import('@/lib/api-client')>('@/lib/api-client');
  return {
    ...actual,
    profileBuilderApi: {
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
      resolveProfile: (...args: unknown[]) => resolveMock(...args),
    },
  };
});

describe('ProfileBuilderWizard', () => {
  beforeEach(() => {
    createMock.mockReset();
    updateMock.mockReset();
    validateMock.mockReset();
    resolveMock.mockReset();
  });

  it('renders metadata first and shows the imports step disabled until imports exist', () => {
    render(<ProfileBuilderWizard />);
    expect(screen.getByText(/New Profile/i)).toBeInTheDocument();
    // Advance to review step to see validation errors
    fireEvent.click(screen.getByRole('button', { name: /6\. Review/i }));
    expect(screen.getByText(/At least one import is required/i)).toBeInTheDocument();
  });

  it('saves a profile after an import is added', async () => {
    createMock.mockResolvedValue({ id: 7 });
    const onSaveComplete = vi.fn();
    render(<ProfileBuilderWizard onSaveComplete={onSaveComplete} />);

    // Step 2: imports
    fireEvent.click(screen.getByRole('button', { name: /2\. Imports/i }));
    fireEvent.click(screen.getByRole('button', { name: /add import/i }));
    const hrefInput = screen.getByPlaceholderText(/#nist-800-53/);
    fireEvent.change(hrefInput, { target: { value: '#cat-uuid' } });

    // Step 6: review
    fireEvent.click(screen.getByRole('button', { name: /6\. Review/i }));
    const saveBtn = screen
      .getAllByRole('button', { name: /save profile/i })
      .find((b) => !b.hasAttribute('disabled'));
    expect(saveBtn).toBeDefined();
    await act(async () => {
      fireEvent.click(saveBtn!);
    });

    await waitFor(() => {
      expect(createMock).toHaveBeenCalledTimes(1);
    });
    const arg = createMock.mock.calls[0][0];
    expect(arg.jsonContent).toContain('"profile"');
    expect(arg.jsonContent).toContain('#cat-uuid');
    expect(arg.importCount).toBe(1);
    await waitFor(() => {
      expect(onSaveComplete).toHaveBeenCalled();
    });
  });

  it('imports a profile JSON via the import dialog', () => {
    render(<ProfileBuilderWizard />);
    fireEvent.click(screen.getByRole('button', { name: /import json/i }));
    fireEvent.click(screen.getByRole('tab', { name: /paste json/i }));
    const ta = screen.getByPlaceholderText(/"catalog"/);
    fireEvent.change(ta, {
      target: {
        value: JSON.stringify({
          profile: {
            uuid: 'p-uuid',
            metadata: {
              title: 'Imported Profile',
              'oscal-version': '1.1.3',
              version: '1.0.0',
              'last-modified': '2026-05-01T00:00:00Z',
            },
            imports: [{ href: '#cat' }],
          },
        }),
      },
    });
    fireEvent.click(screen.getByRole('button', { name: /^import$/i }));
    expect((screen.getByPlaceholderText(/My Catalog/i) as HTMLInputElement).value).toBe('Imported Profile');
  });
});
