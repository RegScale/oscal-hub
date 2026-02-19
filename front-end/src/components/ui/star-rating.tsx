'use client';

import { useState } from 'react';
import { Star } from 'lucide-react';
import { cn } from '@/lib/utils';

interface StarRatingProps {
  rating: number;
  maxRating?: number;
  readonly?: boolean;
  onRatingChange?: (rating: number) => void;
  showCount?: boolean;
  totalRatings?: number;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const sizeClasses = {
  sm: 'h-3.5 w-3.5',
  md: 'h-5 w-5',
  lg: 'h-6 w-6',
};

const textSizeClasses = {
  sm: 'text-xs',
  md: 'text-sm',
  lg: 'text-base',
};

export function StarRating({
  rating,
  maxRating = 5,
  readonly = false,
  onRatingChange,
  showCount = false,
  totalRatings,
  size = 'md',
  className,
}: StarRatingProps) {
  const [hoverRating, setHoverRating] = useState<number | null>(null);

  const handleClick = (index: number) => {
    if (!readonly && onRatingChange) {
      onRatingChange(index + 1);
    }
  };

  const handleMouseEnter = (index: number) => {
    if (!readonly) {
      setHoverRating(index + 1);
    }
  };

  const handleMouseLeave = () => {
    if (!readonly) {
      setHoverRating(null);
    }
  };

  const displayRating = hoverRating !== null ? hoverRating : rating;

  const renderStar = (index: number) => {
    const filled = index < displayRating;
    const halfFilled = !filled && index < displayRating + 0.5 && displayRating % 1 !== 0;

    return (
      <button
        key={index}
        type="button"
        disabled={readonly}
        onClick={() => handleClick(index)}
        onMouseEnter={() => handleMouseEnter(index)}
        onMouseLeave={handleMouseLeave}
        className={cn(
          'transition-colors focus:outline-none',
          readonly ? 'cursor-default' : 'cursor-pointer hover:scale-110'
        )}
        aria-label={`Rate ${index + 1} out of ${maxRating} stars`}
      >
        <Star
          className={cn(
            sizeClasses[size],
            'transition-colors',
            filled
              ? 'fill-yellow-400 text-yellow-400'
              : halfFilled
              ? 'fill-yellow-400/50 text-yellow-400'
              : 'fill-none text-gray-300'
          )}
        />
      </button>
    );
  };

  const formattedRating = rating > 0 ? rating.toFixed(1) : '0.0';

  return (
    <div className={cn('flex items-center gap-1', className)}>
      <div className="flex items-center">
        {Array.from({ length: maxRating }, (_, index) => renderStar(index))}
      </div>
      {showCount && (
        <span className={cn('text-muted-foreground ml-1', textSizeClasses[size])}>
          {formattedRating}
          {totalRatings !== undefined && (
            <span className="ml-0.5">
              ({totalRatings} {totalRatings === 1 ? 'rating' : 'ratings'})
            </span>
          )}
        </span>
      )}
    </div>
  );
}
