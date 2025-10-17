'use client';

import { ArrowRight } from 'lucide-react';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import type { OscalFormat } from '@/types/oscal';

interface FormatSelectorProps {
  fromFormat: OscalFormat;
  toFormat: OscalFormat;
  onFromFormatChange: (format: OscalFormat) => void;
  onToFormatChange: (format: OscalFormat) => void;
  disabled?: boolean;
}

const FORMATS: { value: OscalFormat; label: string }[] = [
  { value: 'xml', label: 'XML' },
  { value: 'json', label: 'JSON' },
  { value: 'yaml', label: 'YAML' },
];

export function FormatSelector({
  fromFormat,
  toFormat,
  onFromFormatChange,
  onToFormatChange,
  disabled,
}: FormatSelectorProps) {
  return (
    <div className="space-y-2">
      <label className="text-sm font-medium">Conversion Direction</label>
      <div className="flex items-center gap-3">
        <Select
          value={fromFormat}
          onValueChange={onFromFormatChange}
          disabled={disabled}
        >
          <SelectTrigger className="flex-1">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {FORMATS.map((format) => (
              <SelectItem
                key={format.value}
                value={format.value}
                disabled={format.value === toFormat}
              >
                {format.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <ArrowRight className="h-5 w-5 text-muted-foreground flex-shrink-0" />

        <Select value={toFormat} onValueChange={onToFormatChange} disabled={disabled}>
          <SelectTrigger className="flex-1">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {FORMATS.map((format) => (
              <SelectItem
                key={format.value}
                value={format.value}
                disabled={format.value === fromFormat}
              >
                {format.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
    </div>
  );
}
