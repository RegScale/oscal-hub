'use client';

import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { usePathname, useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { TOURS } from '@/lib/tours/registry';
import { markTourCompleted, markTourDismissed } from '@/lib/tours/storage';
import type { TourDefinition } from '@/lib/tours/types';
import { TourSpotlight } from './TourSpotlight';
import { TourPopover } from './TourPopover';

const TARGET_POLL_INTERVAL_MS = 100;
const DEFAULT_TARGET_TIMEOUT_MS = 2000;

interface TourContextValue {
  activeTour: TourDefinition | null;
  stepIndex: number;
  startTour: (tourId: string) => void;
  endTour: (reason: 'completed' | 'dismissed') => void;
  next: () => void;
  back: () => void;
}

const TourContext = createContext<TourContextValue | null>(null);

export function useTour(): TourContextValue {
  const ctx = useContext(TourContext);
  if (!ctx) throw new Error('useTour must be used within a TourProvider');
  return ctx;
}

interface TourProviderProps {
  children: React.ReactNode;
  /** Overridable for tests; defaults to the app registry. */
  tours?: TourDefinition[];
  /** How long to wait for a step's [data-tour] target before skipping it. */
  targetTimeoutMs?: number;
}

/** Target element resolved for a specific tour step, keyed to detect staleness. */
interface ResolvedTarget {
  key: string;
  el: HTMLElement;
}

export function TourProvider({
  children,
  tours = TOURS,
  targetTimeoutMs = DEFAULT_TARGET_TIMEOUT_MS,
}: TourProviderProps) {
  const { user } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  const [activeTour, setActiveTour] = useState<TourDefinition | null>(null);
  const [stepIndex, setStepIndex] = useState(0);
  const [resolved, setResolved] = useState<ResolvedTarget | null>(null);
  const [endMessage, setEndMessage] = useState('');
  // Repaint trigger so the spotlight/popover track resize and scroll.
  const [, setRepaintTick] = useState(0);

  const directionRef = useRef<1 | -1>(1);
  // Set while startTour's router.push is in flight so the route-change
  // watcher doesn't mistake our own navigation for the user leaving.
  const awaitingRouteRef = useRef<string | null>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);

  const step = activeTour ? activeTour.steps[stepIndex] : null;
  const stepKey = activeTour ? `${activeTour.id}:${stepIndex}` : null;
  // Derived rather than stored: a resolved entry only counts for the step it
  // was resolved for, so stale targets from earlier steps are ignored.
  const targetEl = step?.target && resolved?.key === stepKey ? resolved.el : null;
  const resolving = Boolean(step?.target) && targetEl === null;

  const endTour = useCallback(
    (reason: 'completed' | 'dismissed') => {
      if (!activeTour) return;
      if (user) {
        if (reason === 'completed') {
          markTourCompleted(user.userId, activeTour.id, activeTour.version);
        } else {
          markTourDismissed(user.userId, activeTour.id, activeTour.version, stepIndex);
        }
      }
      setEndMessage(reason === 'completed' ? 'Tour completed.' : 'Tour closed.');
      previousFocusRef.current?.focus?.();
      previousFocusRef.current = null;
      setActiveTour(null);
      setStepIndex(0);
      setResolved(null);
    },
    [activeTour, user, stepIndex],
  );

  const startTour = useCallback(
    (tourId: string) => {
      const tour = tours.find((t) => t.id === tourId);
      if (!tour || !tour.eligible(user)) return;
      // Width 0 = unmeasurable (embedded/hidden contexts), not narrow: only a
      // positive sub-minimum width blocks the start.
      const width = window.innerWidth;
      if (tour.minViewportWidth && width > 0 && width < tour.minViewportWidth) return;
      previousFocusRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
      directionRef.current = 1;
      if (pathname !== tour.startRoute) {
        awaitingRouteRef.current = tour.startRoute;
        router.push(tour.startRoute);
      } else {
        awaitingRouteRef.current = null;
      }
      setEndMessage('');
      setStepIndex(0);
      setActiveTour(tour);
    },
    [tours, user, pathname, router],
  );

  const goTo = useCallback(
    (index: number, direction: 1 | -1) => {
      if (!activeTour) return;
      if (index >= activeTour.steps.length) {
        endTour('completed');
        return;
      }
      if (index < 0) {
        // Backward-skipped past the first step (only possible when step 0's
        // target vanished): nothing sensible to show, so close out.
        endTour('dismissed');
        return;
      }
      directionRef.current = direction;
      setStepIndex(index);
    },
    [activeTour, endTour],
  );

  const next = useCallback(() => goTo(stepIndex + 1, 1), [goTo, stepIndex]);
  const back = useCallback(() => goTo(stepIndex - 1, -1), [goTo, stepIndex]);

  // Resolve the current step's target element. Targets can mount a tick after
  // navigation, so poll briefly before declaring the step missing. All state
  // writes happen inside timer callbacks, never synchronously in the effect.
  useEffect(() => {
    if (!activeTour || !stepKey) return;
    const currentStep = activeTour.steps[stepIndex];
    if (!currentStep.target) return;
    let cancelled = false;
    let elapsed = 0;
    let timer: number;
    const tick = () => {
      if (cancelled) return;
      const el = document.querySelector<HTMLElement>(`[data-tour="${currentStep.target}"]`);
      if (el) {
        el.scrollIntoView?.({ block: 'center', behavior: 'auto' });
        setResolved({ key: stepKey, el });
        return;
      }
      elapsed += TARGET_POLL_INTERVAL_MS;
      if (elapsed >= targetTimeoutMs) {
        if (currentStep.skipIfMissing !== false) {
          goTo(stepIndex + directionRef.current, directionRef.current);
        } else {
          endTour('dismissed');
        }
        return;
      }
      timer = window.setTimeout(tick, TARGET_POLL_INTERVAL_MS);
    };
    timer = window.setTimeout(tick, 0);
    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [activeTour, stepIndex, stepKey, targetTimeoutMs, goTo, endTour]);

  // Esc exits from anywhere.
  useEffect(() => {
    if (!activeTour) return;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') endTour('dismissed');
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [activeTour, endTour]);

  // A route change the tour didn't initiate ends it. On the final step the
  // finish-screen CTAs are links, so leaving then counts as completion.
  // Deferred so the tour teardown doesn't run inside the render commit.
  useEffect(() => {
    if (!activeTour) return;
    if (awaitingRouteRef.current) {
      if (pathname === awaitingRouteRef.current) awaitingRouteRef.current = null;
      return;
    }
    if (pathname !== activeTour.startRoute) {
      const isLast = stepIndex === activeTour.steps.length - 1;
      const timer = window.setTimeout(() => endTour(isLast ? 'completed' : 'dismissed'), 0);
      return () => window.clearTimeout(timer);
    }
  }, [pathname, activeTour, stepIndex, endTour]);

  // Track resize/scroll so the spotlight and popover follow the target.
  useEffect(() => {
    if (!activeTour || !targetEl) return;
    const bump = () => setRepaintTick((t) => t + 1);
    window.addEventListener('resize', bump);
    window.addEventListener('scroll', bump, true);
    return () => {
      window.removeEventListener('resize', bump);
      window.removeEventListener('scroll', bump, true);
    };
  }, [activeTour, targetEl]);

  const targetRect = targetEl ? targetEl.getBoundingClientRect() : null;

  // Derived step announcement while touring; end message after it closes.
  // The aria-live region announces each change without any effect involved.
  const liveMessage =
    activeTour && step ? `Step ${stepIndex + 1} of ${activeTour.steps.length}: ${step.title}` : endMessage;

  const overlay =
    activeTour && step && !resolving
      ? createPortal(
          <div className="fixed inset-0 z-[100]" role="presentation">
            <TourSpotlight targetRect={targetRect} />
            <TourPopover
              step={step}
              stepIndex={stepIndex}
              totalSteps={activeTour.steps.length}
              targetRect={targetRect}
              onNext={next}
              onBack={back}
              onSkip={() => endTour('dismissed')}
            />
          </div>,
          document.body,
        )
      : null;

  return (
    <TourContext.Provider value={{ activeTour, stepIndex, startTour, endTour, next, back }}>
      {children}
      <div aria-live="polite" className="sr-only">
        {liveMessage}
      </div>
      {overlay}
    </TourContext.Provider>
  );
}
