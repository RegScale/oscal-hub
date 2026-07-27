'use client';

import { CheckCircle2 } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/contexts/AuthContext';
import { useTour } from '@/components/tour/TourProvider';
import { TOURS } from '@/lib/tours/registry';
import { loadTourState } from '@/lib/tours/storage';

interface TourMenuProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/** Dialog listing every tour the current user is eligible for. */
export function TourMenu({ open, onOpenChange }: TourMenuProps) {
  const { user } = useAuth();
  const { startTour } = useTour();

  if (!user) return null;

  const state = loadTourState(user.userId);
  const available = TOURS.filter((tour) => tour.eligible(user));

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Guided tours</DialogTitle>
          <DialogDescription>Short interactive walkthroughs of OSCAL Hub features.</DialogDescription>
        </DialogHeader>
        <ul className="space-y-2">
          {available.map((tour) => {
            const completed = state.tours[tour.id]?.completedVersion !== undefined;
            return (
              <li
                key={tour.id}
                className="flex items-start justify-between gap-3 rounded-md border border-border p-3"
              >
                <div>
                  <div className="flex items-center gap-2 text-sm font-medium">
                    {tour.title}
                    {completed && (
                      <span className="inline-flex items-center gap-1 text-xs text-green-500">
                        <CheckCircle2 className="h-3.5 w-3.5" aria-hidden="true" />
                        Completed
                      </span>
                    )}
                  </div>
                  <p className="mt-0.5 text-xs text-muted-foreground">{tour.description}</p>
                </div>
                <Button
                  size="sm"
                  variant={completed ? 'outline' : 'default'}
                  onClick={() => {
                    onOpenChange(false);
                    startTour(tour.id);
                  }}
                >
                  {completed ? 'Replay' : 'Start'}
                </Button>
              </li>
            );
          })}
        </ul>
      </DialogContent>
    </Dialog>
  );
}
