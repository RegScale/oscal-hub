'use client';

import type { ReactNode } from 'react';
import { SharingAccessCard } from './sharing-access-card';
import type { AuthorizationResponse } from '@/types/oscal';

interface Props {
  authorization: AuthorizationResponse;
  /**
   * The existing detail content extracted from the parent page (details card,
   * digital signature card, conditions card, authorization document card).
   * Passed in as children to keep this initial refactor mechanical — a future
   * cleanup can decompose into smaller components.
   */
  children: ReactNode;
  onAuthorizationUpdated: (a: AuthorizationResponse) => void;
}

export function OverviewTab({ authorization, children, onAuthorizationUpdated }: Props) {
  const isOwner = authorization.effectiveRole === 'OWNER';
  return (
    <div className="space-y-6">
      {children}
      {isOwner && (
        <SharingAccessCard
          authorization={authorization}
          onAuthorizationUpdated={onAuthorizationUpdated}
        />
      )}
    </div>
  );
}
