'use client';

/** Padding between the target's bounding box and the spotlight cutout. */
const PADDING = 8;

interface TourSpotlightProps {
  /** Viewport-relative rect of the highlighted element; null dims everything. */
  targetRect: DOMRect | null;
}

/**
 * Full-viewport dimming layer with a rounded cutout over the tour target.
 * Purely decorative: hidden from AT, and the parent overlay blocks pointer
 * events so the page can't be interacted with mid-tour.
 */
export function TourSpotlight({ targetRect }: TourSpotlightProps) {
  const cutout = targetRect
    ? {
        x: targetRect.x - PADDING,
        y: targetRect.y - PADDING,
        width: targetRect.width + PADDING * 2,
        height: targetRect.height + PADDING * 2,
      }
    : null;

  return (
    <div className="absolute inset-0" aria-hidden="true">
      <svg className="h-full w-full">
        <defs>
          <mask id="tour-spotlight-mask">
            <rect width="100%" height="100%" fill="white" />
            {cutout && <rect {...cutout} rx={8} fill="black" />}
          </mask>
        </defs>
        <rect width="100%" height="100%" fill="black" fillOpacity={0.65} mask="url(#tour-spotlight-mask)" />
        {cutout && <rect {...cutout} rx={8} fill="none" stroke="var(--ring)" strokeWidth={2} />}
      </svg>
    </div>
  );
}
