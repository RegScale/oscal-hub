'use client';

import { useEffect, useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/contexts/AuthContext';
import { useTour } from '@/components/tour/TourProvider';
import { GET_STARTED_TOUR_ID, getTour } from '@/lib/tours/registry';
import {
  dismissWelcomePrompt,
  loadTourState,
  recordWelcomeDeferral,
  shouldShowWelcomePrompt,
  wasDeferredThisSession,
} from '@/lib/tours/storage';

/**
 * One-time "Take a 2-minute tour?" offer, rendered on the dashboard.
 * Self-gating: shows only for eligible users who haven't completed, dismissed,
 * or opted out of the Get Started tour. "Maybe later" defers to the next
 * session (max 3 times, then treated as "don't ask again").
 */
export function TourWelcomeDialog() {
  const { user } = useAuth();
  const { startTour, activeTour } = useTour();
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!user || activeTour) return;
    const tour = getTour(GET_STARTED_TOUR_ID);
    if (!tour || !tour.eligible(user)) return;
    if (tour.minViewportWidth && window.innerWidth < tour.minViewportWidth) return;
    if (wasDeferredThisSession()) return;
    if (!shouldShowWelcomePrompt(loadTourState(user.userId), GET_STARTED_TOUR_ID)) return;
    // Deferred so the prompt opens after the dashboard's commit, not during it.
    const timer = window.setTimeout(() => setOpen(true), 0);
    return () => window.clearTimeout(timer);
  }, [user, activeTour]);

  // Unmount entirely when closed: Radix keeps a data-state="closed" dialog in
  // the DOM while its exit animation settles, which leaves a stale role=dialog
  // behind the tour overlay.
  if (!user || !open) return null;

  const handleStart = () => {
    dismissWelcomePrompt(user.userId);
    setOpen(false);
    startTour(GET_STARTED_TOUR_ID);
  };

  const handleLater = () => {
    recordWelcomeDeferral(user.userId);
    setOpen(false);
  };

  const handleNever = () => {
    dismissWelcomePrompt(user.userId);
    setOpen(false);
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        // Closing via overlay click / Esc / X counts as "maybe later".
        if (!next) handleLater();
      }}
    >
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>New here? Take a two-minute tour</DialogTitle>
          <DialogDescription>
            A quick walkthrough of where the tools live and how to find help. You can replay it anytime from
            your avatar menu.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter className="gap-2 sm:justify-between">
          <Button variant="ghost" onClick={handleNever}>
            Don&apos;t ask again
          </Button>
          <div className="flex gap-2">
            <Button variant="outline" onClick={handleLater}>
              Maybe later
            </Button>
            <Button onClick={handleStart}>Start tour</Button>
          </div>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
