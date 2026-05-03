import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { AiFeatureGate } from './AiFeatureGate';
import { aiClient } from '@/lib/ai-client';

vi.mock('@/lib/ai-client', () => ({
  aiClient: { getSettingsStatus: vi.fn() },
}));

describe('AiFeatureGate', () => {
  it('renders children when enabled', async () => {
    (aiClient.getSettingsStatus as unknown as { mockResolvedValue: (v: unknown) => void })
      .mockResolvedValue({ enabled: true });
    render(<AiFeatureGate organizationId={1}><div>secret</div></AiFeatureGate>);
    await waitFor(() => expect(screen.getByText('secret')).toBeInTheDocument());
  });

  it('renders fallback when disabled', async () => {
    (aiClient.getSettingsStatus as unknown as { mockResolvedValue: (v: unknown) => void })
      .mockResolvedValue({ enabled: false });
    render(
      <AiFeatureGate organizationId={1} fallback={<div>nope</div>}>
        <div>secret</div>
      </AiFeatureGate>,
    );
    await waitFor(() => expect(screen.getByText('nope')).toBeInTheDocument());
  });
});
