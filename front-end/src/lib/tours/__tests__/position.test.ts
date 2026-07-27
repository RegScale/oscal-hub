import { describe, expect, it } from 'vitest';
import { computePopoverPosition } from '@/lib/tours/position';

const VIEWPORT = { width: 1280, height: 800 };
const POPOVER = { width: 320, height: 180 };

describe('computePopoverPosition', () => {
  it('places below the target when there is room (default)', () => {
    const target = { x: 480, y: 100, width: 200, height: 40 };
    const pos = computePopoverPosition(target, POPOVER, VIEWPORT);
    expect(pos.placement).toBe('bottom');
    expect(pos.top).toBe(100 + 40 + 12); // target bottom + 12px gap
    expect(pos.left).toBe(480 + 100 - 160); // centered on target
  });

  it('flips above when the target is near the bottom edge', () => {
    const target = { x: 480, y: 740, width: 200, height: 40 };
    const pos = computePopoverPosition(target, POPOVER, VIEWPORT);
    expect(pos.placement).toBe('top');
    expect(pos.top).toBe(740 - 12 - 180);
  });

  it('honors a preferred placement that fits', () => {
    const target = { x: 600, y: 400, width: 100, height: 40 };
    const pos = computePopoverPosition(target, POPOVER, VIEWPORT, 'right');
    expect(pos.placement).toBe('right');
    expect(pos.left).toBe(600 + 100 + 12);
  });

  it('clamps fully inside the viewport when nothing fits (oversized target)', () => {
    const target = { x: 0, y: 0, width: 1280, height: 800 };
    const pos = computePopoverPosition(target, POPOVER, VIEWPORT);
    expect(pos.top).toBeGreaterThanOrEqual(16);
    expect(pos.left).toBeGreaterThanOrEqual(16);
    expect(pos.top + POPOVER.height).toBeLessThanOrEqual(VIEWPORT.height - 16);
    expect(pos.left + POPOVER.width).toBeLessThanOrEqual(VIEWPORT.width - 16);
  });
});
