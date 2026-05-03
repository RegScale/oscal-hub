import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { CatalogWizardForm } from './CatalogWizardForm';
import { aiClient } from '@/lib/ai-client';

vi.mock('@/lib/ai-client', () => ({
  aiClient: {
    startSession: vi.fn(),
    startSessionWithUpload: vi.fn(),
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
});
