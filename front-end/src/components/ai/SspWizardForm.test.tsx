import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { SspWizardForm } from './SspWizardForm';
import { aiClient } from '@/lib/ai-client';
import { libraryListApi } from '@/lib/api/library';

vi.mock('@/lib/ai-client', () => ({
  aiClient: {
    startSession: vi.fn(),
    startSessionWithUpload: vi.fn(),
  },
}));

vi.mock('@/lib/api/library', () => ({
  libraryListApi: {
    listByOscalType: vi.fn(),
  },
}));

vi.mock('sonner', () => ({ toast: { error: vi.fn() } }));

type MockFn = { mockResolvedValue: (v: unknown) => void };

describe('SspWizardForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (libraryListApi.listByOscalType as unknown as MockFn).mockResolvedValue([
      { itemId: 'p-1', title: 'FedRAMP Moderate', version: '5', oscalType: 'profile', visibility: 'PUBLIC', updatedAt: '2026-01-01' },
    ]);
  });

  it('library profile + paste text starts session with library:<id> profileHref', async () => {
    (aiClient.startSession as unknown as MockFn).mockResolvedValue({ sessionId: 's-1' });
    const onStarted = vi.fn();
    render(<SspWizardForm organizationId={42} onSessionStarted={onStarted} />);

    await waitFor(() => expect(screen.getByText('FedRAMP Moderate (v5)')).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText('Profile from library'), { target: { value: 'p-1' } });

    fireEvent.click(screen.getByRole('button', { name: /paste text/i }));
    fireEvent.change(screen.getByLabelText('Paste source content'), { target: { value: 'system description text' } });

    fireEvent.click(screen.getByRole('button', { name: /run ai wizard/i }));

    await waitFor(() => expect(aiClient.startSession).toHaveBeenCalled());
    expect(aiClient.startSession).toHaveBeenCalledWith(expect.objectContaining({
      organizationId: 42,
      wizardKind: 'SSP',
      input: 'system description text',
      profileHref: 'library:p-1',
    }));
    expect(onStarted).toHaveBeenCalledWith('s-1');
  });

  it('skip mode + file upload starts session with null profileHref', async () => {
    (aiClient.startSessionWithUpload as unknown as MockFn).mockResolvedValue({ sessionId: 's-2' });
    const onStarted = vi.fn();
    render(<SspWizardForm organizationId={42} onSessionStarted={onStarted} />);

    await waitFor(() => expect(screen.getByText('FedRAMP Moderate (v5)')).toBeInTheDocument());

    fireEvent.click(screen.getByLabelText(/Skip — let AI infer/i));

    const file = new File(['hello'], 'sys.pdf', { type: 'application/pdf' });
    fireEvent.change(screen.getByLabelText('Source document'), { target: { files: [file] } });

    fireEvent.click(screen.getByRole('button', { name: /run ai wizard/i }));

    await waitFor(() => expect(aiClient.startSessionWithUpload).toHaveBeenCalled());
    expect(aiClient.startSessionWithUpload).toHaveBeenCalledWith(
      42,
      'SSP',
      file,
      { profileHref: null },
    );
  });

  it('URL mode requires non-empty URL', async () => {
    render(<SspWizardForm organizationId={42} onSessionStarted={vi.fn()} />);
    await waitFor(() => expect(screen.getByText('FedRAMP Moderate (v5)')).toBeInTheDocument());

    // Switch profile to URL mode but leave it empty.
    fireEvent.click(screen.getByLabelText(/Paste a profile URL/i));

    // Fill the source-doc side so only the profile is gating Run.
    fireEvent.click(screen.getByRole('button', { name: /paste text/i }));
    fireEvent.change(screen.getByLabelText('Paste source content'), { target: { value: 'doc' } });

    // With empty URL, Run is disabled.
    expect(screen.getByRole('button', { name: /run ai wizard/i })).toBeDisabled();

    // Filling the URL re-enables Run.
    fireEvent.change(screen.getByLabelText('Profile URL'), { target: { value: 'https://example.com/p.json' } });
    expect(screen.getByRole('button', { name: /run ai wizard/i })).not.toBeDisabled();
  });
});
