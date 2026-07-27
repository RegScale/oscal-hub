import type { TourPlacement } from './types';

export interface Rect {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface Size {
  width: number;
  height: number;
}

export interface PopoverPosition {
  top: number;
  left: number;
  placement: TourPlacement;
}

/** Gap between the target's spotlight and the popover. */
const GAP = 12;
/** Minimum distance from any viewport edge. */
const MARGIN = 16;

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), Math.max(min, max));
}

function positionFor(placement: TourPlacement, target: Rect, popover: Size): { top: number; left: number } {
  switch (placement) {
    case 'top':
      return { top: target.y - GAP - popover.height, left: target.x + target.width / 2 - popover.width / 2 };
    case 'bottom':
      return { top: target.y + target.height + GAP, left: target.x + target.width / 2 - popover.width / 2 };
    case 'left':
      return { top: target.y + target.height / 2 - popover.height / 2, left: target.x - GAP - popover.width };
    case 'right':
      return { top: target.y + target.height / 2 - popover.height / 2, left: target.x + target.width + GAP };
  }
}

function fits(pos: { top: number; left: number }, popover: Size, viewport: Size): boolean {
  return (
    pos.top >= MARGIN &&
    pos.left >= MARGIN &&
    pos.top + popover.height <= viewport.height - MARGIN &&
    pos.left + popover.width <= viewport.width - MARGIN
  );
}

export function computePopoverPosition(
  target: Rect,
  popover: Size,
  viewport: Size,
  preferred?: TourPlacement,
): PopoverPosition {
  const order: TourPlacement[] = preferred
    ? [preferred, 'bottom', 'top', 'right', 'left']
    : ['bottom', 'top', 'right', 'left'];
  for (const placement of order) {
    const pos = positionFor(placement, target, popover);
    if (fits(pos, popover, viewport)) {
      return { ...pos, placement };
    }
  }
  // Oversized target or tiny viewport: fall back to the first choice, clamped
  // fully inside the viewport so the dialog is always reachable.
  const fallback = positionFor(order[0], target, popover);
  return {
    top: clamp(fallback.top, MARGIN, viewport.height - popover.height - MARGIN),
    left: clamp(fallback.left, MARGIN, viewport.width - popover.width - MARGIN),
    placement: order[0],
  };
}
