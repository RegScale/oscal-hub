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
  const [targetEl, setTargetEl] = useState<HTMLElement | null>(null);
  const [resolving, setResolving] = useState(false);
  const [announcement, setAnnouncement] = useState('');
  // Repaint trigger so the spotlight/popover track resize and scroll.
  const [, setRepaintTick] = useState(0);

  const directionRef = useRef<1 | -1>(1);
  // Set while startTour's router.push is in flight so the route-change
  // watcher doesn't mistake our own navigation for the user leaving.
  const awaitingRouteRef = useRef<string | null>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);

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
      setAnnouncement(reason === 'completed' ? 'Tour completed.' : 'Tour closed.');
      previousFocusRef.current?.focus?.();
      previousFocusRef.current = null;
      setActiveTour(null);
      setStepIndex(0);
      setTargetEl(null);
      setResolving(false);
    },
    [activeTour, user, stepIndex],
  );

  const startTour = useCallback(
    (tourId: string) => {
      const tour = tours.find((t) => t.id === tourId);
      if (!tour || !tour.eligible(user)) return;
      if (tour.minViewportWidth && window.innerWidth < tour.minViewportWidth) return;
      previousFocusRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
      directionRef.current = 1;
      if (pathname !== tour.startRoute) {
        awaitingRouteRef.current = tour.startRoute;
        router.push(tour.startRoute);
      } else {
        awaitingRouteRef.current = null;
      }
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
  // navigation, so poll briefly before declaring the step missing.
  useEffect(() => {
    if (!activeTour) return;
    const step = activeTour.steps[stepIndex];
    if (!step.target) {
      setTargetEl(null);
      setResolving(false);
      return;
    }
    let cancelled = false;
    let elapsed = 0;
    setResolving(true);
    setTargetEl(null);
    const tick = () => {
      if (cancelled) return;
      const el = document.querySelector<HTMLElement>(`[data-tour="${step.target}"]`);
      if (el) {
        el.scrollIntoView?.({ block: 'center', behavior: 'auto' });
        setTargetEl(el);
        setResolving(false);
        return;
      }
      elapsed += TARGET_POLL_INTERVAL_MS;
      if (elapsed >= targetTimeoutMs) {
        setResolving(false);
        if (step.skipIfMissing !== false) {
          goTo(stepIndex + directionRef.current, directionRef.current);
        } else {
          endTour('dismissed');
        }
        return;
      }
      window.setTimeout(tick, TARGET_POLL_INTERVAL_MS);
    };
    tick();
    return () => {
      cancelled = true;
    };
  }, [activeTour, stepIndex, targetTimeoutMs, goTo, endTour]);

  // Announce each step for screen readers.
  useEffect(() => {
    if (!activeTour) return;
    const step = activeTour.steps[stepIndex];
    setAnnouncement(`Step ${stepIndex + 1} of ${activeTour.steps.length}: ${step.title}`);
  }, [activeTour, stepIndex]);

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
  useEffect(() => {
    if (!activeTour) return;
    if (awaitingRouteRef.current) {
      if (pathname === awaitingRouteRef.current) awaitingRouteRef.current = null;
      return;
    }
    if (pathname !== activeTour.startRoute) {
      endTour(stepIndex === activeTour.steps.length - 1 ? 'completed' : 'dismissed');
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

  const step = activeTour ? activeTour.steps[stepIndex] : null;
  const targetRect = step?.target && targetEl ? targetEl.getBoundingClientRect() : null;

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
        {announcement}
      </div>
      {overlay}
    </TourContext.Provider>
  );
}
