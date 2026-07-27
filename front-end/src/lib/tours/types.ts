import type { ReactNode } from 'react';
import type { User } from '@/types/auth';
import type { HelpSlug } from '@/lib/help-targets';

export type TourPlacement = 'top' | 'bottom' | 'left' | 'right';

export interface TourStep {
  /** Unique within the tour; also used for aria ids. */
  id: string;
  /** Matches an element with [data-tour="<target>"]. Omit for a centered modal step. */
  target?: string;
  title: string;
  body: ReactNode;
  /** Renders a "Learn more in the guide" link (opens in a new tab). */
  helpSlug?: HelpSlug;
  /** Preferred popover side; auto-flips when it doesn't fit. */
  placement?: TourPlacement;
  /** When the target is absent, skip the step instead of ending the tour. Default true. */
  skipIfMissing?: boolean;
}

export interface TourDefinition {
  id: string;
  /** Bump after major UI changes so the launcher can badge the tour as updated. */
  version: number;
  title: string;
  description: string;
  /** Route the tour runs on; startTour navigates here first. */
  startRoute: string;
  eligible: (user: User | null) => boolean;
  /** Anchored steps target sm:-hidden nav items; block starting below this width. */
  minViewportWidth?: number;
  steps: TourStep[];
}

export interface TourRecord {
  completedVersion?: number;
  dismissedVersion?: number;
  dismissedAtStep?: number;
}

export interface TourStorageState {
  welcomePrompt: { seen: boolean; deferrals: number };
  tours: Record<string, TourRecord>;
}
