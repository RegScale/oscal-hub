import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { OscalDocumentWizard } from './OscalDocumentWizard';

const createMock = vi.fn();
const updateMock = vi.fn();
const getContentMock = vi.fn();
const validateMock = vi.fn();

vi.mock('@/lib/api-client', async () => {
  const actual = await vi.importActual<typeof import('@/lib/api-client')>('@/lib/api-client');
  return {
    ...actual,
    oscalDocumentApi: {
      create: (...args: unknown[]) => createMock(...args),
      update: (...args: unknown[]) => updateMock(...args),
      get: vi.fn(),
      getContent: (...args: unknown[]) => getContentMock(...args),
      list: vi.fn(),
      search: vi.fn(),
      remove: vi.fn(),
    },
    apiClient: {
      ...(actual.apiClient ?? {}),
      validate: (...args: unknown[]) => validateMock(...args),
    },
  };
});

// Stub the lazy Monaco editor with a textarea that lets tests drive bodyText.
vi.mock('@/components/lazy/LazyMonacoEditor', () => ({
  LazyMonacoEditor: ({
    value,
    onChange,
  }: {
    value?: string;
    onChange?: (next: string | undefined) => void;
  }) => (
    <textarea
      data-testid="monaco-stub"
      value={value ?? ''}
      onChange={(e) => onChange?.(e.target.value)}
    />
  ),
}));

describe('OscalDocumentWizard', () => {
  beforeEach(() => {
    createMock.mockReset();
    updateMock.mockReset();
    getContentMock.mockReset();
    validateMock.mockReset();
  });

  it('renders with the right title for SSP and shows the seeded body in Monaco', () => {
    render(<OscalDocumentWizard modelType="system-security-plan" />);
    expect(screen.getByText(/New System Security Plan/i)).toBeInTheDocument();
    // Step 3 is where Monaco lives — switch to it
    fireEvent.click(screen.getByRole('button', { name: /3\. Body/i }));
    const monaco = screen.getByTestId('monaco-stub') as HTMLTextAreaElement;
    expect(monaco.value).toContain('system-characteristics');
  });

  it('does not require import-profile href for SSP final save', () => {
    render(<OscalDocumentWizard modelType="system-security-plan" />);
    fireEvent.click(screen.getByRole('button', { name: /5\. Review/i }));
    expect(screen.queryByText(/import-profile href is required/i)).not.toBeInTheDocument();
  });

  it('saves an SSP draft without the import href', async () => {
    createMock.mockResolvedValue({ id: 9, draft: true });
    const onSaveComplete = vi.fn();
    render(<OscalDocumentWizard modelType="system-security-plan" onSaveComplete={onSaveComplete} />);

    fireEvent.change(screen.getByPlaceholderText(/My Catalog/i), {
      target: { value: 'WIP SSP' },
    });
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /save draft/i }));
    });
    await waitFor(() => {
      expect(createMock).toHaveBeenCalledTimes(1);
    });
    const arg = createMock.mock.calls[0][0];
    expect(arg.modelType).toBe('system-security-plan');
    expect(arg.draft).toBe(true);
    expect(arg.title).toBe('WIP SSP');
    expect(arg.jsonContent).toContain('"system-security-plan"');
  });

  it('disables Save draft when Monaco JSON is invalid', async () => {
    render(<OscalDocumentWizard modelType="assessment-plan" />);
    fireEvent.change(screen.getByPlaceholderText(/My Catalog/i), {
      target: { value: 'AP draft' },
    });
    fireEvent.click(screen.getByRole('button', { name: /3\. Body/i }));
    const monaco = screen.getByTestId('monaco-stub') as HTMLTextAreaElement;
    fireEvent.change(monaco, { target: { value: '{ this is not json' } });
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /save draft/i })).toBeDisabled();
    });
  });

  it('saves a final POA&M without requiring an import href', async () => {
    createMock.mockResolvedValue({ id: 7, draft: false });
    render(<OscalDocumentWizard modelType="plan-of-action-and-milestones" />);
    fireEvent.change(screen.getByPlaceholderText(/My Catalog/i), {
      target: { value: 'Final POAM' },
    });
    fireEvent.click(screen.getByRole('button', { name: /5\. Review/i }));
    // No basic-validation alert blocking save
    expect(screen.queryByText(/href is required/i)).toBeNull();
    const saveBtn = screen
      .getAllByRole('button', { name: /save plan of action and milestones/i })
      .find((b) => !b.hasAttribute('disabled'));
    expect(saveBtn).toBeDefined();
    await act(async () => {
      fireEvent.click(saveBtn!);
    });
    await waitFor(() => {
      expect(createMock).toHaveBeenCalledTimes(1);
    });
    const arg = createMock.mock.calls[0][0];
    expect(arg.modelType).toBe('plan-of-action-and-milestones');
    expect(arg.draft).toBe(false);
  });

  it('initialDocument seeds the wizard with metadata, import, and body', async () => {
    const draft = {
      'system-security-plan': {
        uuid: '00000000-0000-0000-0000-000000000ff0',
        metadata: {
          title: 'Acme Trust Center SSP',
          version: '1.0',
          'oscal-version': '1.1.2',
          'last-modified': '2026-05-08T00:00:00Z',
        },
        'import-profile': { href: 'library:p-1' },
        'system-characteristics': {
          'system-name': 'Acme Trust Center',
          description: 'Web app',
          'system-ids': [{ id: 'acme-trust' }],
          'security-sensitivity-level': 'moderate',
          'system-information': { 'information-types': [] },
          'security-impact-level': {
            'security-objective-confidentiality': 'moderate',
            'security-objective-integrity': 'moderate',
            'security-objective-availability': 'moderate',
          },
          status: { state: 'operational' },
          'authorization-boundary': { description: 'Cloud Run.' },
        },
        'system-implementation': { users: [], components: [] },
        'control-implementation': {
          description: 'Drafted from source',
          'implemented-requirements': [],
        },
      },
    };

    const { getByDisplayValue } = render(
      <OscalDocumentWizard
        modelType="system-security-plan"
        initialDocument={draft}
      />,
    );

    // Title input is populated from initialDocument.metadata.title
    await waitFor(() => expect(getByDisplayValue('Acme Trust Center SSP')).toBeInTheDocument());
  });

  it('renders AI confidence panel on SSP step 3 when body has ai-confidence props', async () => {
    const draft = {
      'system-security-plan': {
        uuid: '00000000-0000-0000-0000-000000000abc',
        metadata: { title: 't', version: '1', 'oscal-version': '1.1.2', 'last-modified': 'now' },
        'import-profile': { href: 'library:p-1' },
        'system-characteristics': {
          'system-name': 't',
          description: '',
          'system-ids': [{ id: 's' }],
          'security-sensitivity-level': 'moderate',
          'system-information': { 'information-types': [] },
          'security-impact-level': {
            'security-objective-confidentiality': 'moderate',
            'security-objective-integrity': 'moderate',
            'security-objective-availability': 'moderate',
          },
          status: { state: 'operational' },
          'authorization-boundary': { description: '' },
        },
        'system-implementation': { users: [], components: [] },
        'control-implementation': {
          description: '',
          'implemented-requirements': [
            {
              uuid: 'u1',
              'control-id': 'ac-1',
              description: 'D1',
              props: [{ name: 'ai-confidence', ns: 'https://oscal-hub.io/ns', value: 'high' }],
            },
            {
              uuid: 'u2',
              'control-id': 'ac-2',
              description: 'D2',
              props: [{ name: 'ai-confidence', ns: 'https://oscal-hub.io/ns', value: 'low' }],
            },
          ],
        },
      },
    };

    render(<OscalDocumentWizard modelType="system-security-plan" initialDocument={draft} />);

    // Wait for the wizard to seed from the initialDocument before navigating.
    await waitFor(() => expect(screen.getByDisplayValue('t')).toBeInTheDocument());

    // Navigate to step 3 (Body) where the AI confidence panel renders.
    fireEvent.click(screen.getByRole('button', { name: /3\. Body/i }));

    await waitFor(() => expect(screen.getByText(/AI confidence/i)).toBeInTheDocument());
    expect(screen.getByText(/1 high/i)).toBeInTheDocument();
    expect(screen.getByText(/0 medium/i)).toBeInTheDocument();
    expect(screen.getByText(/1 low/i)).toBeInTheDocument();
  });

  it('AI confidence panel does not render for non-SSP models', async () => {
    const draft = {
      'plan-of-action-and-milestones': {
        uuid: 'x',
        metadata: { title: 'p', version: '1', 'oscal-version': '1.1.2', 'last-modified': 'now' },
        observations: [],
        risks: [],
        findings: [],
        'poam-items': [],
      },
    };
    render(
      <OscalDocumentWizard
        modelType="plan-of-action-and-milestones"
        initialDocument={draft}
      />,
    );
    await waitFor(() => expect(screen.getByDisplayValue('p')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /3\. Body/i }));
    expect(screen.queryByText(/AI confidence/i)).not.toBeInTheDocument();
  });

  it('loads and edits an existing SSP, preserving the draft flag', async () => {
    getContentMock.mockResolvedValue(
      JSON.stringify({
        'system-security-plan': {
          uuid: 'existing',
          metadata: { title: 'Existing SSP', 'oscal-version': '1.1.3', version: '1.0.0', 'last-modified': '2026-05-02T00:00:00Z' },
          'import-profile': { href: '#some-profile' },
          'system-characteristics': { 'system-name': 'demo' },
        },
      }),
    );

    render(
      <OscalDocumentWizard
        modelType="system-security-plan"
        editingDocument={{
          id: 42,
          oscalUuid: 'existing',
          modelType: 'system-security-plan',
          title: 'Existing SSP',
          oscalVersion: '1.1.3',
          storagePath: '',
          filename: 'ssp.json',
          fileSize: 0,
          draft: true,
          createdBy: 'me',
          createdAt: '',
          updatedAt: '',
        }}
      />,
    );

    await waitFor(() => {
      expect(screen.getByDisplayValue('Existing SSP')).toBeInTheDocument();
      expect(screen.getByText('Draft')).toBeInTheDocument();
    });
  });
});
