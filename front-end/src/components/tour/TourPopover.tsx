'use client';

import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { Button } from '@/components/ui/button';
import { HELP_TARGETS } from '@/lib/help-targets';
import { computePopoverPosition } from '@/lib/tours/position';
import type { TourStep } from '@/lib/tours/types';

interface TourPopoverProps {
  step: TourStep;
  stepIndex: number;
  totalSteps: number;
  /** Viewport-relative rect of the target; null renders a centered modal step. */
  targetRect: DOMRect | null;
  onNext: () => void;
  onBack: () => void;
  onSkip: () => void;
}

/**
 * The step card: a focus-trapped dialog positioned next to the spotlighted
 * element (or centered for modal steps). Keyboard: Tab cycles inside,
 * ArrowRight/Enter advance, ArrowLeft goes back, Esc (handled by the
 * provider) exits.
 */
export function TourPopover({ step, stepIndex, totalSteps, targetRect, onNext, onBack, onSkip }: TourPopoverProps) {
  const ref = useRef<HTMLDivElement>(null);
  const [pos, setPos] = useState<{ top: number; left: number } | null>(null);
  const isLast = stepIndex === totalSteps - 1;

  useLayoutEffect(() => {
    if (!targetRect || !ref.current) {
      setPos(null);
      return;
    }
    const { width, height } = ref.current.getBoundingClientRect();
    const computed = computePopoverPosition(
      { x: targetRect.x, y: targetRect.y, width: targetRect.width, height: targetRect.height },
      { width: width || 320, height: height || 200 },
      { width: window.innerWidth, height: window.innerHeight },
      step.placement,
    );
    setPos({ top: computed.top, left: computed.left });
  }, [targetRect, step]);

  // Move focus into the dialog on every step change.
  useEffect(() => {
    ref.current?.focus();
  }, [step.id]);

  function handleKeyDown(event: React.KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'ArrowRight' || (event.key === 'Enter' && event.target === ref.current)) {
      event.preventDefault();
      onNext();
      return;
    }
    if (event.key === 'ArrowLeft' && stepIndex > 0) {
      event.preventDefault();
      onBack();
      return;
    }
    if (event.key !== 'Tab') return;
    // Minimal focus trap: cycle Tab within the dialog's focusable elements.
    const focusables = ref.current?.querySelectorAll<HTMLElement>('a[href], button:not([disabled])');
    if (!focusables || focusables.length === 0) return;
    const first = focusables[0];
    const last = focusables[focusables.length - 1];
    if (event.shiftKey && (document.activeElement === first || document.activeElement === ref.current)) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }

  const anchored = targetRect !== null && pos !== null;

  return (
    <div
      ref={ref}
      role="dialog"
      aria-modal="true"
      aria-labelledby={`tour-step-title-${step.id}`}
      aria-describedby={`tour-step-body-${step.id}`}
      tabIndex={-1}
      onKeyDown={handleKeyDown}
      className={`fixed z-[101] w-80 max-w-[calc(100vw-2rem)] rounded-lg border border-border bg-popover p-4 text-popover-foreground shadow-lg focus:outline-none focus:ring-2 focus:ring-ring ${
        anchored ? '' : 'left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2'
      }`}
      style={anchored ? { top: pos.top, left: pos.left } : undefined}
    >
      <p className="text-xs text-muted-foreground">
        Step {stepIndex + 1} of {totalSteps}
      </p>
      <h2 id={`tour-step-title-${step.id}`} className="mt-1 text-base font-semibold">
        {step.title}
      </h2>
      <div id={`tour-step-body-${step.id}`} className="mt-2 text-sm text-muted-foreground">
        {step.body}
      </div>
      {step.helpSlug && (
        <a
          href={`/guide/${HELP_TARGETS[step.helpSlug]}`}
          target="_blank"
          rel="noopener noreferrer"
          className="mt-3 inline-block text-sm text-primary underline"
        >
          Learn more in the guide
        </a>
      )}
      <div className="mt-4 flex items-center justify-between gap-2">
        <Button variant="ghost" size="sm" onClick={onSkip}>
          Skip tour
        </Button>
        <div className="flex gap-2">
          {stepIndex > 0 && (
            <Button variant="outline" size="sm" onClick={onBack}>
              Back
            </Button>
          )}
          <Button size="sm" onClick={onNext}>
            {isLast ? 'Finish' : 'Next'}
          </Button>
        </div>
      </div>
    </div>
  );
}
