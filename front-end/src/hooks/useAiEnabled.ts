'use client';
import { useEffect, useState } from 'react';
import { aiClient } from '@/lib/ai-client';

export function useAiEnabled(organizationId: number | null): {
  enabled: boolean;
  loading: boolean;
} {
  const [enabled, setEnabled] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!organizationId) {
      setLoading(false);
      return;
    }
    let active = true;
    aiClient
      .getSettingsStatus(organizationId)
      .then((res) => {
        if (active) {
          setEnabled(res.enabled);
          setLoading(false);
        }
      })
      .catch(() => {
        if (active) {
          setEnabled(false);
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [organizationId]);

  return { enabled, loading };
}
