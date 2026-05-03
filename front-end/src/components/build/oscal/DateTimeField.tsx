'use client';

import * as React from 'react';
import { parseISO, isValid } from 'date-fns';
import { Calendar as CalendarIcon, Clock } from 'lucide-react';

const pad2 = (n: number) => String(n).padStart(2, '0');

/** UTC-anchored "yyyy-MM-dd" — `date-fns` `format()` uses local time, which we don't want. */
function formatUtcDate(d: Date): string {
  return `${d.getUTCFullYear()}-${pad2(d.getUTCMonth() + 1)}-${pad2(d.getUTCDate())}`;
}

/** UTC-anchored "HH:mm". */
function formatUtcTime(d: Date): string {
  return `${pad2(d.getUTCHours())}:${pad2(d.getUTCMinutes())}`;
}
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Calendar } from '@/components/ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';

interface DateTimeFieldProps {
  /** Stringified ISO 8601 datetime (with timezone), e.g. `2026-05-02T12:00:00Z`. */
  value: string;
  onChange: (next: string) => void;
  label?: string;
  /** Allow clearing the value (returns ''). Defaults to true. */
  allowClear?: boolean;
  className?: string;
  id?: string;
}

/**
 * Combines a calendar date picker with an HH:MM time input. Always emits
 * UTC-anchored ISO 8601 strings ending in `Z` so the value is OSCAL-compliant
 * regardless of the user's local timezone. Parses any ISO 8601 input.
 */
export function DateTimeField({
  value,
  onChange,
  label,
  allowClear = true,
  className,
  id,
}: DateTimeFieldProps) {
  const parsed = React.useMemo(() => {
    if (!value) return undefined;
    const d = parseISO(value);
    return isValid(d) ? d : undefined;
  }, [value]);

  const [calendarOpen, setCalendarOpen] = React.useState(false);

  const datePart = parsed ? formatUtcDate(parsed) : '';
  const timePart = parsed ? formatUtcTime(parsed) : '';

  const emit = (date: Date | undefined) => {
    if (!date) {
      onChange('');
      return;
    }
    onChange(date.toISOString());
  };

  const handleDateSelect = (selected: Date | undefined) => {
    if (!selected) {
      emit(undefined);
      setCalendarOpen(false);
      return;
    }
    // The Calendar selection comes back as local-midnight; preserve the existing UTC time-of-day.
    const next = parsed ? new Date(parsed) : new Date(0);
    next.setUTCFullYear(selected.getFullYear(), selected.getMonth(), selected.getDate());
    if (!parsed) {
      next.setUTCHours(0, 0, 0, 0);
    }
    emit(next);
    setCalendarOpen(false);
  };

  const handleDateInputChange = (raw: string) => {
    if (!raw) {
      emit(undefined);
      return;
    }
    // Expect yyyy-MM-dd from <input type="date">
    const [y, m, d] = raw.split('-').map(Number);
    if (!y || !m || !d) return;
    const next = parsed ? new Date(parsed) : new Date(Date.UTC(y, m - 1, d, 0, 0, 0));
    next.setUTCFullYear(y, m - 1, d);
    emit(next);
  };

  const handleTimeChange = (raw: string) => {
    const [h, mi] = raw.split(':').map(Number);
    if (Number.isNaN(h) || Number.isNaN(mi)) return;
    const next = parsed ? new Date(parsed) : new Date(Date.UTC(1970, 0, 1));
    next.setUTCHours(h, mi, 0, 0);
    emit(next);
  };

  return (
    <div className={`space-y-1 ${className ?? ''}`}>
      {label && <Label className="text-xs">{label}</Label>}
      <div className="flex flex-wrap items-center gap-1">
        <div className="flex items-center gap-1">
          <Input
            id={id}
            type="date"
            value={datePart}
            onChange={(e) => handleDateInputChange(e.target.value)}
            className="w-40 h-9"
          />
          <Popover open={calendarOpen} onOpenChange={setCalendarOpen}>
            <PopoverTrigger asChild>
              <Button
                type="button"
                variant="outline"
                size="icon"
                className="h-9 w-9 shrink-0"
                aria-label="Open calendar"
              >
                <CalendarIcon className="h-4 w-4" />
              </Button>
            </PopoverTrigger>
            <PopoverContent className="w-auto p-0" align="start">
              <Calendar
                mode="single"
                selected={parsed}
                onSelect={handleDateSelect}
                defaultMonth={parsed}
                initialFocus
              />
            </PopoverContent>
          </Popover>
        </div>
        <div className="flex items-center gap-1">
          <Clock className="h-3.5 w-3.5 text-muted-foreground" aria-hidden />
          <Input
            type="time"
            value={timePart}
            onChange={(e) => handleTimeChange(e.target.value)}
            className="w-28 h-9 font-mono"
            aria-label="Time"
            step={60}
          />
          <span className="text-xs text-muted-foreground">UTC</span>
        </div>
        {allowClear && parsed && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => emit(undefined)}
            className="h-7 text-xs"
          >
            Clear
          </Button>
        )}
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => emit(new Date())}
          className="h-7 text-xs"
        >
          Now
        </Button>
      </div>
      {parsed && (
        <p className="text-[10px] font-mono text-muted-foreground">
          {value}
        </p>
      )}
    </div>
  );
}
