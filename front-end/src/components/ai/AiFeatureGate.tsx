'use client';
import { ReactNode } from 'react';
import { useAiEnabled } from '@/hooks/useAiEnabled';

interface AiFeatureGateProps {
  organizationId: number | null;
  fallback?: ReactNode;
  children: ReactNode;
}

export function AiFeatureGate({ organizationId, fallback = null, children }: AiFeatureGateProps) {
  const { enabled, loading } = useAiEnabled(organizationId);
  if (loading) return null;
  if (!enabled) return <>{fallback}</>;
  return <>{children}</>;
}
