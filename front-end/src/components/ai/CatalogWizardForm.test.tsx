import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { CatalogWizardForm } from './CatalogWizardForm';
import { aiClient } from '@/lib/ai-client';

vi.mock('@/lib/ai-client', () => ({
  aiClient: {
    startSession: vi.fn(),
    startSessionWithUpload: vi.fn(),
    startSessionWithUrl: vi.fn(),
  },
}));

vi.mock('sonner', () => ({
  toast: { error: vi.fn() },
}));

beforeEach(() => {
  vi.clearAllMocks();
});

describe('CatalogWizardForm', () => {
  it('Run button disabled until input provided', () => {
    render(<CatalogWizardForm organizationId={1} onSessionStarted={vi.fn()} />);
    expect(screen.getByText('Run AI Wizard')).toBeDisabled();
  });

  it('paste mode: enables Run after typing and calls startSession on click', async () => {
    const onStart = vi.fn();
    (aiClient.startSession as unknown as { mockResolvedValue: (v: unknown) => void })
      .mockResolvedValue({ sessionId: 'abc' });
    render(<CatalogWizardForm organizationId={1} onSessionStarted={onStart} />);
    fireEvent.click(screen.getByText('Paste text'));
    fireEvent.change(screen.getByLabelText(/Paste source content/i), {
      target: { value: 'some controls' },
    });
    const btn = screen.getByText('Run AI Wizard');
    expect(btn).not.toBeDisabled();
    fireEvent.click(btn);
    await new Promise((r) => setTimeout(r, 0));
    expect(aiClient.startSession).toHaveBeenCalled();
    expect(onStart).toHaveBeenCalledWith('abc');
  });

  it('URL mode: tab is present and field is rendered', () => {
    render(<CatalogWizardForm organizationId={1} onSessionStarted={vi.fn()} />);
    const tab = screen.getByText('From URL');
    fireEvent.click(tab);
    expect(screen.getByLabelText(/Source URL/i)).toBeInTheDocument();
  });

  it('URL mode: Run is disabled when URL is blank', () => {
    render(<CatalogWizardForm organizationId={1} onSessionStarted={vi.fn()} />);
    fireEvent.click(screen.getByText('From URL'));
    expect(screen.getByText('Run AI Wizard')).toBeDisabled();
  });

  it('URL mode: Run is disabled when URL is only whitespace', () => {
    render(<CatalogWizardForm organizationId={1} onSessionStarted={vi.fn()} />);
    fireEvent.click(screen.getByText('From URL'));
    fireEvent.change(screen.getByLabelText(/Source URL/i), { target: { value: '   ' } });
    expect(screen.getByText('Run AI Wizard')).toBeDisabled();
  });

  it('URL mode: calls startSessionWithUrl with trimmed URL on click', async () => {
    const onStart = vi.fn();
    (aiClient.startSessionWithUrl as unknown as { mockResolvedValue: (v: unknown) => void })
      .mockResolvedValue({ sessionId: 'url-session' });

    render(<CatalogWizardForm organizationId={7} onSessionStarted={onStart} />);
    fireEvent.click(screen.getByText('From URL'));
    fireEvent.change(screen.getByLabelText(/Source URL/i), {
      target: { value: '  https://nist.gov/sp800-53.pdf  ' },
    });
    const btn = screen.getByText('Run AI Wizard');
    expect(btn).not.toBeDisabled();
    fireEvent.click(btn);
    await new Promise((r) => setTimeout(r, 0));
    expect(aiClient.startSessionWithUrl).toHaveBeenCalledWith(
      7,
      'CATALOG',
      'https://nist.gov/sp800-53.pdf',
    );
    expect(aiClient.startSession).not.toHaveBeenCalled();
    expect(aiClient.startSessionWithUpload).not.toHaveBeenCalled();
    expect(onStart).toHaveBeenCalledWith('url-session');
  });

  it('URL mode: shows toast and does not call onSessionStarted on backend failure', async () => {
    const { toast } = await import('sonner');
    (aiClient.startSessionWithUrl as unknown as { mockRejectedValue: (v: unknown) => void })
      .mockRejectedValue(new Error('HTTP 400 — Refusing to fetch loopback address: localhost'));
    const onStart = vi.fn();

    render(<CatalogWizardForm organizationId={1} onSessionStarted={onStart} />);
    fireEvent.click(screen.getByText('From URL'));
    fireEvent.change(screen.getByLabelText(/Source URL/i), {
      target: { value: 'http://localhost/admin' },
    });
    fireEvent.click(screen.getByText('Run AI Wizard'));
    await new Promise((r) => setTimeout(r, 0));

    expect(toast.error).toHaveBeenCalledWith(
      expect.stringContaining('Refusing to fetch loopback'),
    );
    expect(onStart).not.toHaveBeenCalled();
  });

  it('switching tabs preserves entered values', () => {
    render(<CatalogWizardForm organizationId={1} onSessionStarted={vi.fn()} />);

    fireEvent.click(screen.getByText('Paste text'));
    fireEvent.change(screen.getByLabelText(/Paste source content/i), {
      target: { value: 'hello' },
    });

    fireEvent.click(screen.getByText('From URL'));
    fireEvent.change(screen.getByLabelText(/Source URL/i), {
      target: { value: 'https://example.com/x.html' },
    });

    // Back to Paste — value still there
    fireEvent.click(screen.getByText('Paste text'));
    expect((screen.getByLabelText(/Paste source content/i) as HTMLTextAreaElement).value)
      .toBe('hello');

    fireEvent.click(screen.getByText('From URL'));
    expect((screen.getByLabelText(/Source URL/i) as HTMLInputElement).value)
      .toBe('https://example.com/x.html');
  });
});
